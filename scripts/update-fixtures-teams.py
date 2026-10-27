"""Refresh app/src/main/assets/fixtures-teams.json: Top 5 league clubs and their fixtur.es feeds.

Run by the maintainer once per season (5 page requests). The app itself only reads each chosen
club's iCal feed (https://ics.fixtur.es/v2/<slug>.ics), as agreed with fixtur.es.

    python scripts/update-fixtures-teams.py
"""
import html
import json
import os
import re
import sys
import urllib.request

LEAGUES = [  # (code, fixtur.es top-league page, second-division page)
    ("en.1", "premier-league", "efl-championship"),
    ("es.1", "primera-division", "segunda-division-a"),
    ("de.1", "bundesliga", "2-bundesliga"),
    ("it.1", "serie-a", "serie-b"),
    ("fr.1", "ligue-1", "ligue-2"),
]
UA = "LinarTV-maintenance (+https://github.com/domlin92/linartv-launcher-open)"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "fixtures-teams.json")


def get(url):
    request = urllib.request.Request(url, headers={"User-Agent": UA})
    return urllib.request.urlopen(request, timeout=30).read().decode("utf-8")


def clubs(page):
    text = get(f"https://fixtur.es/en/{page}")
    found = {}
    for slug, inner in re.findall(r'href="(?:https://fixtur\.es)?/en/team/([a-z0-9-]+)"[^>]*>(.*?)</a>', text, re.S):
        name = html.unescape(re.sub(r"<[^>]+>", " ", inner))
        name = re.sub(r"\s+", " ", name).strip()
        if name and slug not in found and name.lower() != "national team":
            found[slug] = name
    return found


result = {}
for code, page, lower in LEAGUES:
    teams = clubs(page)
    # A league page can still list last season's relegated clubs; they also appear on the
    # second-division page, and their untagged (league) matches are no longer this league.
    relegated = sorted(set(teams) & set(clubs(lower)))
    for slug in relegated:
        del teams[slug]
    if relegated:
        print(f"{page}: removed (now in {lower}): {', '.join(relegated)}")
    if len(teams) < 16:
        sys.exit(f"{page}: only {len(teams)} teams found - page layout changed?")
    result[code] = sorted(({"name": n, "slug": s} for s, n in teams.items()), key=lambda t: t["name"].lower())
    print(f"{page}: {len(teams)} teams")

with open(OUT, "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, indent=1)
print("written", os.path.normpath(OUT))

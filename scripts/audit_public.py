"""Fail on obvious private artifacts before a public release."""

from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BLOCKED_NAMES = {"local.properties", "light-keys.json", ".env", "keystore.properties"}
BLOCKED_PARTS = {".git", ".gradle", "build", "dist", "wallpapers", "recordings", "tmp"}
TEXT_SUFFIXES = {".java", ".gradle", ".xml", ".md", ".py", ".properties", ".txt", ".gitignore",
                 ".json", ".svg", ".ps1", ".html"}
PRIVATE = [
    ("private IP", re.compile(r"\b(?:10\.\d{1,3}|192\.168\.\d{1,3}|172\.(?:1[6-9]|2\d|3[01])\.\d{1,3})\.\d{1,3}\b")),
    ("embedded credential", re.compile(r"(?i)(?:password|api[_-]?key|device[_-]?key|secret)\s*[=:]\s*['\"][^'\"]+['\"]")),
    ("credential in URL", re.compile(r"(?i)[?&](?:token|api[_-]?key|secret)=([^&\s'\"]+)")),
    ("personal Windows path", re.compile(r"(?i)[A-Z]:[/\\]Users[/\\][^/\\\s]+")),
    ("local project path", re.compile(r"(?i)[A-Z]:[/\\]Linart TV(?:[/\\]|\b)")),
    ("private room name", re.compile(r"(?i)\bSary\b")),
]

issues = []
checked = 0
tracked = subprocess.run(["git", "ls-files", "-z"], cwd=ROOT, capture_output=True, check=True).stdout
for name in tracked.split(b"\0"):
    if not name:
        continue
    relative = Path(name.decode("utf-8", errors="replace"))
    if (any(part in BLOCKED_PARTS for part in relative.parts)
            or relative.name in BLOCKED_NAMES
            or relative.suffix.lower() in {".apk", ".aab", ".jks", ".keystore", ".pem", ".p12"}):
        issues.append(f"Forbidden tracked file: {relative}")
# Git-ignored files (keystore.properties, local.properties) never reach the repository or a
# release, so they may exist locally; release signing needs keystore.properties on disk.
ignored_output = subprocess.run(["git", "ls-files", "--others", "--ignored", "--exclude-standard", "-z"],
                                cwd=ROOT, capture_output=True, check=True).stdout
ignored = {Path(name.decode("utf-8", errors="replace")) for name in ignored_output.split(b"\0") if name}
for path in ROOT.rglob("*"):
    if not path.is_file() or ".git" in path.relative_to(ROOT).parts:
        continue
    relative = path.relative_to(ROOT)
    if any(part in BLOCKED_PARTS for part in relative.parts) or relative in ignored:
        continue
    if path.name in BLOCKED_NAMES or path.suffix.lower() in {".apk", ".aab", ".jks", ".keystore", ".pem", ".p12"}:
        issues.append(f"Forbidden file: {relative}")
        continue
    if path.suffix.lower() not in TEXT_SUFFIXES and path.name != ".gitignore":
        continue
    checked += 1
    content = path.read_text(encoding="utf-8", errors="replace")
    for label, pattern in PRIVATE:
        if pattern.search(content):
            issues.append(f"{relative}: {label}")

if issues:
    print("Public audit failed:")
    for issue in issues:
        print("- " + issue)
    raise SystemExit(1)
print(f"Public audit passed ({checked} text files checked).")

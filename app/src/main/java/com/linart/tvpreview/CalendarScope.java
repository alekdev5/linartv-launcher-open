package com.linart.tvpreview;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Calendar sources supported by the public version. */
final class CalendarScope {
    static final List<String> LEAGUES = Collections.unmodifiableList(Arrays.asList(
            "ekstraklasa", "1-liga", "2-liga", "3-liga-grupa-1", "3-liga-grupa-2",
            "3-liga-grupa-3", "3-liga-grupa-4"));
    /** IV liga groups are per voivodeship, e.g. "4-liga-malopolska". */
    static final String FOURTH_LEAGUE_PREFIX = "4-liga-";

    static boolean isLeague(String slug) {
        return LEAGUES.contains(slug)
                || slug.startsWith(FOURTH_LEAGUE_PREFIX) && slug.matches("4-liga-[a-z]+(-[a-z]+)?");
    }

    /** Foreign clubs: "openfootball:<league file>/<club name>", e.g. "openfootball:en.1/Arsenal FC". */
    static final String FOREIGN_PREFIX = "openfootball:";
    static final List<String> FOREIGN_LEAGUES = Collections.unmodifiableList(Arrays.asList(
            "en.1", "es.1", "de.1", "it.1", "fr.1"));

    static String foreignUrl(String league, String team) { return FOREIGN_PREFIX + league + "/" + team; }
    static boolean isForeign(String address) { return address != null && address.startsWith(FOREIGN_PREFIX); }
    static String foreignLeague(String address) {
        String rest = address.substring(FOREIGN_PREFIX.length());
        return rest.substring(0, rest.indexOf('/'));
    }
    static String foreignTeam(String address) {
        String rest = address.substring(FOREIGN_PREFIX.length());
        return rest.substring(rest.indexOf('/') + 1);
    }

    static boolean accepts(String address) {
        if (isForeign(address)) {
            String rest = address.substring(FOREIGN_PREFIX.length());
            int slash = rest.indexOf('/');
            if (slash < 1) return false;
            String team = rest.substring(slash + 1);
            return FOREIGN_LEAGUES.contains(rest.substring(0, slash)) && !team.trim().isEmpty()
                    && team.length() <= 80 && !team.matches(".*[\\p{Cntrl}/].*");
        }
        try {
            URI uri = new URI(address);
            if (!"https".equals(uri.getScheme()) || !"kalendarzeligowe.pl".equals(uri.getHost())
                    || uri.getUserInfo() != null || uri.getPort() != -1
                    || uri.getQuery() != null || uri.getFragment() != null) return false;
            String[] path = uri.getPath().split("/", -1);
            return path.length == 4 && isLeague(path[1])
                    && path[2].matches("[a-z0-9-]+-[0-9]+") && "calendar.ics".equals(path[3]);
        } catch (Exception invalid) { return false; }
    }
}

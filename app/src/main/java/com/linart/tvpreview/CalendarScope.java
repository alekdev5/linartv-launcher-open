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

    static boolean accepts(String address) {
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

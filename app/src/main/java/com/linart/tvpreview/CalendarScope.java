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

    static boolean accepts(String address) {
        try {
            URI uri = new URI(address);
            if (!"https".equals(uri.getScheme()) || !"kalendarzeligowe.pl".equals(uri.getHost())
                    || uri.getUserInfo() != null || uri.getPort() != -1
                    || uri.getQuery() != null || uri.getFragment() != null) return false;
            String[] path = uri.getPath().split("/", -1);
            return path.length == 4 && LEAGUES.contains(path[1])
                    && path[2].matches("[a-z0-9-]+-[0-9]+") && "calendar.ics".equals(path[3]);
        } catch (Exception invalid) { return false; }
    }
}

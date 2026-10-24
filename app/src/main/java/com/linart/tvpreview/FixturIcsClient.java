package com.linart.tvpreview;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/** Reads the selected fixtur.es calendar and supplements its public competition metadata. */
final class FixturIcsClient {
    static List<IcsMatchParser.Match> teamMatches(String url, long fromMillis) throws Exception {
        URL target = new URL(url);
        if (!"https".equalsIgnoreCase(target.getProtocol())
                || !"ics.fixtur.es".equalsIgnoreCase(target.getHost())
                || target.getPort() != -1
                || !target.getPath().matches("/v2/[a-zA-Z0-9_-]+\\.ics")
                || target.getQuery() != null || target.getRef() != null)
            throw new IllegalArgumentException("Unsupported fixtur.es calendar URL");
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        try {
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException("Calendar HTTP " + connection.getResponseCode());
            try (InputStream stream = connection.getInputStream();
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                    if (bytes.size() > 800_000) throw new IllegalStateException("Calendar too large");
                }
                String ics = bytes.toString("UTF-8");
                if (!ics.startsWith("BEGIN:VCALENDAR") || !ics.contains("END:VCALENDAR"))
                    throw new IllegalStateException("Invalid calendar");
                String slug = target.getPath().substring("/v2/".length()).replaceFirst("\\.ics$", "");
                return FixturMatchMetadata.enrich(IcsMatchParser.parse(ics, null, fromMillis), slug);
            }
        } finally {
            connection.disconnect();
        }
    }
}

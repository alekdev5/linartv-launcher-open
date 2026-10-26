package com.linart.tvpreview;

public final class CalendarScopeCheck {
    public static void main(String[] args) {
        int checked = 0;
        if (CalendarScope.LEAGUES.size() != 7) throw new AssertionError("Expected seven leagues");
        for (String league : CalendarScope.LEAGUES) {
            if (!CalendarScope.accepts("https://kalendarzeligowe.pl/" + league + "/test-team-123/calendar.ics"))
                throw new AssertionError("Missing league: " + league);
            checked++;
        }
        String[] fourth = {"4-liga-swietokrzyska", "4-liga-kujawsko-pomorska", "4-liga-malopolska"};
        for (String league : fourth) {
            if (!CalendarScope.accepts("https://kalendarzeligowe.pl/" + league + "/test-team-123/calendar.ics"))
                throw new AssertionError("Missing IV liga group: " + league);
            checked++;
        }
        String[] rejected = {
                "https://ics.fixtur.es/v2/arsenal.ics",
                "https://kalendarzeligowe.pl/4-liga/team-1/calendar.ics",
                "https://kalendarzeligowe.pl/4-liga-/team-1/calendar.ics",
                "https://kalendarzeligowe.pl/4-liga-a-b-c/team-1/calendar.ics",
                "https://kalendarzeligowe.pl/klasa-okregowa-swietokrzyska/team-1/calendar.ics",
                "https://kalendarzeligowe.pl/reprezentacja/team-1/calendar.ics",
                "https://kalendarzeligowe.pl.evil.example/ekstraklasa/team-1/calendar.ics",
                "https://kalendarzeligowe.pl@evil.example/ekstraklasa/team-1/calendar.ics",
                "https://evil.example/ekstraklasa/team-1/calendar.ics",
                "http://kalendarzeligowe.pl/ekstraklasa/team-1/calendar.ics",
                "https://kalendarzeligowe.pl/ekstraklasa/team-1/calendar.ics?redirect=other",
                "https://kalendarzeligowe.pl/ekstraklasa/team-1/calendar.ics#other",
                "https://kalendarzeligowe.pl:8443/ekstraklasa/team-1/calendar.ics",
                "https://kalendarzeligowe.pl/ekstraklasa/../calendar.ics",
                "", "not a URL"
        };
        for (String address : rejected) {
            if (CalendarScope.accepts(address)) throw new AssertionError("Unexpected calendar: " + address);
            checked++;
        }
        System.out.println("OK: " + checked + " calendar scope checks");
    }
}

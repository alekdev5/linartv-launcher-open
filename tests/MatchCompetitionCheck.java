package com.linart.tvpreview;

public final class MatchCompetitionCheck {
    public static void main(String[] args) {
        check("I liga", "FIRST_LEAGUE", "Kolejka 3 - sierpień", 3, "I liga", "", 3);
        check("Ekstraklasa", "EKSTRAKLASA", "Kolejka 12", 12, "Ekstraklasa", "", 12);
        check("III liga — grupa IV", "THIRD_LEAGUE", "Kolejka 15", 15, "III liga — grupa IV", "", 15);
        // Arka Gdynia–Stal Rzeszów, 29 October 2026: API roundNumber=3 is a cup round.
        check("Puchar Polski", "POLISH_CUP", "1/16 finału", 3, "Puchar Polski", "1/16 finału", -1);
        check("Puchar Polski", "POLISH_CUP", "", 3, "Puchar Polski", "", -1);
        check("Puchar Polski", "POLISH_CUP", "Kolejka 3", 3, "Puchar Polski", "", -1);
        check("Puchar Polski", "", "Finał", 8, "Puchar Polski", "Finał", -1);
        check("", "", "", 3, "", "", -1);
        check(null, null, null, 3, "", "", -1);
        check("I liga", "FIRST_LEAGUE", "", 0, "I liga", "", -1);
        System.out.println("OK: 10 match competition regression cases");
    }

    private static void check(String name, String category, String roundName, int number,
                              String expectedName, String expectedStage, int expectedRound) {
        MatchCompetition actual = MatchCompetition.from(name, category, roundName, number);
        if (!expectedName.equals(actual.name) || !expectedStage.equals(actual.stage)
                || expectedRound != actual.round)
            throw new AssertionError(name + ": " + actual.name + " / " + actual.stage + " / " + actual.round);
    }
}

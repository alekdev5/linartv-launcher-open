package com.linart.tvpreview;

/** Uses the match's own competition; cup round numbers are not league rounds. */
final class MatchCompetition {
    final String name, stage;
    final int round;

    private MatchCompetition(String name, String stage, int round) {
        this.name = name; this.stage = stage; this.round = round;
    }

    static MatchCompetition from(String name, String category, String roundName, int roundNumber) {
        name = name == null ? "" : name.trim();
        category = category == null ? "" : category.trim();
        roundName = roundName == null ? "" : roundName.trim();
        boolean cup = category.contains("CUP")
                || name.toLowerCase(java.util.Locale.ROOT).contains("puchar");
        boolean league = !cup && ("EKSTRAKLASA".equals(category) || category.endsWith("_LEAGUE"));
        if (league) return new MatchCompetition(name, "", roundNumber > 0 ? roundNumber : -1);
        // Prefer the provider's human-readable label to technical enum values.
        // For example, ROUND_OF_16 is paired with "1/16 finału" in the source.
        boolean usableStage = !roundName.isEmpty()
                && !roundName.toLowerCase(java.util.Locale.ROOT).startsWith("kolejka");
        return new MatchCompetition(name, usableStage ? roundName : "", -1);
    }
}

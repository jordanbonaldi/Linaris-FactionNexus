package net.neferett.linaris.thenexus;

public enum Quest {
    ;

    public static class QuestLevel {
        private int count;
        private int funcoins;
    }

    private String name;
    private QuestLevel[] levels;

    private Quest(String name, QuestLevel[] levels) {
        this.name = name;
        this.levels = levels;
    }
}

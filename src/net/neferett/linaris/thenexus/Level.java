package net.neferett.linaris.thenexus;

public enum Level {
    FIRST(1, true, 4, 500, 7, null, null),
    SECOND(2, false, 6, 500, 8, new Objective(ObjectiveType.BREAK_CHESTS, 15), new Objective(ObjectiveType.BREAK_NEXUS, 2)),
    THIRD(3, false, 8, 500, 9, new Objective(ObjectiveType.BREAK_CHESTS, 30), new Objective(ObjectiveType.BREAK_NEXUS, 4)),
    FOURTH(4, false, 12, 650, 9, new Objective(ObjectiveType.BREAK_CHESTS, 60), new Objective(ObjectiveType.BREAK_NEXUS, 8)),
    FIFTH(5, false, 16, 800, 10, new Objective(ObjectiveType.BREAK_CHESTS, 120), new Objective(ObjectiveType.BREAK_NEXUS, 16)),
    SIXTH(6, false, 22, 1000, 12, new Objective(ObjectiveType.BREAK_CHESTS, 240), new Objective(ObjectiveType.BREAK_NEXUS, 32));

    private int id;
    private boolean defaultLev;
    private int explosions;
    private int maxHealth;
    private int maxMembers;
    private Objective brokenChests;
    private Objective brokenNexus;

    private Level(final int id, final boolean defaultLev, final int explosions, final int maxHealth, final int maxMembers, final Objective brokenChests, final Objective brokenNexus) {
        this.id = id;
        this.defaultLev = defaultLev;
        this.explosions = explosions;
        this.maxHealth = maxHealth;
        this.maxMembers = maxMembers;
        this.brokenChests = brokenChests;
        this.brokenNexus = brokenNexus;
    }

    public int getId() {
        return id;
    }

    public boolean isDefault() {
        return defaultLev;
    }

    public int getExplosions() {
        return explosions;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public Objective getBrokenChests() {
        return brokenChests;
    }

    public Objective getBrokenNexus() {
        return brokenNexus;
    }

    public static class Objective {
        private final ObjectiveType type;
        private final int count;

        public Objective(final ObjectiveType type, final int count) {
            this.type = type;
            this.count = count;
        }

        public ObjectiveType getType() {
            return type;
        }

        public int getCount() {
            return count;
        }
    }

    public static enum ObjectiveType {
        BREAK_CHESTS, BREAK_NEXUS;
    }
}

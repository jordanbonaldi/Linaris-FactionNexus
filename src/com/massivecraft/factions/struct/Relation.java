package com.massivecraft.factions.struct;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Conf;

public enum Relation {
    MEMBER(3, "member"),
    ALLY(2, "ally"),
    NEUTRAL(1, "neutral"),
    ENEMY(0, "enemy");

    public final int value;
    public final String nicename;

    private Relation(final int value, final String nicename) {
        this.value = value;
        this.nicename = nicename;
    }

    @Override
    public String toString() {
        return nicename;
    }

    public boolean isMember() {
        return this == MEMBER;
    }

    public boolean isAlly() {
        return this == ALLY;
    }

    public boolean isNeutral() {
        return this == NEUTRAL;
    }

    public boolean isEnemy() {
        return this == ENEMY;
    }

    public boolean isAtLeast(final Relation relation) {
        return value >= relation.value;
    }

    public boolean isAtMost(final Relation relation) {
        return value <= relation.value;
    }

    public ChatColor getColor() {
        if (this == MEMBER) {
            return Conf.colorMember;
        } else if (this == ALLY) {
            return Conf.colorAlly;
        } else if (this == NEUTRAL) {
            return Conf.colorNeutral;
        } else {
            return Conf.colorEnemy;
        }
    }

    // return appropriate Conf setting for DenyBuild based on this relation and their online status
    public boolean confDenyBuild(final boolean online) {
        if (this.isMember()) { return false; }

        if (online) {
            if (this.isEnemy()) {
                return Conf.territoryEnemyDenyBuild;
            } else if (this.isAlly()) {
                return Conf.territoryAllyDenyBuild;
            } else {
                return Conf.territoryDenyBuild;
            }
        } else {
            if (this.isEnemy()) {
                return Conf.territoryEnemyDenyBuildWhenOffline;
            } else if (this.isAlly()) {
                return Conf.territoryAllyDenyBuildWhenOffline;
            } else {
                return Conf.territoryDenyBuildWhenOffline;
            }
        }
    }

    // return appropriate Conf setting for PainBuild based on this relation and their online status
    public boolean confPainBuild(final boolean online) {
        if (this.isMember()) { return false; }

        if (online) {
            if (this.isEnemy()) {
                return Conf.territoryEnemyPainBuild;
            } else if (this.isAlly()) {
                return Conf.territoryAllyPainBuild;
            } else {
                return Conf.territoryPainBuild;
            }
        } else {
            if (this.isEnemy()) {
                return Conf.territoryEnemyPainBuildWhenOffline;
            } else if (this.isAlly()) {
                return Conf.territoryAllyPainBuildWhenOffline;
            } else {
                return Conf.territoryPainBuildWhenOffline;
            }
        }
    }

    // return appropriate Conf setting for DenyUseage based on this relation
    public boolean confDenyUseage() {
        if (this.isMember()) {
            return false;
        } else if (this.isEnemy()) {
            return Conf.territoryEnemyDenyUseage;
        } else if (this.isAlly()) {
            return Conf.territoryAllyDenyUseage;
        } else {
            return Conf.territoryDenyUseage;
        }
    }

    public double getRelationCost() {
        if (this.isEnemy()) {
            return Conf.econCostEnemy;
        } else if (this.isAlly()) {
            return Conf.econCostAlly;
        } else {
            return Conf.econCostNeutral;
        }
    }
}

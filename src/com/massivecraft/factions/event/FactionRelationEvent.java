package com.massivecraft.factions.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Relation;

public class FactionRelationEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Faction fsender;
    private final Faction ftarget;
    private final Relation foldrel;
    private final Relation frel;

    public FactionRelationEvent(final Faction sender, final Faction target, final Relation oldrel, final Relation rel) {
        fsender = sender;
        ftarget = target;
        foldrel = oldrel;
        frel = rel;
    }

    @Override
    public HandlerList getHandlers() {
        return FactionRelationEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return FactionRelationEvent.handlers;
    }

    public Relation getOldRelation() {
        return foldrel;
    }

    public Relation getRelation() {
        return frel;
    }

    public Faction getFaction() {
        return fsender;
    }

    public Faction getTargetFaction() {
        return ftarget;
    }
}

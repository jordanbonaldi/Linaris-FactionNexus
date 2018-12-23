package com.massivecraft.factions.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;

public class FactionRenameEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;
    private final FPlayer fplayer;
    private final Faction faction;
    private final String tag;

    public FactionRenameEvent(final FPlayer sender, final String newTag) {
        fplayer = sender;
        faction = sender.getFaction();
        tag = newTag;
        cancelled = false;
    }

    public Faction getFaction() {
        return faction;
    }

    public FPlayer getFPlayer() {
        return fplayer;
    }

    public Player getPlayer() {
        return fplayer.getPlayer();
    }

    public String getOldFactionTag() {
        return faction.getTag();
    }

    public String getFactionTag() {
        return tag;
    }

    @Override
    public HandlerList getHandlers() {
        return FactionRenameEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return FactionRenameEvent.handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean c) {
        cancelled = c;
    }
}

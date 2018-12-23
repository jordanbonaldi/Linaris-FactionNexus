package com.massivecraft.factions.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;

public class FactionDisbandEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;
    private final String id;
    private final Player sender;

    public FactionDisbandEvent(final Player sender, final String factionId) {
        cancelled = false;
        this.sender = sender;
        id = factionId;
    }

    @Override
    public HandlerList getHandlers() {
        return FactionDisbandEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return FactionDisbandEvent.handlers;
    }

    public Faction getFaction() {
        return Factions.i.get(id);
    }

    public FPlayer getFPlayer() {
        return FPlayers.i.get(sender);
    }

    public Player getPlayer() {
        return sender;
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

package com.massivecraft.factions.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;

public class FPlayerLeaveEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final PlayerLeaveReason reason;
    FPlayer FPlayer;
    Faction Faction;
    boolean cancelled = false;

    public enum PlayerLeaveReason {
        KICKED, DISBAND, RESET, JOINOTHER, LEAVE
    }

    public FPlayerLeaveEvent(final FPlayer p, final Faction f, final PlayerLeaveReason r) {
        FPlayer = p;
        Faction = f;
        reason = r;
    }

    @Override
    public HandlerList getHandlers() {
        return FPlayerLeaveEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return FPlayerLeaveEvent.handlers;
    }

    public PlayerLeaveReason getReason() {
        return reason;
    }

    public FPlayer getFPlayer() {
        return FPlayer;
    }

    public Faction getFaction() {
        return Faction;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean c) {
        if (reason == PlayerLeaveReason.DISBAND || reason == PlayerLeaveReason.RESET) {
            cancelled = false;
            return;
        }
        cancelled = c;
    }
}
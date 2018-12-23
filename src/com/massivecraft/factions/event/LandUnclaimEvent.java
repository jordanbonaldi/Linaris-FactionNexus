package com.massivecraft.factions.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;

public class LandUnclaimEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;
    private final FLocation location;
    private final Faction faction;
    private final FPlayer fplayer;

    public LandUnclaimEvent(final FLocation loc, final Faction f, final FPlayer p) {
        cancelled = false;
        location = loc;
        faction = f;
        fplayer = p;
    }

    @Override
    public HandlerList getHandlers() {
        return LandUnclaimEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return LandUnclaimEvent.handlers;
    }

    public FLocation getLocation() {
        return location;
    }

    public Faction getFaction() {
        return faction;
    }

    public String getFactionId() {
        return faction.getId();
    }

    public String getFactionTag() {
        return faction.getTag();
    }

    public FPlayer getFPlayer() {
        return fplayer;
    }

    public Player getPlayer() {
        return fplayer.getPlayer();
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

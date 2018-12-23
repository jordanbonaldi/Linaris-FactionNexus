package com.massivecraft.factions.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;

public class FactionsWorldListener implements Listener {
    public P p;

    public FactionsWorldListener(final P p) {
        this.p = p;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChunkLoad(final ChunkLoadEvent event) {
        Faction factionHere = Board.getFactionAt(new FLocation(event.getChunk().getBlock(0, 0, 0).getLocation()));
        if (factionHere != null && factionHere.isNormal()) {
            factionHere.spawnNexus(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChunkUnload(final ChunkUnloadEvent event) {
        Faction factionHere = Board.getFactionAt(new FLocation(event.getChunk().getBlock(0, 0, 0).getLocation()));
        if (factionHere != null && factionHere.isNormal()) {
            factionHere.removeNexus();
        }
    }
}
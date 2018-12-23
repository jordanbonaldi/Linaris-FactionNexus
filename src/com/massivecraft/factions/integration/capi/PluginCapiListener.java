package com.massivecraft.factions.integration.capi;

import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.massivecraft.capi.Channel;
import com.massivecraft.capi.Channels;
import com.massivecraft.capi.events.CAPIListChannelsEvent;
import com.massivecraft.capi.events.CAPIMessageToChannelEvent;
import com.massivecraft.capi.events.CAPIMessageToPlayerEvent;
import com.massivecraft.capi.events.CAPISelectChannelEvent;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Relation;

public class PluginCapiListener implements Listener {
    P p;

    Set<String> myChannelIds = new LinkedHashSet<String>();

    public PluginCapiListener(final P p) {
        this.p = p;

        myChannelIds.add("faction");
        myChannelIds.add("allies");
    }

    private String replacePlayerTags(String format, final FPlayer me, final FPlayer you) {
        final String meFactionTag = me.getChatTag(you);
        format = format.replace("{ME_FACTIONTAG}", meFactionTag.length() == 0 ? "" : meFactionTag);
        format = format.replace("{ME_FACTIONTAG_PADR}", meFactionTag.length() == 0 ? "" : meFactionTag + " ");
        format = format.replace("{ME_FACTIONTAG_PADL}", meFactionTag.length() == 0 ? "" : " " + meFactionTag);
        format = format.replace("{ME_FACTIONTAG_PADB}", meFactionTag.length() == 0 ? "" : " " + meFactionTag + " ");

        final String youFactionTag = you.getChatTag(me);
        format = format.replace("{YOU_FACTIONTAG}", youFactionTag.length() == 0 ? "" : youFactionTag);
        format = format.replace("{YOU_FACTIONTAG_PADR}", youFactionTag.length() == 0 ? "" : youFactionTag + " ");
        format = format.replace("{YOU_FACTIONTAG_PADL}", youFactionTag.length() == 0 ? "" : " " + youFactionTag);
        format = format.replace("{YOU_FACTIONTAG_PADB}", youFactionTag.length() == 0 ? "" : " " + youFactionTag + " ");

        return format;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onListChannelsEvent(final CAPIListChannelsEvent event) {
        for (final Channel c : Channels.i.getAll()) {
            if (myChannelIds.contains(c.getId())) {
                event.getChannels().add(c);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMessageToChannel(final CAPIMessageToChannelEvent event) {
        if (event.isCancelled()) { return; }
        if (!myChannelIds.contains(event.getChannel().getId())) { return; }

        final Player me = event.getMe();
        final FPlayer fme = FPlayers.i.get(me);
        final Faction myFaction = fme.getFaction();

        if (event.getChannel().getId().equals("faction") && myFaction.isNormal()) {
            event.getThem().addAll(myFaction.getOnlinePlayers());

            // Send to any players who are spying chat... could probably be implemented better than this
            for (final FPlayer fplayer : FPlayers.i.getOnline()) {
                if (fplayer.isSpyingChat() && fplayer.getFaction() != myFaction) {
                    fplayer.sendMessage("[FCspy] " + myFaction.getTag() + ": " + event.getMessage());
                }
            }
        } else if (event.getChannel().getId().equals("allies")) {
            for (final Player somePlayer : Bukkit.getServer().getOnlinePlayers()) {
                final FPlayer someFPlayer = FPlayers.i.get(somePlayer);
                if (someFPlayer.getRelationTo(fme).isAtLeast(Relation.ALLY)) {
                    event.getThem().add(somePlayer);
                } else if (someFPlayer.isSpyingChat()) {
                    someFPlayer.sendMessage("[ACspy]: " + event.getMessage());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMessageToPlayer(final CAPIMessageToPlayerEvent event) {
        if (event.isCancelled()) { return; }
        event.setFormat(this.replacePlayerTags(event.getFormat(), FPlayers.i.get(event.getMe()), FPlayers.i.get(event.getYou())));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSelectChannel(final CAPISelectChannelEvent event) {
        if (event.isCancelled()) { return; }
        final String channelId = event.getChannel().getId();
        if (!myChannelIds.contains(channelId)) { return; }

        final Player me = event.getMe();
        final FPlayer fme = FPlayers.i.get(me);

        if (!fme.hasFaction()) {
            event.setFailMessage(p.txt.parse("<b>You must be member in a faction to use this channel."));
            event.setCancelled(true);
        }
    }
}

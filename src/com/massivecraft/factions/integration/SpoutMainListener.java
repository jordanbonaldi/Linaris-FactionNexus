package com.massivecraft.factions.integration;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.event.spout.SpoutCraftEnableEvent;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;

public class SpoutMainListener implements Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSpoutCraftEnable(final SpoutCraftEnableEvent event) {
        final FPlayer me = FPlayers.i.get(event.getPlayer());

        SpoutFeatures.updateAppearances(me.getPlayer());
        this.updateTerritoryDisplay(me, true);
    }

    //-----------------------------------------------------------------------------------------//
    // Everything below this is handled in here to prevent errors on servers not running Spout
    //-----------------------------------------------------------------------------------------//

    private transient static Map<String, GenericLabel> territoryLabels = new HashMap<String, GenericLabel>();
    private transient static Map<String, NoticeLabel> territoryChangeLabels = new HashMap<String, NoticeLabel>();
    private transient static Map<String, GenericLabel> ownerLabels = new HashMap<String, GenericLabel>();
    private final static int SCREEN_WIDTH = 427;

    //	private final static int SCREEN_HEIGHT = 240;

    public boolean updateTerritoryDisplay(final FPlayer player, final boolean notify) {
        final Player p = player.getPlayer();
        if (p == null) { return false; }

        final SpoutPlayer sPlayer = SpoutManager.getPlayer(p);
        if (!sPlayer.isSpoutCraftEnabled() || Conf.spoutTerritoryDisplaySize <= 0 && !Conf.spoutTerritoryNoticeShow) { return false; }

        this.doLabels(player, sPlayer, notify);

        return true;
    }

    public void updateOwnerList(final FPlayer player) {
        final SpoutPlayer sPlayer = SpoutManager.getPlayer(player.getPlayer());
        if (!sPlayer.isSpoutCraftEnabled() || Conf.spoutTerritoryDisplaySize <= 0 && !Conf.spoutTerritoryNoticeShow) { return; }

        final FLocation here = player.getLastStoodAt();
        final Faction factionHere = Board.getFactionAt(here);

        this.doOwnerList(player, sPlayer, here, factionHere);

        return;
    }

    public void removeTerritoryLabels(final String playerName) {
        SpoutMainListener.territoryLabels.remove(playerName);
        SpoutMainListener.territoryChangeLabels.remove(playerName);
        SpoutMainListener.ownerLabels.remove(playerName);
    }

    private void doLabels(final FPlayer player, final SpoutPlayer sPlayer, final boolean notify) {
        final FLocation here = player.getLastStoodAt();
        final Faction factionHere = Board.getFactionAt(here);
        final String tag = factionHere.getColorTo(player).toString() + factionHere.getTag();

        // ----------------------
        // Main territory display
        // ----------------------
        if (Conf.spoutTerritoryDisplayPosition > 0 && Conf.spoutTerritoryDisplaySize > 0) {
            GenericLabel label;
            if (SpoutMainListener.territoryLabels.containsKey(player.getName())) {
                label = SpoutMainListener.territoryLabels.get(player.getName());
            } else {
                label = new GenericLabel();
                label.setWidth(1).setHeight(1); // prevent Spout's questionable new "no default size" warning
                label.setScale(Conf.spoutTerritoryDisplaySize);

                sPlayer.getMainScreen().attachWidget(P.p, label);
                SpoutMainListener.territoryLabels.put(player.getName(), label);
            }

            String msg = tag;

            if (Conf.spoutTerritoryDisplayShowDescription && !factionHere.getDescription().isEmpty()) {
                msg += " - " + factionHere.getDescription();
            }

            label.setText(msg);
            this.alignLabel(label, msg);
            label.setDirty(true);
        }

        // -----------------------
        // Fading territory notice
        // -----------------------
        if (notify && Conf.spoutTerritoryNoticeShow && Conf.spoutTerritoryNoticeSize > 0) {
            NoticeLabel label;
            if (SpoutMainListener.territoryChangeLabels.containsKey(player.getName())) {
                label = SpoutMainListener.territoryChangeLabels.get(player.getName());
            } else {
                label = new NoticeLabel(Conf.spoutTerritoryNoticeLeaveAfterSeconds);
                label.setWidth(1).setHeight(1); // prevent Spout's questionable new "no default size" warning
                label.setScale(Conf.spoutTerritoryNoticeSize);
                label.setY(Conf.spoutTerritoryNoticeTop);
                sPlayer.getMainScreen().attachWidget(P.p, label);
                SpoutMainListener.territoryChangeLabels.put(player.getName(), label);
            }

            String msg = tag;

            if (Conf.spoutTerritoryNoticeShowDescription && !factionHere.getDescription().isEmpty()) {
                msg += " - " + factionHere.getDescription();
            }

            label.setText(msg);
            this.alignLabel(label, msg, 2);
            label.resetNotice();
            label.setDirty(true);
        }

        // and owner list, of course
        this.doOwnerList(player, sPlayer, here, factionHere);
    }

    private void doOwnerList(final FPlayer player, final SpoutPlayer sPlayer, final FLocation here, final Faction factionHere) {
        // ----------
        // Owner list
        // ----------
        if (Conf.spoutTerritoryDisplayPosition > 0 && Conf.spoutTerritoryDisplaySize > 0 && Conf.spoutTerritoryOwnersShow && Conf.ownedAreasEnabled) {
            GenericLabel label;
            if (SpoutMainListener.ownerLabels.containsKey(player.getName())) {
                label = SpoutMainListener.ownerLabels.get(player.getName());
            } else {
                label = new GenericLabel();
                label.setWidth(1).setHeight(1); // prevent Spout's questionable new "no default size" warning
                label.setScale(Conf.spoutTerritoryDisplaySize);
                label.setY((int) (10 * Conf.spoutTerritoryDisplaySize));
                sPlayer.getMainScreen().attachWidget(P.p, label);
                SpoutMainListener.ownerLabels.put(player.getName(), label);
            }

            String msg = "";

            if (player.getFaction() == factionHere) {
                msg = factionHere.getOwnerListString(here);

                if (!msg.isEmpty()) {
                    msg = Conf.ownedLandMessage + msg;
                }
            }

            label.setText(msg);
            this.alignLabel(label, msg);
            label.setDirty(true);
        }
    }

    // this is only necessary because Spout text size scaling is currently bugged and breaks their built-in alignment methods
    public void alignLabel(final GenericLabel label, final String text) {
        this.alignLabel(label, text, Conf.spoutTerritoryDisplayPosition);
    }

    public void alignLabel(final GenericLabel label, final String text, final int alignment) {
        final int labelWidth = (int) (GenericLabel.getStringWidth(text) * Conf.spoutTerritoryDisplaySize);
        if (labelWidth > SpoutMainListener.SCREEN_WIDTH) {
            label.setX(0);
            return;
        }

        switch (alignment) {
        case 1: // left aligned
            label.setX(0);
            break;
        case 2: // center aligned
            label.setX((SpoutMainListener.SCREEN_WIDTH - labelWidth) / 2);
            break;
        default: // right aligned
            label.setX(SpoutMainListener.SCREEN_WIDTH - labelWidth);
        }
    }

    private static class NoticeLabel extends GenericLabel {
        private final int initial;
        private int countdown; // current delay countdown

        public NoticeLabel(final float secondsOfLife) {
            initial = (int) (secondsOfLife * 20);
            this.resetNotice();
        }

        public final void resetNotice() {
            countdown = initial;
        }

        @Override
        public void onTick() {
            if (countdown <= 0) { return; }

            countdown -= 1;

            if (countdown <= 0) {
                this.setText("");
                this.setDirty(true);
            }
        }
    }
}
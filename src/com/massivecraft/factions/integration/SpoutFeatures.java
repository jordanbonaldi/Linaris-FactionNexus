package com.massivecraft.factions.integration;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;

public class SpoutFeatures {
    private transient static boolean spoutMe = false;
    private transient static SpoutMainListener mainListener;
    private transient static boolean listenersHooked;

    public static void setup() {
        final Plugin test = Bukkit.getServer().getPluginManager().getPlugin("Spout");
        if (test == null || !test.isEnabled()) { return; }

        SpoutFeatures.setAvailable(true, test.getDescription().getFullName());
    }

    // set integration availability
    public static void setAvailable(final boolean enable, final String pluginName) {
        SpoutFeatures.spoutMe = enable;
        if (!SpoutFeatures.spoutMe) { return; }

        P.p.log("Found and will use features of " + pluginName);

        if (!SpoutFeatures.listenersHooked) {
            SpoutFeatures.listenersHooked = true;
            SpoutFeatures.mainListener = new SpoutMainListener();
            Bukkit.getServer().getPluginManager().registerEvents(SpoutFeatures.mainListener, P.p);
        }
    }

    // If we're successfully hooked into Spout
    public static boolean enabled() {
        return SpoutFeatures.spoutMe;
    }

    // If Spout is available and the specified Player is running the Spoutcraft client
    public static boolean availableFor(final Player player) {
        return SpoutFeatures.spoutMe && SpoutManager.getPlayer(player).isSpoutCraftEnabled();
    }

    // update displayed current territory for all players inside a specified chunk; if specified chunk is null, then simply update everyone online
    public static void updateTerritoryDisplayLoc(final FLocation fLoc) {
        if (!SpoutFeatures.enabled()) { return; }

        final Set<FPlayer> players = FPlayers.i.getOnline();

        for (final FPlayer player : players) {
            if (fLoc == null) {
                SpoutFeatures.mainListener.updateTerritoryDisplay(player, false);
            } else if (player.getLastStoodAt().equals(fLoc)) {
                SpoutFeatures.mainListener.updateTerritoryDisplay(player, true);
            }
        }
    }

    // update displayed current territory for specified player; returns false if unsuccessful
    public static boolean updateTerritoryDisplay(final FPlayer player) {
        if (!SpoutFeatures.enabled()) { return false; }

        return SpoutFeatures.mainListener.updateTerritoryDisplay(player, true);
    }

    // update owner list for all players inside a specified chunk; if specified chunk is null, then simply update everyone online
    public static void updateOwnerListLoc(final FLocation fLoc) {
        if (!SpoutFeatures.enabled()) { return; }

        final Set<FPlayer> players = FPlayers.i.getOnline();

        for (final FPlayer player : players) {
            if (fLoc == null || player.getLastStoodAt().equals(fLoc)) {
                SpoutFeatures.mainListener.updateOwnerList(player);
            }
        }
    }

    // update owner list for specified player
    public static void updateOwnerList(final FPlayer player) {
        if (!SpoutFeatures.enabled()) { return; }

        SpoutFeatures.mainListener.updateOwnerList(player);
    }

    public static void playerDisconnect(final FPlayer player) {
        if (!SpoutFeatures.enabled()) { return; }

        SpoutFeatures.mainListener.removeTerritoryLabels(player.getName());
    }

    // update all appearances between every player
    public static void updateAppearances() {
        if (!SpoutFeatures.enabled()) { return; }

        final Set<FPlayer> players = FPlayers.i.getOnline();

        for (final FPlayer playerA : players) {
            for (final FPlayer playerB : players) {
                SpoutFeatures.updateSingle(playerB, playerA);
            }
        }
    }

    // update all appearances related to a specific player
    public static void updateAppearances(final Player player) {
        if (!SpoutFeatures.enabled() || player == null) { return; }

        final Set<FPlayer> players = FPlayers.i.getOnline();
        final FPlayer playerA = FPlayers.i.get(player);

        for (final FPlayer playerB : players) {
            SpoutFeatures.updateSingle(playerB, playerA);
            SpoutFeatures.updateSingle(playerA, playerB);
        }
    }

    // as above method, but with a delay added; useful for after-login update which doesn't always propagate if done immediately
    public static void updateAppearancesShortly(final Player player) {
        P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, new Runnable() {
            @Override
            public void run() {
                SpoutFeatures.updateAppearances(player);
            }
        }, 100);
    }

    // update all appearances related to a single faction
    public static void updateAppearances(final Faction faction) {
        if (!SpoutFeatures.enabled() || faction == null) { return; }

        final Set<FPlayer> players = FPlayers.i.getOnline();
        Faction factionA;

        for (final FPlayer playerA : players) {
            factionA = playerA.getFaction();

            for (final FPlayer playerB : players) {
                if (factionA != faction && playerB.getFaction() != faction) {
                    continue;
                }

                SpoutFeatures.updateSingle(playerB, playerA);
            }
        }
    }

    // update all appearances between two factions
    public static void updateAppearances(final Faction factionA, final Faction factionB) {
        if (!SpoutFeatures.enabled() || factionA == null || factionB == null) { return; }

        for (final FPlayer playerA : factionA.getFPlayersWhereOnline(true)) {
            for (final FPlayer playerB : factionB.getFPlayersWhereOnline(true)) {
                SpoutFeatures.updateSingle(playerB, playerA);
                SpoutFeatures.updateSingle(playerA, playerB);
            }
        }
    }

    // update a single appearance; internal use only by above public methods
    private static void updateSingle(final FPlayer viewer, final FPlayer viewed) {
        if (viewer == null || viewed == null) { return; }

        final Faction viewedFaction = viewed.getFaction();
        if (viewedFaction == null) { return; }

        // these still end up returning null on occasion at this point, mucking up the SpoutManager.getPlayer() method
        if (viewer.getPlayer() == null || viewed.getPlayer() == null) { return; }

        final SpoutPlayer pViewer = SpoutManager.getPlayer(viewer.getPlayer());
        final SpoutPlayer pViewed = SpoutManager.getPlayer(viewed.getPlayer());
        if (pViewed == null || pViewer == null) { return; }

        final String viewedTitle = viewed.getTitle();
        final Role viewedRole = viewed.getRole();

        if ((Conf.spoutFactionTagsOverNames || Conf.spoutFactionTitlesOverNames) && viewer != viewed) {
            if (viewedFaction.isNormal()) {
                String addTag = "";
                if (Conf.spoutFactionTagsOverNames) {
                    addTag += viewedFaction.getTag(viewed.getColorTo(viewer).toString() + "[") + "]";
                }

                final String rolePrefix = viewedRole.getPrefix();
                if (Conf.spoutFactionTitlesOverNames && (!viewedTitle.isEmpty() || !rolePrefix.isEmpty())) {
                    addTag += (addTag.isEmpty() ? "" : " ") + viewedRole.getPrefix() + viewedTitle;
                }

                pViewed.setTitleFor(pViewer, addTag + "\n" + pViewed.getDisplayName());
            } else {
                pViewed.setTitleFor(pViewer, pViewed.getDisplayName());
            }
        }

        if (Conf.spoutFactionAdminCapes && viewedRole.equals(Role.ADMIN) || Conf.spoutFactionModeratorCapes && viewedRole.equals(Role.MODERATOR)) {
            final Relation relation = viewer.getRelationTo(viewed);
            String cape = "";
            if (!viewedFaction.isNormal()) {
                // yeah, no cape if no faction
            } else if (viewedFaction.isPeaceful()) {
                cape = Conf.capePeaceful;
            } else if (relation.isNeutral()) {
                cape = Conf.capeNeutral;
            } else if (relation.isMember()) {
                cape = Conf.capeMember;
            } else if (relation.isEnemy()) {
                cape = Conf.capeEnemy;
            } else if (relation.isAlly()) {
                cape = Conf.capeAlly;
            }

            if (cape.isEmpty()) {
                pViewed.resetCapeFor(pViewer);
            } else {
                pViewed.setCapeFor(pViewer, cape);
            }
        } else if (Conf.spoutFactionAdminCapes || Conf.spoutFactionModeratorCapes) {
            pViewed.resetCapeFor(pViewer);
        }
    }

    // method to convert a Bukkit ChatColor to a Spout Color
    protected static Color getSpoutColor(final ChatColor inColor, final int alpha) {
        if (inColor == null) { return SpoutFeatures.SpoutFixedColor(191, 191, 191, alpha); }

        switch (inColor.getChar()) {
        case 0x1:
            return SpoutFeatures.SpoutFixedColor(0, 0, 191, alpha);
        case 0x2:
            return SpoutFeatures.SpoutFixedColor(0, 191, 0, alpha);
        case 0x3:
            return SpoutFeatures.SpoutFixedColor(0, 191, 191, alpha);
        case 0x4:
            return SpoutFeatures.SpoutFixedColor(191, 0, 0, alpha);
        case 0x5:
            return SpoutFeatures.SpoutFixedColor(191, 0, 191, alpha);
        case 0x6:
            return SpoutFeatures.SpoutFixedColor(191, 191, 0, alpha);
        case 0x7:
            return SpoutFeatures.SpoutFixedColor(191, 191, 191, alpha);
        case 0x8:
            return SpoutFeatures.SpoutFixedColor(64, 64, 64, alpha);
        case 0x9:
            return SpoutFeatures.SpoutFixedColor(64, 64, 255, alpha);
        case 0xA:
            return SpoutFeatures.SpoutFixedColor(64, 255, 64, alpha);
        case 0xB:
            return SpoutFeatures.SpoutFixedColor(64, 255, 255, alpha);
        case 0xC:
            return SpoutFeatures.SpoutFixedColor(255, 64, 64, alpha);
        case 0xD:
            return SpoutFeatures.SpoutFixedColor(255, 64, 255, alpha);
        case 0xE:
            return SpoutFeatures.SpoutFixedColor(255, 255, 64, alpha);
        case 0xF:
            return SpoutFeatures.SpoutFixedColor(255, 255, 255, alpha);
        default:
            return SpoutFeatures.SpoutFixedColor(0, 0, 0, alpha);
        }
    }

    private static Color SpoutFixedColor(final int r, final int g, final int b, final int a) {
        return new Color(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
    }
}

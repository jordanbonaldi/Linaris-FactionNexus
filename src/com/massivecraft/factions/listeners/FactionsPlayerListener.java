package com.massivecraft.factions.listeners;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.NumberConversions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.integration.SpoutFeatures;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.zcore.util.TextUtil;

import net.neferett.linaris.thenexus.executors.BukkitExecutors;
import net.neferett.linaris.thenexus.executors.BukkitFutures;
import net.neferett.linaris.thenexus.executors.BukkitScheduledExecutorService;

public class FactionsPlayerListener implements Listener {
    // The Nexus Start
    private final BukkitScheduledExecutorService sync;
    // The Nexus End
    public P p;

    public FactionsPlayerListener(final P p) {
        this.p = p;
        // The Nexus Start
        sync = BukkitExecutors.newSynchronous(p);
        // The Nexus End
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        // Make sure that all online players do have a fplayer.
        final FPlayer me = FPlayers.i.get(event.getPlayer());

        // Update the lastLoginTime for this fplayer
        me.setLastLoginTime(System.currentTimeMillis());

        // Store player's current FLocation and notify them where they are
        me.setLastStoodAt(new FLocation(event.getPlayer().getLocation()));
        if (!SpoutFeatures.updateTerritoryDisplay(me)) {
            me.sendFactionHereMessage();
        }

        SpoutFeatures.updateAppearancesShortly(event.getPlayer());

        // The Nexus Start
        final Player player = event.getPlayer();
        new BukkitRunnable() {

            @Override
            public void run() {
                try {
                    final ResultSet res = P.database.querySQL("SELECT id FROM players WHERE uuid=UNHEX('" + player.getUniqueId().toString().replace("-", "") + "')");
                    if (res == null || !res.first()) {
                        P.database.updateSQL("INSERT INTO players(name, uuid, coins, updated_at, created_at) VALUES('" + player.getName() + "', UNHEX('" + player.getUniqueId().toString().replace("-", "") + "'), 0, NOW(), NOW())");
                    }
                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(P.p);
        FPlayers.i.get(player).updateScoreboard();
        // The Nexus End
    }

    // The Nexus Start
    @EventHandler
    public void onPlayerItemConsume(final PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.GOLDEN_APPLE && event.getItem().getDurability() == 1) {
            final Player player = event.getPlayer();
            final FPlayer fplayer = FPlayers.i.get(player);
            if ((System.currentTimeMillis() - fplayer.getLastGoldenApple()) / 1000 < 300) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Vous devez attendre 5 minutes avant de manger à nouveau une pomme cheat.");
                return;
            }
            fplayer.setLastGoldenApple(System.currentTimeMillis());
        }
    }

    // The Nexus End

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final FPlayer me = FPlayers.i.get(event.getPlayer());

        // Make sure player's power is up to date when they log off.
        me.getPower();
        // and update their last login time to point to when the logged off, for auto-remove routine
        me.setLastLoginTime(System.currentTimeMillis());

        final Faction myFaction = me.getFaction();
        if (myFaction != null) {
            myFaction.memberLoggedOff();
        }
        SpoutFeatures.playerDisconnect(me);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (event.isCancelled()) { return; }

        // quick check to make sure player is moving between chunks; good performance boost
        if (event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4 && event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4 && event.getFrom().getWorld() == event.getTo().getWorld()) { return; }

        final Player player = event.getPlayer();
        final FPlayer me = FPlayers.i.get(player);

        // Did we change coord?
        final FLocation from = me.getLastStoodAt();
        final FLocation to = new FLocation(event.getTo());

        if (from.equals(to)) { return; }

        // Yes we did change coord (:

        me.setLastStoodAt(to);

        // Did we change "host"(faction)?
        final boolean spoutClient = SpoutFeatures.availableFor(player);
        final Faction factionFrom = Board.getFactionAt(from);
        final Faction factionTo = Board.getFactionAt(to);
        boolean changedFaction = factionFrom != factionTo;

        if (changedFaction && SpoutFeatures.updateTerritoryDisplay(me)) {
            changedFaction = false;
        }

        if (me.isMapAutoUpdating()) {
            me.sendMessage(Board.getMap(me.getFaction(), to, player.getLocation().getYaw()));

            if (spoutClient && Conf.spoutTerritoryOwnersShow) {
                SpoutFeatures.updateOwnerList(me);
            }
        } else {
            final Faction myFaction = me.getFaction();
            final String ownersTo = myFaction.getOwnerListString(to);

            if (changedFaction) {
                me.sendFactionHereMessage();
                if (Conf.ownedAreasEnabled && Conf.ownedMessageOnBorder && (!spoutClient || !Conf.spoutTerritoryOwnersShow) && myFaction == factionTo && !ownersTo.isEmpty()) {
                    me.sendMessage(Conf.ownedLandMessage + ownersTo);
                }
            } else if (spoutClient && Conf.spoutTerritoryOwnersShow) {
                SpoutFeatures.updateOwnerList(me);
            } else if (Conf.ownedAreasEnabled && Conf.ownedMessageInsideTerritory && factionFrom == factionTo && myFaction == factionTo) {
                final String ownersFrom = myFaction.getOwnerListString(from);
                if (Conf.ownedMessageByChunk || !ownersFrom.equals(ownersTo)) {
                    if (!ownersTo.isEmpty()) {
                        me.sendMessage(Conf.ownedLandMessage + ownersTo);
                    } else if (!Conf.publicLandMessage.isEmpty()) {
                        me.sendMessage(Conf.publicLandMessage);
                    }
                }
            }
        }

        if (me.getAutoClaimFor() != null) {
            me.attemptClaim(me.getAutoClaimFor(), event.getTo(), true);
        } else if (me.isAutoSafeClaimEnabled()) {
            if (!Permission.MANAGE_SAFE_ZONE.has(player)) {
                me.setIsAutoSafeClaimEnabled(false);
            } else {
                if (!Board.getFactionAt(to).isSafeZone()) {
                    Board.setFactionAt(Factions.i.getSafeZone(), to);
                    me.msg("<i>This land is now a safe zone.");
                }
            }
        } else if (me.isAutoWarClaimEnabled()) {
            if (!Permission.MANAGE_WAR_ZONE.has(player)) {
                me.setIsAutoWarClaimEnabled(false);
            } else {
                if (!Board.getFactionAt(to).isWarZone()) {
                    Board.setFactionAt(Factions.i.getWarZone(), to);
                    me.msg("<i>This land is now a war zone.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.isCancelled()) { return; }
        // The Nexus Start
        if (event.hasItem() && event.getItem().getType() == Material.GOLDEN_APPLE && event.getItem().getDurability() == 1) {
            final FPlayer fplayer = FPlayers.i.get(event.getPlayer());
            if ((System.currentTimeMillis() - fplayer.getLastGoldenApple()) / 1000 < 300) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Vous devez attendre avant de pouvoir manger une pomme dorée.");
                return;
            }
        }
        // The Nexus End
        // only need to check right-clicks and physical as of MC 1.4+; good performance boost
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) { return; }

        final Block block = event.getClickedBlock();
        final Player player = event.getPlayer(); // The Nexus

        if (block == null) { return; // clicked in air, apparently
        }

        if (!FactionsPlayerListener.canPlayerUseBlock(player, block, false)) {
            event.setCancelled(true);
            if (Conf.handleExploitInteractionSpam) {
                final String name = player.getName();
                InteractAttemptSpam attempt = interactSpammers.get(name);
                if (attempt == null) {
                    attempt = new InteractAttemptSpam();
                    interactSpammers.put(name, attempt);
                }
                final int count = attempt.increment();
                if (count >= 10) {
                    final FPlayer me = FPlayers.i.get(name);
                    me.msg("<b>Ouch, that is starting to hurt. You should give it a rest.");
                    player.damage(NumberConversions.floor((double) count / 10));
                }
            }
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; // only interested on right-clicks for below
        }

        if (!FactionsPlayerListener.playerCanUseItemHere(player, block.getLocation(), event.getMaterial(), false)) {
            event.setCancelled(true);
            return;
        }
        // The Nexus Start
        else if (event.hasItem() && event.getMaterial() == Material.MONSTER_EGG && event.getItem().getDurability() == 50) {
            final ListenableFuture<CreatureSpawnEvent> chat = BukkitFutures.nextEvent(p, CreatureSpawnEvent.class);

            Futures.addCallback(chat, new FutureCallback<CreatureSpawnEvent>() {
                @Override
                public void onSuccess(final CreatureSpawnEvent event) {
                    if (event.getCreatureType() == CreatureType.CREEPER) {
                        event.getEntity().setMetadata("nexus_owner", new FixedMetadataValue(p, player.getName()));
                    }
                }

                @Override
                public void onFailure(final Throwable ex) {}
            }, sync);
        }
        // The Nexus End
    }

    // for handling people who repeatedly spam attempts to open a door (or similar) in another faction's territory
    private final Map<String, InteractAttemptSpam> interactSpammers = new HashMap<String, InteractAttemptSpam>();

    private static class InteractAttemptSpam {
        private int attempts = 0;
        private long lastAttempt = System.currentTimeMillis();

        // returns the current attempt count
        public int increment() {
            final long Now = System.currentTimeMillis();
            if (Now > lastAttempt + 2000) {
                attempts = 1;
            } else {
                attempts++;
            }
            lastAttempt = Now;
            return attempts;
        }
    }

    public static boolean playerCanUseItemHere(final Player player, final Location location, final Material material, final boolean justCheck) {
        final String name = player.getName();
        if (Conf.playersWhoBypassAllProtection.contains(name)) { return true; }

        final FPlayer me = FPlayers.i.get(name);
        if (me.isAdminBypassing()) { return true; }

        final FLocation loc = new FLocation(location);
        final Faction otherFaction = Board.getFactionAt(loc);

        if (otherFaction.hasPlayersOnline()) {
            if (!Conf.territoryDenyUseageMaterials.contains(material)) { return true; // Item isn't one we're preventing for online factions.
            }
        } else {
            if (!Conf.territoryDenyUseageMaterialsWhenOffline.contains(material)) { return true; // Item isn't one we're preventing for offline factions.
            }
        }

        if (otherFaction.isNone()) {
            if (!Conf.wildernessDenyUseage || Conf.worldsNoWildernessProtection.contains(location.getWorld().getName())) { return true; // This is not faction territory. Use whatever you like here.
            }

            if (!justCheck) {
                me.msg("<b>You can't use <h>%s<b> in the wilderness.", TextUtil.getMaterialName(material));
            }

            return false;
        } else if (otherFaction.isSafeZone()) {
            if (!Conf.safeZoneDenyUseage || Permission.MANAGE_SAFE_ZONE.has(player)) { return true; }

            if (!justCheck) {
                me.msg("<b>You can't use <h>%s<b> in a safe zone.", TextUtil.getMaterialName(material));
            }

            return false;
        } else if (otherFaction.isWarZone()) {
            if (!Conf.warZoneDenyUseage || Permission.MANAGE_WAR_ZONE.has(player)) { return true; }

            if (!justCheck) {
                me.msg("<b>You can't use <h>%s<b> in a war zone.", TextUtil.getMaterialName(material));
            }

            return false;
        }

        final Faction myFaction = me.getFaction();
        final Relation rel = myFaction.getRelationTo(otherFaction);

        // Cancel if we are not in our own territory
        if (rel.confDenyUseage()) {
            if (!justCheck) {
                me.msg("<b>You can't use <h>%s<b> in the territory of <h>%s<b>.", TextUtil.getMaterialName(material), otherFaction.getTag(myFaction));
            }

            return false;
        }

        // Also cancel if player doesn't have ownership rights for this claim
        if (Conf.ownedAreasEnabled && Conf.ownedAreaDenyUseage && !otherFaction.playerHasOwnershipRights(me, loc)) {
            if (!justCheck) {
                me.msg("<b>You can't use <h>%s<b> in this territory, it is owned by: %s<b>.", TextUtil.getMaterialName(material), otherFaction.getOwnerListString(loc));
            }

            return false;
        }

        return true;
    }

    // The Nexus Start
    private static final Set<Material> canUseMaterials = new LinkedHashSet<Material>(Arrays.asList(Material.CHEST, Material.STONE_BUTTON, Material.WOOD_BUTTON, Material.WOODEN_DOOR, Material.TRAP_DOOR, Material.TRAPPED_CHEST, Material.LEVER, Material.WOOD_DOOR));

    // The Nexus End

    public static boolean canPlayerUseBlock(final Player player, final Block block, final boolean justCheck) {
        final String name = player.getName();
        if (Conf.playersWhoBypassAllProtection.contains(name)) { return true; }

        final FPlayer me = FPlayers.i.get(name);
        if (me.isAdminBypassing()) { return true; }

        final Material material = block.getType();
        final FLocation loc = new FLocation(block);
        final Faction otherFaction = Board.getFactionAt(loc);

        // no door/chest/whatever protection in wilderness, war zones, or safe zones
        if (!otherFaction.isNormal()) { return true; }

        // We only care about some material types.
        if (otherFaction.hasPlayersOnline()) {
            if (!Conf.territoryProtectedMaterials.contains(material)) { return true; }
        } else {
            if (!Conf.territoryProtectedMaterialsWhenOffline.contains(material)) { return true; }
        }

        final Faction myFaction = me.getFaction();
        final Relation rel = myFaction.getRelationTo(otherFaction);

        // You may use any block unless it is another faction's territory...
        if (rel.isNeutral() || rel.isEnemy() && Conf.territoryEnemyProtectMaterials || rel.isAlly() && Conf.territoryAllyProtectMaterials) {
            // The Nexus Start
            if (otherFaction.getHealth() <= 0 && !FactionsBlockListener.isInRange(otherFaction.getHome(), block.getLocation(), 4) && FactionsPlayerListener.canUseMaterials.contains(block.getType())) { return true; }

            if (!justCheck) {
                me.msg("<b>You can't %s <h>%s<b> in the territory of <h>%s<b>.", material == Material.SOIL ? "trample" : "use", TextUtil.getMaterialName(material), otherFaction.getTag(myFaction));
            }

            return false;
        }

        // Also cancel if player doesn't have ownership rights for this claim
        if (Conf.ownedAreasEnabled && Conf.ownedAreaProtectMaterials && !otherFaction.playerHasOwnershipRights(me, loc)) {
            if (!justCheck) {
                me.msg("<b>You can't use <h>%s<b> in this territory, it is owned by: %s<b>.", TextUtil.getMaterialName(material), otherFaction.getOwnerListString(loc));
            }

            return false;
        }

        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final FPlayer me = FPlayers.i.get(event.getPlayer());

        me.getPower(); // update power, so they won't have gained any while dead

        final Location home = me.getFaction().getHome();
        if (Conf.homesEnabled && Conf.homesTeleportToOnDeath && home != null && (Conf.homesRespawnFromNoPowerLossWorlds || !Conf.worldsNoPowerLoss.contains(event.getPlayer().getWorld().getName()))) {
            event.setRespawnLocation(home);
        }
    }

    // For some reason onPlayerInteract() sometimes misses bucket events depending on distance (something like 2-3 blocks away isn't detected),
    // but these separate bucket events below always fire without fail
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) { return; }

        final Block block = event.getBlockClicked();
        final Player player = event.getPlayer();

        if (!FactionsPlayerListener.playerCanUseItemHere(player, block.getLocation(), event.getBucket(), false)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBucketFill(final PlayerBucketFillEvent event) {
        if (event.isCancelled()) { return; }

        final Block block = event.getBlockClicked();
        final Player player = event.getPlayer();

        if (!FactionsPlayerListener.playerCanUseItemHere(player, block.getLocation(), event.getBucket(), false)) {
            event.setCancelled(true);
            return;
        }
    }

    public static boolean preventCommand(String fullCmd, final Player player) {
        if (Conf.territoryNeutralDenyCommands.isEmpty() && Conf.territoryEnemyDenyCommands.isEmpty() && Conf.permanentFactionMemberDenyCommands.isEmpty()) { return false; }

        fullCmd = fullCmd.toLowerCase();

        final FPlayer me = FPlayers.i.get(player);

        String shortCmd; // command without the slash at the beginning
        if (fullCmd.startsWith("/")) {
            shortCmd = fullCmd.substring(1);
        } else {
            shortCmd = fullCmd;
            fullCmd = "/" + fullCmd;
        }

        if (me.hasFaction() && !me.isAdminBypassing() && !Conf.permanentFactionMemberDenyCommands.isEmpty() && me.getFaction().isPermanent() && FactionsPlayerListener.isCommandInList(fullCmd, shortCmd, Conf.permanentFactionMemberDenyCommands.iterator())) {
            me.msg("<b>You can't use the command \"" + fullCmd + "\" because you are in a permanent faction.");
            return true;
        }

        if (!me.isInOthersTerritory()) { return false; }

        final Relation rel = me.getRelationToLocation();
        if (rel.isAtLeast(Relation.ALLY)) { return false; }

        if (rel.isNeutral() && !Conf.territoryNeutralDenyCommands.isEmpty() && !me.isAdminBypassing() && FactionsPlayerListener.isCommandInList(fullCmd, shortCmd, Conf.territoryNeutralDenyCommands.iterator())) {
            me.msg("<b>You can't use the command \"" + fullCmd + "\" in neutral territory.");
            return true;
        }

        if (rel.isEnemy() && !Conf.territoryEnemyDenyCommands.isEmpty() && !me.isAdminBypassing() && FactionsPlayerListener.isCommandInList(fullCmd, shortCmd, Conf.territoryEnemyDenyCommands.iterator())) {
            me.msg("<b>You can't use the command \"" + fullCmd + "\" in enemy territory.");
            return true;
        }

        return false;
    }

    private static boolean isCommandInList(final String fullCmd, final String shortCmd, final Iterator<String> iter) {
        String cmdCheck;
        while (iter.hasNext()) {
            cmdCheck = iter.next();
            if (cmdCheck == null) {
                iter.remove();
                continue;
            }

            cmdCheck = cmdCheck.toLowerCase();
            if (fullCmd.startsWith(cmdCheck) || shortCmd.startsWith(cmdCheck)) { return true; }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerKick(final PlayerKickEvent event) {
        if (event.isCancelled()) { return; }

        final FPlayer badGuy = FPlayers.i.get(event.getPlayer());
        if (badGuy == null) { return; }

        SpoutFeatures.playerDisconnect(badGuy);

        // if player was banned (not just kicked), get rid of their stored info
        if (Conf.removePlayerDataWhenBanned && event.getReason().equals("Banned by admin.")) {
            if (badGuy.getRole() == Role.ADMIN) {
                badGuy.getFaction().promoteNewLeader();
            }

            badGuy.leave(false);
            badGuy.detach();
        }
    }
}

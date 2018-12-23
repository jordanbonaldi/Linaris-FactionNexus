package com.massivecraft.factions.listeners;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.integration.Worldguard;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;

import net.neferett.linaris.thenexus.Level;
import net.neferett.linaris.thenexus.PlayerUtils;

public class FactionsBlockListener implements Listener {
    public P p;

    public FactionsBlockListener(final P p) {
        this.p = p;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if (event.isCancelled()) { return; }
        if (!event.canBuild()) { return; }

        // special case for flint&steel, which should only be prevented by DenyUsage list
        if (event.getBlockPlaced().getType() == Material.FIRE) { return; }

        if (!FactionsBlockListener.playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "build", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.isCancelled()) { return; }

        if (!FactionsBlockListener.playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "destroy", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockDamage(final BlockDamageEvent event) {
        if (event.isCancelled()) { return; }

        if (event.getInstaBreak() && !FactionsBlockListener.playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "destroy", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
        if (event.isCancelled()) { return; }
        if (!Conf.pistonProtectionThroughDenyBuild) { return; }

        final Faction pistonFaction = Board.getFactionAt(new FLocation(event.getBlock()));

        // target end-of-the-line empty (air) block which is being pushed into, including if piston itself would extend into air
        final Block targetBlock = event.getBlock().getRelative(event.getDirection(), event.getLength() + 1);

        // if potentially pushing into air/water/lava in another territory, we need to check it out
        if ((targetBlock.isEmpty() || targetBlock.isLiquid()) && !this.canPistonMoveBlock(pistonFaction, targetBlock.getLocation())) {
            event.setCancelled(true);
            return;
        }

        /*
         * note that I originally was testing the territory of each affected block, but since I found that pistons can only push
         * up to 12 blocks and the width of any territory is 16 blocks, it should be safe (and much more lightweight) to test
         * only the final target block as done above
         */
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
        // if not a sticky piston, retraction should be fine
        if (event.isCancelled() || !event.isSticky() || !Conf.pistonProtectionThroughDenyBuild) { return; }

        final Location targetLoc = event.getRetractLocation();

        // if potentially retracted block is just air/water/lava, no worries
        if (targetLoc.getBlock().isEmpty() || targetLoc.getBlock().isLiquid()) { return; }

        final Faction pistonFaction = Board.getFactionAt(new FLocation(event.getBlock()));

        if (!this.canPistonMoveBlock(pistonFaction, targetLoc)) {
            event.setCancelled(true);
            return;
        }
    }

    private boolean canPistonMoveBlock(final Faction pistonFaction, final Location target) {

        final Faction otherFaction = Board.getFactionAt(new FLocation(target));

        if (pistonFaction == otherFaction) { return true; }

        if (otherFaction.isNone()) {
            if (!Conf.wildernessDenyBuild || Conf.worldsNoWildernessProtection.contains(target.getWorld().getName())) { return true; }

            return false;
        } else if (otherFaction.isSafeZone()) {
            if (!Conf.safeZoneDenyBuild) { return true; }

            return false;
        } else if (otherFaction.isWarZone()) {
            if (!Conf.warZoneDenyBuild) { return true; }

            return false;
        }

        final Relation rel = pistonFaction.getRelationTo(otherFaction);

        if (rel.confDenyBuild(otherFaction.hasPlayersOnline())) { return false; }

        return true;
    }

    // The Nexus Start
    private static final Set<Material> canDestroyMaterials = new LinkedHashSet<Material>(Arrays.asList(Material.CHEST, Material.TRAPPED_CHEST));

    // The Nexus End

    public static boolean playerCanBuildDestroyBlock(final Player player, final Location location, final String action, final boolean justCheck) {
        final String name = player.getName();
        if (Conf.playersWhoBypassAllProtection.contains(name)) { return true; }

        final FPlayer me = FPlayers.i.get(name);
        if (me.isAdminBypassing()) { return true; }

        final FLocation loc = new FLocation(location);
        final Faction otherFaction = Board.getFactionAt(loc);

        if (otherFaction.isNone()) {
            if (Conf.worldGuardBuildPriority && Worldguard.playerCanBuild(player, location)) { return true; }

            if (!Conf.wildernessDenyBuild || Conf.worldsNoWildernessProtection.contains(location.getWorld().getName())) { return true; // This is not faction territory. Use whatever you like here.
            }

            if (!justCheck) {
                me.msg("<b>You can't " + action + " in the wilderness.");
            }

            return false;
        } else if (otherFaction.isSafeZone()) {
            if (Conf.worldGuardBuildPriority && Worldguard.playerCanBuild(player, location)) { return true; }

            if (!Conf.safeZoneDenyBuild || Permission.MANAGE_SAFE_ZONE.has(player)) { return true; }

            if (!justCheck) {
                me.msg("<b>You can't " + action + " in a safe zone.");
            }

            return false;
        } else if (otherFaction.isWarZone()) {
            if (Conf.worldGuardBuildPriority && Worldguard.playerCanBuild(player, location)) { return true; }

            if (!Conf.warZoneDenyBuild || Permission.MANAGE_WAR_ZONE.has(player)) { return true; }

            if (!justCheck) {
                me.msg("<b>You can't " + action + " in a war zone.");
            }

            return false;
        }

        final Faction myFaction = me.getFaction();
        final Relation rel = myFaction.getRelationTo(otherFaction);
        final boolean online = otherFaction.hasPlayersOnline();
        final boolean pain = !justCheck && rel.confPainBuild(online);
        final boolean deny = rel.confDenyBuild(online);

        // hurt the player for building/destroying in other territory?
        if (pain) {
            player.damage(Conf.actionDeniedPainAmount);

            if (!deny) {
                me.msg("<b>It is painful to try to " + action + " in the territory of " + otherFaction.getTag(myFaction));
            }
        }

        // cancel building/destroying in other territory?
        if (deny) {
            // The Nexus Start
            if (otherFaction.getHealth() <= 0 && action.equals("destroy") && !FactionsBlockListener.isInRange(otherFaction.getHome(), location, 4) && FactionsBlockListener.canDestroyMaterials.contains(location.getBlock().getType())) {
                if (otherFaction.getBrokenChestsTemp() == 20) {
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
                    player.sendMessage(ChatColor.RED + "20 coffres ont déjà été cassés pour ce pillage.");
                    return false;
                }
                otherFaction.incrementBrokenChestsTemp();
                if (myFaction.isNormal()) {
                    myFaction.incrementBrokenChestsCount();
                    final Level nextLevel = myFaction.getNextLevel();
                    if (nextLevel != null && myFaction.getBrokenChestsCount() < nextLevel.getBrokenChests().getCount()) {
                        player.sendMessage(ChatColor.GRAY + "Avancement de la casse des coffres : " + ChatColor.GOLD + myFaction.getBrokenChestsCount() + ChatColor.AQUA + "/" + nextLevel.getBrokenChests().getCount());
                    } else if (nextLevel != null && myFaction.getBrokenChestsCount() == nextLevel.getBrokenChests().getCount()) {
                        myFaction.sendMessage(Arrays.asList("", "", "", "", "", "", "", "", ChatColor.DARK_PURPLE + "[-----------" + ChatColor.GOLD + "AVANCEMENT" + ChatColor.DARK_PURPLE + "-----------]", ChatColor.GRAY + "Votre faction a détruit tous les coffres nécéssaires pour passer au level supérieur.", ChatColor.DARK_PURPLE + "[--------------------------------]"));
                    }
                }
                PlayerUtils.giveCoins(player, 10);
                return true;
            }
            // The Nexus End

            if (!justCheck) {
                me.msg("<b>You can't " + action + " in the territory of " + otherFaction.getTag(myFaction));
            }

            return false;
        }

        // Also cancel and/or cause pain if player doesn't have ownership rights for this claim
        if (Conf.ownedAreasEnabled && (Conf.ownedAreaDenyBuild || Conf.ownedAreaPainBuild) && !otherFaction.playerHasOwnershipRights(me, loc)) {
            if (!pain && Conf.ownedAreaPainBuild && !justCheck) {
                player.damage(Conf.actionDeniedPainAmount);

                if (!Conf.ownedAreaDenyBuild) {
                    me.msg("<b>It is painful to try to " + action + " in this territory, it is owned by: " + otherFaction.getOwnerListString(loc));
                }
            }
            if (Conf.ownedAreaDenyBuild) {
                if (!justCheck) {
                    me.msg("<b>You can't " + action + " in this territory, it is owned by: " + otherFaction.getOwnerListString(loc));
                }

                return false;
            }
        }
        // The Nexus Start
        else if (otherFaction == myFaction && myFaction.hasHome() && FactionsBlockListener.isInRange(myFaction.getHome(), location, 4)) {
            me.getPlayer().sendMessage(ChatColor.RED + "Vous ne pouvez pas construire à moins de 4 blocs du coeur.");
            return false;
        }
        // The Nexus End

        return true;
    }

    // The Nexus Start
    public static boolean isInRange(final Location nexus, final Location location, final int range) {
        final int maxX = Math.max(nexus.getBlockX(), location.getBlockX());
        final int maxZ = Math.max(nexus.getBlockZ(), location.getBlockZ());
        final int minX = Math.min(nexus.getBlockX(), location.getBlockX());
        final int minZ = Math.min(nexus.getBlockZ(), location.getBlockZ());
        return maxX - minX <= range - 1 && maxZ - minZ <= range - 1 && location.getBlockY() - nexus.getBlockY() <= range + 1;
    }
    // The Nexus End
}

package com.massivecraft.factions.cmd;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.integration.EssentialsFeatures;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.zcore.util.SmokeUtil;

public class CmdHome extends FCommand {

    public CmdHome() {
        super();
        aliases.add("home");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("", "");

        permission = Permission.HOME.node;
        disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = true;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        // TODO: Hide this command on help also.
        if (!Conf.homesEnabled) {
            fme.msg("<b>Sorry, Faction homes are disabled on this server.");
            return;
        }

        if (!Conf.homesTeleportCommandEnabled) {
            fme.msg("<b>Sorry, the ability to teleport to Faction homes is disabled on this server.");
            return;
        }

        if (!myFaction.hasHome()) {
            fme.msg("<b>Your faction does not have a home. " + (fme.getRole().value < Role.MODERATOR.value ? "<i> Ask your leader to:" : "<i>You should:"));
            fme.sendMessage(p.cmdBase.cmdSethome.getUseageTemplate());
            return;
        }

        if (!Conf.homesTeleportAllowedFromEnemyTerritory && fme.isInEnemyTerritory()) {
            fme.msg("<b>You cannot teleport to your faction home while in the territory of an enemy faction.");
            return;
        }

        if (!Conf.homesTeleportAllowedFromDifferentWorld && me.getWorld().getUID() != myFaction.getHome().getWorld().getUID()) {
            fme.msg("<b>You cannot teleport to your faction home while in a different world.");
            return;
        }

        final Faction faction = Board.getFactionAt(new FLocation(me.getLocation()));
        final Location loc = me.getLocation().clone();

        // if player is not in a safe zone or their own faction territory, only allow teleport if no enemies are nearby
        if (Conf.homesTeleportAllowedEnemyDistance > 0 && !faction.isSafeZone() && (!fme.isInOwnTerritory() || fme.isInOwnTerritory() && !Conf.homesTeleportIgnoreEnemiesIfInOwnTerritory)) {
            final World w = loc.getWorld();
            final double x = loc.getX();
            final double y = loc.getY();
            final double z = loc.getZ();

            for (final Player p : me.getServer().getOnlinePlayers()) {
                if (p == null || !p.isOnline() || p.isDead() || p == me || p.getWorld() != w) {
                    continue;
                }

                final FPlayer fp = FPlayers.i.get(p);
                if (fme.getRelationTo(fp) != Relation.ENEMY) {
                    continue;
                }

                final Location l = p.getLocation();
                final double dx = Math.abs(x - l.getX());
                final double dy = Math.abs(y - l.getY());
                final double dz = Math.abs(z - l.getZ());
                final double max = Conf.homesTeleportAllowedEnemyDistance;

                // box-shaped distance check
                if (dx > max || dy > max || dz > max) {
                    continue;
                }

                fme.msg("<b>You cannot teleport to your faction home while an enemy is within " + Conf.homesTeleportAllowedEnemyDistance + " blocks of you.");
                return;
            }
        }

        // if Essentials teleport handling is enabled and available, pass the teleport off to it (for delay and cooldown)
        if (EssentialsFeatures.handleTeleport(me, myFaction.getHome())) { return; }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!this.payForCommand(Conf.econCostHome, "to teleport to your faction home", "for teleporting to your faction home")) { return; }

        // Create a smoke effect
        if (Conf.homesTeleportCommandSmokeEffectEnabled) {
            final List<Location> smokeLocations = new ArrayList<Location>();
            smokeLocations.add(loc);
            smokeLocations.add(loc.add(0, 1, 0));
            smokeLocations.add(myFaction.getHome());
            smokeLocations.add(myFaction.getHome().clone().add(0, 1, 0));
            SmokeUtil.spawnCloudRandom(smokeLocations, Conf.homesTeleportCommandSmokeEffectThickness);
        }

        me.teleport(myFaction.getHome());
    }

}

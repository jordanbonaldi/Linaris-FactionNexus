package com.massivecraft.factions.cmd;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;

public class CmdSethome extends FCommand {
    public CmdSethome() {
        aliases.add("sethome");

        //this.requiredArgs.add("");
        optionalArgs.put("faction tag", "mine");

        permission = Permission.SETHOME.node;
        disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        if (!Conf.homesEnabled) {
            fme.msg("<b>Sorry, Faction homes are disabled on this server.");
            return;
        }

        final Faction faction = this.argAsFaction(0, myFaction);
        if (faction == null) { return; }

        // Can the player set the home for this faction?
        if (faction == myFaction) {
            if (!Permission.SETHOME_ANY.has(sender) && !this.assertMinRole(Role.MODERATOR)) { return; }
        } else {
            if (!Permission.SETHOME_ANY.has(sender, true)) { return; }
        }

        // Can the player set the faction home HERE?
        if (
        /* ! Permission.BYPASS.has(me) The Nexus
        && */
        Conf.homesMustBeInClaimedTerritory && Board.getFactionAt(new FLocation(me)) != faction) {
            fme.msg("<b>Sorry, your faction home can only be set inside your own claimed territory.");
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!this.payForCommand(Conf.econCostSethome, "to set the faction home", "for setting the faction home")) { return; }

        // The Nexus Start
        if ((System.currentTimeMillis() - myFaction.getLastHomeSet()) / 1000 < 86400) {
            fme.getPlayer().sendMessage(ChatColor.RED + "Vous ne pouvez définir votre home que toutes les 24 heures.");
            return;
        }
        final Location playerLoc = me.getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                final Location loc = playerLoc.clone().add(x, -1, z);
                if (Board.getFactionAt(new FLocation(loc)) != myFaction || loc.getBlock().getType() == Material.BEDROCK) {
                    fme.getPlayer().sendMessage(ChatColor.RED + "Impossible de définir votre home ici. Placez-vous au milieu du chunk ou claimez les zones alentours.");
                    return;
                }
            }
        }

        if (faction.hasHome()) {
            faction.breakHome();
        }
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y < 6; y++) {
                for (int z = -3; z <= 3; z++) {
                    final Block b = playerLoc.clone().add(x, y, z).getBlock();
                    b.breakNaturally();
                    if (y == -1 || x == -3 && z == -3 || x == 3 && z == 3 || x == -3 && z == 3 || x == 3 && z == -3) {
                        b.setType(Material.OBSIDIAN);
                    }
                }
            }
        }
        final Location homeLocation = playerLoc.getBlock().getLocation().add(0.5, 0, 0.5);
        faction.setHome(homeLocation);
        faction.spawnNexus(true);
        myFaction.setLastHomeSet(System.currentTimeMillis());
        // The Nexus End
        //faction.setHome(me.getLocation());

        faction.msg("%s<i> set the home for your faction. You can now use:", fme.describeTo(myFaction, true));
        faction.sendMessage(p.cmdBase.cmdHome.getUseageTemplate());
        if (faction != myFaction) {
            fme.msg("<b>You have set the home for the " + faction.getTag(fme) + "<i> faction.");
        }
    }

}

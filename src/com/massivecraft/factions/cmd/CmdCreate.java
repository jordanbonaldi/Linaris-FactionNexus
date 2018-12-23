package com.massivecraft.factions.cmd;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.event.FPlayerJoinEvent;
import com.massivecraft.factions.event.FactionCreateEvent;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;

public class CmdCreate extends FCommand {
    public CmdCreate() {
        super();
        aliases.add("create");

        requiredArgs.add("faction tag");
        //this.optionalArgs.put("", "");

        permission = Permission.CREATE.node;
        disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final String tag = this.argAsString(0);

        if (fme.hasFaction()) {
            this.msg("<b>You must leave your current faction first.");
            return;
        }

        if (Factions.i.isTagTaken(tag)) {
            this.msg("<b>That tag is already in use.");
            return;
        }

        final ArrayList<String> tagValidationErrors = Factions.validateTag(tag);
        if (tagValidationErrors.size() > 0) {
            this.sendMessage(tagValidationErrors);
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make sure they can pay
        if (!this.canAffordCommand(Conf.econCostCreate, "to create a new faction")) { return; }

        // trigger the faction creation event (cancellable)
        final FactionCreateEvent createEvent = new FactionCreateEvent(me, tag);
        Bukkit.getServer().getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled()) { return; }

        // then make 'em pay (if applicable)
        if (!this.payForCommand(Conf.econCostCreate, "to create a new faction", "for creating a new faction")) { return; }

        // The Nexus Start
        if ((System.currentTimeMillis() - fme.getLastCreatedFaction()) / 1000 < 86400) {
            me.sendMessage(ChatColor.RED + "Vous ne pouvez créer qu'une faction toutes les 24 heures.");
            return;
        }
        fme.setLastCreatedFaction(System.currentTimeMillis());
        // The Nexus End

        final Faction faction = Factions.i.create();

        // TODO: Why would this even happen??? Auto increment clash??
        if (faction == null) {
            // The Nexus Start
            fme.setLastCreatedFaction(0);
            // The Nexus End
            this.msg("<b>There was an internal error while trying to create your faction. Please try again.");
            return;
        }

        // finish setting up the Faction
        faction.setTag(tag);

        // trigger the faction join event for the creator
        final FPlayerJoinEvent joinEvent = new FPlayerJoinEvent(fme, faction, FPlayerJoinEvent.PlayerJoinReason.CREATE); // The Nexus
        Bukkit.getServer().getPluginManager().callEvent(joinEvent);
        // join event cannot be cancelled or you'll have an empty faction

        // finish setting up the FPlayer
        fme.setRole(Role.ADMIN);
        fme.setFaction(faction);

        for (final FPlayer follower : FPlayers.i.getOnline()) {
            follower.msg("%s<i> created a new faction %s", fme.describeTo(follower, true), faction.getTag(follower));
        }

        this.msg("<i>You should now: %s", p.cmdBase.cmdDescription.getUseageTemplate());

        if (Conf.logFactionCreate) {
            P.p.log(fme.getName() + " created a new faction: " + tag);
        }
    }

}

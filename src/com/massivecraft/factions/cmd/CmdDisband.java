package com.massivecraft.factions.cmd;

import org.bukkit.Bukkit;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.event.FPlayerLeaveEvent;
import com.massivecraft.factions.event.FactionDisbandEvent;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.SpoutFeatures;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;

public class CmdDisband extends FCommand {
    public CmdDisband() {
        super();
        aliases.add("disband");

        //this.requiredArgs.add("");
        optionalArgs.put("faction tag", "yours");

        permission = Permission.DISBAND.node;
        disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        // The faction, default to your own.. but null if console sender.
        final Faction faction = this.argAsFaction(0, fme == null ? null : myFaction);
        if (faction == null) { return; }

        final boolean isMyFaction = fme == null ? false : faction == myFaction;

        if (isMyFaction) {
            if (!this.assertMinRole(Role.ADMIN)) { return; }
        } else {
            if (!Permission.DISBAND_ANY.has(sender, true)) { return; }
        }

        if (!faction.isNormal()) {
            this.msg("<i>You cannot disband the Wilderness, SafeZone, or WarZone.");
            return;
        }
        if (faction.isPermanent()) {
            this.msg("<i>This faction is designated as permanent, so you cannot disband it.");
            return;
        }

        final FactionDisbandEvent disbandEvent = new FactionDisbandEvent(me, faction.getId());
        Bukkit.getServer().getPluginManager().callEvent(disbandEvent);
        if (disbandEvent.isCancelled()) { return; }

        // Send FPlayerLeaveEvent for each player in the faction
        for (final FPlayer fplayer : faction.getFPlayers()) {
            Bukkit.getServer().getPluginManager().callEvent(new FPlayerLeaveEvent(fplayer, faction, FPlayerLeaveEvent.PlayerLeaveReason.DISBAND));
        }

        // Inform all players
        for (final FPlayer fplayer : FPlayers.i.getOnline()) {
            final String who = senderIsConsole ? "A server admin" : fme.describeTo(fplayer);
            if (fplayer.getFaction() == faction) {
                fplayer.msg("<h>%s<i> disbanded your faction.", who);
            } else {
                fplayer.msg("<h>%s<i> disbanded the faction %s.", who, faction.getTag(fplayer));
            }
        }
        if (Conf.logFactionDisband) {
            P.p.log("The faction " + faction.getTag() + " (" + faction.getId() + ") was disbanded by " + (senderIsConsole ? "console command" : fme.getName()) + ".");
        }

        if (Econ.shouldBeUsed() && !senderIsConsole) {
            //Give all the faction's money to the disbander
            final double amount = Econ.getBalance(faction.getAccountId());
            Econ.transferMoney(fme, faction, fme, amount, false);

            if (amount > 0.0) {
                final String amountString = Econ.moneyString(amount);
                this.msg("<i>You have been given the disbanded faction's bank, totaling %s.", amountString);
                P.p.log(fme.getName() + " has been given bank holdings of " + amountString + " from disbanding " + faction.getTag() + ".");
            }
        }

        faction.detach();

        SpoutFeatures.updateAppearances();
    }
}

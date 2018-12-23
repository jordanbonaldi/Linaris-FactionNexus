package com.massivecraft.factions.cmd;

import org.bukkit.Bukkit;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.P;
import com.massivecraft.factions.event.LandUnclaimAllEvent;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.SpoutFeatures;
import com.massivecraft.factions.struct.Permission;

public class CmdUnclaimall extends FCommand {
    public CmdUnclaimall() {
        aliases.add("unclaimall");
        aliases.add("declaimall");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("", "");

        permission = Permission.UNCLAIM_ALL.node;
        disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        if (Econ.shouldBeUsed()) {
            final double refund = Econ.calculateTotalLandRefund(myFaction.getLandRounded());
            if (Conf.bankEnabled && Conf.bankFactionPaysLandCosts) {
                if (!Econ.modifyMoney(myFaction, refund, "to unclaim all faction land", "for unclaiming all faction land")) { return; }
            } else {
                if (!Econ.modifyMoney(fme, refund, "to unclaim all faction land", "for unclaiming all faction land")) { return; }
            }
        }

        final LandUnclaimAllEvent unclaimAllEvent = new LandUnclaimAllEvent(myFaction, fme);
        Bukkit.getServer().getPluginManager().callEvent(unclaimAllEvent);
        // this event cannot be cancelled

        Board.unclaimAll(myFaction.getId());
        // The Nexus Start
        final boolean hasHome = myFaction.hasHome();
        // The Nexus End
        myFaction.msg("%s<i> unclaimed ALL of your faction's land.", fme.describeTo(myFaction, true));
        SpoutFeatures.updateTerritoryDisplayLoc(null);

        if (Conf.logLandUnclaims) {
            P.p.log(fme.getName() + " unclaimed everything for the faction: " + myFaction.getTag());
        }
    }

}

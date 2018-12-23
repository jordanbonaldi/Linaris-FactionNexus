package com.massivecraft.factions.cmd;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.P;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;

public class CmdMoneyTransferFp extends FCommand {
    public CmdMoneyTransferFp() {
        aliases.add("fp");

        requiredArgs.add("amount");
        requiredArgs.add("faction");
        requiredArgs.add("player");

        //this.optionalArgs.put("", "");

        permission = Permission.MONEY_F2P.node;
        this.setHelpShort("transfer f -> p");

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final double amount = this.argAsDouble(0, 0d);
        final EconomyParticipator from = this.argAsFaction(1);
        if (from == null) { return; }
        final EconomyParticipator to = this.argAsBestFPlayerMatch(2);
        if (to == null) { return; }

        final boolean success = Econ.transferMoney(fme, from, to, amount);

        if (success && Conf.logMoneyTransactions) {
            P.p.log(ChatColor.stripColor(P.p.txt.parse("%s transferred %s from the faction \"%s\" to the player \"%s\"", fme.getName(), Econ.moneyString(amount), from.describeTo(null), to.describeTo(null))));
        }
    }
}

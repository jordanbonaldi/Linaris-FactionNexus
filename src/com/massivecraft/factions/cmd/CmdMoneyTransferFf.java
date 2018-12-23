package com.massivecraft.factions.cmd;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.P;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;

public class CmdMoneyTransferFf extends FCommand {
    public CmdMoneyTransferFf() {
        aliases.add("ff");

        requiredArgs.add("amount");
        requiredArgs.add("faction");
        requiredArgs.add("faction");

        //this.optionalArgs.put("", "");

        permission = Permission.MONEY_F2F.node;
        this.setHelpShort("transfer f -> f");

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
        final EconomyParticipator to = this.argAsFaction(2);
        if (to == null) { return; }

        final boolean success = Econ.transferMoney(fme, from, to, amount);

        if (success && Conf.logMoneyTransactions) {
            P.p.log(ChatColor.stripColor(P.p.txt.parse("%s transferred %s from the faction \"%s\" to the faction \"%s\"", fme.getName(), Econ.moneyString(amount), from.describeTo(null), to.describeTo(null))));
        }
    }
}

package com.massivecraft.factions.cmd;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.P;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;

public class CmdMoneyDeposit extends FCommand {

    public CmdMoneyDeposit() {
        super();
        aliases.add("d");
        aliases.add("deposit");

        requiredArgs.add("amount");
        optionalArgs.put("faction", "yours");

        permission = Permission.MONEY_DEPOSIT.node;
        this.setHelpShort("deposit money");

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final double amount = this.argAsDouble(0, 0d);
        final EconomyParticipator faction = this.argAsFaction(1, myFaction);
        if (faction == null) { return; }
        final boolean success = Econ.transferMoney(fme, fme, faction, amount);

        if (success && Conf.logMoneyTransactions) {
            P.p.log(ChatColor.stripColor(P.p.txt.parse("%s deposited %s in the faction bank: %s", fme.getName(), Econ.moneyString(amount), faction.describeTo(null))));
        }
    }

}

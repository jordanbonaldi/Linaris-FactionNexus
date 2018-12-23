package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.struct.Permission;

public class CmdPower extends FCommand {

    public CmdPower() {
        super();
        aliases.add("power");
        aliases.add("pow");

        //this.requiredArgs.add("faction tag");
        optionalArgs.put("player name", "you");

        permission = Permission.POWER.node;
        disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final FPlayer target = this.argAsBestFPlayerMatch(0, fme);
        if (target == null) { return; }

        if (target != fme && !Permission.POWER_ANY.has(sender, true)) { return; }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!this.payForCommand(Conf.econCostPower, "to show player power info", "for showing player power info")) { return; }

        final double powerBoost = target.getPowerBoost();
        final String boost = powerBoost == 0.0 ? "" : (powerBoost > 0.0 ? " (bonus: " : " (penalty: ") + powerBoost + ")";
        this.msg("%s<a> - Power / Maxpower: <i>%d / %d %s", target.describeTo(fme, true), target.getPowerRounded(), target.getPowerMaxRounded(), boost);
    }

}

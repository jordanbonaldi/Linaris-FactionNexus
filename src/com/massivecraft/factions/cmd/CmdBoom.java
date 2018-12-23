package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.struct.Permission;

public class CmdBoom extends FCommand {
    public CmdBoom() {
        super();
        aliases.add("noboom");

        //this.requiredArgs.add("");
        optionalArgs.put("on/off", "flip");

        permission = Permission.NO_BOOM.node;
        disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        if (!myFaction.isPeaceful()) {
            fme.msg("<b>This command is only usable by factions which are specially designated as peaceful.");
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!this.payForCommand(Conf.econCostNoBoom, "to toggle explosions", "for toggling explosions")) { return; }

        myFaction.setPeacefulExplosionsEnabled(this.argAsBool(0, !myFaction.getPeacefulExplosionsEnabled()));

        final String enabled = myFaction.noExplosionsInTerritory() ? "disabled" : "enabled";

        // Inform
        myFaction.msg("%s<i> has " + enabled + " explosions in your faction's territory.", fme.describeTo(myFaction));
    }
}

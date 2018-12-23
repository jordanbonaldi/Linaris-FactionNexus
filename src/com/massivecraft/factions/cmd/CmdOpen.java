package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.struct.Permission;

public class CmdOpen extends FCommand {
    public CmdOpen() {
        super();
        aliases.add("open");

        //this.requiredArgs.add("");
        optionalArgs.put("yes/no", "flip");

        permission = Permission.OPEN.node;
        disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!this.payForCommand(Conf.econCostOpen, "to open or close the faction", "for opening or closing the faction")) { return; }

        myFaction.setOpen(this.argAsBool(0, !myFaction.getOpen()));

        final String open = myFaction.getOpen() ? "open" : "closed";

        // Inform
        myFaction.msg("%s<i> changed the faction to <h>%s<i>.", fme.describeTo(myFaction, true), open);
        for (final Faction faction : Factions.i.get()) {
            if (faction == myFaction) {
                continue;
            }
            faction.msg("<i>The faction %s<i> is now %s", myFaction.getTag(faction), open);
        }
    }

}

package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.struct.Permission;

public class CmdDeinvite extends FCommand {

    public CmdDeinvite() {
        super();
        aliases.add("deinvite");
        aliases.add("deinv");

        requiredArgs.add("player name");
        //this.optionalArgs.put("", "");

        permission = Permission.DEINVITE.node;
        disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final FPlayer you = this.argAsBestFPlayerMatch(0);
        if (you == null) { return; }

        if (you.getFaction() == myFaction) {
            this.msg("%s<i> is already a member of %s", you.getName(), myFaction.getTag());
            this.msg("<i>You might want to: %s", p.cmdBase.cmdKick.getUseageTemplate(false));
            return;
        }

        myFaction.deinvite(you);

        you.msg("%s<i> revoked your invitation to <h>%s<i>.", fme.describeTo(you), myFaction.describeTo(you));

        myFaction.msg("%s<i> revoked %s's<i> invitation.", fme.describeTo(myFaction), you.describeTo(myFaction));
    }

}

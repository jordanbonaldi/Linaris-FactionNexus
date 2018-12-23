package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;

public class CmdWarunclaimall extends FCommand {

    public CmdWarunclaimall() {
        aliases.add("warunclaimall");
        aliases.add("wardeclaimall");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("", "");

        permission = Permission.MANAGE_WAR_ZONE.node;
        disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;

        this.setHelpShort("unclaim all warzone land");
    }

    @Override
    public void perform() {
        Board.unclaimAll(Factions.i.getWarZone().getId());
        this.msg("<i>You unclaimed ALL war zone land.");

        if (Conf.logLandUnclaims) {
            P.p.log(fme.getName() + " unclaimed all war zones.");
        }
    }

}

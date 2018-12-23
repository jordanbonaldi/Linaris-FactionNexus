package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;

public class CmdSafeunclaimall extends FCommand {

    public CmdSafeunclaimall() {
        aliases.add("safeunclaimall");
        aliases.add("safedeclaimall");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("radius", "0");

        permission = Permission.MANAGE_SAFE_ZONE.node;
        disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;

        this.setHelpShort("Unclaim all safezone land");
    }

    @Override
    public void perform() {
        Board.unclaimAll(Factions.i.getSafeZone().getId());
        this.msg("<i>You unclaimed ALL safe zone land.");

        if (Conf.logLandUnclaims) {
            P.p.log(fme.getName() + " unclaimed all safe zones.");
        }
    }

}

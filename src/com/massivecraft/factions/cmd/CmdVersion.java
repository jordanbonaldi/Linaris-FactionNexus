package com.massivecraft.factions.cmd;

import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;

public class CmdVersion extends FCommand {
    public CmdVersion() {
        aliases.add("version");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("", "");

        permission = Permission.VERSION.node;
        disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        this.msg("<i>You are running " + P.p.getDescription().getFullName());
    }
}

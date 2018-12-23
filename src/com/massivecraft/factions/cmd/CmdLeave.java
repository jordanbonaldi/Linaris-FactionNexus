package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Permission;

public class CmdLeave extends FCommand {

    public CmdLeave() {
        super();
        aliases.add("leave");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("", "");

        permission = Permission.LEAVE.node;
        disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = true;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        fme.leave(true);
    }

}

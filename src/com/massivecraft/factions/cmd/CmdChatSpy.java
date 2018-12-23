package com.massivecraft.factions.cmd;

import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;

public class CmdChatSpy extends FCommand {
    public CmdChatSpy() {
        super();
        aliases.add("chatspy");

        optionalArgs.put("on/off", "flip");

        permission = Permission.CHATSPY.node;
        disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        fme.setSpyingChat(this.argAsBool(0, !fme.isSpyingChat()));

        if (fme.isSpyingChat()) {
            fme.msg("<i>You have enabled chat spying mode.");
            P.p.log(fme.getName() + " has ENABLED chat spying mode.");
        } else {
            fme.msg("<i>You have disabled chat spying mode.");
            P.p.log(fme.getName() + " DISABLED chat spying mode.");
        }
    }
}
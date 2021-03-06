package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.struct.Permission;

public class CmdSaveAll extends FCommand {

    public CmdSaveAll() {
        super();
        aliases.add("saveall");
        aliases.add("save");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("", "");

        permission = Permission.SAVE.node;
        disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        FPlayers.i.saveToDisc();
        Factions.i.saveToDisc();
        Board.save();
        Conf.save();
        this.msg("<i>Factions saved to disk!");
    }

}
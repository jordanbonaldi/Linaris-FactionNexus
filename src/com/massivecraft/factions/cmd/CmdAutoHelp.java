package com.massivecraft.factions.cmd;

import java.util.ArrayList;

import com.massivecraft.factions.P;
import com.massivecraft.factions.zcore.CommandVisibility;
import com.massivecraft.factions.zcore.MCommand;

public class CmdAutoHelp extends MCommand<P> {
    public CmdAutoHelp() {
        super(P.p);
        aliases.add("?");
        aliases.add("h");
        aliases.add("help");

        this.setHelpShort("");

        optionalArgs.put("page", "1");
    }

    @Override
    public void perform() {
        if (commandChain.size() == 0) { return; }
        final MCommand<?> pcmd = commandChain.get(commandChain.size() - 1);

        final ArrayList<String> lines = new ArrayList<String>();

        lines.addAll(pcmd.helpLong);

        for (final MCommand<?> scmd : pcmd.subCommands) {
            if (scmd.visibility == CommandVisibility.VISIBLE || scmd.visibility == CommandVisibility.SECRET && scmd.validSenderPermissions(sender, false)) {
                lines.add(scmd.getUseageTemplate(commandChain, true));
            }
        }

        this.sendMessage(p.txt.getPage(lines, this.argAsInt(0, 1), "Help for command \"" + pcmd.aliases.get(0) + "\""));
    }
}

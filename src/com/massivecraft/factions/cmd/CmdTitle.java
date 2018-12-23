package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.integration.SpoutFeatures;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TextUtil;

public class CmdTitle extends FCommand {
    public CmdTitle() {
        aliases.add("title");

        requiredArgs.add("player name");
        optionalArgs.put("title", "");

        permission = Permission.TITLE.node;
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

        args.remove(0);
        final String title = TextUtil.implode(args, " ");

        if (!this.canIAdministerYou(fme, you)) { return; }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!this.payForCommand(Conf.econCostTitle, "to change a players title", "for changing a players title")) { return; }

        you.setTitle(title);

        // Inform
        myFaction.msg("%s<i> changed a title: %s", fme.describeTo(myFaction, true), you.describeTo(myFaction, true));

        if (Conf.spoutFactionTitlesOverNames) {
            SpoutFeatures.updateAppearances(me);
        }
    }

}

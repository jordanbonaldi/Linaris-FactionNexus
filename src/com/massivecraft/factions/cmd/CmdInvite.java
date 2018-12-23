package com.massivecraft.factions.cmd;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.struct.Permission;

public class CmdInvite extends FCommand {
    public CmdInvite() {
        super();
        aliases.add("invite");
        aliases.add("inv");

        requiredArgs.add("player name");
        //this.optionalArgs.put("", "");

        permission = Permission.INVITE.node;
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
            this.msg("<i>You might want to: " + p.cmdBase.cmdKick.getUseageTemplate(false));
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!this.payForCommand(Conf.econCostInvite, "to invite someone", "for inviting someone")) { return; }

        // The Nexus Start
        if (myFaction.getFPlayers().size() >= myFaction.getLevel().getMaxMembers()) {
            fme.getPlayer().sendMessage(ChatColor.RED + "Votre faction a atteint le nombre de joueur maximum...");
            return;
        }
        // The Nexus End

        myFaction.invite(you);

        you.msg("%s<i> invited you to %s", fme.describeTo(you, true), myFaction.describeTo(you));
        myFaction.msg("%s<i> invited %s<i> to your faction.", fme.describeTo(myFaction, true), you.describeTo(myFaction));
    }

}

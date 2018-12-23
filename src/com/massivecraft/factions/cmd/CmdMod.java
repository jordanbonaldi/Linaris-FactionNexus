package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;

public class CmdMod extends FCommand {

    public CmdMod() {
        super();
        aliases.add("mod");

        requiredArgs.add("player name");
        //this.optionalArgs.put("", "");

        permission = Permission.MOD.node;
        disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final FPlayer you = this.argAsBestFPlayerMatch(0);
        if (you == null) { return; }

        final boolean permAny = Permission.MOD_ANY.has(sender, false);
        final Faction targetFaction = you.getFaction();

        if (targetFaction != myFaction && !permAny) {
            this.msg("%s<b> is not a member in your faction.", you.describeTo(fme, true));
            return;
        }

        if (fme != null && fme.getRole() != Role.ADMIN && !permAny) {
            this.msg("<b>You are not the faction admin.");
            return;
        }

        if (you == fme && !permAny) {
            this.msg("<b>The target player musn't be yourself.");
            return;
        }

        if (you.getRole() == Role.ADMIN) {
            this.msg("<b>The target player is a faction admin. Demote them first.");
            return;
        }

        if (you.getRole() == Role.MODERATOR) {
            // Revoke
            you.setRole(Role.NORMAL);
            targetFaction.msg("%s<i> is no longer moderator in your faction.", you.describeTo(targetFaction, true));
            this.msg("<i>You have removed moderator status from %s<i>.", you.describeTo(fme, true));
        } else {
            // Give
            you.setRole(Role.MODERATOR);
            targetFaction.msg("%s<i> was promoted to moderator in your faction.", you.describeTo(targetFaction, true));
            this.msg("<i>You have promoted %s<i> to moderator.", you.describeTo(fme, true));
        }
    }

}

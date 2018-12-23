package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;

public class CmdAutoClaim extends FCommand {
    public CmdAutoClaim() {
        super();
        aliases.add("autoclaim");

        //this.requiredArgs.add("");
        optionalArgs.put("faction", "your");

        permission = Permission.AUTOCLAIM.node;
        disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final Faction forFaction = this.argAsFaction(0, myFaction);
        if (forFaction == null || forFaction == fme.getAutoClaimFor()) {
            fme.setAutoClaimFor(null);
            this.msg("<i>Auto-claiming of land disabled.");
            return;
        }

        if (!fme.canClaimForFaction(forFaction)) {
            if (myFaction == forFaction) {
                this.msg("<b>You must be <h>%s<b> to claim land.", Role.MODERATOR.toString());
            } else {
                this.msg("<b>You can't claim land for <h>%s<b>.", forFaction.describeTo(fme));
            }

            return;
        }

        fme.setAutoClaimFor(forFaction);

        this.msg("<i>Now auto-claiming land for <h>%s<i>.", forFaction.describeTo(fme));
        fme.attemptClaim(forFaction, me.getLocation(), true);
    }

}
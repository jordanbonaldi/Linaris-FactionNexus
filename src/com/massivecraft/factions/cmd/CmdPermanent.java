package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;

public class CmdPermanent extends FCommand {
    public CmdPermanent() {
        super();
        aliases.add("permanent");

        requiredArgs.add("faction tag");
        //this.optionalArgs.put("", "");

        permission = Permission.SET_PERMANENT.node;
        disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final Faction faction = this.argAsFaction(0);
        if (faction == null) { return; }

        String change;
        if (faction.isPermanent()) {
            change = "removed permanent status from";
            faction.setPermanent(false);
        } else {
            change = "added permanent status to";
            faction.setPermanent(true);
        }

        P.p.log((fme == null ? "A server admin" : fme.getName()) + " " + change + " the faction \"" + faction.getTag() + "\".");

        // Inform all players
        for (final FPlayer fplayer : FPlayers.i.getOnline()) {
            if (fplayer.getFaction() == faction) {
                fplayer.msg((fme == null ? "A server admin" : fme.describeTo(fplayer, true)) + "<i> " + change + " your faction.");
            } else {
                fplayer.msg((fme == null ? "A server admin" : fme.describeTo(fplayer, true)) + "<i> " + change + " the faction \"" + faction.getTag(fplayer) + "\".");
            }
        }
    }
}

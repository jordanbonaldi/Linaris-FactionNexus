package com.massivecraft.factions.cmd;

import org.bukkit.Bukkit;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.event.FPlayerJoinEvent;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;

public class CmdAdmin extends FCommand {
    public CmdAdmin() {
        super();
        aliases.add("admin");

        requiredArgs.add("player name");
        //this.optionalArgs.put("", "");

        permission = Permission.ADMIN.node;
        disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final FPlayer fyou = this.argAsBestFPlayerMatch(0);
        if (fyou == null) { return; }

        final boolean permAny = Permission.ADMIN_ANY.has(sender, false);
        final Faction targetFaction = fyou.getFaction();

        if (targetFaction != myFaction && !permAny) {
            this.msg("%s<i> is not a member in your faction.", fyou.describeTo(fme, true));
            return;
        }

        if (fme != null && fme.getRole() != Role.ADMIN && !permAny) {
            this.msg("<b>You are not the faction admin.");
            return;
        }

        if (fyou == fme && !permAny) {
            this.msg("<b>The target player musn't be yourself.");
            return;
        }

        // only perform a FPlayerJoinEvent when newLeader isn't actually in the faction
        if (fyou.getFaction() != targetFaction) {
            final FPlayerJoinEvent event = new FPlayerJoinEvent(FPlayers.i.get(me), targetFaction, FPlayerJoinEvent.PlayerJoinReason.LEADER);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) { return; }
        }

        final FPlayer admin = targetFaction.getFPlayerAdmin();

        // if target player is currently admin, demote and replace him
        if (fyou == admin) {
            targetFaction.promoteNewLeader();
            this.msg("<i>You have demoted %s<i> from the position of faction admin.", fyou.describeTo(fme, true));
            fyou.msg("<i>You have been demoted from the position of faction admin by %s<i>.", senderIsConsole ? "a server admin" : fme.describeTo(fyou, true));
            return;
        }

        // promote target player, and demote existing admin if one exists
        if (admin != null) {
            admin.setRole(Role.MODERATOR);
        }
        fyou.setRole(Role.ADMIN);
        this.msg("<i>You have promoted %s<i> to the position of faction admin.", fyou.describeTo(fme, true));

        // Inform all players
        for (final FPlayer fplayer : FPlayers.i.getOnline()) {
            fplayer.msg("%s<i> gave %s<i> the leadership of %s<i>.", senderIsConsole ? "A server admin" : fme.describeTo(fplayer, true), fyou.describeTo(fplayer), targetFaction.describeTo(fplayer));
        }
    }

}

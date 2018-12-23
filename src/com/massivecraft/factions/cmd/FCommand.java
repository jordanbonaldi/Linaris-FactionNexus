package com.massivecraft.factions.cmd;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.zcore.MCommand;

public abstract class FCommand extends MCommand<P> {
    public boolean disableOnLock;

    public FPlayer fme;
    public Faction myFaction;
    public boolean senderMustBeMember;
    public boolean senderMustBeModerator;
    public boolean senderMustBeAdmin;

    public boolean isMoneyCommand;

    public FCommand() {
        super(P.p);

        // Due to safety reasons it defaults to disable on lock.
        disableOnLock = true;

        // The money commands must be disabled if money should not be used.
        isMoneyCommand = false;

        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void execute(final CommandSender sender, final List<String> args, final List<MCommand<?>> commandChain) {
        if (sender instanceof Player) {
            fme = FPlayers.i.get((Player) sender);
            myFaction = fme.getFaction();
        } else {
            fme = null;
            myFaction = null;
        }
        super.execute(sender, args, commandChain);
    }

    @Override
    public boolean isEnabled() {
        if (p.getLocked() && disableOnLock) {
            this.msg("<b>Factions was locked by an admin. Please try again later.");
            return false;
        }

        if (isMoneyCommand && !Conf.econEnabled) {
            this.msg("<b>Faction economy features are disabled on this server.");
            return false;
        }

        if (isMoneyCommand && !Conf.bankEnabled) {
            this.msg("<b>The faction bank system is disabled on this server.");
            return false;
        }

        return true;
    }

    @Override
    public boolean validSenderType(final CommandSender sender, final boolean informSenderIfNot) {
        final boolean superValid = super.validSenderType(sender, informSenderIfNot);
        if (!superValid) { return false; }

        if (!(senderMustBeMember || senderMustBeModerator || senderMustBeAdmin)) { return true; }

        if (!(sender instanceof Player)) { return false; }

        final FPlayer fplayer = FPlayers.i.get((Player) sender);

        if (!fplayer.hasFaction()) {
            sender.sendMessage(p.txt.parse("<b>You are not member of any faction."));
            return false;
        }

        if (senderMustBeModerator && !fplayer.getRole().isAtLeast(Role.MODERATOR)) {
            sender.sendMessage(p.txt.parse("<b>Only faction moderators can %s.", this.getHelpShort()));
            return false;
        }

        if (senderMustBeAdmin && !fplayer.getRole().isAtLeast(Role.ADMIN)) {
            sender.sendMessage(p.txt.parse("<b>Only faction admins can %s.", this.getHelpShort()));
            return false;
        }

        return true;
    }

    // -------------------------------------------- //
    // Assertions
    // -------------------------------------------- //

    public boolean assertHasFaction() {
        if (me == null) { return true; }

        if (!fme.hasFaction()) {
            this.sendMessage("You are not member of any faction.");
            return false;
        }
        return true;
    }

    public boolean assertMinRole(final Role role) {
        if (me == null) { return true; }

        if (fme.getRole().value < role.value) {
            this.msg("<b>You <h>must be " + role + "<b> to " + this.getHelpShort() + ".");
            return false;
        }
        return true;
    }

    // -------------------------------------------- //
    // Argument Readers
    // -------------------------------------------- //

    // FPLAYER ======================
    public FPlayer strAsFPlayer(final String name, final FPlayer def, final boolean msg) {
        FPlayer ret = def;

        if (name != null) {
            final FPlayer fplayer = FPlayers.i.get(name);
            if (fplayer != null) {
                ret = fplayer;
            }
        }

        if (msg && ret == null) {
            this.msg("<b>No player \"<p>%s<b>\" could be found.", name);
        }

        return ret;
    }

    public FPlayer argAsFPlayer(final int idx, final FPlayer def, final boolean msg) {
        return this.strAsFPlayer(this.argAsString(idx), def, msg);
    }

    public FPlayer argAsFPlayer(final int idx, final FPlayer def) {
        return this.argAsFPlayer(idx, def, true);
    }

    public FPlayer argAsFPlayer(final int idx) {
        return this.argAsFPlayer(idx, null);
    }

    // BEST FPLAYER MATCH ======================
    public FPlayer strAsBestFPlayerMatch(final String name, final FPlayer def, final boolean msg) {
        FPlayer ret = def;

        if (name != null) {
            final FPlayer fplayer = FPlayers.i.getBestIdMatch(name);
            if (fplayer != null) {
                ret = fplayer;
            }
        }

        if (msg && ret == null) {
            this.msg("<b>No player match found for \"<p>%s<b>\".", name);
        }

        return ret;
    }

    public FPlayer argAsBestFPlayerMatch(final int idx, final FPlayer def, final boolean msg) {
        return this.strAsBestFPlayerMatch(this.argAsString(idx), def, msg);
    }

    public FPlayer argAsBestFPlayerMatch(final int idx, final FPlayer def) {
        return this.argAsBestFPlayerMatch(idx, def, true);
    }

    public FPlayer argAsBestFPlayerMatch(final int idx) {
        return this.argAsBestFPlayerMatch(idx, null);
    }

    // FACTION ======================
    public Faction strAsFaction(final String name, final Faction def, final boolean msg) {
        Faction ret = def;

        if (name != null) {
            Faction faction = null;

            // First we try an exact match
            if (faction == null) {
                faction = Factions.i.getByTag(name);
            }

            // Next we match faction tags
            if (faction == null) {
                faction = Factions.i.getBestTagMatch(name);
            }

            // Next we match player names
            if (faction == null) {
                final FPlayer fplayer = FPlayers.i.getBestIdMatch(name);
                if (fplayer != null) {
                    faction = fplayer.getFaction();
                }
            }

            if (faction != null) {
                ret = faction;
            }
        }

        if (msg && ret == null) {
            this.msg("<b>The faction or player \"<p>%s<b>\" could not be found.", name);
        }

        return ret;
    }

    public Faction argAsFaction(final int idx, final Faction def, final boolean msg) {
        return this.strAsFaction(this.argAsString(idx), def, msg);
    }

    public Faction argAsFaction(final int idx, final Faction def) {
        return this.argAsFaction(idx, def, true);
    }

    public Faction argAsFaction(final int idx) {
        return this.argAsFaction(idx, null);
    }

    // -------------------------------------------- //
    // Commonly used logic
    // -------------------------------------------- //

    public boolean canIAdministerYou(final FPlayer i, final FPlayer you) {
        if (!i.getFaction().equals(you.getFaction())) {
            i.sendMessage(p.txt.parse("%s <b>is not in the same faction as you.", you.describeTo(i, true)));
            return false;
        }

        if (i.getRole().value > you.getRole().value || i.getRole().equals(Role.ADMIN)) { return true; }

        if (you.getRole().equals(Role.ADMIN)) {
            i.sendMessage(p.txt.parse("<b>Only the faction admin can do that."));
        } else if (i.getRole().equals(Role.MODERATOR)) {
            if (i == you) {
                return true; //Moderators can control themselves
            } else {
                i.sendMessage(p.txt.parse("<b>Moderators can't control each other..."));
            }
        } else {
            i.sendMessage(p.txt.parse("<b>You must be a faction moderator to do that."));
        }

        return false;
    }

    // if economy is enabled and they're not on the bypass list, make 'em pay; returns true unless person can't afford the cost
    public boolean payForCommand(final double cost, final String toDoThis, final String forDoingThis) {
        if (!Econ.shouldBeUsed() || fme == null || cost == 0.0 || fme.isAdminBypassing()) { return true; }

        if (Conf.bankEnabled && Conf.bankFactionPaysCosts && fme.hasFaction()) {
            return Econ.modifyMoney(myFaction, -cost, toDoThis, forDoingThis);
        } else {
            return Econ.modifyMoney(fme, -cost, toDoThis, forDoingThis);
        }
    }

    // like above, but just make sure they can pay; returns true unless person can't afford the cost
    public boolean canAffordCommand(final double cost, final String toDoThis) {
        if (!Econ.shouldBeUsed() || fme == null || cost == 0.0 || fme.isAdminBypassing()) { return true; }

        if (Conf.bankEnabled && Conf.bankFactionPaysCosts && fme.hasFaction()) {
            return Econ.hasAtLeast(myFaction, cost, toDoThis);
        } else {
            return Econ.hasAtLeast(fme, cost, toDoThis);
        }
    }
}

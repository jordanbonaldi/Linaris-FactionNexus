package com.massivecraft.factions.cmd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.event.FactionRelationEvent;
import com.massivecraft.factions.integration.SpoutFeatures;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;

public abstract class FRelationCommand extends FCommand {
    public Relation targetRelation;

    public FRelationCommand() {
        super();
        requiredArgs.add("faction tag");
        //this.optionalArgs.put("player name", "you");

        permission = Permission.RELATION.node;
        disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final Faction them = this.argAsFaction(0);
        if (them == null) { return; }

        if (!them.isNormal()) {
            this.msg("<b>Nope! You can't.");
            return;
        }

        if (them == myFaction) {
            this.msg("<b>Nope! You can't declare a relation to yourself :)");
            return;
        }

        if (myFaction.getRelationWish(them) == targetRelation) {
            this.msg("<b>You already have that relation wish set with %s.", them.getTag());
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!this.payForCommand(targetRelation.getRelationCost(), "to change a relation wish", "for changing a relation wish")) { return; }

        // try to set the new relation
        final Relation oldRelation = myFaction.getRelationTo(them, true);
        myFaction.setRelationWish(them, targetRelation);
        final Relation currentRelation = myFaction.getRelationTo(them, true);
        final ChatColor currentRelationColor = currentRelation.getColor();

        // if the relation change was successful
        if (targetRelation.value == currentRelation.value) {
            // trigger the faction relation event
            final FactionRelationEvent relationEvent = new FactionRelationEvent(myFaction, them, oldRelation, currentRelation);
            Bukkit.getServer().getPluginManager().callEvent(relationEvent);

            them.msg("<i>Your faction is now " + currentRelationColor + targetRelation.toString() + "<i> to " + currentRelationColor + myFaction.getTag());
            myFaction.msg("<i>Your faction is now " + currentRelationColor + targetRelation.toString() + "<i> to " + currentRelationColor + them.getTag());
        }
        // inform the other faction of your request
        else {
            them.msg(currentRelationColor + myFaction.getTag() + "<i> wishes to be your " + targetRelation.getColor() + targetRelation.toString());
            them.msg("<i>Type <c>/" + Conf.baseCommandAliases.get(0) + " " + targetRelation + " " + myFaction.getTag() + "<i> to accept.");
            myFaction.msg(currentRelationColor + them.getTag() + "<i> were informed that you wish to be " + targetRelation.getColor() + targetRelation);
        }

        if (!targetRelation.isNeutral() && them.isPeaceful()) {
            them.msg("<i>This will have no effect while your faction is peaceful.");
            myFaction.msg("<i>This will have no effect while their faction is peaceful.");
        }

        if (!targetRelation.isNeutral() && myFaction.isPeaceful()) {
            them.msg("<i>This will have no effect while their faction is peaceful.");
            myFaction.msg("<i>This will have no effect while your faction is peaceful.");
        }

        SpoutFeatures.updateAppearances(myFaction, them);
        SpoutFeatures.updateTerritoryDisplayLoc(null);
    }
}

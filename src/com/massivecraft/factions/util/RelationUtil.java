package com.massivecraft.factions.util;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.zcore.util.TextUtil;

public class RelationUtil {
    public static String describeThatToMe(final RelationParticipator that, final RelationParticipator me, final boolean ucfirst) {
        String ret = "";

        final Faction thatFaction = RelationUtil.getFaction(that);
        if (thatFaction == null) { return "ERROR"; // ERROR
        }

        final Faction myFaction = RelationUtil.getFaction(me);
        //		if (myFaction == null) return that.describeTo(null); // no relation, but can show basic name or tag

        if (that instanceof Faction) {
            if (me instanceof FPlayer && myFaction == thatFaction) {
                ret = "your faction";
            } else {
                ret = thatFaction.getTag();
            }
        } else if (that instanceof FPlayer) {
            final FPlayer fplayerthat = (FPlayer) that;
            if (that == me) {
                ret = "you";
            } else if (thatFaction == myFaction) {
                ret = fplayerthat.getNameAndTitle();
            } else {
                ret = fplayerthat.getNameAndTag();
            }
        }

        if (ucfirst) {
            ret = TextUtil.upperCaseFirst(ret);
        }

        return "" + RelationUtil.getColorOfThatToMe(that, me) + ret;
    }

    public static String describeThatToMe(final RelationParticipator that, final RelationParticipator me) {
        return RelationUtil.describeThatToMe(that, me, false);
    }

    public static Relation getRelationTo(final RelationParticipator me, final RelationParticipator that) {
        return RelationUtil.getRelationTo(that, me, false);
    }

    public static Relation getRelationTo(final RelationParticipator me, final RelationParticipator that, final boolean ignorePeaceful) {
        final Faction fthat = RelationUtil.getFaction(that);
        if (fthat == null) { return Relation.NEUTRAL; // ERROR
        }

        final Faction fme = RelationUtil.getFaction(me);
        if (fme == null) { return Relation.NEUTRAL; // ERROR
        }

        if (!fthat.isNormal() || !fme.isNormal()) { return Relation.NEUTRAL; }

        if (fthat.equals(fme)) { return Relation.MEMBER; }

        if (!ignorePeaceful && (fme.isPeaceful() || fthat.isPeaceful())) { return Relation.NEUTRAL; }

        if (fme.getRelationWish(fthat).value >= fthat.getRelationWish(fme).value) { return fthat.getRelationWish(fme); }

        return fme.getRelationWish(fthat);
    }

    public static Faction getFaction(final RelationParticipator rp) {
        if (rp instanceof Faction) { return (Faction) rp; }

        if (rp instanceof FPlayer) { return ((FPlayer) rp).getFaction(); }

        // ERROR
        return null;
    }

    public static ChatColor getColorOfThatToMe(final RelationParticipator that, final RelationParticipator me) {
        final Faction thatFaction = RelationUtil.getFaction(that);
        if (thatFaction != null) {
            if (thatFaction.isPeaceful() && thatFaction != RelationUtil.getFaction(me)) { return Conf.colorPeaceful; }

            if (thatFaction.isSafeZone() && thatFaction != RelationUtil.getFaction(me)) { return Conf.colorPeaceful; }

            if (thatFaction.isWarZone() && thatFaction != RelationUtil.getFaction(me)) { return Conf.colorWar; }
        }

        return RelationUtil.getRelationTo(that, me).getColor();
    }
}

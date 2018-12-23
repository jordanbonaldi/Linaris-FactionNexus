package com.massivecraft.factions.iface;

import org.bukkit.ChatColor;

import com.massivecraft.factions.struct.Relation;

public interface RelationParticipator {
    public String describeTo(final RelationParticipator that);

    public String describeTo(final RelationParticipator that, final boolean ucfirst);

    public Relation getRelationTo(final RelationParticipator that);

    public Relation getRelationTo(final RelationParticipator that, final boolean ignorePeaceful);

    public ChatColor getColorTo(final RelationParticipator to);
}

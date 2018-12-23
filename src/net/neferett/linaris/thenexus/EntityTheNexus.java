package net.neferett.linaris.thenexus;

import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityCreeper;
import net.minecraft.server.v1_7_R4.EntityEnderCrystal;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EntityTNTPrimed;
import net.minecraft.server.v1_7_R4.World;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;

public class EntityTheNexus extends EntityEnderCrystal {
    private String factionId;
    private Hologram hologram;

    public EntityTheNexus(final World world) {
        super(world);
    }

    public EntityTheNexus(final World world, final String factionId, final Hologram hologram) {
        super(world);
        this.factionId = factionId;
        this.hologram = hologram;
    }

    public Hologram getHologram() {
        return hologram;
    }

    @Override
    public void h() {}

    @Override
    public boolean damageEntity(final DamageSource damagesource, final float f) {
        try {
            if (factionId == null) { return false; }
            final Faction faction = Factions.i.getBestIdMatch(factionId);
            if (faction == null) { return false; }
            final Entity damager = damagesource.i();
            if (damager == null || damager instanceof EntityTNTPrimed) {
                final TNTPrimed primed = damager != null ? (TNTPrimed) ((EntityTNTPrimed) damager).getBukkitEntity() : null;
                faction.handleDamage(50, primed != null && primed.getSource() != null && primed.getSource() instanceof Player ? (Player) primed.getSource() : null);
            } else if (damager != null && damager instanceof EntityCreeper) {
                final Creeper creeper = (Creeper) damager.getBukkitEntity();
                if (!creeper.hasMetadata("nexus_owner")) { return false; }
                final Player player = Bukkit.getPlayer(creeper.getMetadata("nexus_owner").get(0).asString());
                creeper.removeMetadata("nexus_owner", com.massivecraft.factions.P.p);
                if (player == null || !player.isOnline()) { return false; }
                faction.handleDamage(20, player);
            } else if (damager != null && damager instanceof EntityPlayer) {
                final Player player = ((EntityPlayer) damager).getBukkitEntity().getPlayer();
                if (player.getItemInHand() != null && player.getItemInHand().getType() != null && player.getItemInHand().getType().name().contains("SWORD") && player.getItemInHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL) >= 3 && player.getItemInHand().hasItemMeta() && player.getItemInHand().getItemMeta().hasDisplayName() && player.getItemInHand().getItemMeta().getDisplayName().contains(faction.getTag())) {
                    faction.handleDamage(10, player);
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

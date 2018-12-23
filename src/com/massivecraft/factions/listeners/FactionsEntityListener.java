package com.massivecraft.factions.listeners;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.confuser.barapi.BarAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.event.PowerLossEvent;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.MiscUtil;

public class FactionsEntityListener implements Listener {
    public P p;

    public FactionsEntityListener(final P p) {
        this.p = p;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(final EntityDeathEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) { return; }

        final Player player = (Player) entity;
        final FPlayer fplayer = FPlayers.i.get(player);
        final Faction faction = Board.getFactionAt(new FLocation(player.getLocation()));

        final PowerLossEvent powerLossEvent = new PowerLossEvent(faction, fplayer);
        // Check for no power loss conditions
        if (faction.isWarZone()) {
            // war zones always override worldsNoPowerLoss either way, thus this layout
            if (!Conf.warZonePowerLoss) {
                powerLossEvent.setMessage("<i>You didn't lose any power since you were in a war zone.");
                powerLossEvent.setCancelled(true);
            }
            if (Conf.worldsNoPowerLoss.contains(player.getWorld().getName())) {
                powerLossEvent.setMessage("<b>The world you are in has power loss normally disabled, but you still lost power since you were in a war zone.\n<i>Your power is now <h>%d / %d");
            }
        } else if (faction.isNone() && !Conf.wildernessPowerLoss && !Conf.worldsNoWildernessProtection.contains(player.getWorld().getName())) {
            powerLossEvent.setMessage("<i>You didn't lose any power since you were in the wilderness.");
            powerLossEvent.setCancelled(true);
        } else if (Conf.worldsNoPowerLoss.contains(player.getWorld().getName())) {
            powerLossEvent.setMessage("<i>You didn't lose any power due to the world you died in.");
            powerLossEvent.setCancelled(true);
        } else if (Conf.peacefulMembersDisablePowerLoss && fplayer.hasFaction() && fplayer.getFaction().isPeaceful()) {
            powerLossEvent.setMessage("<i>You didn't lose any power since you are in a peaceful faction.");
            powerLossEvent.setCancelled(true);
        } else {
            powerLossEvent.setMessage("<i>Your power is now <h>%d / %d");
        }

        // The Nexus Start
        fplayer.incrementDeaths();
        if (player.getKiller() != null) {
            final FPlayer fkiller = FPlayers.i.get(player.getKiller());
            fkiller.incrementKills();
        }
        // The Nexus End

        // call Event
        Bukkit.getPluginManager().callEvent(powerLossEvent);

        // Call player onDeath if the event is not cancelled
        if (!powerLossEvent.isCancelled()) {
            fplayer.onDeath();
        }
        // Send the message from the powerLossEvent
        final String msg = powerLossEvent.getMessage();
        if (msg != null && !msg.isEmpty()) {
            fplayer.msg(msg, fplayer.getPowerRounded(), fplayer.getPowerMaxRounded());
        }
    }

    /**
     * Who can I hurt?
     * I can never hurt members or allies.
     * I can always hurt enemies.
     * I can hurt neutrals as long as they are outside their own territory.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    // The Nexus: NORMAL to HIGHEST
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.isCancelled()) { return; }

        if (event instanceof EntityDamageByEntityEvent) {
            final EntityDamageByEntityEvent sub = (EntityDamageByEntityEvent) event;
            if (!this.canDamagerHurtDamagee(sub, true)) {
                event.setCancelled(true);
            }
            // The Nexus Start
            else if (event.getEntity() instanceof Player && sub.getDamager() instanceof Player) {
                final Player player = (Player) event.getEntity();
                final Player damager = (Player) sub.getDamager();
                final Damageable playerDmg = player;
                final Damageable damagerDmg = damager;
                final String message = ChatColor.GRAY + "Joueur : " + ChatColor.AQUA + player.getName() + ChatColor.DARK_PURPLE + " | " + ChatColor.GRAY + "Vie du joueur : " + (playerDmg.getHealth() <= 5 ? ChatColor.DARK_RED : playerDmg.getHealth() <= 10 ? ChatColor.RED : playerDmg.getHealth() <= 15 ? ChatColor.GOLD : ChatColor.GREEN) + (int)(playerDmg.getHealth()) + ChatColor.AQUA + "/" + playerDmg.getMaxHealth();
                final String playerMsg = ChatColor.GRAY + "Joueur : " + ChatColor.AQUA + damager.getName() + ChatColor.DARK_PURPLE + " | " + ChatColor.GRAY + "Vie du joueur : " + (damagerDmg.getHealth() <= 5 ? ChatColor.DARK_RED : damagerDmg.getHealth() <= 10 ? ChatColor.RED : damagerDmg.getHealth() <= 15 ? ChatColor.GOLD : ChatColor.GREEN) + (int)(damagerDmg.getHealth()) + ChatColor.AQUA + "/" + damagerDmg.getMaxHealth();
                BarAPI.setMessage(player, playerMsg, (float) (damager.getHealthScale() / 20 * 100));
                BarAPI.setMessage(damager, message, (float) (player.getHealthScale() / 20 * 100));
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        if (BarAPI.getMessage(player).equals(playerMsg)) {
                            BarAPI.removeBar(player);
                        }
                        if (BarAPI.getMessage(damager).equals(message)) {
                            BarAPI.removeBar(damager);
                        }
                    }
                }.runTaskLater(P.p, 200);
            }
            // The Nexus End
        } else if (Conf.safeZonePreventAllDamageToPlayers && this.isPlayerInSafeZone(event.getEntity())) {
            // Players can not take any damage in a Safe Zone
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(final EntityExplodeEvent event) {
        if (event.isCancelled()) { return; }

        final Location loc = event.getLocation();
        final Entity boomer = event.getEntity();
        final Faction faction = Board.getFactionAt(new FLocation(loc));

        if (faction.noExplosionsInTerritory()) {
            // faction is peaceful and has explosions set to disabled
            event.setCancelled(true);
            return;
        }

        final boolean online = faction.hasPlayersOnline();

        if (boomer instanceof Creeper && (faction.isNone() && Conf.wildernessBlockCreepers && !Conf.worldsNoWildernessProtection.contains(loc.getWorld().getName()) || faction.isNormal() && (online ? Conf.territoryBlockCreepers : Conf.territoryBlockCreepersWhenOffline) || faction.isWarZone() && Conf.warZoneBlockCreepers || faction.isSafeZone())) {
            // creeper which needs prevention
            event.setCancelled(true);
        } else if (
        // it's a bit crude just using fireball protection for Wither boss too, but I'd rather not add in a whole new set of xxxBlockWitherExplosion or whatever
        (boomer instanceof Fireball || boomer instanceof WitherSkull || boomer instanceof Wither) && (faction.isNone() && Conf.wildernessBlockFireballs && !Conf.worldsNoWildernessProtection.contains(loc.getWorld().getName()) || faction.isNormal() && (online ? Conf.territoryBlockFireballs : Conf.territoryBlockFireballsWhenOffline) || faction.isWarZone() && Conf.warZoneBlockFireballs || faction.isSafeZone())) {
            // ghast fireball which needs prevention
            event.setCancelled(true);
        } else if ((boomer instanceof TNTPrimed || boomer instanceof ExplosiveMinecart) && (faction.isNone() && Conf.wildernessBlockTNT && !Conf.worldsNoWildernessProtection.contains(loc.getWorld().getName()) || faction.isNormal() && (online ? Conf.territoryBlockTNT : Conf.territoryBlockTNTWhenOffline) || faction.isWarZone() && Conf.warZoneBlockTNT || faction.isSafeZone() && Conf.safeZoneBlockTNT)) {
            // TNT which needs prevention
            event.setCancelled(true);
        } else if ((boomer instanceof TNTPrimed || boomer instanceof ExplosiveMinecart) && Conf.handleExploitTNTWaterlog) {
            // TNT in water/lava doesn't normally destroy any surrounding blocks, which is usually desired behavior, but...
            // this change below provides workaround for waterwalling providing perfect protection,
            // and makes cheap (non-obsidian) TNT cannons require minor maintenance between shots
            final Block center = loc.getBlock();
            if (center.isLiquid()) {
                // a single surrounding block in all 6 directions is broken if the material is weak enough
                final List<Block> targets = new ArrayList<Block>();
                targets.add(center.getRelative(0, 0, 1));
                targets.add(center.getRelative(0, 0, -1));
                targets.add(center.getRelative(0, 1, 0));
                targets.add(center.getRelative(0, -1, 0));
                targets.add(center.getRelative(1, 0, 0));
                targets.add(center.getRelative(-1, 0, 0));
                for (final Block target : targets) {
                    final int id = target.getTypeId();
                    // ignore air, bedrock, water, lava, obsidian, enchanting table, etc.... too bad we can't get a blast resistance value through Bukkit yet
                    if (id != 0 && (id < 7 || id > 11) && id != 49 && id != 90 && id != 116 && id != 119 && id != 120 && id != 130) {
                        target.breakNaturally();
                    }
                }
            }
        }
        // The Nexus Start
        if (boomer instanceof TNTPrimed || boomer instanceof Creeper) {
            for (int x = -3; x <= 3; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -3; z <= 3; z++) {
                        final Block block = loc.clone().add(x, y, z).getBlock();
                        if (block.getType() == Material.OBSIDIAN) {
                            final Location blockLoc = block.getLocation();
                            final Faction otherFaction = Board.getFactionAt(new FLocation(blockLoc));
                            if (otherFaction.isNormal() && otherFaction.hasHome() && !FactionsBlockListener.isInRange(otherFaction.getHome(), blockLoc, 4)) {
                                int explosions = block.hasMetadata("nexus_explosions") ? block.getMetadata("nexus_explosions").get(0).asInt() : 0;
                                block.removeMetadata("nexus_explosions", P.p);
                                if (++explosions >= otherFaction.getLevel().getExplosions()) {
                                    block.setType(Material.AIR);
                                } else {
                                    block.setMetadata("nexus_explosions", new FixedMetadataValue(P.p, explosions));
                                }
                            }
                        }
                    }
                }
            }
        }
        // The Nexus End
    }

    // mainly for flaming arrows; don't want allies or people in safe zones to be ignited even after damage event is cancelled
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityCombustByEntity(final EntityCombustByEntityEvent event) {
        if (event.isCancelled()) { return; }

        EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(event.getCombuster(), event.getEntity(), EntityDamageEvent.DamageCause.FIRE, 0);
        if (!this.canDamagerHurtDamagee(sub, false)) {
            event.setCancelled(true);
        }
        sub = null;
    }

    private static final Set<PotionEffectType> badPotionEffects = new LinkedHashSet<PotionEffectType>(Arrays.asList(PotionEffectType.BLINDNESS, PotionEffectType.CONFUSION, PotionEffectType.HARM, PotionEffectType.HUNGER, PotionEffectType.POISON, PotionEffectType.SLOW, PotionEffectType.SLOW_DIGGING, PotionEffectType.WEAKNESS, PotionEffectType.WITHER));

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPotionSplashEvent(final PotionSplashEvent event) {
        if (event.isCancelled()) { return; }

        // The Nexus Start
        if (event.getEntity().getShooter() instanceof Player && !event.getPotion().getEffects().isEmpty()) {
            final PotionEffect potionEffect = event.getPotion().getEffects().iterator().next();
            final Player player = (Player) event.getEntity().getShooter();
            final Faction faction = FPlayers.i.get(player).getFaction();
            if (potionEffect.getType().getName().equals("REGENERATION") && faction.isNormal() && faction.hasHome() && faction.getNexus().getBukkitEntity().getLocation().distanceSquared(event.getPotion().getLocation()) <= 5) {
                if ((System.currentTimeMillis() - faction.getLastBrokenNexus()) / 1000 < 300) {
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
                    player.sendMessage(ChatColor.RED + "Vous devez attendre 5 minutes après un pillage pour régénérer votre coeur.");
                    return;
                }
                faction.regenHealth(10 * (potionEffect.getAmplifier() + 1));
                for (final LivingEntity entity : event.getAffectedEntities()) {
                    event.setIntensity(entity, 0.0D);
                }
            }
            return;
        }
        // The Nexus End

        // see if the potion has a harmful effect
        boolean badjuju = false;
        for (final PotionEffect effect : event.getPotion().getEffects()) {
            if (FactionsEntityListener.badPotionEffects.contains(effect.getType())) {
                badjuju = true;
                break;
            }
        }
        if (!badjuju) { return; }

        final Entity thrower = event.getPotion().getShooter();

        // scan through affected entities to make sure they're all valid targets
        final Iterator<LivingEntity> iter = event.getAffectedEntities().iterator();
        while (iter.hasNext()) {
            final LivingEntity target = iter.next();
            EntityDamageByEntityEvent sub = new EntityDamageByEntityEvent(thrower, target, EntityDamageEvent.DamageCause.CUSTOM, 0);
            if (!this.canDamagerHurtDamagee(sub, true)) {
                event.setIntensity(target, 0.0); // affected entity list doesn't accept modification (so no iter.remove()), but this works
            }
            sub = null;
        }
    }

    public boolean isPlayerInSafeZone(final Entity damagee) {
        if (!(damagee instanceof Player)) { return false; }
        if (Board.getFactionAt(new FLocation(damagee.getLocation())).isSafeZone()) { return true; }
        return false;
    }

    public boolean canDamagerHurtDamagee(final EntityDamageByEntityEvent sub) {
        return this.canDamagerHurtDamagee(sub, true);
    }

    public boolean canDamagerHurtDamagee(final EntityDamageByEntityEvent sub, final boolean notify) {
        Entity damager = sub.getDamager();
        final Entity damagee = sub.getEntity();
        final double damage = sub.getDamage(); // The Nexus

        if (!(damagee instanceof Player)) { return true; }

        final FPlayer defender = FPlayers.i.get((Player) damagee);

        if (defender == null || defender.getPlayer() == null) { return true; }

        final Location defenderLoc = defender.getPlayer().getLocation();
        final Faction defLocFaction = Board.getFactionAt(new FLocation(defenderLoc));

        // for damage caused by projectiles, getDamager() returns the projectile... what we need to know is the source
        if (damager instanceof Projectile) {
            damager = ((Projectile) damager).getShooter();
        }

        if (damager == damagee) { return true; }

        // Players can not take attack damage in a SafeZone, or possibly peaceful territory
        if (defLocFaction.noPvPInTerritory()) {
            if (damager instanceof Player) {
                if (notify) {
                    final FPlayer attacker = FPlayers.i.get((Player) damager);
                    attacker.msg("<i>You can't hurt other players in " + (defLocFaction.isSafeZone() ? "a SafeZone." : "peaceful territory."));
                }
                return false;
            }
            return !defLocFaction.noMonstersInTerritory();
        }

        if (!(damager instanceof Player)) { return true; }

        final FPlayer attacker = FPlayers.i.get((Player) damager);

        if (attacker == null || attacker.getPlayer() == null) { return true; }

        if (Conf.playersWhoBypassAllProtection.contains(attacker.getName())) { return true; }

        if (attacker.hasLoginPvpDisabled()) {
            if (notify) {
                attacker.msg("<i>You can't hurt other players for " + Conf.noPVPDamageToOthersForXSecondsAfterLogin + " seconds after logging in.");
            }
            return false;
        }

        final Faction locFaction = Board.getFactionAt(new FLocation(attacker));

        // so we know from above that the defender isn't in a safezone... what about the attacker, sneaky dog that he might be?
        if (locFaction.noPvPInTerritory()) {
            if (notify) {
                attacker.msg("<i>You can't hurt other players while you are in " + (locFaction.isSafeZone() ? "a SafeZone." : "peaceful territory."));
            }
            return false;
        }

        if (locFaction.isWarZone() && Conf.warZoneFriendlyFire) { return true; }

        if (Conf.worldsIgnorePvP.contains(defenderLoc.getWorld().getName())) { return true; }

        final Faction defendFaction = defender.getFaction();
        final Faction attackFaction = attacker.getFaction();

        if (attackFaction.isNone() && Conf.disablePVPForFactionlessPlayers) {
            if (notify) {
                attacker.msg("<i>You can't hurt other players until you join a faction.");
            }
            return false;
        } else if (defendFaction.isNone()) {
            if (defLocFaction == attackFaction && Conf.enablePVPAgainstFactionlessInAttackersLand) {
                return true;
            } else if (Conf.disablePVPForFactionlessPlayers) {
                if (notify) {
                    attacker.msg("<i>You can't hurt players who are not currently in a faction.");
                }
                return false;
            }
        }

        if (defendFaction.isPeaceful()) {
            if (notify) {
                attacker.msg("<i>You can't hurt players who are in a peaceful faction.");
            }
            return false;
        } else if (attackFaction.isPeaceful()) {
            if (notify) {
                attacker.msg("<i>You can't hurt players while you are in a peaceful faction.");
            }
            return false;
        }

        final Relation relation = defendFaction.getRelationTo(attackFaction);

        // You can not hurt neutral factions
        if (Conf.disablePVPBetweenNeutralFactions && relation.isNeutral()) {
            if (notify) {
                attacker.msg("<i>You can't hurt neutral factions. Declare them as an enemy.");
            }
            return false;
        }

        // Players without faction may be hurt anywhere
        if (!defender.hasFaction()) { return true; }

        // You can never hurt faction members or allies
        if (relation.isMember() || relation.isAlly()) {
            if (notify) {
                attacker.msg("<i>You can't hurt %s<i>.", defender.describeTo(attacker));
            }
            return false;
        }

        final boolean ownTerritory = defender.isInOwnTerritory();

        // You can not hurt neutrals in their own territory.
        if (ownTerritory && relation.isNeutral()) {
            if (notify) {
                attacker.msg("<i>You can't hurt %s<i> in their own territory unless you declare them as an enemy.", defender.describeTo(attacker));
                defender.msg("%s<i> tried to hurt you.", attacker.describeTo(defender, true));
            }
            return false;
        }

        // Damage will be dealt. However check if the damage should be reduced.
        if (damage > 0.0 && ownTerritory && Conf.territoryShieldFactor > 0) {
            final int newDamage = (int) Math.ceil(damage * (1D - Conf.territoryShieldFactor));
            sub.setDamage(newDamage);

            // Send message
            if (notify) {
                final String perc = MessageFormat.format("{0,number,#%}", Conf.territoryShieldFactor); // TODO does this display correctly??
                defender.msg("<i>Enemy damage reduced by <rose>%s<i>.", perc);
            }
        }

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        if (event.isCancelled() || event.getLocation() == null) { return; }

        if (Conf.safeZoneNerfedCreatureTypes.contains(event.getEntityType()) && Board.getFactionAt(new FLocation(event.getLocation())).noMonstersInTerritory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTarget(final EntityTargetEvent event) {
        if (event.isCancelled()) { return; }

        // if there is a target
        final Entity target = event.getTarget();
        if (target == null) { return; }

        // We are interested in blocking targeting for certain mobs:
        if (!Conf.safeZoneNerfedCreatureTypes.contains(MiscUtil.creatureTypeFromEntity(event.getEntity()))) { return; }

        // in case the target is in a safe zone.
        if (Board.getFactionAt(new FLocation(target.getLocation())).noMonstersInTerritory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPaintingBreak(final HangingBreakEvent event) {
        if (event.isCancelled()) { return; }
        if (event.getCause() == RemoveCause.EXPLOSION) {
            final Location loc = event.getEntity().getLocation();
            final Faction faction = Board.getFactionAt(new FLocation(loc));
            if (faction.noExplosionsInTerritory()) {
                // faction is peaceful and has explosions set to disabled
                event.setCancelled(true);
                return;
            }

            final boolean online = faction.hasPlayersOnline();

            if (faction.isNone() && !Conf.worldsNoWildernessProtection.contains(loc.getWorld().getName()) && (Conf.wildernessBlockCreepers || Conf.wildernessBlockFireballs || Conf.wildernessBlockTNT) || faction.isNormal() && (online ? Conf.territoryBlockCreepers || Conf.territoryBlockFireballs || Conf.territoryBlockTNT : Conf.territoryBlockCreepersWhenOffline || Conf.territoryBlockFireballsWhenOffline || Conf.territoryBlockTNTWhenOffline) || faction.isWarZone() && (Conf.warZoneBlockCreepers || Conf.warZoneBlockFireballs || Conf.warZoneBlockTNT) || faction.isSafeZone()) {
                // explosion which needs prevention
                event.setCancelled(true);
            }
        }

        if (!(event instanceof HangingBreakByEntityEvent)) { return; }

        final Entity breaker = ((HangingBreakByEntityEvent) event).getRemover();
        if (!(breaker instanceof Player)) { return; }

        if (!FactionsBlockListener.playerCanBuildDestroyBlock((Player) breaker, event.getEntity().getLocation(), "remove paintings", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPaintingPlace(final HangingPlaceEvent event) {
        if (event.isCancelled()) { return; }

        if (!FactionsBlockListener.playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "place paintings", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        if (event.isCancelled()) { return; }

        final Entity entity = event.getEntity();

        // for now, only interested in Enderman and Wither boss tomfoolery
        if (!(entity instanceof Enderman) && !(entity instanceof Wither)) { return; }

        final Location loc = event.getBlock().getLocation();

        if (entity instanceof Enderman) {
            if (this.stopEndermanBlockManipulation(loc)) {
                event.setCancelled(true);
            }
        } else if (entity instanceof Wither) {
            final Faction faction = Board.getFactionAt(new FLocation(loc));
            // it's a bit crude just using fireball protection, but I'd rather not add in a whole new set of xxxBlockWitherExplosion or whatever
            if (faction.isNone() && Conf.wildernessBlockFireballs && !Conf.worldsNoWildernessProtection.contains(loc.getWorld().getName()) || faction.isNormal() && (faction.hasPlayersOnline() ? Conf.territoryBlockFireballs : Conf.territoryBlockFireballsWhenOffline) || faction.isWarZone() && Conf.warZoneBlockFireballs || faction.isSafeZone()) {
                event.setCancelled(true);
            }
        }
    }

    private boolean stopEndermanBlockManipulation(final Location loc) {
        if (loc == null) { return false; }
        // quick check to see if all Enderman deny options are enabled; if so, no need to check location
        if (Conf.wildernessDenyEndermanBlocks && Conf.territoryDenyEndermanBlocks && Conf.territoryDenyEndermanBlocksWhenOffline && Conf.safeZoneDenyEndermanBlocks && Conf.warZoneDenyEndermanBlocks) { return true; }

        final FLocation fLoc = new FLocation(loc);
        final Faction claimFaction = Board.getFactionAt(fLoc);

        if (claimFaction.isNone()) {
            return Conf.wildernessDenyEndermanBlocks;
        } else if (claimFaction.isNormal()) {
            return claimFaction.hasPlayersOnline() ? Conf.territoryDenyEndermanBlocks : Conf.territoryDenyEndermanBlocksWhenOffline;
        } else if (claimFaction.isSafeZone()) {
            return Conf.safeZoneDenyEndermanBlocks;
        } else if (claimFaction.isWarZone()) { return Conf.warZoneDenyEndermanBlocks; }

        return false;
    }
}

package com.massivecraft.factions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.v1_7_R4.WorldServer;
import net.neferett.linaris.thenexus.EntityTheNexus;
import net.neferett.linaris.thenexus.Level;
import net.neferett.linaris.thenexus.ParticleEffect;
import net.neferett.linaris.thenexus.PlayerUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.LWCFeatures;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.LazyLocation;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.util.RelationUtil;
import com.massivecraft.factions.zcore.persist.Entity;

public class Faction extends Entity implements EconomyParticipator {
    // The Nexus Start
    // FIELD: nexus
    private transient EntityTheNexus nexus;

    public EntityTheNexus getNexus() {
        return nexus;
    }

    public void setNexus(final EntityTheNexus nexus) {
        this.nexus = nexus;
    }

    // FIELD: health
    private int health = 500;

    public int getHealth() {
        return health;
    }

    public void setHealth(final int health) {
        this.health = health;
    }

    // FIELD: level
    private Level level = Level.FIRST;

    public Level getLevel() {
        return level;
    }

    public void setLevel(final Level level) {
        this.level = level;
    }

    public Level getNextLevel() {
        for (final Level level : Level.values()) {
            if (this.level.getId() + 1 == level.getId()) { return level; }
        }
        return null;
    }

    // FIELD: brokenChestsCount
    private int brokenChestsCount;

    public int getBrokenChestsCount() {
        return brokenChestsCount;
    }

    public void incrementBrokenChestsCount() {
        brokenChestsCount++;
        this.checkForNewLevel();
    }

    // FIELD: brokenNexusCount
    private int brokenNexusCount;

    public int getBrokenNexusCount() {
        return brokenNexusCount;
    }

    public void incrementBrokenNexusCount() {
        brokenNexusCount++;
        this.checkForNewLevel();
    }

    // FIELD: lastHomeSet
    private long lastHomeSet;

    public long getLastHomeSet() {
        return lastHomeSet;
    }

    public void setLastHomeSet(final long lastHomeSet) {
        this.lastHomeSet = lastHomeSet;
    }

    // FIELD: lastBrokenNexus
    private long lastBrokenNexus;

    public long getLastBrokenNexus() {
        return lastBrokenNexus;
    }

    public void setLastBrokenNexus(final long lastBrokenNexus) {
        this.lastBrokenNexus = lastBrokenNexus;
    }

    // FIELD: brokenChestsTemp
    private int brokenChestsTemp;

    public int getBrokenChestsTemp() {
        return brokenChestsTemp;
    }

    public void incrementBrokenChestsTemp() {
        brokenChestsTemp++;
    }

    public void resetBrokenChestsTemp() {
        brokenChestsTemp = 0;
    }

    // FIELD: ennemies
    private final Map<String, Long> nexusEnnemies = new HashMap<>();

    public Map<String, Long> getNexusEnnemies() {
        return nexusEnnemies;
    }

    // METHODS
    public ChatColor getColorForHealth() {
        return health <= 50 ? ChatColor.DARK_RED : health <= 150 ? ChatColor.RED : health <= 350 ? ChatColor.GOLD : ChatColor.GREEN;
    }

    public void checkForNewLevel() {
        final Level nextLevel = this.getNextLevel();
        if (nextLevel != null && brokenChestsCount >= nextLevel.getBrokenChests().getCount() && brokenNexusCount >= nextLevel.getBrokenNexus().getCount()) {
            Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[" + ChatColor.GOLD + "LEVEL UP" + ChatColor.DARK_PURPLE + "] " + ChatColor.GRAY + "La faction " + ChatColor.AQUA + tag + ChatColor.GRAY + " est maintenant niveau " + ChatColor.AQUA + nextLevel.getId() + ChatColor.GRAY + " !");
            this.sendMessage(Arrays.asList("", "", "", "", "", "", "", "", ChatColor.DARK_PURPLE + "[-----------" + ChatColor.GOLD + " LEVEL UP ! " + ChatColor.DARK_PURPLE + "-----------]", ChatColor.GRAY + "Votre faction est désormais niveau " + ChatColor.GREEN + nextLevel.getId() + ChatColor.AQUA + "/6", ChatColor.GRAY + "Membres de joueurs maximum : " + ChatColor.AQUA + nextLevel.getMaxMembers(), ChatColor.GRAY + "Vie maximale du coeur : " + ChatColor.AQUA + nextLevel.getMaxHealth(), ChatColor.GRAY + "TNT requises pour casser l'obsidienne : " + ChatColor.AQUA + nextLevel.getExplosions(), ChatColor.DARK_PURPLE + "[--------------------------------]", ""));
            level = nextLevel;
            if (nexus != null) {
                nexus.getHologram().clearLines();
                nexus.getHologram().insertTextLine(1, this.getColorForHealth() + "" + health + ChatColor.AQUA + "/" + ChatColor.GREEN + nextLevel.getMaxHealth());
            }
        }
    }

    public void handleDamage(final int damage, final Player killer) {
        if (health == 0) {
            if (killer != null) {
                killer.sendMessage(ChatColor.RED + "Le coeur est déjà détruit !");
                killer.playSound(killer.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
            }
            return;
        } else {
            FPlayer fkiller = null;
            Faction faction = null;
            if (killer != null) {
                fkiller = FPlayers.i.get(killer);
                faction = fkiller.getFaction();
                if (fplayers.contains(fkiller)) {
                    return;
                } else if (faction.getNexusEnnemies().containsKey(id) && (System.currentTimeMillis() - faction.getNexusEnnemies().get(id)) / 1000 < 86400) {
                    killer.playSound(killer.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
                    killer.sendMessage(ChatColor.RED + "Vous avez déjà pillé la faction " + ChatColor.DARK_RED + tag + ChatColor.RED + " il y a moins de 24 heures.");
                    return;
                }
                faction.getNexusEnnemies().remove(id);
            }
            if (health - damage > 0) {
                health -= damage;
                ParticleEffect.SPELL_WITCH.display(home.getLocation(), 16);
                for (final FPlayer fplayer : this.getFPlayersWhereOnline(true)) {
                    fplayer.getPlayer().playSound(fplayer.getPlayer().getLocation(), Sound.WITHER_SPAWN, 1.0F, 0.5F);
                    fplayer.sendMessage(ChatColor.RED + "!!! Le coeur est attaqué" + (fkiller != null && faction.isNormal() ? " par la faction " + faction.getTag() : "") + " !!! (" + health + "/" + level.getMaxHealth() + ")");
                }
            } else {
                health = 0;
                final Faction finalFaction = faction;
                final FPlayer finalKiller = fkiller;
                this.setLastBrokenNexus(System.currentTimeMillis());
                this.sendMessage(ChatColor.DARK_RED + "Le coeur n'a plus d'énergie..." + ChatColor.LIGHT_PURPLE + " Vous devez attendre 5 minutes avant de pouvoir le régénérer.");
                if (fkiller != null) {
                    faction.getNexusEnnemies().put(id, System.currentTimeMillis());
                    final int coins = this.getFPlayersWhereOnline(true).size() * 100;
                    if (coins > 0) {
                        PlayerUtils.giveCoins(killer, coins);
                    }
                    if (!faction.isNormal()) {
                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                brokenChestsTemp = 0;
                                if (!finalKiller.isOffline() && Board.getFactionAt(new FLocation(finalKiller)) == Faction.this) {
                                    finalKiller.sendMessage(Arrays.asList(ChatColor.DARK_PURPLE + "[-----------" + ChatColor.GOLD + "FIN DE L'ASSAUT" + ChatColor.DARK_PURPLE + "-----------]", ChatColor.GRAY + "L'assaut est terminé, vous allez être téléporté !", ChatColor.DARK_PURPLE + "[-----------------------------------]"));
                                    finalKiller.getPlayer().teleport(finalKiller.getPlayer().getBedSpawnLocation() == null ? Bukkit.getWorlds().get(0).getSpawnLocation() : finalKiller.getPlayer().getBedSpawnLocation());
                                }
                            }
                        }.runTaskLater(P.p, 6000);
                        fkiller.sendMessage(Arrays.asList(ChatColor.DARK_PURPLE + "[-----------" + ChatColor.GOLD + "NOUVEL ASSAUT" + ChatColor.DARK_PURPLE + "-----------]", ChatColor.GRAY + "Vous avez détruit " + ChatColor.RED + "le coeur" + ChatColor.GRAY + " de la faction " + ChatColor.RED + tag, ChatColor.GRAY + "Vous avez " + ChatColor.AQUA + "5 minutes" + ChatColor.GRAY + " pour piller cette faction avant d'être téléporté.", ChatColor.GRAY + "Vous pouvez casser au maximum " + ChatColor.AQUA + "20 coffres" + ChatColor.GRAY + " pas un de plus : choisissez bien lesquels casser.", ChatColor.DARK_PURPLE + "[-----------------------------------]"));
                    } else {
                        faction.incrementBrokenNexusCount();
                        final Level nextLevel = faction.getNextLevel();
                        if (nextLevel != null && faction.getBrokenNexusCount() < nextLevel.getBrokenNexus().getCount()) {
                            killer.sendMessage(ChatColor.GRAY + "Avancement de la casse des nexus : " + ChatColor.GOLD + faction.getBrokenNexusCount() + ChatColor.AQUA + "/" + nextLevel.getBrokenNexus().getCount());
                        } else if (nextLevel != null && faction.getBrokenNexusCount() == nextLevel.getBrokenNexus().getCount()) {
                            faction.sendMessage(Arrays.asList(ChatColor.DARK_PURPLE + "[-----------" + ChatColor.GOLD + "AVANCEMENT" + ChatColor.DARK_PURPLE + "-----------]", ChatColor.GRAY + "Votre faction a détruit tous les nexus nécéssaires pour passer au level supérieur.", ChatColor.DARK_PURPLE + "[--------------------------------]"));
                        }
                        final Set<FPlayer> fplayers = faction.getFPlayersWhereOnline(true);
                        for (final Player online : Bukkit.getOnlinePlayers()) {
                            if (!fplayers.contains(FPlayers.i.get(online))) {
                                online.sendMessage(ChatColor.GOLD + "[The Nexus] " + ChatColor.GRAY + "La faction " + ChatColor.BLUE + fkiller.getFaction().getTag() + ChatColor.GRAY + " vient de détruire le coeur de la faction " + ChatColor.RED + tag + ChatColor.GRAY + " !");
                            }
                        }
                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                brokenChestsTemp = 0;
                                final Location home = finalFaction.getHome();
                                Location spawnLocation = null;
                                if (home == null) {
                                    spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
                                }
                                finalFaction.sendMessage(Arrays.asList(ChatColor.DARK_PURPLE + "[-----------" + ChatColor.GOLD + "FIN DE L'ASSAUT" + ChatColor.DARK_PURPLE + "-----------]", ChatColor.GRAY + "L'assaut est terminé, les joueurs se trouvant dans les claims de la faction " + ChatColor.RED + tag + ChatColor.GRAY + " vont être téléportés !", ChatColor.DARK_PURPLE + "[-----------------------------------]"));
                                for (final FPlayer fplayer : finalFaction.getFPlayersWhereOnline(true)) {
                                    if (Board.getFactionAt(new FLocation(fplayer)) == Faction.this) {
                                        if (home != null) {
                                            fplayer.getPlayer().teleport(home);
                                        } else {
                                            fplayer.getPlayer().teleport(fplayer.getPlayer().getBedSpawnLocation() == null ? spawnLocation : fplayer.getPlayer().getBedSpawnLocation());
                                        }
                                    }
                                }
                            }
                        }.runTaskLater(P.p, 6000);
                        faction.sendMessage(Arrays.asList(ChatColor.DARK_PURPLE + "[-----------" + ChatColor.GOLD + "NOUVEL ASSAUT" + ChatColor.DARK_PURPLE + "-----------]", ChatColor.GRAY + "Votre faction a détruit " + ChatColor.RED + "le coeur" + ChatColor.GRAY + " de la faction " + ChatColor.RED + tag, ChatColor.GRAY + "Vous avez " + ChatColor.AQUA + "5 minutes" + ChatColor.GRAY + " pour piller cette faction avant d'être téléporté.", ChatColor.GRAY + "Vous pouvez casser au maximum " + ChatColor.AQUA + "20 coffres" + ChatColor.GRAY + " pas un de plus : choisissez bien lesquels casser.", ChatColor.DARK_PURPLE + "[-----------------------------------]"));
                    }
                }
            }
        }
        if (nexus != null) {
            final Hologram hologram = nexus.getHologram();
            hologram.clearLines();
            hologram.appendTextLine(this.getColorForHealth() + "" + health + ChatColor.AQUA + "/" + ChatColor.GREEN + level.getMaxHealth());
        }
    }

    public void regenHealth(final int regen) {
        if (regen + health > level.getMaxHealth()) {
            health = level.getMaxHealth();
        } else {
            health += regen;
        }
        if (nexus != null) {
            final Hologram hologram = nexus.getHologram();
            hologram.clearLines();
            hologram.appendTextLine(this.getColorForHealth() + "" + health + ChatColor.AQUA + "/" + ChatColor.GREEN + level.getMaxHealth());
        }
    }

    public void breakHome() {
        if (home != null) {
            for (int x = -3; x <= 3; x++) {
                for (int y = -1; y < 6; y++) {
                    for (int z = -3; z <= 3; z++) {
                        final Block b = home.getLocation().clone().add(x, y, z).getBlock();
                        b.setType(Material.AIR);
                    }
                }
            }
            if (nexus != null) {
                final Location homeLocation = home.getLocation();
                final WorldServer world = ((CraftWorld) homeLocation.getWorld()).getHandle();
                world.removeEntity(nexus);
                nexus.getHologram().delete();
                nexus = null;
            }
        }
    }

    public void removeNexus() {
        if (!this.hasHome()) { return; }
        if (nexus != null) {
            nexus.getBukkitEntity().remove();
            if (!nexus.getHologram().isDeleted()) {
                nexus.getHologram().delete();
            }
            nexus = null;
        }
        for (org.bukkit.entity.Entity entity : home.getLocation().getChunk().getEntities()) {
            if (entity instanceof EnderCrystal) {
                entity.remove();
            }
        }
    }

    public void spawnNexus(boolean bypassChunk) {
        if (!this.hasHome()) { return; }
        final Location homeLocation = home.getLocation();
        if (bypassChunk || homeLocation.getChunk().isLoaded()) {
            this.removeNexus();
            final WorldServer world = ((CraftWorld) homeLocation.getWorld()).getHandle();
            final Hologram hologram = HologramsAPI.createHologram(P.p, homeLocation.clone().add(0, 3.5, 0));
            hologram.appendTextLine(ChatColor.GRAY + "Niveau : " + ChatColor.AQUA + level.getId());
            hologram.appendTextLine(this.getColorForHealth() + "" + health + ChatColor.AQUA + "/" + ChatColor.GREEN + level.getMaxHealth());
            nexus = new EntityTheNexus(world, this.getId(), hologram);
            nexus.setLocation(homeLocation.getX(), homeLocation.getBlockY() + 1, homeLocation.getZ(), 0.0F, 0.0F);
            world.addEntity(nexus);
        }
    }

    @Override
    public void detach() {
        super.detach();
        this.breakHome();
    }

    // The Nexus End

    // FIELD: relationWish
    private final Map<String, Relation> relationWish;

    // FIELD: claimOwnership
    private final Map<FLocation, Set<String>> claimOwnership = new ConcurrentHashMap<FLocation, Set<String>>();

    // FIELD: fplayers
    // speedy lookup of players in faction
    private transient Set<FPlayer> fplayers = new HashSet<FPlayer>();

    // FIELD: invites
    // Where string is a lowercase player name
    private final Set<String> invites;

    public void invite(final FPlayer fplayer) {
        invites.add(fplayer.getName().toLowerCase());
    }

    public void deinvite(final FPlayer fplayer) {
        invites.remove(fplayer.getName().toLowerCase());
    }

    public boolean isInvited(final FPlayer fplayer) {
        return invites.contains(fplayer.getName().toLowerCase());
    }

    // FIELD: open
    private boolean open;

    public boolean getOpen() {
        return open;
    }

    public void setOpen(final boolean isOpen) {
        open = isOpen;
    }

    // FIELD: peaceful
    // "peaceful" status can only be set by server admins/moderators/ops, and prevents PvP and land capture to/from the faction
    private boolean peaceful;

    public boolean isPeaceful() {
        return peaceful;
    }

    public void setPeaceful(final boolean isPeaceful) {
        peaceful = isPeaceful;
    }

    // FIELD: peacefulExplosionsEnabled
    private boolean peacefulExplosionsEnabled;

    public void setPeacefulExplosionsEnabled(final boolean val) {
        peacefulExplosionsEnabled = val;
    }

    public boolean getPeacefulExplosionsEnabled() {
        return peacefulExplosionsEnabled;
    }

    public boolean noExplosionsInTerritory() {
        return peaceful && !peacefulExplosionsEnabled;
    }

    // FIELD: permanent
    // "permanent" status can only be set by server admins/moderators/ops, and allows the faction to remain even with 0 members
    private boolean permanent;

    public boolean isPermanent() {
        return permanent || !this.isNormal();
    }

    public void setPermanent(final boolean isPermanent) {
        permanent = isPermanent;
    }

    // FIELD: tag
    private String tag;

    public String getTag() {
        return tag;
    }

    public String getTag(final String prefix) {
        return prefix + tag;
    }

    public String getTag(final Faction otherFaction) {
        if (otherFaction == null) { return this.getTag(); }
        return this.getTag(this.getColorTo(otherFaction).toString());
    }

    public String getTag(final FPlayer otherFplayer) {
        if (otherFplayer == null) { return this.getTag(); }
        return this.getTag(this.getColorTo(otherFplayer).toString());
    }

    public void setTag(String str) {
        if (Conf.factionTagForceUpperCase) {
            str = str.toUpperCase();
        }
        tag = str;
    }

    public String getComparisonTag() {
        return MiscUtil.getComparisonString(tag);
    }

    // FIELD: description
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(final String value) {
        description = value;
    }

    // FIELD: home
    private LazyLocation home;

    public void setHome(final Location home) {
        this.home = new LazyLocation(home);
    }

    public boolean hasHome() {
        return this.getHome() != null;
    }

    public Location getHome() {
        this.confirmValidHome();
        return home != null ? home.getLocation() : null;
    }

    public void confirmValidHome() {
        if (!Conf.homesMustBeInClaimedTerritory || home == null || home.getLocation() != null && Board.getFactionAt(new FLocation(home.getLocation())) == this) { return; }

        // The Nexus Start
        this.breakHome();
        // The Nexus End
        this.msg("<b>Your faction home has been un-set since it is no longer in your territory.");
        home = null;
    }

    // FIELD: lastPlayerLoggedOffTime
    private transient long lastPlayerLoggedOffTime;

    // FIELD: account (fake field)
    // Bank functions
    public double money;

    @Override
    public String getAccountId() {
        final String aid = "faction-" + this.getId();

        // We need to override the default money given to players.
        if (!Econ.hasAccount(aid)) {
            Econ.setBalance(aid, 0);
        }

        return aid;
    }

    // FIELD: permanentPower
    private Integer permanentPower;

    public Integer getPermanentPower() {
        return permanentPower;
    }

    public void setPermanentPower(final Integer permanentPower) {
        this.permanentPower = permanentPower;
    }

    public boolean hasPermanentPower() {
        return permanentPower != null;
    }

    // FIELD: powerBoost
    // special increase/decrease to default and max power for this faction
    private double powerBoost;

    public double getPowerBoost() {
        return powerBoost;
    }

    public void setPowerBoost(final double powerBoost) {
        this.powerBoost = powerBoost;
    }

    // -------------------------------------------- //
    // Construct
    // -------------------------------------------- //

    public Faction() {
        relationWish = new HashMap<String, Relation>();
        invites = new HashSet<String>();
        open = Conf.newFactionsDefaultOpen;
        tag = "???";
        description = "Default faction description :(";
        lastPlayerLoggedOffTime = 0;
        peaceful = false;
        peacefulExplosionsEnabled = false;
        permanent = false;
        money = 0.0;
        powerBoost = 0.0;
    }

    // -------------------------------------------- //
    // Extra Getters And Setters
    // -------------------------------------------- //

    public boolean noPvPInTerritory() {
        return this.isSafeZone() || peaceful && Conf.peacefulTerritoryDisablePVP;
    }

    public boolean noMonstersInTerritory() {
        return this.isSafeZone() || peaceful && Conf.peacefulTerritoryDisableMonsters;
    }

    // -------------------------------
    // Understand the types
    // -------------------------------

    public boolean isNormal() {
        return !(this.isNone() || this.isSafeZone() || this.isWarZone());
    }

    public boolean isNone() {
        return this.getId().equals("0");
    }

    public boolean isSafeZone() {
        return this.getId().equals("-1");
    }

    public boolean isWarZone() {
        return this.getId().equals("-2");
    }

    public boolean isPlayerFreeType() {
        return this.isSafeZone() || this.isWarZone();
    }

    // -------------------------------
    // Relation and relation colors
    // -------------------------------

    @Override
    public String describeTo(final RelationParticipator that, final boolean ucfirst) {
        return RelationUtil.describeThatToMe(this, that, ucfirst);
    }

    @Override
    public String describeTo(final RelationParticipator that) {
        return RelationUtil.describeThatToMe(this, that);
    }

    @Override
    public Relation getRelationTo(final RelationParticipator rp) {
        return RelationUtil.getRelationTo(this, rp);
    }

    @Override
    public Relation getRelationTo(final RelationParticipator rp, final boolean ignorePeaceful) {
        return RelationUtil.getRelationTo(this, rp, ignorePeaceful);
    }

    @Override
    public ChatColor getColorTo(final RelationParticipator rp) {
        return RelationUtil.getColorOfThatToMe(this, rp);
    }

    public Relation getRelationWish(final Faction otherFaction) {
        if (relationWish.containsKey(otherFaction.getId())) { return relationWish.get(otherFaction.getId()); }
        return Relation.NEUTRAL;
    }

    public void setRelationWish(final Faction otherFaction, final Relation relation) {
        if (relationWish.containsKey(otherFaction.getId()) && relation.equals(Relation.NEUTRAL)) {
            relationWish.remove(otherFaction.getId());
        } else {
            relationWish.put(otherFaction.getId(), relation);
        }
    }

    //----------------------------------------------//
    // Power
    //----------------------------------------------//
    public double getPower() {
        if (this.hasPermanentPower()) { return this.getPermanentPower(); }

        double ret = 0;
        for (final FPlayer fplayer : fplayers) {
            ret += fplayer.getPower();
        }
        if (Conf.powerFactionMax > 0 && ret > Conf.powerFactionMax) {
            ret = Conf.powerFactionMax;
        }
        return ret + powerBoost;
    }

    public double getPowerMax() {
        if (this.hasPermanentPower()) { return this.getPermanentPower(); }

        double ret = 0;
        for (final FPlayer fplayer : fplayers) {
            ret += fplayer.getPowerMax();
        }
        if (Conf.powerFactionMax > 0 && ret > Conf.powerFactionMax) {
            ret = Conf.powerFactionMax;
        }
        return ret + powerBoost;
    }

    public int getPowerRounded() {
        return (int) Math.round(this.getPower());
    }

    public int getPowerMaxRounded() {
        return (int) Math.round(this.getPowerMax());
    }

    public int getLandRounded() {
        return Board.getFactionCoordCount(this);
    }

    public int getLandRoundedInWorld(final String worldName) {
        return Board.getFactionCoordCountInWorld(this, worldName);
    }

    public boolean hasLandInflation() {
        return this.getLandRounded() > this.getPowerRounded();
    }

    // -------------------------------
    // FPlayers
    // -------------------------------

    // maintain the reference list of FPlayers in this faction
    public void refreshFPlayers() {
        fplayers.clear();
        if (this.isPlayerFreeType()) { return; }

        for (final FPlayer fplayer : FPlayers.i.get()) {
            if (fplayer.getFaction() == this) {
                fplayers.add(fplayer);
            }
        }
    }

    protected boolean addFPlayer(final FPlayer fplayer) {
        if (this.isPlayerFreeType()) { return false; }

        return fplayers.add(fplayer);
    }

    protected boolean removeFPlayer(final FPlayer fplayer) {
        if (this.isPlayerFreeType()) { return false; }

        return fplayers.remove(fplayer);
    }

    public Set<FPlayer> getFPlayers() {
        // return a shallow copy of the FPlayer list, to prevent tampering and concurrency issues
        final Set<FPlayer> ret = new HashSet<FPlayer>(fplayers);
        return ret;
    }

    public Set<FPlayer> getFPlayersWhereOnline(final boolean online) {
        final Set<FPlayer> ret = new HashSet<FPlayer>();

        for (final FPlayer fplayer : fplayers) {
            if (fplayer.isOnline() == online) {
                ret.add(fplayer);
            }
        }

        return ret;
    }

    public FPlayer getFPlayerAdmin() {
        if (!this.isNormal()) { return null; }

        for (final FPlayer fplayer : fplayers) {
            if (fplayer.getRole() == Role.ADMIN) { return fplayer; }
        }
        return null;
    }

    public ArrayList<FPlayer> getFPlayersWhereRole(final Role role) {
        final ArrayList<FPlayer> ret = new ArrayList<FPlayer>();
        if (!this.isNormal()) { return ret; }

        for (final FPlayer fplayer : fplayers) {
            if (fplayer.getRole() == role) {
                ret.add(fplayer);
            }
        }

        return ret;
    }

    public ArrayList<Player> getOnlinePlayers() {
        final ArrayList<Player> ret = new ArrayList<Player>();
        if (this.isPlayerFreeType()) { return ret; }

        for (final Player player : P.p.getServer().getOnlinePlayers()) {
            final FPlayer fplayer = FPlayers.i.get(player);
            if (fplayer.getFaction() == this) {
                ret.add(player);
            }
        }

        return ret;
    }

    // slightly faster check than getOnlinePlayers() if you just want to see if there are any players online
    public boolean hasPlayersOnline() {
        // only real factions can have players online, not safe zone / war zone
        if (this.isPlayerFreeType()) { return false; }

        for (final Player player : P.p.getServer().getOnlinePlayers()) {
            final FPlayer fplayer = FPlayers.i.get(player);
            if (fplayer.getFaction() == this) { return true; }
        }

        // even if all players are technically logged off, maybe someone was on recently enough to not consider them officially offline yet
        if (Conf.considerFactionsReallyOfflineAfterXMinutes > 0 && System.currentTimeMillis() < lastPlayerLoggedOffTime + Conf.considerFactionsReallyOfflineAfterXMinutes * 60000) { return true; }
        return false;
    }

    public void memberLoggedOff() {
        if (this.isNormal()) {
            lastPlayerLoggedOffTime = System.currentTimeMillis();
        }
    }

    // used when current leader is about to be removed from the faction; promotes new leader, or disbands faction if no other members left
    public void promoteNewLeader() {
        if (!this.isNormal()) { return; }
        if (this.isPermanent() && Conf.permanentFactionsDisableLeaderPromotion) { return; }

        final FPlayer oldLeader = this.getFPlayerAdmin();

        // get list of moderators, or list of normal members if there are no moderators
        ArrayList<FPlayer> replacements = this.getFPlayersWhereRole(Role.MODERATOR);
        if (replacements == null || replacements.isEmpty()) {
            replacements = this.getFPlayersWhereRole(Role.NORMAL);
        }

        if (replacements == null || replacements.isEmpty()) { // faction admin is the only member; one-man faction
            if (this.isPermanent()) {
                if (oldLeader != null) {
                    oldLeader.setRole(Role.NORMAL);
                }
                return;
            }

            // no members left and faction isn't permanent, so disband it
            if (Conf.logFactionDisband) {
                P.p.log("The faction " + this.getTag() + " (" + this.getId() + ") has been disbanded since it has no members left.");
            }

            for (final FPlayer fplayer : FPlayers.i.getOnline()) {
                fplayer.msg("The faction %s<i> was disbanded.", this.getTag(fplayer));
            }

            this.detach();
        } else { // promote new faction admin
            if (oldLeader != null) {
                oldLeader.setRole(Role.NORMAL);
            }
            replacements.get(0).setRole(Role.ADMIN);
            this.msg("<i>Faction admin <h>%s<i> has been removed. %s<i> has been promoted as the new faction admin.", oldLeader == null ? "" : oldLeader.getName(), replacements.get(0).getName());
            P.p.log("Faction " + this.getTag() + " (" + this.getId() + ") admin was removed. Replacement admin: " + replacements.get(0).getName());
        }
    }

    //----------------------------------------------//
    // Messages
    //----------------------------------------------//
    @Override
    public void msg(String message, final Object... args) {
        message = P.p.txt.parse(message, args);

        for (final FPlayer fplayer : this.getFPlayersWhereOnline(true)) {
            fplayer.sendMessage(message);
        }
    }

    public void sendMessage(final String message) {
        for (final FPlayer fplayer : this.getFPlayersWhereOnline(true)) {
            fplayer.sendMessage(message);
        }
    }

    public void sendMessage(final List<String> messages) {
        for (final FPlayer fplayer : this.getFPlayersWhereOnline(true)) {
            fplayer.sendMessage(messages);
        }
    }

    //----------------------------------------------//
    // Ownership of specific claims
    //----------------------------------------------//

    public void clearAllClaimOwnership() {
        claimOwnership.clear();
    }

    public void clearClaimOwnership(final FLocation loc) {
        if (Conf.onUnclaimResetLwcLocks && LWCFeatures.getEnabled()) {
            LWCFeatures.clearAllChests(loc);
        }
        claimOwnership.remove(loc);
    }

    public void clearClaimOwnership(final String playerName) {
        if (playerName == null || playerName.isEmpty()) { return; }

        Set<String> ownerData;
        final String player = playerName.toLowerCase();

        for (final Entry<FLocation, Set<String>> entry : claimOwnership.entrySet()) {
            ownerData = entry.getValue();

            if (ownerData == null) {
                continue;
            }

            final Iterator<String> iter = ownerData.iterator();
            while (iter.hasNext()) {
                if (iter.next().equals(player)) {
                    iter.remove();
                }
            }

            if (ownerData.isEmpty()) {
                if (Conf.onUnclaimResetLwcLocks && LWCFeatures.getEnabled()) {
                    LWCFeatures.clearAllChests(entry.getKey());
                }
                claimOwnership.remove(entry.getKey());
            }
        }
    }

    public int getCountOfClaimsWithOwners() {
        return claimOwnership.isEmpty() ? 0 : claimOwnership.size();
    }

    public boolean doesLocationHaveOwnersSet(final FLocation loc) {
        if (claimOwnership.isEmpty() || !claimOwnership.containsKey(loc)) { return false; }

        final Set<String> ownerData = claimOwnership.get(loc);
        return ownerData != null && !ownerData.isEmpty();
    }

    public boolean isPlayerInOwnerList(final String playerName, final FLocation loc) {
        if (claimOwnership.isEmpty()) { return false; }
        final Set<String> ownerData = claimOwnership.get(loc);
        if (ownerData == null) { return false; }
        if (ownerData.contains(playerName.toLowerCase())) { return true; }

        return false;
    }

    public void setPlayerAsOwner(final String playerName, final FLocation loc) {
        Set<String> ownerData = claimOwnership.get(loc);
        if (ownerData == null) {
            ownerData = new HashSet<String>();
        }
        ownerData.add(playerName.toLowerCase());
        claimOwnership.put(loc, ownerData);
    }

    public void removePlayerAsOwner(final String playerName, final FLocation loc) {
        final Set<String> ownerData = claimOwnership.get(loc);
        if (ownerData == null) { return; }
        ownerData.remove(playerName.toLowerCase());
        claimOwnership.put(loc, ownerData);
    }

    public Set<String> getOwnerList(final FLocation loc) {
        return claimOwnership.get(loc);
    }

    public String getOwnerListString(final FLocation loc) {
        final Set<String> ownerData = claimOwnership.get(loc);
        if (ownerData == null || ownerData.isEmpty()) { return ""; }

        String ownerList = "";

        final Iterator<String> iter = ownerData.iterator();
        while (iter.hasNext()) {
            if (!ownerList.isEmpty()) {
                ownerList += ", ";
            }
            ownerList += iter.next();
        }
        return ownerList;
    }

    public boolean playerHasOwnershipRights(final FPlayer fplayer, final FLocation loc) {
        // in own faction, with sufficient role or permission to bypass ownership?
        if (fplayer.getFaction() == this && (fplayer.getRole().isAtLeast(Conf.ownedAreaModeratorsBypass ? Role.MODERATOR : Role.ADMIN) || Permission.OWNERSHIP_BYPASS.has(fplayer.getPlayer()))) { return true; }

        // make sure claimOwnership is initialized
        if (claimOwnership.isEmpty()) { return true; }

        // need to check the ownership list, then
        final Set<String> ownerData = claimOwnership.get(loc);

        // if no owner list, owner list is empty, or player is in owner list, they're allowed
        if (ownerData == null || ownerData.isEmpty() || ownerData.contains(fplayer.getName().toLowerCase())) { return true; }

        return false;
    }

    //----------------------------------------------//
    // Persistance and entity management
    //----------------------------------------------//

    @Override
    public void postDetach() {
        if (Econ.shouldBeUsed()) {
            Econ.setBalance(this.getAccountId(), 0);
        }

        // Clean the board
        Board.clean();

        // Clean the fplayers
        FPlayers.i.clean();
    }
}

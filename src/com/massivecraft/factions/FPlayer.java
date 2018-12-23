package com.massivecraft.factions;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;

import com.massivecraft.factions.event.FPlayerLeaveEvent;
import com.massivecraft.factions.event.LandClaimEvent;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.LWCFeatures;
import com.massivecraft.factions.integration.SpoutFeatures;
import com.massivecraft.factions.integration.Worldguard;
import com.massivecraft.factions.struct.ChatMode;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.RelationUtil;
import com.massivecraft.factions.zcore.persist.PlayerEntity;

import net.neferett.linaris.thenexus.Level;
import net.neferett.linaris.thenexus.ScoreboardBuilder;

/**
 * Logged in players always have exactly one FPlayer instance.
 * Logged out players may or may not have an FPlayer instance. They will always have one if they are part of a faction.
 * This is because only players with a faction are saved to disk (in order to not waste disk space).
 * 
 * The FPlayer is linked to a minecraft player using the player name.
 * 
 * The same instance is always returned for the same player.
 * This means you can use the == operator. No .equals method necessary.
 */

public class FPlayer extends PlayerEntity implements EconomyParticipator {
    // The Nexus Start
    // FIELD: lastCreatedFaction
    private long lastCreatedFaction;

    public long getLastCreatedFaction() {
        return lastCreatedFaction;
    }

    public void setLastCreatedFaction(final long lastCreatedFaction) {
        this.lastCreatedFaction = lastCreatedFaction;
    }

    private int butcherCount;

    public int getButcherCount() {
        return butcherCount;
    }

    private int bakerCount;

    public int getBakerCount() {
        return bakerCount;
    }

    private int woodcutterCount;

    public int getWoodcutterCount() {
        return woodcutterCount;
    }

    private int potionCount;

    public int getPotionCount() {
        return potionCount;
    }

    private int archerCount;

    public int getArcherCount() {
        return archerCount;
    }

    private int runnerCount;

    public int getRunnerCount() {
        return runnerCount;
    }

    private int murdererCount;

    public int getMurdererCount() {
        return murdererCount;
    }

    private int minerCount;

    public int getMinerCount() {
        return minerCount;
    }

    private int builderCount;

    public int getBuilderCount() {
        return builderCount;
    }

    private int deaths;

    public int getDeaths() {
        return deaths;
    }

    public void incrementDeaths() {
        deaths++;
    }

    private int kills;

    public int getKills() {
        return kills;
    }

    public void incrementKills() {
        kills++;
    }

    private long lastGoldenApple;

    public void setLastGoldenApple(final long lastGoldenApple) {
        this.lastGoldenApple = lastGoldenApple;
    }

    public long getLastGoldenApple() {
        return lastGoldenApple;
    }

    private transient ScoreboardBuilder board;

    public void updateScoreboard() {
        board = ScoreboardBuilder.builder("nexus", ChatColor.DARK_GRAY + "-" + ChatColor.BLUE + "The Nexus" + ChatColor.DARK_GRAY + "-", DisplaySlot.SIDEBAR);
        final Player player = FPlayer.this.getPlayer();
        if (player != null) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    if (player == null || !player.isOnline()) { return; }
                    board.setLine(-1, "Joueur");
                    board.setLine(-2, " " + ChatColor.GREEN + player.getName());
                    board.setLine(-3, ChatColor.GRAY + "--- Infos ---");
                    final long diff = System.currentTimeMillis() - lastGoldenApple;
                    if (diff / 1000 >= 300) {
                        board.setLine(-4, ChatColor.GOLD + "Pomme cheat " + ChatColor.GREEN + "✔");
                    } else {
                        final int seconds = 300 - (int) (diff / 1000);
                        final int remainingMins = seconds / 60 % 60;
                        final int remainingSecs = seconds % 60;
                        board.setLine(-4, ChatColor.GOLD + "Pomme cheat >> " + ChatColor.YELLOW + remainingMins + "min" + (remainingSecs < 10 ? "0" : "") + remainingSecs + "s");
                    }
                    board.setLine(-5, ChatColor.GOLD + "Argent : " + ChatColor.YELLOW + Econ.getBalance(player.getName()) + "$");
                    board.setLine(-6, ChatColor.GOLD + "Meurtres : " + ChatColor.YELLOW + kills);
                    board.setLine(-7, ChatColor.GOLD + "Morts : " + ChatColor.YELLOW + deaths);
                    board.setLine(-8, ChatColor.GRAY + "--- Faction ---");
                    board.setLine(-9, "Faction");
                    Faction faction = FPlayer.this.getFaction();
                    board.setLine(-10, " " + (faction.isNormal() ? ChatColor.YELLOW + faction.getTag() : ChatColor.GRAY + "Aucune"));
                    if (!faction.isNormal()) {
                        board.removeLines(-11, -12, -13, -14, -15);
                    } else {
                        final Level nextLevel = faction.getNextLevel();
                        board.setLine(-11, ChatColor.GOLD + "Niveau : " + ChatColor.GREEN + faction.getLevel().getId() + ChatColor.AQUA + "/" + Level.values().length);
                        board.setLine(-12, ChatColor.GOLD + "Power : " + ChatColor.GREEN + faction.getPowerRounded() + ChatColor.AQUA + "/" + faction.getPowerMaxRounded());
                        board.setLine(-13, ChatColor.GOLD + "Membres : " + ChatColor.GREEN + faction.getFPlayers().size() + ChatColor.AQUA + "/" + faction.getLevel().getMaxMembers());
                        board.setLine(-14, ChatColor.GOLD + "Nexus : " + ChatColor.GREEN + faction.getBrokenNexusCount() + (nextLevel == null ? "" : ChatColor.AQUA + "/" + nextLevel.getBrokenNexus().getCount()));
                        board.setLine(-15, ChatColor.GOLD + "Coffres : " + ChatColor.GREEN + faction.getBrokenChestsCount() + (nextLevel == null ? "" : ChatColor.AQUA + "/" + nextLevel.getBrokenChests().getCount()));
                    }
                }
            }.runTaskTimer(P.p, 0, 20);
            player.setScoreboard(board.getScoreboard());
        }
    }

    // The Nexus End

    //private transient String playerName;
    private transient FLocation lastStoodAt = new FLocation(); // Where did this player stand the last time we checked?

    // FIELD: factionId
    private String factionId;

    public Faction getFaction() {
        if (factionId == null) { return null; }
        return Factions.i.get(factionId);
    }

    public String getFactionId() {
        return factionId;
    }

    public boolean hasFaction() {
        return !factionId.equals("0");
    }

    public void setFaction(final Faction faction) {
        final Faction oldFaction = this.getFaction();
        if (oldFaction != null) {
            oldFaction.removeFPlayer(this);
        }
        faction.addFPlayer(this);
        factionId = faction.getId();
        SpoutFeatures.updateAppearances(this.getPlayer());
    }

    // FIELD: role
    private Role role;

    public Role getRole() {
        return role;
    }

    public void setRole(final Role role) {
        this.role = role;
        SpoutFeatures.updateAppearances(this.getPlayer());
    }

    // FIELD: title
    private String title;

    // FIELD: power
    private double power;

    // FIELD: powerBoost
    // special increase/decrease to min and max power for this player
    private double powerBoost;

    public double getPowerBoost() {
        return powerBoost;
    }

    public void setPowerBoost(final double powerBoost) {
        this.powerBoost = powerBoost;
    }

    // FIELD: lastPowerUpdateTime
    private long lastPowerUpdateTime;

    // FIELD: lastLoginTime
    private long lastLoginTime;

    // FIELD: mapAutoUpdating
    private transient boolean mapAutoUpdating;

    // FIELD: autoClaimEnabled
    private transient Faction autoClaimFor;

    public Faction getAutoClaimFor() {
        return autoClaimFor;
    }

    public void setAutoClaimFor(final Faction faction) {
        autoClaimFor = faction;
        if (autoClaimFor != null) {
            // TODO: merge these into same autoclaim
            autoSafeZoneEnabled = false;
            autoWarZoneEnabled = false;
        }
    }

    // FIELD: autoSafeZoneEnabled
    private transient boolean autoSafeZoneEnabled;

    public boolean isAutoSafeClaimEnabled() {
        return autoSafeZoneEnabled;
    }

    public void setIsAutoSafeClaimEnabled(final boolean enabled) {
        autoSafeZoneEnabled = enabled;
        if (enabled) {
            autoClaimFor = null;
            autoWarZoneEnabled = false;
        }
    }

    // FIELD: autoWarZoneEnabled
    private transient boolean autoWarZoneEnabled;

    public boolean isAutoWarClaimEnabled() {
        return autoWarZoneEnabled;
    }

    public void setIsAutoWarClaimEnabled(final boolean enabled) {
        autoWarZoneEnabled = enabled;
        if (enabled) {
            autoClaimFor = null;
            autoSafeZoneEnabled = false;
        }
    }

    private transient boolean isAdminBypassing = false;

    public boolean isAdminBypassing() {
        return isAdminBypassing;
    }

    public void setIsAdminBypassing(final boolean val) {
        isAdminBypassing = val;
    }

    // FIELD: loginPvpDisabled
    private transient boolean loginPvpDisabled;

    // FIELD: deleteMe
    private transient boolean deleteMe;

    // FIELD: chatMode
    private ChatMode chatMode;

    public void setChatMode(final ChatMode chatMode) {
        this.chatMode = chatMode;
    }

    public ChatMode getChatMode() {
        if (factionId.equals("0") || !Conf.factionOnlyChat) {
            chatMode = ChatMode.PUBLIC;
        }
        return chatMode;
    }

    // FIELD: chatSpy
    private transient boolean spyingChat = false;

    public void setSpyingChat(final boolean chatSpying) {
        spyingChat = chatSpying;
    }

    public boolean isSpyingChat() {
        return spyingChat;
    }

    // FIELD: account
    @Override
    public String getAccountId() {
        return this.getId();
    }

    // -------------------------------------------- //
    // Construct
    // -------------------------------------------- //

    // GSON need this noarg constructor.
    public FPlayer() {
        this.resetFactionData(false);
        power = Conf.powerPlayerStarting;
        lastPowerUpdateTime = System.currentTimeMillis();
        lastLoginTime = System.currentTimeMillis();
        mapAutoUpdating = false;
        autoClaimFor = null;
        autoSafeZoneEnabled = false;
        autoWarZoneEnabled = false;
        loginPvpDisabled = Conf.noPVPDamageToOthersForXSecondsAfterLogin > 0 ? true : false;
        deleteMe = false;
        powerBoost = 0.0;

        if (!Conf.newPlayerStartingFactionID.equals("0") && Factions.i.exists(Conf.newPlayerStartingFactionID)) {
            factionId = Conf.newPlayerStartingFactionID;
        }
    }

    public final void resetFactionData(final boolean doSpoutUpdate) {
        // clean up any territory ownership in old faction, if there is one
        if (Factions.i.exists(this.getFactionId())) {
            final Faction currentFaction = this.getFaction();
            currentFaction.removeFPlayer(this);
            if (currentFaction.isNormal()) {
                currentFaction.clearClaimOwnership(this.getId());
            }
        }

        factionId = "0"; // The default neutral faction
        chatMode = ChatMode.PUBLIC;
        role = Role.NORMAL;
        title = "";
        autoClaimFor = null;

        if (doSpoutUpdate) {
            SpoutFeatures.updateAppearances(this.getPlayer());
        }
    }

    public void resetFactionData() {
        this.resetFactionData(true);
    }

    // -------------------------------------------- //
    // Getters And Setters
    // -------------------------------------------- //

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(final long lastLoginTime) {
        this.losePowerFromBeingOffline();
        this.lastLoginTime = lastLoginTime;
        lastPowerUpdateTime = lastLoginTime;
        if (Conf.noPVPDamageToOthersForXSecondsAfterLogin > 0) {
            loginPvpDisabled = true;
        }
    }

    public boolean isMapAutoUpdating() {
        return mapAutoUpdating;
    }

    public void setMapAutoUpdating(final boolean mapAutoUpdating) {
        this.mapAutoUpdating = mapAutoUpdating;
    }

    public boolean hasLoginPvpDisabled() {
        if (!loginPvpDisabled) { return false; }
        if (lastLoginTime + Conf.noPVPDamageToOthersForXSecondsAfterLogin * 1000 < System.currentTimeMillis()) {
            loginPvpDisabled = false;
            return false;
        }
        return true;
    }

    public FLocation getLastStoodAt() {
        return lastStoodAt;
    }

    public void setLastStoodAt(final FLocation flocation) {
        lastStoodAt = flocation;
    }

    public void markForDeletion(final boolean delete) {
        deleteMe = delete;
    }

    //----------------------------------------------//
    // Title, Name, Faction Tag and Chat
    //----------------------------------------------//

    // Base:

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getName() {
        return this.getId(); // TODO: ... display name or remove completeley
    }

    public String getTag() {
        if (!this.hasFaction()) { return ""; }
        return this.getFaction().getTag();
    }

    // Base concatenations:

    public String getNameAndSomething(final String something) {
        String ret = role.getPrefix();
        if (something.length() > 0) {
            ret += something + " ";
        }
        ret += this.getName();
        return ret;
    }

    public String getNameAndTitle() {
        return this.getNameAndSomething(this.getTitle());
    }

    public String getNameAndTag() {
        return this.getNameAndSomething(this.getTag());
    }

    // Colored concatenations:
    // These are used in information messages

    public String getNameAndTitle(final Faction faction) {
        return this.getColorTo(faction) + this.getNameAndTitle();
    }

    public String getNameAndTitle(final FPlayer fplayer) {
        return this.getColorTo(fplayer) + this.getNameAndTitle();
    }

    /*public String getNameAndTag(Faction faction)
    {
    	return this.getRelationColor(faction)+this.getNameAndTag();
    }
    public String getNameAndTag(FPlayer fplayer)
    {
    	return this.getRelationColor(fplayer)+this.getNameAndTag();
    }*/

    // TODO: REmovded for refactoring.

    /*public String getNameAndRelevant(Faction faction)
    {
    	// Which relation?
    	Relation rel = this.getRelationTo(faction);
    	
    	// For member we show title
    	if (rel == Relation.MEMBER) {
    		return rel.getColor() + this.getNameAndTitle();
    	}
    	
    	// For non members we show tag
    	return rel.getColor() + this.getNameAndTag();
    }
    public String getNameAndRelevant(FPlayer fplayer)
    {
    	return getNameAndRelevant(fplayer.getFaction());
    }*/

    // Chat Tag: 
    // These are injected into the format of global chat messages.

    public String getChatTag() {
        if (!this.hasFaction()) { return ""; }

        return String.format(Conf.chatTagFormat, role.getPrefix() + this.getTag());
    }

    // Colored Chat Tag
    public String getChatTag(final Faction faction) {
        if (!this.hasFaction()) { return ""; }

        return this.getRelationTo(faction).getColor() + this.getChatTag();
    }

    public String getChatTag(final FPlayer fplayer) {
        if (!this.hasFaction()) { return ""; }

        return this.getColorTo(fplayer) + this.getChatTag();
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

    public Relation getRelationToLocation() {
        return Board.getFactionAt(new FLocation(this)).getRelationTo(this);
    }

    @Override
    public ChatColor getColorTo(final RelationParticipator rp) {
        return RelationUtil.getColorOfThatToMe(this, rp);
    }

    //----------------------------------------------//
    // Health
    //----------------------------------------------//
    public void heal(final int amnt) {
        final Player player = this.getPlayer();
        if (player == null) { return; }
        player.setHealth(((Damageable) player).getHealth() + amnt); // The Nexus
    }

    //----------------------------------------------//
    // Power
    //----------------------------------------------//
    public double getPower() {
        this.updatePower();
        return power;
    }

    protected void alterPower(final double delta) {
        power += delta;
        if (power > this.getPowerMax()) {
            power = this.getPowerMax();
        } else if (power < this.getPowerMin()) {
            power = this.getPowerMin();
        }
    }

    public double getPowerMax() {
        return Conf.powerPlayerMax + powerBoost;
    }

    public double getPowerMin() {
        return Conf.powerPlayerMin + powerBoost;
    }

    public int getPowerRounded() {
        return (int) Math.round(this.getPower());
    }

    public int getPowerMaxRounded() {
        return (int) Math.round(this.getPowerMax());
    }

    public int getPowerMinRounded() {
        return (int) Math.round(this.getPowerMin());
    }

    protected void updatePower() {
        if (this.isOffline()) {
            this.losePowerFromBeingOffline();
            if (!Conf.powerRegenOffline) { return; }
        }
        final long now = System.currentTimeMillis();
        final long millisPassed = now - lastPowerUpdateTime;
        lastPowerUpdateTime = now;

        final Player thisPlayer = this.getPlayer();
        if (thisPlayer != null && thisPlayer.isDead()) { return; // don't let dead players regain power until they respawn
        }

        final int millisPerMinute = 60 * 1000;
        this.alterPower(millisPassed * Conf.powerPerMinute / millisPerMinute);
    }

    protected void losePowerFromBeingOffline() {
        if (Conf.powerOfflineLossPerDay > 0.0 && power > Conf.powerOfflineLossLimit) {
            final long now = System.currentTimeMillis();
            final long millisPassed = now - lastPowerUpdateTime;
            lastPowerUpdateTime = now;

            double loss = millisPassed * Conf.powerOfflineLossPerDay / (24 * 60 * 60 * 1000);
            if (power - loss < Conf.powerOfflineLossLimit) {
                loss = power;
            }
            this.alterPower(-loss);
        }
    }

    public void onDeath() {
        this.updatePower();
        this.alterPower(-Conf.powerPerDeath);
    }

    //----------------------------------------------//
    // Territory
    //----------------------------------------------//
    public boolean isInOwnTerritory() {
        return Board.getFactionAt(new FLocation(this)) == this.getFaction();
    }

    public boolean isInOthersTerritory() {
        final Faction factionHere = Board.getFactionAt(new FLocation(this));
        return factionHere != null && factionHere.isNormal() && factionHere != this.getFaction();
    }

    public boolean isInAllyTerritory() {
        return Board.getFactionAt(new FLocation(this)).getRelationTo(this).isAlly();
    }

    public boolean isInNeutralTerritory() {
        return Board.getFactionAt(new FLocation(this)).getRelationTo(this).isNeutral();
    }

    public boolean isInEnemyTerritory() {
        return Board.getFactionAt(new FLocation(this)).getRelationTo(this).isEnemy();
    }

    public void sendFactionHereMessage() {
        if (SpoutFeatures.updateTerritoryDisplay(this)) { return; }
        final Faction factionHere = Board.getFactionAt(this.getLastStoodAt());
        String msg = P.p.txt.parse("<i>") + " ~ " + factionHere.getTag(this);
        if (factionHere.getDescription().length() > 0) {
            msg += " - " + factionHere.getDescription();
        }
        this.sendMessage(msg);
    }

    // -------------------------------
    // Actions
    // -------------------------------

    public void leave(boolean makePay) {
        final Faction myFaction = this.getFaction();
        makePay = makePay && Econ.shouldBeUsed() && !this.isAdminBypassing();

        if (myFaction == null) {
            this.resetFactionData();
            return;
        }

        final boolean perm = myFaction.isPermanent();

        if (!perm && this.getRole() == Role.ADMIN && myFaction.getFPlayers().size() > 1) {
            this.msg("<b>You must give the admin role to someone else first.");
            return;
        }

        if (!Conf.canLeaveWithNegativePower && this.getPower() < 0) {
            this.msg("<b>You cannot leave until your power is positive.");
            return;
        }

        // if economy is enabled and they're not on the bypass list, make sure they can pay
        if (makePay && !Econ.hasAtLeast(this, Conf.econCostLeave, "to leave your faction.")) { return; }

        final FPlayerLeaveEvent leaveEvent = new FPlayerLeaveEvent(this, myFaction, FPlayerLeaveEvent.PlayerLeaveReason.LEAVE);
        Bukkit.getServer().getPluginManager().callEvent(leaveEvent);
        if (leaveEvent.isCancelled()) { return; }

        // then make 'em pay (if applicable)
        if (makePay && !Econ.modifyMoney(this, -Conf.econCostLeave, "to leave your faction.", "for leaving your faction.")) { return; }

        // Am I the last one in the faction?
        if (myFaction.getFPlayers().size() == 1) {
            // Transfer all money
            if (Econ.shouldBeUsed()) {
                Econ.transferMoney(this, myFaction, this, Econ.getBalance(myFaction.getAccountId()));
            }
        }

        if (myFaction.isNormal()) {
            for (final FPlayer fplayer : myFaction.getFPlayersWhereOnline(true)) {
                fplayer.msg("%s<i> left %s<i>.", this.describeTo(fplayer, true), myFaction.describeTo(fplayer));
            }

            if (Conf.logFactionLeave) {
                P.p.log(this.getName() + " left the faction: " + myFaction.getTag());
            }
        }

        this.resetFactionData();

        if (myFaction.isNormal() && !perm && myFaction.getFPlayers().isEmpty()) {
            // Remove this faction
            for (final FPlayer fplayer : FPlayers.i.getOnline()) {
                fplayer.msg("<i>%s<i> was disbanded.", myFaction.describeTo(fplayer, true));
            }

            myFaction.detach();
            if (Conf.logFactionDisband) {
                P.p.log("The faction " + myFaction.getTag() + " (" + myFaction.getId() + ") was disbanded due to the last player (" + this.getName() + ") leaving.");
            }
        }
    }

    public boolean canClaimForFaction(final Faction forFaction) {
        if (forFaction.isNone()) { return false; }

        if (this.isAdminBypassing() || forFaction == this.getFaction() && this.getRole().isAtLeast(Role.MODERATOR) || forFaction.isSafeZone() && Permission.MANAGE_SAFE_ZONE.has(this.getPlayer()) || forFaction.isWarZone() && Permission.MANAGE_WAR_ZONE.has(this.getPlayer())) { return true; }

        return false;
    }

    public boolean canClaimForFactionAtLocation(final Faction forFaction, final Location location, final boolean notifyFailure) {
        String error = null;
        final FLocation flocation = new FLocation(location);
        final Faction myFaction = this.getFaction();
        final Faction currentFaction = Board.getFactionAt(flocation);
        final int ownedLand = forFaction.getLandRounded();

        if (Conf.worldGuardChecking && Worldguard.checkForRegionsInChunk(location)) {
            // Checks for WorldGuard regions in the chunk attempting to be claimed
            error = P.p.txt.parse("<b>This land is protected");
        } else if (Conf.worldsNoClaiming.contains(flocation.getWorldName())) {
            error = P.p.txt.parse("<b>Sorry, this world has land claiming disabled.");
        } else if (this.isAdminBypassing()) {
            return true;
        } else if (forFaction.isSafeZone() && Permission.MANAGE_SAFE_ZONE.has(this.getPlayer())) {
            return true;
        } else if (forFaction.isWarZone() && Permission.MANAGE_WAR_ZONE.has(this.getPlayer())) {
            return true;
        } else if (myFaction != forFaction) {
            error = P.p.txt.parse("<b>You can't claim land for <h>%s<b>.", forFaction.describeTo(this));
        } else if (forFaction == currentFaction) {
            error = P.p.txt.parse("%s<i> already own this land.", forFaction.describeTo(this, true));
        } else if (this.getRole().value < Role.MODERATOR.value) {
            error = P.p.txt.parse("<b>You must be <h>%s<b> to claim land.", Role.MODERATOR.toString());
        } else if (forFaction.getFPlayers().size() < Conf.claimsRequireMinFactionMembers) {
            error = P.p.txt.parse("Factions must have at least <h>%s<b> members to claim land.", Conf.claimsRequireMinFactionMembers);
        } else if (currentFaction.isSafeZone()) {
            error = P.p.txt.parse("<b>You can not claim a Safe Zone.");
        } else if (currentFaction.isWarZone()) {
            error = P.p.txt.parse("<b>You can not claim a War Zone.");
        } else if (ownedLand >= forFaction.getPowerRounded()) {
            error = P.p.txt.parse("<b>You can't claim more land! You need more power!");
        } else if (Conf.claimedLandsMax != 0 && ownedLand >= Conf.claimedLandsMax && forFaction.isNormal()) {
            error = P.p.txt.parse("<b>Limit reached. You can't claim more land!");
        } else if (currentFaction.getRelationTo(forFaction) == Relation.ALLY) {
            error = P.p.txt.parse("<b>You can't claim the land of your allies.");
        } else if (Conf.claimsMustBeConnected && !this.isAdminBypassing() && myFaction.getLandRoundedInWorld(flocation.getWorldName()) > 0 && !Board.isConnectedLocation(flocation, myFaction) && (!Conf.claimsCanBeUnconnectedIfOwnedByOtherFaction || !currentFaction.isNormal())) {
            if (Conf.claimsCanBeUnconnectedIfOwnedByOtherFaction) {
                error = P.p.txt.parse("<b>You can only claim additional land which is connected to your first claim or controlled by another faction!");
            } else {
                error = P.p.txt.parse("<b>You can only claim additional land which is connected to your first claim!");
            }
        } else if (currentFaction.isNormal()) {
            if (myFaction.isPeaceful()) {
                error = P.p.txt.parse("%s<i> owns this land. Your faction is peaceful, so you cannot claim land from other factions.", currentFaction.getTag(this));
            } else if (currentFaction.isPeaceful()) {
                error = P.p.txt.parse("%s<i> owns this land, and is a peaceful faction. You cannot claim land from them.", currentFaction.getTag(this));
            } else if (!currentFaction.hasLandInflation()) {
                // TODO more messages WARN current faction most importantly
                error = P.p.txt.parse("%s<i> owns this land and is strong enough to keep it.", currentFaction.getTag(this));
            } else if (!Board.isBorderLocation(flocation)) {
                error = P.p.txt.parse("<b>You must start claiming land at the border of the territory.");
            }
        }

        if (notifyFailure && error != null) {
            this.msg(error);
        }
        return error == null;
    }

    public boolean attemptClaim(final Faction forFaction, final Location location, final boolean notifyFailure) {
        // notifyFailure is false if called by auto-claim; no need to notify on every failure for it
        // return value is false on failure, true on success

        final FLocation flocation = new FLocation(location);
        final Faction currentFaction = Board.getFactionAt(flocation);

        final int ownedLand = forFaction.getLandRounded();

        if (!this.canClaimForFactionAtLocation(forFaction, location, notifyFailure)) { return false; }

        // if economy is enabled and they're not on the bypass list, make sure they can pay
        final boolean mustPay = Econ.shouldBeUsed() && !this.isAdminBypassing() && !forFaction.isSafeZone() && !forFaction.isWarZone();
        double cost = 0.0;
        EconomyParticipator payee = null;
        if (mustPay) {
            cost = Econ.calculateClaimCost(ownedLand, currentFaction.isNormal());

            if (Conf.econClaimUnconnectedFee != 0.0 && forFaction.getLandRoundedInWorld(flocation.getWorldName()) > 0 && !Board.isConnectedLocation(flocation, forFaction)) {
                cost += Conf.econClaimUnconnectedFee;
            }

            if (Conf.bankEnabled && Conf.bankFactionPaysLandCosts && this.hasFaction()) {
                payee = this.getFaction();
            } else {
                payee = this;
            }

            if (!Econ.hasAtLeast(payee, cost, "to claim this land")) { return false; }
        }

        // The Nexus Start
        if (!currentFaction.isNone() && !currentFaction.isWarZone() && !currentFaction.isSafeZone() && currentFaction.getHealth() > 0) {
            this.sendMessage(ChatColor.RED + "Vous devez commencer par détruire le " + ChatColor.DARK_RED + "coeur ennemi" + ChatColor.RED + " pour s'emparer de ses territoires...");
            return false;
        }
        // The Nexus End

        final LandClaimEvent claimEvent = new LandClaimEvent(flocation, forFaction, this);
        Bukkit.getServer().getPluginManager().callEvent(claimEvent);
        if (claimEvent.isCancelled()) { return false; }

        // then make 'em pay (if applicable)
        if (mustPay && !Econ.modifyMoney(payee, -cost, "to claim this land", "for claiming this land")) { return false; }

        if (LWCFeatures.getEnabled() && forFaction.isNormal() && Conf.onCaptureResetLwcLocks) {
            LWCFeatures.clearOtherChests(flocation, this.getFaction());
        }

        // announce success
        final Set<FPlayer> informTheseFPlayers = new HashSet<FPlayer>();
        informTheseFPlayers.add(this);
        informTheseFPlayers.addAll(forFaction.getFPlayersWhereOnline(true));
        for (final FPlayer fp : informTheseFPlayers) {
            fp.msg("<h>%s<i> claimed land for <h>%s<i> from <h>%s<i>.", this.describeTo(fp, true), forFaction.describeTo(fp), currentFaction.describeTo(fp));
        }

        Board.setFactionAt(forFaction, flocation);
        // The Nexus Start
        final boolean hasHome = currentFaction.hasHome();
        // The Nexus End
        SpoutFeatures.updateTerritoryDisplayLoc(flocation);

        if (Conf.logLandClaims) {
            P.p.log(this.getName() + " claimed land at (" + flocation.getCoordString() + ") for the faction: " + forFaction.getTag());
        }

        return true;
    }

    // -------------------------------------------- //
    // Persistance
    // -------------------------------------------- //

    @Override
    public boolean shouldBeSaved() {
        if (!this.hasFaction() && (this.getPowerRounded() == this.getPowerMaxRounded() || this.getPowerRounded() == (int) Math.round(Conf.powerPlayerStarting))) { return false; }
        return !deleteMe;
    }

    @Override
    public void msg(final String str, final Object... args) {
        this.sendMessage(P.p.txt.parse(str, args));
    }
}
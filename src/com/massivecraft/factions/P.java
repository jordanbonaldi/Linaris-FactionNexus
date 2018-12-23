package com.massivecraft.factions;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.GsonBuilder;
import org.bukkit.craftbukkit.libs.com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.massivecraft.factions.cmd.CmdAutoHelp;
import com.massivecraft.factions.cmd.FCmdRoot;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.EssentialsFeatures;
import com.massivecraft.factions.integration.LWCFeatures;
import com.massivecraft.factions.integration.SpoutFeatures;
import com.massivecraft.factions.integration.Worldguard;
import com.massivecraft.factions.integration.capi.CapiFeatures;
import com.massivecraft.factions.listeners.FactionsBlockListener;
import com.massivecraft.factions.listeners.FactionsChatListener;
import com.massivecraft.factions.listeners.FactionsEntityListener;
import com.massivecraft.factions.listeners.FactionsExploitListener;
import com.massivecraft.factions.listeners.FactionsPlayerListener;
import com.massivecraft.factions.listeners.FactionsServerListener;
import com.massivecraft.factions.listeners.FactionsWorldListener;
import com.massivecraft.factions.struct.ChatMode;
import com.massivecraft.factions.util.AutoLeaveTask;
import com.massivecraft.factions.util.LazyLocation;
import com.massivecraft.factions.util.MapFLocToStringSetTypeAdapter;
import com.massivecraft.factions.util.MyLocationTypeAdapter;
import com.massivecraft.factions.zcore.MPlugin;
import com.massivecraft.factions.zcore.util.TextUtil;

import net.neferett.linaris.thenexus.CustomEntities;
import net.neferett.linaris.thenexus.MySQL;

public class P extends MPlugin {
    // Our single plugin instance
    public static P p;

    // Listeners
    public final FactionsPlayerListener playerListener;
    public final FactionsChatListener chatListener;
    public final FactionsEntityListener entityListener;
    public final FactionsExploitListener exploitListener;
    public final FactionsBlockListener blockListener;
    public final FactionsServerListener serverListener;

    // Persistance related
    private boolean locked = false;

    public boolean getLocked() {
        return locked;
    }

    public void setLocked(final boolean val) {
        locked = val;
        this.setAutoSave(val);
    }

    private Integer AutoLeaveTask = null;

    // Commands
    public FCmdRoot cmdBase;
    public CmdAutoHelp cmdAutoHelp;

    // The Nexus Start
    public static MySQL database;

    // The Nexus End

    public P() {
        P.p = this;
        playerListener = new FactionsPlayerListener(this);
        chatListener = new FactionsChatListener(this);
        entityListener = new FactionsEntityListener(this);
        exploitListener = new FactionsExploitListener();
        blockListener = new FactionsBlockListener(this);
        serverListener = new FactionsServerListener(this);
    }

    @Override
    public void onEnable() {
        // bit of (apparently absolutely necessary) idiot-proofing for CB version support due to changed GSON lib package name
        try {
            Class.forName("org.bukkit.craftbukkit.libs.com.google.gson.reflect.TypeToken");
        } catch (final ClassNotFoundException ex) {
            this.log(Level.SEVERE, "GSON lib not found. Your CraftBukkit build is too old (< 1.3.2) or otherwise not compatible.");
            this.suicide();
            return;
        }

        if (!this.preEnable()) { return; }
        loadSuccessful = false;

        // Load Conf from disk
        Conf.load();
        FPlayers.i.loadFromDisc();
        Factions.i.loadFromDisc();
        Board.load();

        // Add Base Commands
        cmdBase = new FCmdRoot();
        cmdAutoHelp = new CmdAutoHelp();
        this.getBaseCommands().add(cmdBase);

        EssentialsFeatures.setup();
        SpoutFeatures.setup();
        Econ.setup();
        CapiFeatures.setup();
        LWCFeatures.setup();
        // The Nexus Start
        CustomEntities.registerEntities();
        for (final Faction faction : Factions.i.get()) {
            for (final Entry<String, Long> ennemies : new HashMap<String, Long>(faction.getNexusEnnemies()).entrySet()) {
                if (System.currentTimeMillis() - ennemies.getValue() >= 86400) {
                    faction.getNexusEnnemies().remove(ennemies.getKey());
                }
            }
            faction.resetBrokenChestsTemp();
            faction.spawnNexus(false);
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            FPlayers.i.get(player).updateScoreboard();
        }
        this.getServer().getPluginManager().registerEvents(new FactionsWorldListener(this), this);
        // this.getCommand("quetes").setExecutor(new QuestsCommand());
        this.initDatabase();
        // The Nexus End

        if (Conf.worldGuardChecking || Conf.worldGuardBuildPriority) {
            Worldguard.init(this);
        }

        // start up task which runs the autoLeaveAfterDaysOfInactivity routine
        this.startAutoLeaveTask(false);

        // Register Event Handlers
        this.getServer().getPluginManager().registerEvents(playerListener, this);
        this.getServer().getPluginManager().registerEvents(chatListener, this);
        this.getServer().getPluginManager().registerEvents(entityListener, this);
        this.getServer().getPluginManager().registerEvents(exploitListener, this);
        this.getServer().getPluginManager().registerEvents(blockListener, this);
        this.getServer().getPluginManager().registerEvents(serverListener, this);

        // since some other plugins execute commands directly through this command interface, provide it
        this.getCommand(refCommand).setExecutor(this);

        this.postEnable();
        loadSuccessful = true;
    }

    // The Nexus Start
    public void initDatabase() {
        P.database = new MySQL(this, Conf.dbHost, "3306", Conf.dbName, Conf.dbUser, Conf.dbPass);
        try {
            P.database.openConnection();
            P.database.updateSQL("CREATE TABLE IF NOT EXISTS `players` ( `id` int(11) NOT NULL AUTO_INCREMENT, `name` varchar(30) NOT NULL, `uuid` varbinary(16) NOT NULL, `coins` double NOT NULL, `created_at` datetime NOT NULL, `updated_at` datetime NOT NULL, PRIMARY KEY (`id`) ) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;");
        } catch (ClassNotFoundException | SQLException e) {
            this.getLogger().severe("Impossible de se connecter à la base de données");
            // e.printStackTrace();
        }
    }

    // The Nexus End

    @Override
    public GsonBuilder getGsonBuilder() {
        final Type mapFLocToStringSetType = new TypeToken<Map<FLocation, Set<String>>>() {}.getType();

        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE).registerTypeAdapter(LazyLocation.class, new MyLocationTypeAdapter()).registerTypeAdapter(mapFLocToStringSetType, new MapFLocToStringSetTypeAdapter());
    }

    @Override
    public void onDisable() {
        // only save data if plugin actually completely loaded successfully
        if (loadSuccessful) {
            Board.save();
            Conf.save();
        }
        EssentialsFeatures.unhookChat();
        if (AutoLeaveTask != null) {
            this.getServer().getScheduler().cancelTask(AutoLeaveTask);
            AutoLeaveTask = null;
        }
        // The Nexus Start
        for (final Faction faction : Factions.i.get()) {
            faction.removeNexus();
        }
        CustomEntities.unregisterEntities();
        // The Nexus End
        super.onDisable();
    }

    public void startAutoLeaveTask(final boolean restartIfRunning) {
        if (AutoLeaveTask != null) {
            if (!restartIfRunning) { return; }
            this.getServer().getScheduler().cancelTask(AutoLeaveTask);
        }

        if (Conf.autoLeaveRoutineRunsEveryXMinutes > 0.0) {
            final long ticks = (long) (20 * 60 * Conf.autoLeaveRoutineRunsEveryXMinutes);
            AutoLeaveTask = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new AutoLeaveTask(), ticks, ticks);
        }
    }

    @Override
    public void postAutoSave() {
        Board.save();
        Conf.save();
    }

    @Override
    public boolean logPlayerCommands() {
        return Conf.logPlayerCommands;
    }

    @Override
    public boolean handleCommand(final CommandSender sender, final String commandString, final boolean testOnly) {
        if (sender instanceof Player && FactionsPlayerListener.preventCommand(commandString, (Player) sender)) { return true; }

        return super.handleCommand(sender, commandString, testOnly);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
        // if bare command at this point, it has already been handled by MPlugin's command listeners
        if (split == null || split.length == 0) { return true; }

        // otherwise, needs to be handled; presumably another plugin directly ran the command
        final String cmd = Conf.baseCommandAliases.isEmpty() ? "/f" : "/" + Conf.baseCommandAliases.get(0);
        return this.handleCommand(sender, cmd + " " + TextUtil.implode(Arrays.asList(split), " "), false);
    }

    // -------------------------------------------- //
    // Functions for other plugins to hook into
    // -------------------------------------------- //

    // This value will be updated whenever new hooks are added
    public int hookSupportVersion() {
        return 3;
    }

    // If another plugin is handling insertion of chat tags, this should be used to notify Factions
    public void handleFactionTagExternally(final boolean notByFactions) {
        Conf.chatTagHandledByAnotherPlugin = notByFactions;
    }

    // Simply put, should this chat event be left for Factions to handle? For now, that means players with Faction Chat
    // enabled or use of the Factions f command without a slash; combination of isPlayerFactionChatting() and isFactionsCommand()

    public boolean shouldLetFactionsHandleThisChat(final AsyncPlayerChatEvent event) {
        if (event == null) { return false; }
        return this.isPlayerFactionChatting(event.getPlayer()) || this.isFactionsCommand(event.getMessage());
    }

    // Does player have Faction Chat enabled? If so, chat plugins should preferably not do channels,
    // local chat, or anything else which targets individual recipients, so Faction Chat can be done
    public boolean isPlayerFactionChatting(final Player player) {
        if (player == null) { return false; }
        final FPlayer me = FPlayers.i.get(player);

        if (me == null) { return false; }
        return me.getChatMode().isAtLeast(ChatMode.ALLIANCE);
    }

    // Is this chat message actually a Factions command, and thus should be left alone by other plugins?

    // TODO: GET THIS BACK AND WORKING

    public boolean isFactionsCommand(final String check) {
        if (check == null || check.isEmpty()) { return false; }
        return this.handleCommand(null, check, true);
    }

    // Get a player's faction tag (faction name), mainly for usage by chat plugins for local/channel chat
    public String getPlayerFactionTag(final Player player) {
        return this.getPlayerFactionTagRelation(player, null);
    }

    // Same as above, but with relation (enemy/neutral/ally) coloring potentially added to the tag
    public String getPlayerFactionTagRelation(final Player speaker, final Player listener) {
        String tag = "~";

        if (speaker == null) { return tag; }

        final FPlayer me = FPlayers.i.get(speaker);
        if (me == null) { return tag; }

        // if listener isn't set, or config option is disabled, give back uncolored tag
        if (listener == null || !Conf.chatTagRelationColored) {
            tag = me.getChatTag().trim();
        } else {
            final FPlayer you = FPlayers.i.get(listener);
            if (you == null) {
                tag = me.getChatTag().trim();
            } else {
                tag = me.getChatTag(you).trim();
            }
        }
        if (tag.isEmpty()) {
            tag = "~";
        }

        return tag;
    }

    // Get a player's title within their faction, mainly for usage by chat plugins for local/channel chat
    public String getPlayerTitle(final Player player) {
        if (player == null) { return ""; }

        final FPlayer me = FPlayers.i.get(player);
        if (me == null) { return ""; }

        return me.getTitle().trim();
    }

    // Get a list of all faction tags (names)
    public Set<String> getFactionTags() {
        final Set<String> tags = new HashSet<String>();
        for (final Faction faction : Factions.i.get()) {
            tags.add(faction.getTag());
        }
        return tags;
    }

    // Get a list of all players in the specified faction
    public Set<String> getPlayersInFaction(final String factionTag) {
        final Set<String> players = new HashSet<String>();
        final Faction faction = Factions.i.getByTag(factionTag);
        if (faction != null) {
            for (final FPlayer fplayer : faction.getFPlayers()) {
                players.add(fplayer.getName());
            }
        }
        return players;
    }

    // Get a list of all online players in the specified faction
    public Set<String> getOnlinePlayersInFaction(final String factionTag) {
        final Set<String> players = new HashSet<String>();
        final Faction faction = Factions.i.getByTag(factionTag);
        if (faction != null) {
            for (final FPlayer fplayer : faction.getFPlayersWhereOnline(true)) {
                players.add(fplayer.getName());
            }
        }
        return players;
    }

    // check if player is allowed to build/destroy in a particular location
    public boolean isPlayerAllowedToBuildHere(final Player player, final Location location) {
        return FactionsBlockListener.playerCanBuildDestroyBlock(player, location, "", true);
    }

    // check if player is allowed to interact with the specified block (doors/chests/whatever)
    public boolean isPlayerAllowedToInteractWith(final Player player, final Block block) {
        return FactionsPlayerListener.canPlayerUseBlock(player, block, true);
    }

    // check if player is allowed to use a specified item (flint&steel, buckets, etc) in a particular location
    public boolean isPlayerAllowedToUseThisHere(final Player player, final Location location, final Material material) {
        return FactionsPlayerListener.playerCanUseItemHere(player, location, material, true);
    }
}

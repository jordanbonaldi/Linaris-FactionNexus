package com.massivecraft.factions.zcore;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.GsonBuilder;
import org.bukkit.craftbukkit.libs.com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.zcore.persist.EM;
import com.massivecraft.factions.zcore.persist.SaveTask;
import com.massivecraft.factions.zcore.util.LibLoader;
import com.massivecraft.factions.zcore.util.PermUtil;
import com.massivecraft.factions.zcore.util.Persist;
import com.massivecraft.factions.zcore.util.TextUtil;

public abstract class MPlugin extends JavaPlugin {
    // Some utils
    public Persist persist;
    public TextUtil txt;
    public LibLoader lib;
    public PermUtil perm;

    // Persist related
    public Gson gson;
    private Integer saveTask = null;
    private boolean autoSave = true;
    protected boolean loadSuccessful = false;

    public boolean getAutoSave() {
        return autoSave;
    }

    public void setAutoSave(final boolean val) {
        autoSave = val;
    }

    public String refCommand = "";

    // Listeners
    private MPluginSecretPlayerListener mPluginSecretPlayerListener;
    private MPluginSecretServerListener mPluginSecretServerListener;

    // Our stored base commands
    private final List<MCommand<?>> baseCommands = new ArrayList<MCommand<?>>();

    public List<MCommand<?>> getBaseCommands() {
        return baseCommands;
    }

    // -------------------------------------------- //
    // ENABLE
    // -------------------------------------------- //
    private long timeEnableStart;

    public boolean preEnable() {
        this.log("=== ENABLE START ===");
        timeEnableStart = System.currentTimeMillis();

        // Ensure basefolder exists!
        this.getDataFolder().mkdirs();

        // Create Utility Instances
        perm = new PermUtil(this);
        persist = new Persist(this);
        lib = new LibLoader(this);

        // GSON 2.1 is now embedded in CraftBukkit, used by the auto-updater: https://github.com/Bukkit/CraftBukkit/commit/0ed1d1fdbb1e0bc09a70bc7bfdf40c1de8411665
        //		if ( ! lib.require("gson.jar", "http://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.1/gson-2.1.jar")) return false;
        gson = this.getGsonBuilder().create();

        txt = new TextUtil();
        this.initTXT();

        // attempt to get first command defined in plugin.yml as reference command, if any commands are defined in there
        // reference command will be used to prevent "unknown command" console messages
        try {
            final Map<String, Map<String, Object>> refCmd = this.getDescription().getCommands();
            if (refCmd != null && !refCmd.isEmpty()) {
                refCommand = (String) refCmd.keySet().toArray()[0];
            }
        } catch (final ClassCastException ex) {}

        // Create and register listeners
        mPluginSecretPlayerListener = new MPluginSecretPlayerListener(this);
        mPluginSecretServerListener = new MPluginSecretServerListener(this);
        this.getServer().getPluginManager().registerEvents(mPluginSecretPlayerListener, this);
        this.getServer().getPluginManager().registerEvents(mPluginSecretServerListener, this);

        // Register recurring tasks
        if (saveTask == null && Conf.saveToFileEveryXMinutes > 0.0) {
            final long saveTicks = (long) (20 * 60 * Conf.saveToFileEveryXMinutes); // Approximately every 30 min by default
            saveTask = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new SaveTask(this), saveTicks, saveTicks);
        }

        loadSuccessful = true;
        return true;
    }

    public void postEnable() {
        this.log("=== ENABLE DONE (Took " + (System.currentTimeMillis() - timeEnableStart) + "ms) ===");
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            this.getServer().getScheduler().cancelTask(saveTask);
            saveTask = null;
        }
        // only save data if plugin actually loaded successfully
        if (loadSuccessful) {
            EM.saveAllToDisc();
        }
        this.log("Disabled");
    }

    public void suicide() {
        this.log("Now I suicide!");
        this.getServer().getPluginManager().disablePlugin(this);
    }

    // -------------------------------------------- //
    // Some inits...
    // You are supposed to override these in the plugin if you aren't satisfied with the defaults
    // The goal is that you always will be satisfied though.
    // -------------------------------------------- //

    public GsonBuilder getGsonBuilder() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE);
    }

    // -------------------------------------------- //
    // LANG AND TAGS
    // -------------------------------------------- //

    // These are not supposed to be used directly.
    // They are loaded and used through the TextUtil instance for the plugin.
    public Map<String, String> rawTags = new LinkedHashMap<String, String>();

    public void addRawTags() {
        rawTags.put("l", "<green>"); // logo
        rawTags.put("a", "<gold>"); // art
        rawTags.put("n", "<silver>"); // notice
        rawTags.put("i", "<yellow>"); // info
        rawTags.put("g", "<lime>"); // good
        rawTags.put("b", "<rose>"); // bad
        rawTags.put("h", "<pink>"); // highligh
        rawTags.put("c", "<aqua>"); // command
        rawTags.put("p", "<teal>"); // parameter
    }

    public void initTXT() {
        this.addRawTags();

        final Type type = new TypeToken<Map<String, String>>() {}.getType();

        final Map<String, String> tagsFromFile = persist.load(type, "tags");
        if (tagsFromFile != null) {
            rawTags.putAll(tagsFromFile);
        }
        persist.save(rawTags, "tags");

        for (final Entry<String, String> rawTag : rawTags.entrySet()) {
            txt.tags.put(rawTag.getKey(), TextUtil.parseColor(rawTag.getValue()));
        }
    }

    // -------------------------------------------- //
    // COMMAND HANDLING
    // -------------------------------------------- //

    // can be overridden by P method, to provide option
    public boolean logPlayerCommands() {
        return true;
    }

    public boolean handleCommand(final CommandSender sender, final String commandString, final boolean testOnly) {
        return this.handleCommand(sender, commandString, testOnly, false);
    }

    public boolean handleCommand(final CommandSender sender, String commandString, final boolean testOnly, final boolean async) {
        boolean noSlash = true;
        if (commandString.startsWith("/")) {
            noSlash = false;
            commandString = commandString.substring(1);
        }

        for (final MCommand<?> command : this.getBaseCommands()) {
            if (noSlash && !command.allowNoSlashAccess) {
                continue;
            }

            for (final String alias : command.aliases) {
                // disallow double-space after alias, so specific commands can be prevented (preventing "f home" won't prevent "f  home")
                if (commandString.startsWith(alias + "  ")) { return false; }

                if (commandString.startsWith(alias + " ") || commandString.equals(alias)) {
                    final List<String> args = new ArrayList<String>(Arrays.asList(commandString.split("\\s+")));
                    args.remove(0);

                    if (testOnly) { return true; }

                    if (async) {
                        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                            @Override
                            public void run() {
                                command.execute(sender, args);
                            }
                        });
                    } else {
                        command.execute(sender, args);
                    }

                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleCommand(final CommandSender sender, final String commandString) {
        return this.handleCommand(sender, commandString, false);
    }

    // -------------------------------------------- //
    // HOOKS
    // -------------------------------------------- //
    public void preAutoSave() {

    }

    public void postAutoSave() {

    }

    // -------------------------------------------- //
    // LOGGING
    // -------------------------------------------- //
    public void log(final Object msg) {
        this.log(Level.INFO, msg);
    }

    public void log(final String str, final Object... args) {
        this.log(Level.INFO, txt.parse(str, args));
    }

    public void log(final Level level, final String str, final Object... args) {
        this.log(level, txt.parse(str, args));
    }

    public void log(final Level level, final Object msg) {
        Bukkit.getLogger().log(level, "[" + this.getDescription().getFullName() + "] " + msg);
    }
}

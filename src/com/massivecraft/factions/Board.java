package com.massivecraft.factions;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.libs.com.google.gson.reflect.TypeToken;

import com.massivecraft.factions.integration.LWCFeatures;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.AsciiCompass;
import com.massivecraft.factions.zcore.util.DiscUtil;

public class Board {
    private static transient File file = new File(P.p.getDataFolder(), "board.json");
    private static transient HashMap<FLocation, String> flocationIds = new HashMap<FLocation, String>();

    //----------------------------------------------//
    // Get and Set
    //----------------------------------------------//
    public static String getIdAt(final FLocation flocation) {
        if (!Board.flocationIds.containsKey(flocation)) { return "0"; }

        return Board.flocationIds.get(flocation);
    }

    public static Faction getFactionAt(final FLocation flocation) {
        return Factions.i.get(Board.getIdAt(flocation));
    }

    public static void setIdAt(final String id, final FLocation flocation) {
        Board.clearOwnershipAt(flocation);

        if (id == "0") {
            Board.removeAt(flocation);
        }

        Board.flocationIds.put(flocation, id);
    }

    public static void setFactionAt(final Faction faction, final FLocation flocation) {
        Board.setIdAt(faction.getId(), flocation);
    }

    public static void removeAt(final FLocation flocation) {
        Board.clearOwnershipAt(flocation);
        Board.flocationIds.remove(flocation);
    }

    // not to be confused with claims, ownership referring to further member-specific ownership of a claim
    public static void clearOwnershipAt(final FLocation flocation) {
        final Faction faction = Board.getFactionAt(flocation);
        if (faction != null && faction.isNormal()) {
            faction.clearClaimOwnership(flocation);
        }
    }

    public static void unclaimAll(final String factionId) {
        final Faction faction = Factions.i.get(factionId);
        if (faction != null && faction.isNormal()) {
            faction.clearAllClaimOwnership();
        }

        final Iterator<Entry<FLocation, String>> iter = Board.flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<FLocation, String> entry = iter.next();
            if (entry.getValue().equals(factionId)) {
                if (Conf.onUnclaimResetLwcLocks && LWCFeatures.getEnabled()) {
                    LWCFeatures.clearAllChests(entry.getKey());
                }
                iter.remove();
            }
        }
    }

    // Is this coord NOT completely surrounded by coords claimed by the same faction?
    // Simpler: Is there any nearby coord with a faction other than the faction here?
    public static boolean isBorderLocation(final FLocation flocation) {
        final Faction faction = Board.getFactionAt(flocation);
        final FLocation a = flocation.getRelative(1, 0);
        final FLocation b = flocation.getRelative(-1, 0);
        final FLocation c = flocation.getRelative(0, 1);
        final FLocation d = flocation.getRelative(0, -1);
        return faction != Board.getFactionAt(a) || faction != Board.getFactionAt(b) || faction != Board.getFactionAt(c) || faction != Board.getFactionAt(d);
    }

    // Is this coord connected to any coord claimed by the specified faction?
    public static boolean isConnectedLocation(final FLocation flocation, final Faction faction) {
        final FLocation a = flocation.getRelative(1, 0);
        final FLocation b = flocation.getRelative(-1, 0);
        final FLocation c = flocation.getRelative(0, 1);
        final FLocation d = flocation.getRelative(0, -1);
        return faction == Board.getFactionAt(a) || faction == Board.getFactionAt(b) || faction == Board.getFactionAt(c) || faction == Board.getFactionAt(d);
    }

    //----------------------------------------------//
    // Cleaner. Remove orphaned foreign keys
    //----------------------------------------------//

    public static void clean() {
        final Iterator<Entry<FLocation, String>> iter = Board.flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<FLocation, String> entry = iter.next();
            if (!Factions.i.exists(entry.getValue())) {
                if (Conf.onUnclaimResetLwcLocks && LWCFeatures.getEnabled()) {
                    LWCFeatures.clearAllChests(entry.getKey());
                }
                P.p.log("Board cleaner removed " + entry.getValue() + " from " + entry.getKey());
                iter.remove();
            }
        }
    }

    //----------------------------------------------//
    // Coord count
    //----------------------------------------------//

    public static int getFactionCoordCount(final String factionId) {
        int ret = 0;
        for (final String thatFactionId : Board.flocationIds.values()) {
            if (thatFactionId.equals(factionId)) {
                ret += 1;
            }
        }
        return ret;
    }

    public static int getFactionCoordCount(final Faction faction) {
        return Board.getFactionCoordCount(faction.getId());
    }

    public static int getFactionCoordCountInWorld(final Faction faction, final String worldName) {
        final String factionId = faction.getId();
        int ret = 0;
        final Iterator<Entry<FLocation, String>> iter = Board.flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<FLocation, String> entry = iter.next();
            if (entry.getValue().equals(factionId) && entry.getKey().getWorldName().equals(worldName)) {
                ret += 1;
            }
        }
        return ret;
    }

    //----------------------------------------------//
    // Map generation
    //----------------------------------------------//

    /**
     * The map is relative to a coord and a faction
     * north is in the direction of decreasing x
     * east is in the direction of decreasing z
     */
    public static ArrayList<String> getMap(final Faction faction, final FLocation flocation, final double inDegrees) {
        final ArrayList<String> ret = new ArrayList<String>();
        final Faction factionLoc = Board.getFactionAt(flocation);
        ret.add(P.p.txt.titleize("(" + flocation.getCoordString() + ") " + factionLoc.getTag(faction)));

        final int halfWidth = Conf.mapWidth / 2;
        final int halfHeight = Conf.mapHeight / 2;
        final FLocation topLeft = flocation.getRelative(-halfWidth, -halfHeight);
        final int width = halfWidth * 2 + 1;
        int height = halfHeight * 2 + 1;

        if (Conf.showMapFactionKey) {
            height--;
        }

        final Map<String, Character> fList = new HashMap<String, Character>();
        int chrIdx = 0;

        // For each row
        for (int dz = 0; dz < height; dz++) {
            // Draw and add that row
            String row = "";
            for (int dx = 0; dx < width; dx++) {
                if (dx == halfWidth && dz == halfHeight) {
                    row += ChatColor.AQUA + "+";
                } else {
                    final FLocation flocationHere = topLeft.getRelative(dx, dz);
                    final Faction factionHere = Board.getFactionAt(flocationHere);
                    final Relation relation = faction.getRelationTo(factionHere);
                    if (factionHere.isNone()) {
                        row += ChatColor.GRAY + "-";
                    } else if (factionHere.isSafeZone()) {
                        row += Conf.colorPeaceful + "+";
                    } else if (factionHere.isWarZone()) {
                        row += ChatColor.DARK_RED + "+";
                    } else if (factionHere == faction || factionHere == factionLoc || relation.isAtLeast(Relation.ALLY) || Conf.showNeutralFactionsOnMap && relation.equals(Relation.NEUTRAL) || Conf.showEnemyFactionsOnMap && relation.equals(Relation.ENEMY)) {
                        if (!fList.containsKey(factionHere.getTag())) {
                            fList.put(factionHere.getTag(), Conf.mapKeyChrs[chrIdx++]);
                        }
                        final char tag = fList.get(factionHere.getTag());
                        row += factionHere.getColorTo(faction) + "" + tag;
                    } else {
                        row += ChatColor.GRAY + "-";
                    }
                }
            }
            ret.add(row);
        }

        // Get the compass
        final ArrayList<String> asciiCompass = AsciiCompass.getAsciiCompass(inDegrees, ChatColor.RED, P.p.txt.parse("<a>"));

        // Add the compass
        ret.set(1, asciiCompass.get(0) + ret.get(1).substring(3 * 3));
        ret.set(2, asciiCompass.get(1) + ret.get(2).substring(3 * 3));
        ret.set(3, asciiCompass.get(2) + ret.get(3).substring(3 * 3));

        // Add the faction key
        if (Conf.showMapFactionKey) {
            String fRow = "";
            for (final String key : fList.keySet()) {
                fRow += String.format("%s%s: %s ", ChatColor.GRAY, fList.get(key), key);
            }
            ret.add(fRow);
        }

        return ret;
    }

    // -------------------------------------------- //
    // Persistance
    // -------------------------------------------- //

    public static Map<String, Map<String, String>> dumpAsSaveFormat() {
        final Map<String, Map<String, String>> worldCoordIds = new HashMap<String, Map<String, String>>();

        String worldName, coords;
        String id;

        for (final Entry<FLocation, String> entry : Board.flocationIds.entrySet()) {
            worldName = entry.getKey().getWorldName();
            coords = entry.getKey().getCoordString();
            id = entry.getValue();
            if (!worldCoordIds.containsKey(worldName)) {
                worldCoordIds.put(worldName, new TreeMap<String, String>());
            }

            worldCoordIds.get(worldName).put(coords, id);
        }

        return worldCoordIds;
    }

    public static void loadFromSaveFormat(final Map<String, Map<String, String>> worldCoordIds) {
        Board.flocationIds.clear();

        String worldName;
        String[] coords;
        int x, z;
        String factionId;

        for (final Entry<String, Map<String, String>> entry : worldCoordIds.entrySet()) {
            worldName = entry.getKey();
            for (final Entry<String, String> entry2 : entry.getValue().entrySet()) {
                coords = entry2.getKey().trim().split("[,\\s]+");
                x = Integer.parseInt(coords[0]);
                z = Integer.parseInt(coords[1]);
                factionId = entry2.getValue();
                Board.flocationIds.put(new FLocation(worldName, x, z), factionId);
            }
        }
    }

    public static boolean save() {
        //Factions.log("Saving board to disk");

        try {
            DiscUtil.write(Board.file, P.p.gson.toJson(Board.dumpAsSaveFormat()));
        } catch (final Exception e) {
            e.printStackTrace();
            P.p.log("Failed to save the board to disk.");
            return false;
        }

        return true;
    }

    public static boolean load() {
        P.p.log("Loading board from disk");

        if (!Board.file.exists()) {
            P.p.log("No board to load from disk. Creating new file.");
            Board.save();
            return true;
        }

        try {
            final Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
            final Map<String, Map<String, String>> worldCoordIds = P.p.gson.fromJson(DiscUtil.read(Board.file), type);
            Board.loadFromSaveFormat(worldCoordIds);
        } catch (final Exception e) {
            e.printStackTrace();
            P.p.log("Failed to load the board from disk.");
            return false;
        }

        return true;
    }
}

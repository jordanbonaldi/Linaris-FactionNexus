package com.massivecraft.factions.util;

import java.lang.reflect.Type;
import java.util.logging.Level;

import org.bukkit.craftbukkit.libs.com.google.gson.JsonDeserializationContext;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonDeserializer;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonElement;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonObject;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonParseException;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonSerializationContext;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonSerializer;

import com.massivecraft.factions.P;

public class MyLocationTypeAdapter implements JsonDeserializer<LazyLocation>, JsonSerializer<LazyLocation> {
    private static final String WORLD = "world";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String YAW = "yaw";
    private static final String PITCH = "pitch";

    @Override
    public LazyLocation deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        try {
            final JsonObject obj = json.getAsJsonObject();

            final String worldName = obj.get(MyLocationTypeAdapter.WORLD).getAsString();
            final double x = obj.get(MyLocationTypeAdapter.X).getAsDouble();
            final double y = obj.get(MyLocationTypeAdapter.Y).getAsDouble();
            final double z = obj.get(MyLocationTypeAdapter.Z).getAsDouble();
            final float yaw = obj.get(MyLocationTypeAdapter.YAW).getAsFloat();
            final float pitch = obj.get(MyLocationTypeAdapter.PITCH).getAsFloat();

            return new LazyLocation(worldName, x, y, z, yaw, pitch);

        } catch (final Exception ex) {
            ex.printStackTrace();
            P.p.log(Level.WARNING, "Error encountered while deserializing a LazyLocation.");
            return null;
        }
    }

    @Override
    public JsonElement serialize(final LazyLocation src, final Type typeOfSrc, final JsonSerializationContext context) {
        final JsonObject obj = new JsonObject();

        try {
            obj.addProperty(MyLocationTypeAdapter.WORLD, src.getWorldName());
            obj.addProperty(MyLocationTypeAdapter.X, src.getX());
            obj.addProperty(MyLocationTypeAdapter.Y, src.getY());
            obj.addProperty(MyLocationTypeAdapter.Z, src.getZ());
            obj.addProperty(MyLocationTypeAdapter.YAW, src.getYaw());
            obj.addProperty(MyLocationTypeAdapter.PITCH, src.getPitch());

            return obj;
        } catch (final Exception ex) {
            ex.printStackTrace();
            P.p.log(Level.WARNING, "Error encountered while serializing a LazyLocation.");
            return obj;
        }
    }
}

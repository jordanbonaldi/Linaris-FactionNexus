package com.massivecraft.factions.integration;

import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.earth2me.essentials.chat.EssentialsChat;
import com.earth2me.essentials.chat.IEssentialsChatListener;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;

/*
 * This Essentials integration handler is for older 2.x.x versions of Essentials which have "IEssentialsChatListener"
 */

public class EssentialsOldVersionFeatures {
    private static EssentialsChat essChat;

    public static void integrateChat(final EssentialsChat instance) {
        EssentialsOldVersionFeatures.essChat = instance;
        try {
            EssentialsOldVersionFeatures.essChat.addEssentialsChatListener("Factions", new IEssentialsChatListener() {
                @Override
                public boolean shouldHandleThisChat(final AsyncPlayerChatEvent event) {
                    return P.p.shouldLetFactionsHandleThisChat(event);
                }

                @Override
                public String modifyMessage(final AsyncPlayerChatEvent event, final Player target, final String message) {
                    // The Nexus Start
                    final Faction faction = FPlayers.i.get(target).getFaction();
                    return message.replace(Conf.chatTagReplaceString, P.p.getPlayerFactionTagRelation(event.getPlayer(), target)).replace("[FACTION_TITLE]", P.p.getPlayerTitle(event.getPlayer())).replace("[FACTION_LEVEL]", !faction.isNormal() ? "" : String.valueOf(faction.getLevel().getId()));
                    // The Nexus End
                }
            });
            P.p.log("Found and will integrate chat with " + EssentialsOldVersionFeatures.essChat.getDescription().getFullName());

            // As of Essentials 2.8+, curly braces are not accepted and are instead replaced with square braces, so... deal with it
            if (EssentialsOldVersionFeatures.essChat.getDescription().getVersion().startsWith("2.8.") && Conf.chatTagReplaceString.contains("{")) {
                Conf.chatTagReplaceString = Conf.chatTagReplaceString.replace("{", "[").replace("}", "]");
                P.p.log("NOTE: as of Essentials 2.8+, we've had to switch the default chat replacement tag from \"{FACTION}\" to \"[FACTION]\". This has automatically been updated for you.");
            }
        } catch (final NoSuchMethodError ex) {
            EssentialsOldVersionFeatures.essChat = null;
        }
    }

    public static void unhookChat() {
        if (EssentialsOldVersionFeatures.essChat != null) {
            EssentialsOldVersionFeatures.essChat.removeEssentialsChatListener("Factions");
        }
    }
}

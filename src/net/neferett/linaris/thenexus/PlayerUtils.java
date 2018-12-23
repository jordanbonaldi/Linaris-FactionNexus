package net.neferett.linaris.thenexus;

import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.massivecraft.factions.P;

public class PlayerUtils {

    public static void giveCoins(final Player player, final int count) {
        player.sendMessage(ChatColor.GRAY + "Gain de FunCoins + " + ChatColor.GOLD + count);
        new BukkitRunnable() {

            @Override
            public void run() {
                try {
                    if (!P.database.checkConnection()) {
                        P.p.initDatabase();
                    }
                    try {
                        P.database.updateSQL("UPDATE players SET coins=coins+" + count + ", name='" + player.getName() + "', updated_at=NOW() WHERE uuid=UNHEX('" + player.getUniqueId().toString().replace("-", "") + "')");
                    } catch (ClassNotFoundException | SQLException e) {
                        e.printStackTrace();
                    }
                } catch (SQLException e) {
                    P.p.initDatabase();
                }
            }
        }.runTaskAsynchronously(P.p);
    }
}

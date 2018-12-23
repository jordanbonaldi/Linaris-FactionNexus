package net.neferett.linaris.thenexus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class QuestsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Vous devez être un joueur.");
            return true;
        }
        final Player player = (Player) sender;
        final Inventory inv = Bukkit.createInventory(player, 9, "Quêtes");
        //inv.addItem(new ItemBuilder(Material.RAW_BEEF).setTitle(ChatColor.GOLD+"Boucher").addLores((ChatColor.GOLD + "Niveau actuel : " + ChatColor.YELLOW + data.getSwMoreSheep() + "/2\n" + (data.getSwMoreSheep() <= 0 ? ChatColor.RED + "Non possédé" : ChatColor.GRAY + "Vous obtenez " + ChatColor.AQUA + (data.getSwMoreSheep() < 2 ? data.getSwMoreSheep() : 2) + " mouton" + (data.getSwMoreSheep() > 1 ? "s" : "") + ChatColor.GRAY + " quand\n" + ChatColor.GRAY + "vous tuez quelqu'un") + (data.getSwMoreSheep() < 2 ? "\n\n" + ChatColor.GOLD + "Prochain niveau : " + ChatColor.YELLOW + (data.getSwMoreSheep() == 0 ? 200 : 800) + ChatColor.GOLD + " FC\n" + ChatColor.GRAY + "Vous obtenez " + ChatColor.AQUA + (data.getSwMoreSheep() + 1 < 2 ? data.getSwMoreSheep() + 1 : 2) + " mouton" + (data.getSwMoreSheep() + 1 > 1 ? "s" : "") + ChatColor.GRAY + " quand\n" + ChatColor.GRAY + "vous tuez quelqu'un" : "")).split("\n")).build());
        player.openInventory(inv);
        return false;
    }
}

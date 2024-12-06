package me.mchiappinam.pdghcash;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Comando implements CommandExecutor {

	private Main plugin;
	public Comando(Main main) {
		plugin=main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("cash")) {
			if(sender!=plugin.getServer().getConsoleSender()) {
				if(args.length==0) {
					((Player)sender).openInventory(plugin.menu((Player)sender));
					return true;
				}
				if(args.length!=2) {
					sender.sendMessage("§3§l[Cash] §cUse /cash");
					return true;
				}
				if(args[0].equalsIgnoreCase("desbanir")) {
					if(!plugin.permitidoDesbanir.contains(sender.getName().toLowerCase().trim())) {
						sender.sendMessage("§3§l[Cash] §cCompre o desban no /cash");
						return true;
					}
					plugin.permitidoDesbanir.remove(sender.getName().toLowerCase().trim());
					plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "unban "+args[1].trim());
					plugin.getServer().broadcastMessage("§3§l[Cash] §a"+sender.getName()+" acaba de desbanir o jogador \"§c"+args[1].trim()+"§a\"");
					plugin.log((Player)sender, "DESBANIU "+args[1].trim());
					return true;
				}
				sender.sendMessage("§3§l[Cash] §cUse /cash§a ou §c/cash desbanir <nick>");
				return true;
			}
			if(args[0].equalsIgnoreCase("give")) {
				if(args.length!=3) {
					sender.sendMessage("§3§l[Cash] §cUse /cash give <nick> <valor>");
					return true;
				}
				int quantidade=Integer.parseInt(args[2]);
				plugin.addCash(args[1], quantidade);
				return true;
			}
			sender.sendMessage("§3§l[Cash] §cUse /cash give <nick> <valor>");
			return true;
		}
		return false;
	}
}

package me.mchiappinam.pdghcash;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class Listeners implements Listener {

	private Main plugin;

	public Listeners(Main main) {
		plugin = main;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent e) {
		if(!plugin.jogadores.containsKey(e.getPlayer().getName().toLowerCase()))
			plugin.join(e.getPlayer());
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onCommand(PlayerCommandPreprocessEvent e) {
		if(e.getMessage().toLowerCase().startsWith("/fly")) {
			if(plugin.canFly(e.getPlayer().getName().toLowerCase().trim())) {
				if(!e.getPlayer().getWorld().getName().contains("plotworld")) {
					e.getPlayer().sendMessage("§3§l[Cash] §cVocê pode usar o /fly apenas na cidade.");
					e.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerTeleport(PlayerChangedWorldEvent e) {
		if(e.getFrom().getName().contains("plotworld"))
			if(e.getPlayer().getAllowFlight())
				if(plugin.canFly(e.getPlayer().getName().toLowerCase().trim())) {
					e.getPlayer().setAllowFlight(false);
					e.getPlayer().setFlying(false);
					e.getPlayer().sendMessage("§3§l[Cash] §cVocê pode usar o /fly apenas na cidade.");
					e.getPlayer().sendMessage("§3§l[Cash] §cSeu fly foi desativado.");
				}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		Player p = (Player)e.getWhoClicked();
	    ItemStack clicked = e.getCurrentItem();
	    if(e.getInventory().getName().equalsIgnoreCase("§3PDGHCash§r - §eMenu de Compra")) {
	    	e.setCancelled(true);
			if((clicked == null)||
					(!clicked.containsEnchantment(Enchantment.OXYGEN))||
					(!clicked.hasItemMeta())||
					(!clicked.getItemMeta().hasDisplayName())) {
				p.sendMessage("§3§l[Cash] §cVocê pode apenas clicar nos itens do menu.");
				p.closeInventory();
		    	return;
			}
			if (clicked.getItemMeta().getDisplayName().contains("§3Liberar compras")) {
				if(!plugin.task.containsKey(p.getName())) {
					if(plugin.jogadores.containsKey(p.getName().toLowerCase())) {
						p.sendMessage("§3§l[Cash] §2Compras liberadas por 30 segundos. Digite novamente o comando §c/cash§2 e faça sua compra!");
						plugin.startTask(p);
						p.closeInventory();
						return;
					}else{
						p.sendMessage("§3§l[Cash] §cIdentificamos que você nunca comprou cash.");
						p.sendMessage("§3§l[Cash] §6Compre seu cash no site www.PDGH.net/vip");
						p.sendMessage("§3§l[Cash] §6R$0,01 (1 centavo de real) equivale à 1$ cash.");
						return;
					}
				}else{
					plugin.cancelTask(p);
					p.sendMessage("§3§l[Cash] §cCompras bloqueadas.");
					p.closeInventory();
					return;
				}
			}else if (clicked.getItemMeta().getDisplayName().contains("§6§lVIP")) {
				if(!plugin.task.containsKey(p.getName())) {
					p.sendMessage("§3§l[Cash] §cSuas compras estão bloqueadas. Clique em \"§3Liberar compras§c\" primeiro para efetuar sua compra.");
					p.closeInventory();
					return;
				}
				plugin.cancelTask(p);
				plugin.removeCashCMD(p.getName(), 350, "darvip "+p.getName()+" VIP 10");
				p.closeInventory();
				return;
			}else if (clicked.getItemMeta().getDisplayName().contains("§c§lDESBANIR JOGADOR")) {
				p.closeInventory();
				if(!plugin.task.containsKey(p.getName())) {
					p.sendMessage("§3§l[Cash] §cSuas compras estão bloqueadas. Clique em \"§3Liberar compras§c\" primeiro para efetuar sua compra.");
					return;
				}
				if(plugin.getCashAtual(p.getName())<2500) {
					p.sendMessage("§3§l[Cash] §cVocê não tem cash suficiente para isso.");
					return;
				}
				plugin.cancelTask(p);
				plugin.removeCash(p.getName(), 2500);
				plugin.permitidoDesbanir.add(p.getName().toLowerCase().trim());
				p.sendMessage("§3§l[Cash] §aAgora você pode desbanir o jogador desejado com o comando §c§l/cash desbanir <nick>§a.");
				p.sendMessage("§3§l[Cash] §aApós o servidor se reiniciar, você perderá esse direito caso deixe de usar. Isso não é reembolsável!");
				return;
			}else if (clicked.getItemMeta().getDisplayName().contains("§6§lVoe Livremente")) {
				p.closeInventory();
				if(!plugin.task.containsKey(p.getName())) {
					p.sendMessage("§3§l[Cash] §cSuas compras estão bloqueadas. Clique em \"§3Liberar compras§c\" primeiro para efetuar sua compra.");
					return;
				}
				if(plugin.canFly(p.getName().toLowerCase().trim())) {
					p.sendMessage("§3§l[Cash] §cVocê já pode voar na cidade (PlotMe) com o comando §c§l/fly§a.");
					p.sendMessage("§3§l[Cash] §cCompra cancelada.");
					return;
				}
				if(plugin.getCashAtual(p.getName())<300) {
					p.sendMessage("§3§l[Cash] §cVocê não tem cash suficiente para isso.");
					return;
				}
				plugin.cancelTask(p);
				plugin.removeCash(p.getName(), 300);
				plugin.setFly(p.getName().toLowerCase().trim(),true);
				p.sendMessage("§3§l[Cash] §aAgora você pode voar na cidade (PlotMe) com o comando §c§l/fly§a.");
				p.setFlySpeed(0.1f);
				plugin.getServer().broadcastMessage("§3§l[Cash]");
				plugin.getServer().broadcastMessage("§c"+p.getName()+"§a acaba de comprar seu fly permanente");
				plugin.getServer().broadcastMessage("§3§l[Cash]");
				return;
			}else if (clicked.getItemMeta().getDisplayName().contains("§2§lKit Plantação")) {
				if(!plugin.task.containsKey(p.getName())) {
					p.sendMessage("§3§l[Cash] §cSuas compras estão bloqueadas. Clique em \"§3Liberar compras§c\" primeiro para efetuar sua compra.");
					p.closeInventory();
					return;
				}
				plugin.cancelTask(p);
				plugin.removeCashCMD(p.getName(), 1000, "daritens "+p.getName()+" PLANT 1");
				p.closeInventory();
				return;
			}else if (clicked.getItemMeta().getDisplayName().contains("§2§lKit PP4")) {
				if(!plugin.task.containsKey(p.getName())) {
					p.sendMessage("§3§l[Cash] §cSuas compras estão bloqueadas. Clique em \"§3Liberar compras§c\" primeiro para efetuar sua compra.");
					p.closeInventory();
					return;
				}
				plugin.cancelTask(p);
				plugin.removeCashCMD(p.getName(), 500, "daritens "+p.getName()+" PP4 1");
				p.closeInventory();
				return;
			}else if (clicked.getItemMeta().getDisplayName().contains("§2§lKit GLAD")) {
				if(!plugin.task.containsKey(p.getName())) {
					p.sendMessage("§3§l[Cash] §cSuas compras estão bloqueadas. Clique em \"§3Liberar compras§c\" primeiro para efetuar sua compra.");
					p.closeInventory();
					return;
				}
				plugin.cancelTask(p);
				plugin.removeCashCMD(p.getName(), 250, "daritens "+p.getName()+" GLAD 1");
				p.closeInventory();
				return;
			}
			//p.sendMessage("§3§l[Cash] §cVocê pode apenas clicar nos itens do menu.");
			//p.closeInventory();
			return;
	    }
	}
	  
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		if(plugin.task.containsKey(e.getPlayer().getName()))
			plugin.cancelTask(e.getPlayer());
	}
		
	@EventHandler
	public void onPlayerKick(PlayerKickEvent e) {
		if(plugin.task.containsKey(e.getPlayer().getName()))
			plugin.cancelTask(e.getPlayer());
	}
}

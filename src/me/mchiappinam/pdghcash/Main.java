package me.mchiappinam.pdghcash;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin {
	public static String mysql_url = "";
    public static String mysql_user = "";
    public static String mysql_pass = "";
    public static String tabela = "";
    //public static String tabelafly = "";

    HashMap<String, String> jogadores = new HashMap<String, String>();//0nickLowerCase , [0cashTotal/1comprasTotais/2cashAtual/3canFly]
	List<String> permitidoDesbanir = new ArrayList<String>();
	HashMap<String, Integer> task = new HashMap<String, Integer>();
	protected static Economy econ = null;

	public void onEnable() {
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2iniciando...");
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2verificando se a config existe...");
		File file = new File(getDataFolder(),"config.yml");
		if(!file.exists()) {
			try {
				getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2config inexistente, criando config...");
				saveResource("config_template.yml",false);
				File file2 = new File(getDataFolder(),"config_template.yml");
				file2.renameTo(new File(getDataFolder(),"config.yml"));
				getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2config criada");
			}catch(Exception e) {getLogger().warning("ERRO: Não foi possível criar a config. Mais detalhes: "+e.toString());}
		}

		mysql_url="jdbc:mysql://"+getConfig().getString("mySQL.ip")+":"+getConfig().getString("mySQL.porta")+"/"+getConfig().getString("mySQL.db");
		mysql_user=getConfig().getString("mySQL.usuario");
		mysql_pass=getConfig().getString("mySQL.senha");
		tabela=getConfig().getString("mySQL.tabela");
		//tabelafly=getConfig().getString("mySQL.tabelafly");
		
		try {
			Connection con = DriverManager.getConnection(mysql_url,mysql_user,mysql_pass);
			if (con == null) {
				getLogger().warning("ERRO: Conexao ao banco de dados MySQL falhou!");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}else{
				Statement st = con.createStatement();
				st.execute("CREATE TABLE IF NOT EXISTS `"+tabela+"` ( `id` MEDIUMINT NOT NULL AUTO_INCREMENT, `nick` text, `db` text, PRIMARY KEY (`id`))");
				//st.execute("CREATE TABLE IF NOT EXISTS `"+tabelafly+"` ( `id` MEDIUMINT NOT NULL AUTO_INCREMENT, `nick` text, `db` text, PRIMARY KEY (`id`))");
				st.close();
				getServer().getConsoleSender().sendMessage("§3[PDGHCash] §3Conectado ao banco de dados MySQL!");
			}
			con.close();
		}catch (SQLException e) {
			getLogger().warning("ERRO: Conexao ao banco de dados MySQL falhou!");
			getLogger().warning("ERRO: "+e.toString());
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2registrando eventos...");
		getServer().getPluginManager().registerEvents(new Listeners(this), this);
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2eventos registrados");
	    if(!setupEconomy()) {
	    	getLogger().warning("ERRO: Vault (Economia) nao encontrado. Desativando plugin...");
			getServer().getPluginManager().disablePlugin(this);
			return;
	    }else{
	    	getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2Sucesso: Vault (Economia) encontrado.");
	    }
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2definindo comandos...");
	    getServer().getPluginCommand("cash").setExecutor(new Comando(this));
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2comandos definidos");
		cacheToDB();

		if(getServer().getOnlinePlayers().size() != 0)
	  		for (Player p : getServer().getOnlinePlayers())
	  			join(p);
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2ativado - Developed by mchiappinam");
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2Acesse: http://pdgh.com.br/");
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2Acesse: https://hostload.com.br/");
	}
	    
	public void onDisable() {
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2desativando...");
		if(getServer().getOnlinePlayers().size() != 0)
	  		for (Player p : getServer().getOnlinePlayers())
	  			p.closeInventory();
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2salvando cache...");
		forceSaveAllCache();
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2desativado - Developed by mchiappinam");
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2Acesse: http://pdgh.com.br/");
		getServer().getConsoleSender().sendMessage("§3[PDGHCash] §2Acesse: https://hostload.com.br/");
	}
	
	public void startTask(final Player p) {
		task.put(p.getName(), getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				p.sendMessage("§3§l[Cash]§c Passaram-se 30 segundos. Compras bloqueadas.");
				//task.remove(p.getName());
				cancelTask(p);
			}
		}, 30*20L));
	}
	
	protected boolean jaComprou(String p) {
		if(jogadores.containsKey(p.trim().toLowerCase()))
			return true;
		else
			return false;
	}
	
	////0nickLowerCase , [0cashTotal/1comprasTotais/2cashAtual]
	protected int getCashTotal(String p) {
		if(jaComprou(p.trim().toLowerCase()))
			return Integer.parseInt(jogadores.get(p.trim().toLowerCase()).split(",")[0]);
		else
			return 0;
	}
	protected int getComprasTotais(String p) {
		if(jaComprou(p.trim().toLowerCase()))
			return Integer.parseInt(jogadores.get(p.trim().toLowerCase()).split(",")[1]);
		else
			return 0;
	}
	protected int getCashAtual(String p) {
		if(jaComprou(p.trim().toLowerCase()))
			return Integer.parseInt(jogadores.get(p.trim().toLowerCase()).split(",")[2]);
		else
			return 0;
	}
	/*protected int getFly(String p) {
		if(!jaComprou(p.trim().toLowerCase()))
			return 0;
		if((jogadores.get(p.trim().toLowerCase()).split(",")[3]) == null)
			return 0;
		return Integer.parseInt(jogadores.get(p.trim().toLowerCase()).split(",")[3]);
	}*/
	protected boolean canFly(String p) {
		if(!jaComprou(p.trim().toLowerCase()))
			return false;
		else if(jogadores.get(p.trim().toLowerCase()).split(",").length<4)
			return false;
		else if((jogadores.get(p.trim().toLowerCase()).split(",")[3]) == null)
			return false;
		else if((jogadores.get(p.trim().toLowerCase()).split(",")[3]).contains("1"))
			return true;
		else
			return false;
	}
	
	////0nickLowerCase , [0cashTotal/1comprasTotais/2cashAtual/3canFly]
	protected void setFly(String p, boolean resultado) {
		if(jaComprou(p.trim().toLowerCase()))
			jogadores.put(p.trim().toLowerCase(), getCashTotal(p)+","+getComprasTotais(p)+","+getCashAtual(p)+","+(resultado? "1" : "0"));
		else
			jogadores.put(p.trim().toLowerCase(), "0,0,0,"+(resultado? "1" : "0"));
		if(resultado)
			getServer().dispatchCommand(getServer().getConsoleSender(), "pex user "+p+" add "+getConfig().getString("comando.fly")+" plotworld");
		else {
			getServer().dispatchCommand(getServer().getConsoleSender(), "pex user "+p+" remove "+getConfig().getString("comando.fly"));
			getServer().dispatchCommand(getServer().getConsoleSender(), "pex user "+p+" remove "+getConfig().getString("comando.fly")+" plotworld");
		}
	}
	protected void addCash(String p, int quantidade) {
		if(jaComprou(p.trim().toLowerCase()))
			jogadores.put(p.trim().toLowerCase(), (getCashTotal(p)+quantidade)+","+getComprasTotais(p)+","+(getCashAtual(p)+quantidade)+","+(canFly(p)? "1" : "0"));
		else
			jogadores.put(p.trim().toLowerCase(), quantidade+",0,"+quantidade+",0");
		getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §aVocê acaba de receber §c"+quantidade+"$ cash§a.");
		getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §aUsufrua do seu cash com §c/cash§a.");
	}
	
	protected void removeCashCMD(String p, int quantidade, String comando) {
		if(!jaComprou(p.trim().toLowerCase())) {
			getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §cIdentificamos que você nunca comprou cash.");
			getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §6Compre seu cash no site www.PDGH.net/vip");
			getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §6R$0,01 (1 centavo de real) equivale à 1$ cash.");
			return;
		}
		if(getCashAtual(p)<quantidade) {
			getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §cVocê não tem cash suficiente para isso.");
			return;
		}
		jogadores.put(p.trim().toLowerCase(), getCashTotal(p)+","+(getComprasTotais(p)+1)+","+(getCashAtual(p)-quantidade)+","+(canFly(p)? "1" : "0"));
		getServer().dispatchCommand(getServer().getConsoleSender(), comando);
		getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §aFoi debitado de sua conta §c"+quantidade+"$ cash§a.");
		log(getServer().getPlayerExact(p), "Cash: "+quantidade+" - Comando: "+comando);
	}
	
	protected void removeCash(String p, int quantidade) {
		if(!jaComprou(p.trim().toLowerCase())) {
			getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §cIdentificamos que você nunca comprou cash.");
			getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §6Compre seu cash no site www.PDGH.net/vip");
			getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §6R$0,01 (1 centavo de real) equivale à 1$ cash.");
			return;
		}
		if(getCashAtual(p)<quantidade) {
			getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §cVocê não tem cash suficiente para isso.");
			return;
		}
		jogadores.put(p.trim().toLowerCase(), getCashTotal(p)+","+((int)getComprasTotais(p)+1)+","+(getCashAtual(p)-quantidade)+","+(canFly(p)? "1" : "0"));
		getServer().getPlayerExact(p).sendMessage("§3§l[Cash] §aFoi debitado de sua conta §c"+quantidade+"$ cash§a.");
		log(getServer().getPlayerExact(p), "Cash: "+quantidade+" - SEM COMANDO");
	}
	
	public void cancelTask(Player p) {
		Bukkit.getScheduler().cancelTask(task.get(p.getName()));
		task.remove(p.getName());
	}
	
	public void cacheToDB() {
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
	  		public void run() {
	  			saveAllCache();
	  		}
	  	}, 0, 120*20);
	}
	
	public void saveAllCache() {
		Threads t = new Threads(this,"saveAllCache");
		t.start();
	}
	
	public void join(Player p) {
		Threads t = new Threads(this,"join",p);
		t.start();
	}
	
	public Inventory menu(Player p) {
		Inventory menuu = getServer().createInventory(null, 27, "§3PDGHCash§r - §eMenu de Compra");
	    menuu.setItem(13, vip(p));
	    menuu.setItem(18, plantacao(p));
	    menuu.setItem(19, PP4(p));
	    menuu.setItem(20, GLAD(p));
	    menuu.setItem(22, desban(p));
	    menuu.setItem(23, fly(p));
	    menuu.setItem(25, diamante(p));
	    menuu.setItem(26, barreira(p));
	    return menuu;
	}
	
	public void forceSaveAllCache() {
		for(String nick : jogadores.keySet()) {
			try {
				Connection con = DriverManager.getConnection(Main.mysql_url,Main.mysql_user,Main.mysql_pass);
				//Prepared statement
				PreparedStatement pst = con.prepareStatement("SELECT `nick`,`db` FROM `"+Main.tabela+"` WHERE nick='"+nick.toLowerCase().trim()+"';");
				ResultSet rs = pst.executeQuery();
				boolean existe=false;
				while(rs.next()) {
					existe=true;
					rs.close();
					pst.close();
					PreparedStatement pst1 = con.prepareStatement("UPDATE `"+Main.tabela+"` SET `db`='"+jogadores.get(nick.toLowerCase().trim())+"' WHERE nick='"+nick.toLowerCase().trim()+"';");
					pst1.executeUpdate();
					pst1.close();
					con.close();
					break;
				}
				if(!existe) {
					//Prepared statement
					PreparedStatement pst1 = con.prepareStatement("INSERT INTO `"+Main.tabela+"`(nick, db) VALUES(?, ?)");
					//Values
					pst1.setString(1, nick.toLowerCase().trim());
					pst1.setString(2, jogadores.get(nick.toLowerCase().trim()));
					//Do the MySQL query
					pst1.executeUpdate();
					pst1.close();
					con.close();
					break;
				}
			}catch (SQLException ex) {
				System.out.print(ex);
				break;
			}
		}
	}
    
    public static String calendario(String dateFormat) {
		Calendar agora = Calendar.getInstance();
		SimpleDateFormat gdf = new SimpleDateFormat(dateFormat);
        return gdf.format(agora.getTime());
    }

    public void log(CommandSender sender, String mensagem) {
    	getLogger().info("Adicionando jogador "+sender.getName()+" para a log: "+mensagem);
        try {
		    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(getDataFolder()+File.separator+"log.txt", true)));
            out.println("DIA: "+calendario("dd/MM/yyyy")+" HORAS: "+calendario("HH:mm:ss.SSS")+" - "+sender.getName()+" - "+mensagem);
		    out.close();
        } catch (IOException ex) {
        	getLogger().warning("Erro ao salvar as informações na log. Mensagem do servidor: "+ex.getMessage());
        }
    }
    
	protected ItemStack vip(Player p) {
		ItemStack diamante = new ItemStack(Material.GOLD_INGOT, 1);
		ItemMeta diamantem = diamante.getItemMeta();
		diamantem.setDisplayName("§6§lVIP");
		List<String> diamantel = new ArrayList<String>();
		diamantel.add("§7§lPreço: §c350$ cash");
		diamantel.add(" ");
		diamantel.add("§cEsvazie o inventário antes de ativar seu VIP");
		diamantel.add(" ");
		diamantel.add("§aAo ativar o VIP, você recebe diversos");
		diamantel.add("§aitens e todas as vantagens de um VIP");
		diamantel.add("§apor 10 dias.");
		diamantel.add(" ");
		diamantel.add("§aVeja seus dias atuais com");
		diamantel.add("§ao comando §f/tempovip");
		diamantel.add(" ");
		diamantel.add("§fMais informações: PDGH.net/vip");
		diamantem.setLore(diamantel);
		diamante.setItemMeta(diamantem);
		diamante.addUnsafeEnchantment(Enchantment.OXYGEN, 1);
	    return diamante;
	}
    
	protected ItemStack plantacao(Player p) {
		ItemStack diamante = new ItemStack(Material.WHEAT, 1);
		ItemMeta diamantem = diamante.getItemMeta();
		diamantem.setDisplayName("§2§lKit Plantação");
		List<String> diamantel = new ArrayList<String>();
		diamantel.add("§7§lPreço: §c1000$ cash");
		diamantel.add(" ");
		diamantel.add("§cEsvazie o inventário antes de receber seu kit");
		diamantel.add(" ");
		diamantel.add("§fMais informações: PDGH.net/i/");
		diamantem.setLore(diamantel);
		diamante.setItemMeta(diamantem);
		diamante.addUnsafeEnchantment(Enchantment.OXYGEN, 1);
	    return diamante;
	}
    
	protected ItemStack fly(Player p) {
		ItemStack diamante = new ItemStack(Material.ENDER_PEARL, 1);
		ItemMeta diamantem = diamante.getItemMeta();
		diamantem.setDisplayName("§6§lVoe Livremente");
		List<String> diamantel = new ArrayList<String>();
		diamantel.add("§7§lPreço: §c300$ cash");
		diamantel.add(" ");
		diamantel.add("§aTe da o direito de voar permanentemente pela.");
		diamantel.add("§acidade (PlotMe) com o comando §c/fly.");
		diamantem.setLore(diamantel);
		diamante.setItemMeta(diamantem);
		diamante.addUnsafeEnchantment(Enchantment.OXYGEN, 1);
	    return diamante;
	}
    
	protected ItemStack desban(Player p) {
		ItemStack diamante = new ItemStack(Material.IRON_FENCE, 1);
		ItemMeta diamantem = diamante.getItemMeta();
		diamantem.setDisplayName("§c§lDESBANIR JOGADOR");
		List<String> diamantel = new ArrayList<String>();
		diamantel.add("§7§lPreço: §c2500$ cash");
		diamantel.add(" ");
		diamantel.add("§aDesbani o jogador desejado.");
		diamantel.add("§cNão reembolsável.");
		diamantem.setLore(diamantel);
		diamante.setItemMeta(diamantem);
		diamante.addUnsafeEnchantment(Enchantment.OXYGEN, 1);
	    return diamante;
	}
    
	protected ItemStack PP4(Player p) {
		ItemStack diamante = new ItemStack(Material.RECORD_4, 1);
		ItemMeta diamantem = diamante.getItemMeta();
		diamantem.setDisplayName("§2§lKit PP4");
		List<String> diamantel = new ArrayList<String>();
		diamantel.add("§7§lPreço: §c500$ cash");
		diamantel.add(" ");
		diamantel.add("§cEsvazie o inventário antes de receber seu kit");
		diamantel.add(" ");
		diamantel.add("§fMais informações: PDGH.net/i/");
		diamantem.setLore(diamantel);
		diamante.setItemMeta(diamantem);
		diamante.addUnsafeEnchantment(Enchantment.OXYGEN, 1);
	    return diamante;
	}
    
	protected ItemStack GLAD(Player p) {
		ItemStack diamante = new ItemStack(Material.DIAMOND_CHESTPLATE, 1);
		ItemMeta diamantem = diamante.getItemMeta();
		diamantem.setDisplayName("§2§lKit GLAD");
		List<String> diamantel = new ArrayList<String>();
		diamantel.add("§7§lPreço: §c250$ cash");
		diamantel.add(" ");
		diamantel.add("§cEsvazie o inventário antes de receber seu kit");
		diamantel.add(" ");
		diamantel.add("§fMais informações: PDGH.net/i/");
		diamantem.setLore(diamantel);
		diamante.setItemMeta(diamantem);
		diamante.addUnsafeEnchantment(Enchantment.OXYGEN, 1);
	    return diamante;
	}
	
	protected ItemStack diamante(Player p) {
		ItemStack diamante = new ItemStack(Material.DIAMOND, 1);
		ItemMeta diamantem = diamante.getItemMeta();
		diamantem.setDisplayName("§a§lSuas estatísticas");
		List<String> diamantel = new ArrayList<String>();
		if(jaComprou(p.getName())) {
			diamantel.add("§a§l"+p.getName()+"§a, obrigado por");
			diamantel.add("§anos ajudar a crescer!");
			diamantel.add(" ");
			diamantel.add("§cVocê tem "+getCashAtual(p.getName())+"$ cash§a, sem data de expiração");
			diamantel.add("§c"+getCashTotal(p.getName())+"$ cash§a já foram comprados por você.");
			if(getComprasTotais(p.getName())==1)
				diamantel.add("§c"+getComprasTotais(p.getName())+"§a compra foi feita por você aqui pelo /cash.");
			else
				diamantel.add("§c"+getComprasTotais(p.getName())+"§a compras foram feitas por você aqui pelo /cash.");
		}else{
			diamantel.add("§c§l"+p.getName()+"§c, você nunca comprou cash.");
		}
		if(canFly(p.getName()))
			diamantel.add("§aSeu /Fly está liberado.");
		else
			diamantel.add("§cSeu §a/Fly§c está bloqueado.");
		diamantel.add(" ");
		diamantel.add(" ");
		diamantel.add("§6Site para compra de cash:");
		diamantel.add("§6www.PDGH.net/vip");
		diamantel.add(" ");
		diamantel.add("§aR$0,01 (1 centavo de real) equivale à 1$ cash.");
		diamantem.setLore(diamantel);
		diamante.setItemMeta(diamantem);
		diamante.addUnsafeEnchantment(Enchantment.OXYGEN, 1);
	    return diamante;
	}
	
	protected ItemStack barreira(Player p) {
		ItemStack is = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = is.getItemMeta();
		meta.setDisplayName("§3Liberar compras");
		List<String> lore = new ArrayList<String>();
		lore.add("§cClique neste item");
		lore.add("§capenas quando tiver");
		lore.add("§ccerteza do que irá");
		lore.add("§ccomprar.");
		lore.add(" ");
		lore.add("§c§lA PDGH não devolve cash.");
		meta.setLore(lore);
		is.setItemMeta(meta);
		is.addUnsafeEnchantment(Enchantment.OXYGEN, 1);
	    return is;
	}
	
	protected ItemStack book() {
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
		ItemMeta bookm = book.getItemMeta();
		bookm.setDisplayName("§a§lComo funciona?");
		List<String> bookl = new ArrayList<String>();
		bookl.add("§eCom o Rankup Heads da PDGH você");
		bookl.add("§econsegue ganhar dinheiro facilmente");
		bookl.add("§evendendo cabeça de mobs.");
		bookl.add("§6Quanto maior seu rank, maior seus");
		bookl.add("§6drops de cabeças, pois você só");
		bookl.add("§6consegue dropar a cabeça de um mob");
		bookl.add("§6caso já tenha passado ou esteja no");
		bookl.add("§6rank do mob.");
		bookl.add("§eQuando matar um mob de seu rank ou de");
		bookl.add("§eranks anteriores você tem chances de");
		bookl.add("§edropar 1 cabeça ou mais.");
		bookl.add("§6§lVIPs tem o dobro de chances");
		bookl.add("§6§lde dropar 2 cabeças ou mais.");
		bookl.add("§eOs mobs são encontrados pelo mapa ou");
		bookl.add("§eno teleporte do seu rank (PvP ON).");
		bookl.add("§6§lVIPs conseguem colocar spawners no");
		bookl.add("§6§lpróprio terreno.");
		bookl.add("§7Mais comandos: /rankup");
		bookm.setLore(bookl);
		book.setItemMeta(bookm);
		book.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
	    return book;
	}
	
	private boolean setupEconomy() {
      if (getServer().getPluginManager().getPlugin("Vault") == null) {
          return false;
      }
      RegisteredServiceProvider<Economy> rsp=getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp == null) {
          return false;
      }
      econ=rsp.getProvider();
      return econ != null;
	}
}

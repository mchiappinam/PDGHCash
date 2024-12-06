package me.mchiappinam.pdghcash;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Player;

public class Threads extends Thread {
	private Main plugin;
	private String tipo;
	private Player p;
	//private int tempo;
	//private String motivo;
	
	public Threads(Main pl,String tipo2) {
		plugin=pl;
		tipo=tipo2;
	}
	
	public Threads(Main pl,String tipo2,Player player2) {
		plugin=pl;
		tipo=tipo2;
		p=player2;
	}
	
	public void run() {
		switch(tipo) {
			case "saveAllCache": {
				for(String nick : plugin.jogadores.keySet()) {
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
							PreparedStatement pst1 = con.prepareStatement("UPDATE `"+Main.tabela+"` SET `db`='"+plugin.jogadores.get(nick.toLowerCase().trim())+"' WHERE nick='"+nick.toLowerCase().trim()+"';");
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
							pst1.setString(2, plugin.jogadores.get(nick.toLowerCase().trim()));
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
			case "join": {
				try {
					Connection con = DriverManager.getConnection(Main.mysql_url,Main.mysql_user,Main.mysql_pass);
					//Prepared statement
					PreparedStatement pst = con.prepareStatement("SELECT `nick`,`db` FROM `"+Main.tabela+"` WHERE nick='"+p.getName().toLowerCase().trim()+"';");
					ResultSet rs = pst.executeQuery();
					while(rs.next()) {
						plugin.jogadores.put(p.getName().toLowerCase().trim(), rs.getString("db"));
						rs.close();
						pst.close();
						con.close();
						break;
					}
					break;
				}catch (SQLException ex) {
					System.out.print(ex);
					break;
				}
			}
			/**case "add": {
				try {
					Connection con = DriverManager.getConnection(Main.mysql_url,Main.mysql_user,Main.mysql_pass);
					//Prepared statement
					PreparedStatement pst = con.prepareStatement("SELECT `nick`,`db` FROM `"+Main.tabela+"` WHERE( `nick`='"+p.getName().toLowerCase().trim()+"');");
					ResultSet rs = pst.executeQuery();
					boolean existe=false;
					while(rs.next()) {
						existe=true;
						rs.close();
						pst.close();
						PreparedStatement pst1 = con.prepareStatement("UPDATE `"+Main.tabela+"` SET `tempo`="+plugin.getTempo(p.getName())+" WHERE( `nick`='"+p.getName().toLowerCase().trim()+"');");
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
						pst1.setString(2, plugin.getTempo(db));
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
			}*/
			/**case "add": {
				try {
					Connection con = DriverManager.getConnection(Main.mysql_url,Main.mysql_user,Main.mysql_pass);
					//Prepared statement
					PreparedStatement pst = con.prepareStatement("SELECT `nick`,`tempo` FROM `"+Main.tabela+"` WHERE( `nick`='"+sender.getName().toLowerCase().trim()+"');");
					ResultSet rs = pst.executeQuery();
					boolean existe=false;
					int tempoo;
					while(rs.next()) {
		            	tempoo=(rs.getInt("tempo"))+tempo;
						existe=true;
						plugin.getServer().broadcastMessage("§7[Prisão]§4 "+sender.getName()+" teve sua pena aumentada por mais "+tempo+" segundos. ("+motivo+")");
						if(sender!=null) {
							sender.sendMessage("§c§l"+tempo+" segundos foram adicionados em sua pena! >:)");
							sender.playSound(sender.getLocation(), Sound.ENDERDRAGON_DEATH, 1.0F, 1.0F);
						}
						rs.close();
						pst.close();
						PreparedStatement pst1 = con.prepareStatement("UPDATE `"+Main.tabela+"` SET `tempo`="+tempo+" WHERE( `nick`='"+sender.getName().toLowerCase().trim()+"');");
						pst1.executeUpdate();
						pst1.close();
						con.close();
						break;
					}
					if(!existe) {
						plugin.getServer().broadcastMessage("§7[Prisão]§4 "+sender.getName()+" foi preso por "+tempo+" segundos. ("+motivo+")");
						if(sender!=null) {
							sender.playSound(sender.getLocation(), Sound.ENDERDRAGON_DEATH, 1.0F, 1.0F);
							sender.sendMessage("§c§lVocê foi preso por "+tempo+" segundos! >:)");
							sender.sendMessage("§fHey");
							sender.sendMessage("§fEu sou o dolinho seu amiguinho e quero lhe ajudar!");
							sender.sendMessage("§fQue tal conferir as regras? http://pdgh.com.br/regras/");
							sender.sendMessage("§fNão seja mais um(a) menino(a) mal ein! Boa diversão =D");
						}
						//Prepared statement
						PreparedStatement pst1 = con.prepareStatement("INSERT INTO `"+Main.tabela+"`(nick, tempo) VALUES(?, ?)");
						//Values
						pst1.setString(1, sender.getName().toLowerCase().trim());
						pst1.setInt(2, tempo);
						//Do the MySQL query
						pst1.executeUpdate();
						pst1.close();
						con.close();
						break;
					}
				}catch (SQLException ex) {
					System.out.print(ex);
					sender.sendMessage("§cErro! Contate um staffer!");
					break;
				}
			}*/
		}
	}
}

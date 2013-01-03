package lihad.BMCMall;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class BMCMall extends JavaPlugin implements Listener {
	public static FileConfiguration config;
	protected static String PLUGIN_NAME = "Prefixer";
	protected static String header = "[" + PLUGIN_NAME + "] ";
	public static PermissionHandler handler;
    public static Economy econ;
	private static Logger log = Logger.getLogger("Minecraft");
	
	public static Map<String,PaymentPlan> map = new HashMap<String,PaymentPlan>();
	public static DAY_OF_WEEK currentDay;
	
	
	
	static class PaymentPlan{
		DAY_OF_WEEK day;
		boolean paid;
		
		PaymentPlan(DAY_OF_WEEK d, boolean p){day = d; paid = p;}
	}
	enum DAY_OF_WEEK{
		MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY
	}
	@Override
	public void onDisable() {
		save();
	}
	@Override
	public void onEnable() {
		config = getConfig();

		this.getServer().getPluginManager().registerEvents(this, this);
		setupPermissions();
		setupEconomy();

		this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable(){
			public void run() {
				Calendar cal = Calendar.getInstance();
				DAY_OF_WEEK newCurrent = null;
				
				switch(cal.DAY_OF_WEEK){
				case 0: newCurrent = DAY_OF_WEEK.SUNDAY; break;
				case 1: newCurrent = DAY_OF_WEEK.MONDAY; break;
				case 2: newCurrent = DAY_OF_WEEK.TUESDAY; break;
				case 3: newCurrent = DAY_OF_WEEK.WEDNESDAY; break;
				case 4: newCurrent = DAY_OF_WEEK.THURSDAY; break;
				case 5: newCurrent = DAY_OF_WEEK.FRIDAY; break;
				case 6: newCurrent = DAY_OF_WEEK.SATURDAY; break;
				}

				if(newCurrent != currentDay){
					for(int i = 0; i<map.size();i++){
						if(map.get(map.keySet().toArray()[i].toString()).day == newCurrent){
							map.get(map.keySet().toArray()[i].toString()).paid = false;
						}else if(map.get(map.keySet().toArray()[i].toString()).day != newCurrent && map.get(map.keySet().toArray()[i].toString()).paid == false){
							econ.bankWithdraw(map.keySet().toArray()[i].toString(), 75000);
							//TODO: Needs line for some notification to Op.  See /mall list command.
						}
					}
				}
			}
		}, 0, 1200L);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = (Player)sender;
		if(cmd.getName().equalsIgnoreCase("mall")) {
			if(args.length == 1){
				if(args[0].equalsIgnoreCase("pay") && map.containsKey(player.getName())){
					if(!map.get(player.getName()).paid){
						if(econ.bankBalance(player.getName()).balance >= 75000){
							econ.bankWithdraw(player.getName(), 75000);
							map.get(player.getName()).paid = true;
						}else{
							player.sendMessage("You do not have the money in your acct to pay.");
						}
					}
				}else if(args[0].equalsIgnoreCase("list") && player.isOp()){
					for(int i = 0; i<map.size();i++){
						//TODO: List of all subbed.  Also could do a list for all outstanding here.  Tho additional save/load areas to flatfile needed.
					}
				}
			}else if(args.length == 2){
				if(args[0].equalsIgnoreCase("sub") && player.isOp()){
					Player subby = getServer().getPlayer(args[1]);
					if(subby == null) player.sendMessage("That player does not exist, or is not currently online.");
					else{
						if(econ.bankBalance(subby.getName()).balance >= 75000){
							econ.bankWithdraw(subby.getName(), 75000);
							subby.sendMessage(ChatColor.BLUE+"You are now on a subscription plan for a shop of 75000 a week");
							map.put(subby.getName(), new PaymentPlan(currentDay,true));
							player.sendMessage(subby.getName()+ChatColor.BLUE+"is now on a subscription plan for a shop of 75000 a week");
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPluginEnable(PluginEnableEvent event){
		if((event.getPlugin().getDescription().getName().equals("Permissions"))) setupPermissions();
		if((event.getPlugin().getDescription().getName().equals("Vault"))) setupEconomy();
	}
	public static void setupPermissions() {
		Plugin permissionsPlugin = Bukkit.getServer().getPluginManager().getPlugin("Permissions");
		if (permissionsPlugin != null) {
			info("Succesfully connected to Permissions!");
			handler = ((Permissions) permissionsPlugin).getHandler();
		} else {
			handler = null;
			warning("Disconnected from Permissions...what could possibly go wrong?");
		}
	}
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	public void load(){
		for(int i = 0;i<config.getConfigurationSection("Player").getKeys(false).size();i++){
			String player = config.getConfigurationSection("Player").getKeys(false).toArray()[i].toString();
			map.put(player, new PaymentPlan(DAY_OF_WEEK.valueOf(config.getString("Player."+player+".Day")),config.getBoolean("Player."+player+".Paid")));
		}
	}
	public void save(){
		for(int i = 0;i<map.size();i++){
			String player = map.keySet().toArray()[i].toString();
			String day = map.get(player).day.toString();
			boolean b = map.get(player).paid;

			config.set("Player."+player+".Day", day);
			config.set("Player."+player+".Paid", b);
		}
		this.saveConfig();
	}
	public static void info(String message){ 
		log.info(header + ChatColor.WHITE + message);
	}
	public static void severe(String message){
		log.severe(header + ChatColor.RED + message);
	}
	public static void warning(String message){
		log.warning(header + ChatColor.YELLOW + message);
	}
	public static void log(java.util.logging.Level level, String message){
		log.log(level, header + message);
	}
	
}

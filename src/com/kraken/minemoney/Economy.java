package com.kraken.minemoney;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Economy implements Listener {
	
	private MineMoney plugin;
	
	//Constructor
	public Economy(MineMoney plugin) {
		this.plugin = plugin;
    }
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		
		//Daily login bonus
		Double amount = plugin.getConfig().getDouble("daily_bonus");
		
		//Check if daily login bonus is disabled
		if (amount != 0) {
			
			//Date stuff
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
			Date now = new Date();
			String today = formatter.format(now);
			
	    	//Balance config file
		    File balFile = plugin.getFile("balance");
		    FileConfiguration balConfig = plugin.getFileConfig("balance");
			
		    //Player info
			Player player = e.getPlayer();
			String pId = player.getUniqueId().toString();
			String lastDay = balConfig.getString(pId + ".lastDay");
			int loginCombo = balConfig.getInt(pId + ".loginCombo");
			boolean newPlayer = false;
			
			//Check if the last login day value is present, otherwise set to today's date
			if (lastDay == null) {
				lastDay = today;
				balConfig.set(pId + ".lastDay", today);
				balConfig.set(pId + ".loginCombo", 0);
				loginCombo = 0;
				plugin.saveCustomFile(balConfig, balFile);
				newPlayer = true;
			}
			
			//Check if logging in on new day, and give bonus accordingly
			if (!today.equals(lastDay) || newPlayer) {
				balConfig.set(pId + ".lastDay", today);
				balConfig.set(pId + ".loginCombo", loginCombo+1);
				plugin.saveCustomFile(balConfig, balFile);
				double award = amount;
				int loginMultiplier = plugin.getConfig().getInt("daily_multiplier");
				if (loginMultiplier != 0) {
					award = amount + (loginCombo * loginMultiplier);
				}
				add(player, award);
			}
			
		}
		
	}
	
	//Update the transactions log
	public void updateTransactions(Player sender, Player receiver, double amount) {
		
		//Balance config file
	    File tFile = new File("plugins/MineMoney", "transactions.yml");
	    FileConfiguration tConfig = YamlConfiguration.loadConfiguration(tFile);
	    
	    //Variable values
	    LocalDateTime now = LocalDateTime.now();
	    String keyId = now.toString();
	    
	    //Set the transactions file config
	    tConfig.set(keyId + ".sender", sender.getUniqueId().toString());
	    tConfig.set(keyId + ".receiver", receiver.getUniqueId().toString());
	    tConfig.set(keyId + ".amount", amount);

	    //Save to file
		plugin.saveCustomFile(tConfig, tFile);
		
	}
	
	//Add amount to player balance
	public void add(Player player, double amount) {
		
    	//Balance config file
	    File balFile = plugin.getFile("balance");
	    FileConfiguration balConfig = plugin.getFileConfig("balance");
	    
	    //Variable values
	    String currencySymbol = plugin.getConfig().getString("currency_symbol");
		String pId = player.getUniqueId().toString();
		double bal = balConfig.getDouble(pId + ".balance");
		double newBal = bal + amount;
		
		//Set the balance of the player in config
		balConfig.set(pId + ".balance", newBal);
		plugin.saveCustomFile(balConfig, balFile);
		
		//Create the message string
		String balUpdateStr = "You ";
		String amountStr = currencySymbol;
		int mod = -1;
		
		//Check if amount is positive or negative and create a message string for the new balance
		boolean notNegative = amount >= 0;
		if (notNegative) {
			balUpdateStr += "were ";
			mod = 1;
		} 
		amountStr += String.valueOf(amount*mod);
		balUpdateStr += "paid " + amountStr + ".";
		
		//Send message notification of transactions to player
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[$M] &7| " + balUpdateStr + " Current balance: &a" + currencySymbol + newBal));
		
	}
	
	//Exchange items for currency
	public void exchange(Player player) {

    	//Check that the item material is properly specified
    	String itemId = plugin.getConfig().getString("exchange_item");
    	Material itemMat;
    	if (Material.getMaterial(itemId).isItem()) {
    		itemMat = Material.getMaterial(itemId);
    	} else {
    		plugin.getLogger().info("The item ID listed in the plugin's config.yml for exchange_item is invalid. Please correct it to allow exchanges, or set the exchange_price to 0 to disable exchanges.");
    		return;
    	}

		//Check the player's inventory for a count of the exchange item
    	PlayerInventory inv = player.getInventory();
    	int count = 0;
    	
		for (ItemStack item : inv.getContents()) {
			if (item == null) continue;
			boolean match = item.getType().equals(itemMat);
			if (match) {
				count += item.getAmount();
				inv.remove(item);
			}
		}
		
		//Pay the player the designated price for each item exchanged
    	double price = plugin.getConfig().getDouble("exchange_price");
		double amount = price * count;
		add(player, amount);
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}

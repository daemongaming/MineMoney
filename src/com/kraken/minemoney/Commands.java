package com.kraken.minemoney;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

public class Commands {
	
	//Get the main instance
	private MineMoney plugin;
	private Messages messenger;
	
	//Constructor
	public Commands(MineMoney plugin) {
		this.plugin = plugin;
		this.messenger = plugin.getMessenger();
	}
	
    //Commands
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		String command = cmd.getName();
    	Player player = Bukkit.getServer().getPlayerExact("krakenmyboy");
		boolean isPlayer = false;
		boolean opReq = plugin.getOptions().get("op_required");

		Economy eco = plugin.getEconomy();
		FileConfiguration balConfig = plugin.getFileConfig("balance");
		String currencySymbol = plugin.getConfig().getString("currency_symbol");
		String pId = "";
		
		//Player commands
        if ( sender instanceof Player ) {
        	player = (Player) sender;
        	isPlayer = true;
        	pId = player.getUniqueId().toString();
        }
		
		switch ( command.toLowerCase() ) {
		
			case "mm":
				
				if (args.length == 0) {
					
					if (isPlayer) {
						messenger.makeMsg(player, "cmdVersion");
					} else {
						messenger.makeConsoleMsg("cmdVersion");
					}
					
				} else if (args.length == 1) {
					
					boolean hasBal = balConfig.getKeys(false).contains(pId);
					
					switch(args[0]) {
						
						case "bal":
						case "balance":
						case "bank":
						case "eco":
							
							//Player only command check
							if (playerOnly(isPlayer)) {
								return true;
							}
							
							//Get the player balance and send them a message
							Double bal = 0.0;
							
							if (hasBal) {
								bal = balConfig.getDouble(pId + ".balance");
							}
							
							player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[$M] &7| Your balance is &a" + currencySymbol + bal));
							
							break;
						
						case "exchange":
							
							//Player only command check
							if (playerOnly(isPlayer)) {
								return true;
							}
							
							//Make sure the player is in the balance file
							if (!hasBal) {
								balConfig.set(pId + ".balance", 0);
							}
							
							//Perform the item exchange for currency
							boolean exchangeEnabled = plugin.getConfig().getDouble("exchange_price") != 0;
							if (exchangeEnabled) {
								eco.exchange(player);
							}
							
							break;
						
						default:
							cmdNotRecognized(player);
							break;
							
					}
					
				} else if (args.length == 2) {
					
					switch(args[0]) {
					
						//Toggle config options by command
						case "toggle":
							
							if (isPlayer) {
								if (opReq && opOnly(player)) {
									return true;
								}
							}
							
							if ( !plugin.getOptions().keySet().contains(args[1]) ) {
								cmdNotRecognized(player);
								break;
							}
							
							//Toggle the option
							boolean option = plugin.getOptions().get(args[1]);
							plugin.setOption(args[1], !option);
							
							//Get the message to send
							String toggleMsgLabel = "cmdOptionEnabled";
							toggleMsgLabel = !option ? "cmdOptionEnabled" : "cmdOptionDisabled";
							
							//Send a confirmation message
							if (isPlayer) {
								messenger.makeMsg(player, toggleMsgLabel);
							} else {
								messenger.makeConsoleMsg(toggleMsgLabel);
							}
							
							break;
							
						default:
							cmdNotRecognized(player);
							break;
							
					}
					
				} else if (args.length == 3) {
					
					switch(args[0]) {
					
						//Pay a player from your balance
						case "pay":
						case "give":
						case "send":
							
							//Player only command check
							if (playerOnly(isPlayer)) {
								break;
							}
							
							//Try to get the amount to pay
							double amount = 0;
							
							try {
								int amountInt = Integer.parseInt(args[2]);
								amount = Double.valueOf(amountInt);
							} catch (NumberFormatException e) {
								cmdNotRecognized(player);
								e.printStackTrace();
								break;
							}
							
							//Transaction info
							Player player2 = Bukkit.getServer().getPlayer(args[1]);
							boolean playerFound = Bukkit.getServer().getOnlinePlayers().contains(player2);
							Double bal = balConfig.getDouble(pId + ".balance");
							
							boolean enoughBal = (bal >= amount);
							boolean payable = playerFound && enoughBal;
							
							//Send the money if everything checks out
							if (payable) {
								
								//Checks: amount to send is positive, and not sending to self
								boolean positiveAmount = amount <= 0;
								boolean samePlayer = pId == player2.getUniqueId().toString();
								if (!positiveAmount || !samePlayer) {
									cmdNotRecognized(player);
									break;
								}
								
								//Update the economy
								eco.add(player, (0-amount));
								eco.add(player2, amount);
								eco.updateTransactions(player, player2, amount);
								
							//Error if player not found, or balance is not enough to cover transaction
							} else if (!playerFound) {
								makeMsg(player, "errorPlayerNotFound");
							} else if (!enoughBal) {
								player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[$M] &c| Your balance is too low to make this transaction."));
							}
							
							break;
						
						//Pay a player bonus money, adding money to the economy
						case "add":
						case "bonus":
							
							if (isPlayer) {
								if (opReq && opOnly(player)) {
									break;
								}
							}
							
							//Try to get the amount to pay
							double amountToBonus = 0;
							
							try {
								int amountInt = Integer.parseInt(args[2]);
								amountToBonus = Double.valueOf(amountInt);
							} catch (NumberFormatException e) {
								cmdNotRecognized(player);
								e.printStackTrace();
								break;
							}
							
							//Check that the amount is a positive number
							if (amountToBonus <= 0) {
								cmdNotRecognized(player);
								break;
							}
							
							//Transaction info
							Player playerToBonus = Bukkit.getServer().getPlayer(args[1]);
							boolean playerToBonusFound = Bukkit.getServer().getOnlinePlayers().contains(playerToBonus);
							
							//Send the money if everything checks out
							if (playerToBonusFound) {
								eco.add(playerToBonus, amountToBonus);
							} else {
								makeMsg(player, "errorPlayerNotFound");
							}
							
							break;
							
						default:
							cmdNotRecognized(player);
							break;
					
					}
				
				} else {
					cmdNotRecognized(player);
				}
				
				return true;
				
			default:	
				cmdNotRecognized(player);
				return true;
				
		}
        
    }
	
	public void makeMsg(Player player, String msg) {
		
		if (player instanceof Player) {
			messenger.makeMsg(player, msg);
		} else {
			messenger.makeConsoleMsg(msg);
		}
		
	}
	
	public void cmdNotRecognized(Player player) {
		
		if (player instanceof Player) {
			messenger.makeMsg(player, "errorIllegalCommand");
		} else {
			messenger.makeConsoleMsg("errorCommandFormat");
		}
		
	}
	
	public boolean playerOnly(boolean isPlayer) {
		
		if (!isPlayer) {
			messenger.makeConsoleMsg("errorPlayerCommand");
			return true;
		}
		return false;
		
	}
	
	public boolean opOnly(Player player) {
		
		if (!player.isOp()) {
			messenger.makeConsoleMsg("errorIllegalCommand");
			return true;
		}
		return false;
		
	}

}
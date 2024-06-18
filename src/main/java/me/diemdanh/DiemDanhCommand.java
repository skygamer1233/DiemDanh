package me.diemdanh;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class DiemDanhCommand implements CommandExecutor {

    private final DiemDanh plugin;


    public DiemDanhCommand(DiemDanh plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("diemdanh")) {
            Player player = null;
            if (sender instanceof Player) {
                player = (Player) sender;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("diemdanh.reload")) {
                    sender.sendMessage(plugin.getMessage("NoPermission"));
                    return true;
                }


                File configFile = new File(plugin.getDataFolder(), "config.yml");
                if (!configFile.exists()) {
                    plugin.saveDefaultConfig();
                    configFile = new File(plugin.getDataFolder(), "config.yml");
                }
                try {
                    plugin.getConfig().load(configFile);
                    plugin.guiTitle = color.transalate(plugin.getConfig().getString("Title", "&a&lĐiểm Danh Tháng <month>"));
                } catch (IOException | InvalidConfigurationException e) {
                    sender.sendMessage(color.transalate("&cLỗi khi tải lại config!"));
                    e.printStackTrace();
                    return true;
                }


                plugin.playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
                if (!plugin.playerDataFile.exists()) {
                    plugin.saveResource("playerdata.yml", false);
                }
                plugin.playerData = YamlConfiguration.loadConfiguration(plugin.playerDataFile);

                plugin.topGuiFile = new File(plugin.getDataFolder(), "topgui.yml");
                if (!plugin.topGuiFile.exists()) {
                    plugin.saveResource("topgui.yml", false);
                    plugin.topGuiFile = new File(plugin.getDataFolder(), "topgui.yml");
                }
                try {
                    plugin.topGuiConfig = YamlConfiguration.loadConfiguration(plugin.topGuiFile);
                    plugin.topGuiTitle = color.transalate(plugin.topGuiConfig.getString("TopTitle", "&c&lBảng Xếp Hạng Điểm Danh Tháng <month>"));
                } catch (RuntimeException e) {
                    sender.sendMessage(color.transalate("&cLỗi khi tải lại topgui.yml!"));
                    e.printStackTrace();
                    return true;
                }

                sender.sendMessage(plugin.getMessage("Reload"));
                return true;
            } else if (args.length == 3 && args[0].equalsIgnoreCase("giveticket")) {
                if (!sender.hasPermission("diemdanh.giveticket")) {
                    sender.sendMessage(plugin.getMessage("NoPermission"));
                    return true;
                }

                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(plugin.getMessage("PlayerNotFound"));
                    return true;
                }

                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        sender.sendMessage(plugin.getMessage("InvalidTicketAmount"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getMessage("InvalidTicketAmount"));
                    return true;
                }

                String targetPlayerUUID = targetPlayer.getUniqueId().toString();
                int currentTickets = plugin.playerData.getInt(targetPlayerUUID + ".tickets", 0);
                plugin.playerData.set(targetPlayerUUID + ".tickets", currentTickets + amount);
                plugin.savePlayerData();

                String giveTicketSuccessMessage = plugin.getMessage("GiveTicketSuccess")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%player%", targetPlayer.getName());
                sender.sendMessage(giveTicketSuccessMessage);

                String receiveTicketMessage = plugin.getMessage("ReceiveTicket")
                        .replace("%amount%", String.valueOf(amount));
                targetPlayer.sendMessage(receiveTicketMessage);

                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("top")) { // Xử lý lệnh /diemdanh top
                if (player == null) {
                    sender.sendMessage(plugin.getMessage("NotPlayer"));
                    return true;
                }
                plugin.getDiemDanhTop().openTopDiemDanhGUI(player); // Gọi hàm openTopDiemDanhGUI
                return true;
            } else if (args.length == 0 && sender instanceof Player) {
                plugin.openDiemDanhGUI((Player) sender);
                return true;
            } else {
                sender.sendMessage(plugin.getMessage("SyntaxError"));
                return true;
            }
        }
        return false;
    }

}

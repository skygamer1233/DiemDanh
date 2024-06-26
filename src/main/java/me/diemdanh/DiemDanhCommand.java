package me.diemdanh;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class DiemDanhCommand implements CommandExecutor {

    private final DiemDanh plugin;


    public DiemDanhCommand(DiemDanh plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
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

                plugin.reloadConfig();
                plugin.guiTitle = color.transalate(plugin.getConfig().getString("Title", "&a&lĐiểm Danh Tháng "));

                try {
                    plugin.playerData.load(plugin.playerDataFile);
                } catch (IOException | InvalidConfigurationException e) {
                    sender.sendMessage(color.transalate("&cLỗi khi tải lại playerdata!"));
                    e.printStackTrace();
                    return true;
                }

                try {
                    plugin.topGuiConfig = YamlConfiguration.loadConfiguration(plugin.topGuiFile);
                    plugin.topGuiTitle = color.transalate(plugin.topGuiConfig.getString("TopTitle", "&c&lBảng Xếp Hạng Điểm Danh Tháng <month>"));
                    plugin.TotalTitle = color.transalate(plugin.topGuiConfig.getString("TotalTitle", "&c&lBảng Xếp Hạng Điểm Danh Tổng"));
                } catch (RuntimeException e) {
                    sender.sendMessage(color.transalate("&cLỗi khi tải lại topgui.yml!"));
                    e.printStackTrace();
                    return true;
                }
                plugin.reloadLanguageFiles();

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
            } else if (args.length == 1 && args[0].equalsIgnoreCase("top")) {
                if (player == null) {
                    sender.sendMessage(plugin.getMessage("NotPlayer"));
                    return true;
                }
                plugin.getDiemDanhTop().openTopDiemDanhGUI(player);
                return true;
            } else if (args.length == 0 && sender instanceof Player) {
                plugin.openDiemDanh((Player) sender);
                return true;
            } else {
                sender.sendMessage(plugin.getMessage("SyntaxError"));
                sender.sendMessage(plugin.getMessage("DiemDanhHelp"));
                sender.sendMessage(plugin.getMessage("GiveTicketUsage"));
                sender.sendMessage(plugin.getMessage("ReloadUsage"));
                sender.sendMessage(plugin.getMessage("TopUsage"));
                return true;
            }
        }
        return false;
    }

}

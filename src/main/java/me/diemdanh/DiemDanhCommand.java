package me.diemdanh;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiemDanhCommand implements CommandExecutor {

    private final DiemDanh plugin;

    public DiemDanhCommand(DiemDanh plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!cmd.getName().equalsIgnoreCase("diemdanh")) {
            return false;
        }

        Player player = (sender instanceof Player) ? (Player) sender : null;

        // --- LỆNH /diemdanh reload ---
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("diemdanh.reload")) {
                sender.sendMessage(plugin.getMessage("NoPermission"));
                return true;
            }

            // Gọi duy nhất hàm này để reload TOÀN BỘ file config (bao gồm editor.yml)
            plugin.reloadAllConfigs();

            sender.sendMessage(plugin.getMessage("Reload"));
            return true;
        }

        // --- LỆNH /diemdanh giveticket <player> <amount> ---
        if (args.length == 3 && args[0].equalsIgnoreCase("giveticket")) {
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
        }

        // --- LỆNH /diemdanh top ---
        if (args.length == 1 && args[0].equalsIgnoreCase("top")) {
            if (player == null) {
                sender.sendMessage(plugin.getMessage("NotPlayer"));
                return true;
            }
            plugin.getDiemDanhTop().openTopDiemDanhGUI(player);
            return true;
        }

        // --- LỆNH /diemdanh (mở GUI chính) ---
        if (args.length == 0) {
            if (player == null) {
                sender.sendMessage(plugin.getMessage("NotPlayer"));
                return true;
            }
            plugin.getDiemDanhGUI().openDiemDanhGUI(player);
            return true;
        }

        // --- LỆNH /diemdanh editor ---
        if (args.length == 1 && args[0].equalsIgnoreCase("editor")) {
            if (player == null) {
                sender.sendMessage(plugin.getMessage("NotPlayer"));
                return true;
            }
            if (!player.hasPermission("diemdanh.editor") && !player.hasPermission("diemdanh.admin")) {
                sender.sendMessage(plugin.getMessage("NoPermission"));
                return true;
            }
            plugin.getDiemDanhEditor().openMainMenu(player);
            return true;
        }

        // --- HƯỚNG DẪN SAI CÚ PHÁP ---
        sender.sendMessage(plugin.getMessage("SyntaxError"));
        sender.sendMessage(plugin.getMessage("DiemDanhHelp"));
        if (sender.hasPermission("diemdanh.giveticket")) {
            sender.sendMessage(plugin.getMessage("GiveTicketUsage"));
        }
        if (sender.hasPermission("diemdanh.reload")) {
            sender.sendMessage(plugin.getMessage("ReloadUsage"));
        }
        sender.sendMessage(plugin.getMessage("TopUsage"));
        return true;
    }
}
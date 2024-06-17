package me.diemdanh;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabComplete implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("diemdanh")) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                if (sender.hasPermission("diemdanh.reload")) {
                    completions.add("reload");
                }
                if (sender.hasPermission("diemdanh.giveticket")) {
                    completions.add("giveticket");
                }
                if (sender.hasPermission("diemdanh.top")) {
                    completions.add("top");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("giveticket")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
            return completions;
        }
        return null;
    }
}

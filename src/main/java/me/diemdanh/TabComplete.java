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
                List<String> subCommands = new ArrayList<>();
                subCommands.add("top");
                subCommands.add("chest");
                subCommands.add("gui");
                if (sender.hasPermission("diemdanh.reload")) {
                    subCommands.add("reload");
                }
                if (sender.hasPermission("diemdanh.giveticket")) {
                    subCommands.add("giveticket");
                }
                if (sender.hasPermission("diemdanh.editor") || sender.hasPermission("diemdanh.admin")) {
                    subCommands.add("editor");
                }
                for (String sub : subCommands) {
                    if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("giveticket")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
                if ("chest".startsWith(args[1].toLowerCase())) {
                    completions.add("chest");
                }
            }
            return completions;
        }
        return null;
    }
}

package me.diemdanh;


import net.xconfig.bukkit.model.SimpleConfigurationManager;
import org.bukkit.configuration.file.FileConfiguration;


public class config {

    public static FileConfiguration getConfig() {
        return SimpleConfigurationManager.get().get("config.yml");
    }

    public static FileConfiguration getPlayerdata() {
        return SimpleConfigurationManager.get().get("playerdata.yml");
    }

    public static FileConfiguration getTopGUI() {
        return SimpleConfigurationManager.get().get("topGUI.yml");
    }

    public static void createFiles() {
        SimpleConfigurationManager.get().build("", false, "config.yml", "topgui.yml", "playerdata.yml");

    }

    public static void reloadFile() {
        SimpleConfigurationManager.get().reload("config.yml", "topgui.yml", "playerdata.yml");
    }

    public static void saveFile() {
        SimpleConfigurationManager.get().save("config.yml", "topgui.yml", "playerdata.yml");
    }
}

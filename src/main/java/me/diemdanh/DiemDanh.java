package me.diemdanh;


import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;



import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;

public class DiemDanh extends JavaPlugin implements Listener {

    public FileConfiguration playerData;
    public File playerDataFile;
    public DiemDanhTop diemDanhTop;
    public String topGuiTitle;
    public String TotalTitle;
    public DiemDanhGUI diemDanhGUI;




    public String guiTitle;
    public FileConfiguration topGuiConfig;
    public File topGuiFile;

    private Map<String, FileConfiguration> languageConfigs;





    @Override
    public void onEnable() {
        diemDanhGUI = new DiemDanhGUI(this);
        getServer().getPluginManager().registerEvents(diemDanhGUI, this);
        updateConfigFile("config.yml");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("diemdanh").setExecutor(new DiemDanhCommand(this));
        getCommand("diemdanh").setTabCompleter(new TabComplete());
        diemDanhTop = new DiemDanhTop(this);
        getServer().getPluginManager().registerEvents(diemDanhTop.new TopGUIListener(), this);



        getLogger().info(color.transalate("&7--------------------------------------"));
        getLogger().info(color.transalate("&eDiemDanh Reloaded&a has been enabled"));
        getLogger().info(color.transalate("&8Plugin by SkyGamer"));
        getLogger().info(color.transalate("&7--------------------------------------"));

        loadLanguageFiles();

        guiTitle = color.transalate(getConfig().getString("Title", "&a&lĐiểm Danh Tháng "));


        updateConfigFile("topgui.yml");
        topGuiFile = new File(getDataFolder(), "topgui.yml");
        if (!topGuiFile.exists()) {
            saveResource("topgui.yml", false);
        }
        topGuiConfig = YamlConfiguration.loadConfiguration(topGuiFile);

        topGuiTitle = color.transalate(topGuiConfig.getString("TopTitle", "&c&lBảng Xếp Hạng Điểm Danh Tháng <month>"));
        TotalTitle = color.transalate(topGuiConfig.getString("TotalTitle", "&c&lBảng Xếp Hạng Điểm Danh Tổng"));



        playerDataFile = new File(getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            saveResource("playerdata.yml", false);
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }
    private void updateConfigFile(String fileName) {
        File configFile = new File(getDataFolder(), fileName);
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), fileName));
        int latestVersion = defaultConfig.getInt("version", 1);

        if (configFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);


            if (!config.contains("version")) {
                getLogger().info("Updating " + fileName + " to the latest version...");
                File oldConfigFile = new File(getDataFolder(), fileName + "_old");
                configFile.renameTo(oldConfigFile);
                saveResource(fileName, false);
            } else {
                int currentVersion = config.getInt("version", 1);

                if (currentVersion < latestVersion) {
                    getLogger().info("Updating " + fileName + " from version " + currentVersion + " to " + latestVersion);
                    File oldConfigFile = new File(getDataFolder(), fileName + "_old");
                    configFile.renameTo(oldConfigFile);
                    saveResource(fileName, false);
                }
            }
        } else {
            saveResource(fileName, false);
        }
    }
    private void loadLanguageFiles() {
        languageConfigs = new HashMap<String, FileConfiguration>();
        File languageFolder = new File(getDataFolder(), "language");

        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
        }

        String[] defaultLanguages = {"en", "vi"};
        for (String lang : defaultLanguages) {
            File langFile = new File(languageFolder, "message_" + lang + ".yml");
            if (!langFile.exists()) {
                saveResource("language/message_" + lang + ".yml", false);
            }
        }

        for (File file : languageFolder.listFiles()) {
            if (file.isFile() && file.getName().startsWith("message_") && file.getName().endsWith(".yml")) {
                String langCode = file.getName().substring(8, file.getName().length() - 4);
                FileConfiguration langConfig = YamlConfiguration.loadConfiguration(file);
                languageConfigs.put(langCode, langConfig);
                getLogger().info("Loaded language file: " + file.getName());
            }
        }
    }
    public void reloadLanguageFiles() {
        loadLanguageFiles();
    }
    public void onDisable() {
        getLogger().info(color.transalate("&7--------------------------------------"));
        getLogger().info(color.transalate("&eDiemDanh Reloaded&a has been disabled"));
        getLogger().info(color.transalate("&8Plugin by SkyGamer"));
        getLogger().info(color.transalate("&7--------------------------------------"));
    }
    public DiemDanhTop getDiemDanhTop() {
        return diemDanhTop;
    }
    public DiemDanhGUI getDiemDanhGUI() { return diemDanhGUI;}
    public String getTopGuiTitle() {
        return topGuiTitle;
    }



    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        LocalDate today = LocalDate.now();

        if (!playerData.contains(playerUUID)) {
            playerData.set(playerUUID + ".name", player.getName());
            playerData.set(playerUUID + ".lastCheckIn", LocalDate.now().toString());
            playerData.set(playerUUID + ".daysCheckedIn", 0);
            playerData.set(playerUUID + ".totalDays", 0);
            playerData.set(playerUUID + ".lastCheckInMonth", LocalDate.now().getMonthValue());
            playerData.set(playerUUID + ".checkedDays", new ArrayList<>());
            playerData.set(playerUUID + ".missedDays", new ArrayList<>());

            ConfigurationSection specialDaysSection = getConfig().getConfigurationSection("SpecialDay");
            if (specialDaysSection != null) {
                for (String specialDayKey : specialDaysSection.getKeys(false)) {
                    playerData.set(playerUUID + ".specialDays." + specialDayKey, false);
                }
            }
            savePlayerData();
        }
        updateMissedDays(playerUUID);

        int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);

        if (today.getMonthValue() != lastCheckInMonth) {
            playerData.set(playerUUID + ".checkedDays", new ArrayList<>());
            playerData.set(playerUUID + ".daysCheckedIn", 0);
            playerData.set(playerUUID + ".missedDays", new ArrayList<>());
            for (int daysRequired : new int[]{7, 14, 21}) {
                playerData.set(playerUUID + ".tichluy." + daysRequired + ".claimed", false);
                playerData.set(playerUUID + ".tichluy." + daysRequired + ".month", 0);
            }
            savePlayerData();
        }

        LocalDate lastCheckInDate = LocalDate.parse(playerData.getString(playerUUID + ".lastCheckIn", "1970-01-01"));
        if (today.getYear() != lastCheckInDate.getYear()) {
            for (String key : playerData.getConfigurationSection(playerUUID + ".specialDays").getKeys(false)) {
                playerData.set(playerUUID + ".specialDays." + key, false);
            }
            savePlayerData();
        }
        List<Integer> checkedDays = playerData.getIntegerList(playerUUID + ".checkedDays");
        if (!checkedDays.contains(today.getDayOfMonth())) {
            TextComponent message = new TextComponent(getMessage("ChuaDiemDanhHomNay"));
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/diemdanh"));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(getMessage("Hover")).create()));
            player.spigot().sendMessage(message);
        }
    }

    private void updateMissedDays(String playerUUID) {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);


        if (currentMonth != lastCheckInMonth) {
            playerData.set(playerUUID + ".checkedDays", new ArrayList<>());
            playerData.set(playerUUID + ".daysCheckedIn", 0);
            playerData.set(playerUUID + ".lastCheckInMonth", currentMonth);
        }

        List<Integer> missedDays = playerData.getIntegerList(playerUUID + ".missedDays");
        List<Integer> checkedDays = playerData.getIntegerList(playerUUID + ".checkedDays");


        for (int day = 1; day < today.getDayOfMonth(); day++) {
            if (!checkedDays.contains(day) && !missedDays.contains(day)) {
                missedDays.add(day);
            }
        }

        playerData.set(playerUUID + ".missedDays", missedDays);
        savePlayerData();
    }

    public void savePlayerData() {
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save player data to file", e);
        }
    }



    public String getMessage(String key) {
        String language = getConfig().getString("language", "en");
        FileConfiguration langConfig = languageConfigs.get(language);

        if (langConfig == null) {
            getLogger().warning("Language " + language + " not found. Falling back to English.");
            langConfig = languageConfigs.get("en");
        }

        String message = langConfig.getString("Message." + key);
        if (message == null) {
            getLogger().warning("Missing message key: " + key + " in " + language + " language file");
            return "&cMissing message: " + key;
        }

        return color.transalate(message);
    }


}
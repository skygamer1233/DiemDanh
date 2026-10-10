package me.diemdanh;

import me.diemdanh.data.PlayerDataManager;
import me.diemdanh.hook.DiemDanhExpansion;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

public class DiemDanh extends JavaPlugin implements Listener {

    private PlayerDataManager playerDataManager;
    public FileConfiguration playerData; // Backward-compatibility
    public File playerDataFile;
    public DiemDanhTop diemDanhTop;
    public String topGuiTitle;
    public String TotalTitle;
    public DiemDanhGUI diemDanhGUI;
    public DiemDanhEditor diemDanhEditor;

    public String guiTitle;
    public FileConfiguration topGuiConfig;
    public File topGuiFile;

    public File editorFile;
    public FileConfiguration editorConfig;

    private Map<String, FileConfiguration> languageConfigs;

    @Override
    public void onEnable() {
        updateConfigFile("config.yml");
        saveDefaultConfig();

        guiTitle = ColorUtil.translate(getConfig().getString("Title", "&a&lĐiểm Danh Tháng "));

        // 1. Khoi tao PlayerDataManager (ho tro chuyen doi playerdata.yml cu)
        playerDataManager = new PlayerDataManager(this);
        playerData = new YamlConfiguration();

        loadLanguageFiles();
        createEditorConfig();

        // 2. Load topgui.yml
        updateConfigFile("topgui.yml");
        topGuiFile = new File(getDataFolder(), "topgui.yml");
        if (!topGuiFile.exists()) {
            saveResource("topgui.yml", false);
        }
        topGuiConfig = YamlConfiguration.loadConfiguration(topGuiFile);
        topGuiTitle = ColorUtil.translate(topGuiConfig.getString("TopTitle", "&c&lBảng Xếp Hạng Điểm Danh Tháng <month>"));
        TotalTitle = ColorUtil.translate(topGuiConfig.getString("TotalTitle", "&c&lBảng Xếp Hạng Điểm Danh Tổng"));

        // 3. Khoi tao GUI & Listeners
        diemDanhGUI = new DiemDanhGUI(this);
        getServer().getPluginManager().registerEvents(diemDanhGUI, this);
        getServer().getPluginManager().registerEvents(this, this);

        getCommand("diemdanh").setExecutor(new DiemDanhCommand(this));
        getCommand("diemdanh").setTabCompleter(new TabComplete());

        diemDanhTop = new DiemDanhTop(this);
        getServer().getPluginManager().registerEvents(diemDanhTop.new TopGUIListener(), this);

        diemDanhEditor = new DiemDanhEditor(this);
        getServer().getPluginManager().registerEvents(diemDanhEditor, this);

        // 4. Hook PlaceholderAPI neu co
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DiemDanhExpansion(this).register();
            getLogger().info("Hook vao PlaceholderAPI thanh cong!");
        }

        getLogger().info(ColorUtil.translate("&7--------------------------------------"));
        getLogger().info(ColorUtil.translate("&eDiemDanh Reloaded &a(v1.5-BETA) enabled"));
        getLogger().info(ColorUtil.translate("&8Plugin by SkyGamer"));
        getLogger().info(ColorUtil.translate("&7--------------------------------------"));
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public void createEditorConfig() {
        editorFile = new File(getDataFolder(), "editor.yml");
        if (!editorFile.exists()) {
            saveResource("editor.yml", false);
        }
        editorConfig = YamlConfiguration.loadConfiguration(editorFile);
    }

    public FileConfiguration getEditorConfig() {
        if (editorConfig == null) createEditorConfig();
        return editorConfig;
    }

    // --- HÀM RELOAD TOÀN BỘ CẤU HÌNH PLUGIN ---
    public void reloadAllConfigs() {
        reloadConfig();
        guiTitle = ColorUtil.translate(getConfig().getString("Title", "&a&lĐiểm Danh Tháng "));

        createEditorConfig();
        if (diemDanhEditor != null) {
            diemDanhEditor.loadEditorConfig();
        }

        topGuiFile = new File(getDataFolder(), "topgui.yml");
        if (topGuiFile.exists()) {
            topGuiConfig = YamlConfiguration.loadConfiguration(topGuiFile);
            topGuiTitle = ColorUtil.translate(topGuiConfig.getString("TopTitle", "&c&lBảng Xếp Hạng Điểm Danh Tháng <month>"));
            TotalTitle = ColorUtil.translate(topGuiConfig.getString("TotalTitle", "&c&lBảng Xếp Hạng Điểm Danh Tổng"));
        }

        loadLanguageFiles();

        if (diemDanhTop != null) {
            diemDanhTop.refreshCache();
        }
    }

    private void updateConfigFile(String fileName) {
        File configFile = new File(getDataFolder(), fileName);
        if (!configFile.exists()) {
            saveResource(fileName, false);
            return;
        }

        try (InputStream stream = getResource(fileName)) {
            if (stream == null) return;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                int latestVersion = defaultConfig.getInt("version", 1);
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

                if (!config.contains("version") || config.getInt("version", 1) < latestVersion) {
                    getLogger().info("Cap nhat " + fileName + " len phien ban moi...");
                    File oldConfigFile = new File(getDataFolder(), fileName + "_old");
                    configFile.renameTo(oldConfigFile);
                    saveResource(fileName, false);
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Loi khi kiem tra phien ban file: " + fileName, e);
        }
    }

    private void loadLanguageFiles() {
        languageConfigs = new HashMap<>();
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

        File[] files = languageFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("message_") && file.getName().endsWith(".yml")) {
                    String langCode = file.getName().substring(8, file.getName().length() - 4);
                    FileConfiguration langConfig = YamlConfiguration.loadConfiguration(file);
                    languageConfigs.put(langCode, langConfig);
                    getLogger().info("Loaded language file: " + file.getName());
                }
            }
        }
    }

    public void reloadLanguageFiles() {
        loadLanguageFiles();
        createEditorConfig();
    }

    @Override
    public void onDisable() {
        if (diemDanhTop != null) {
            diemDanhTop.stopAutoUpdateTask();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAllSync();
        }
        getLogger().info(ColorUtil.translate("&7--------------------------------------"));
        getLogger().info(ColorUtil.translate("&eDiemDanh Reloaded&a has been disabled"));
        getLogger().info(ColorUtil.translate("&8Plugin by SkyGamer"));
        getLogger().info(ColorUtil.translate("&7--------------------------------------"));
    }

    public DiemDanhTop getDiemDanhTop() {
        return diemDanhTop;
    }

    public DiemDanhGUI getDiemDanhGUI() { return diemDanhGUI; }
    public DiemDanhEditor getDiemDanhEditor() { return diemDanhEditor; }
    public String getTopGuiTitle() {
        return topGuiTitle;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        LocalDate today = LocalDate.now();

        if (!playerDataManager.hasPlayerData(playerUUID)) {
            playerDataManager.initPlayer(playerUUID, player.getName());
        } else {
            playerDataManager.setPlayerName(playerUUID, player.getName());
        }

        int lastCheckInMonth = playerDataManager.getLastCheckInMonth(playerUUID);

        if (today.getMonthValue() != lastCheckInMonth) {
            playerDataManager.setCheckedDays(playerUUID, new ArrayList<>());
            playerDataManager.setDaysCheckedIn(playerUUID, 0);
            playerDataManager.setMissedDays(playerUUID, new ArrayList<>());
            playerDataManager.setLastCheckInMonth(playerUUID, today.getMonthValue());
            for (int daysRequired : new int[]{7, 14, 21}) {
                playerDataManager.setTichLuyClaimed(playerUUID, daysRequired, false, 0);
            }
        }

        updateMissedDays(playerUUID);

        try {
            LocalDate lastCheckInDate = LocalDate.parse(playerDataManager.getLastCheckIn(playerUUID));
            if (today.getYear() != lastCheckInDate.getYear()) {
                playerDataManager.resetSpecialDays(playerUUID);
            }
        } catch (Exception ignored) {}

        List<Integer> checkedDays = playerDataManager.getCheckedDays(playerUUID);
        if (!checkedDays.contains(today.getDayOfMonth())) {
            TextComponent message = new TextComponent(getMessage("ChuaDiemDanhHomNay"));
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/diemdanh"));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(getMessage("Hover")).create()));
            player.spigot().sendMessage(message);
        }

        playerDataManager.savePlayerDataAsync(playerUUID);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (playerDataManager != null) {
            playerDataManager.unloadPlayer(event.getPlayer().getUniqueId());
        }
    }

    private void updateMissedDays(UUID playerUUID) {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int lastCheckInMonth = playerDataManager.getLastCheckInMonth(playerUUID);

        if (currentMonth != lastCheckInMonth) {
            playerDataManager.setCheckedDays(playerUUID, new ArrayList<>());
            playerDataManager.setDaysCheckedIn(playerUUID, 0);
            playerDataManager.setLastCheckInMonth(playerUUID, currentMonth);
        }

        List<Integer> missedDays = new ArrayList<>(playerDataManager.getMissedDays(playerUUID));
        List<Integer> checkedDays = playerDataManager.getCheckedDays(playerUUID);

        for (int day = 1; day < today.getDayOfMonth(); day++) {
            if (!checkedDays.contains(day) && !missedDays.contains(day)) {
                missedDays.add(day);
            }
        }

        playerDataManager.setMissedDays(playerUUID, missedDays);
    }

    public void savePlayerData() {
        if (playerDataManager != null) {
            playerDataManager.saveAllSync();
        }
    }

    public String getMessage(String key) {
        String language = getConfig().getString("language", "en");
        FileConfiguration langConfig = languageConfigs.get(language);

        if (langConfig == null) {
            getLogger().warning("Language " + language + " not found. Falling back to English.");
            langConfig = languageConfigs.get("en");
        }

        if (langConfig == null) {
            return "&cMissing message: " + key;
        }

        String message = langConfig.getString("Message." + key);
        if (message == null) {
            getLogger().warning("Missing message key: " + key + " in " + language + " language file");
            return "&cMissing message: " + key;
        }

        return ColorUtil.translate(message);
    }
}
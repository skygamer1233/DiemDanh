package me.diemdanh;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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




    public String guiTitle;
    public FileConfiguration topGuiConfig;
    public File topGuiFile;

    private Map<String, FileConfiguration> languageConfigs;





    @Override
    public void onEnable() {
        updateConfigFile("config.yml");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("diemdanh").setExecutor(new DiemDanhCommand(this));
        getCommand("diemdanh").setTabCompleter(new TabComplete());
        diemDanhTop = new DiemDanhTop(this);
        getServer().getPluginManager().registerEvents(diemDanhTop.new TopGUIListener(), this);



        this.getServer().getConsoleSender().sendMessage(color.transalate("&7--------------------------------------"));
        this.getServer().getConsoleSender().sendMessage(color.transalate("&eDiemDanh Reloaded&a has been enabled"));
        this.getServer().getConsoleSender().sendMessage(color.transalate("&8Plugin by SkyGamer"));
        this.getServer().getConsoleSender().sendMessage(color.transalate("&7--------------------------------------"));

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
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), fileName)); // Tải config từ file
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
        this.getServer().getConsoleSender().sendMessage(color.transalate("&7--------------------------------------"));
        this.getServer().getConsoleSender().sendMessage(color.transalate("&eDiemDanh Reloaded&a has been disabled"));
        this.getServer().getConsoleSender().sendMessage(color.transalate("&8Plugin by SkyGamer"));
        this.getServer().getConsoleSender().sendMessage(color.transalate("&7--------------------------------------"));
    }
    public DiemDanhTop getDiemDanhTop() {
        return diemDanhTop;
    }
    public String getTopGuiTitle() {
        return topGuiTitle;
    }





    public void openDiemDanhGUI(Player player) {
        LocalDate today = LocalDate.now();
        String playerUUID = player.getUniqueId().toString();
        String titleWithMonth = guiTitle.replace("<month>", String.valueOf(today.getMonthValue()));
        Inventory gui = Bukkit.createInventory(null, 45, titleWithMonth);

        ConfigurationSection daysSection = getConfig().getConfigurationSection("Days");
        if (daysSection == null) {
            getLogger().severe("Missing 'Days' section in config.yml");
            return;
        }
        List<?> dayEntries = new ArrayList<>(daysSection.getValues(false).values());

        for (String specialDayKey : getConfig().getConfigurationSection("SpecialDay").getKeys(false)) {
            ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey);
            int specialDayDate = specialDaySection.getInt("Require.Date");
            int specialDayMonth = specialDaySection.getInt("Require.Month");
            if (today.getMonthValue() == specialDayMonth) {
                String itemKey = getSpecialDayItemKey(playerUUID, specialDayKey);
                ConfigurationSection itemSection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Icon." + itemKey);
                ItemStack item = createItemFromConfig(itemSection, specialDayDate, playerUUID);
                gui.setItem(specialDayDate - 1, item);
            }
        }

        for (int day = 1; day <= 31; day++) {

            if (gui.getItem(day - 1) != null) {
                continue;
            }

            String itemKey = getItemKeyForDay(day, playerUUID);
            ConfigurationSection itemSection;

            if (itemKey.equals("DiemDanhBu")) {
                itemSection = getConfig().getConfigurationSection("Item." + itemKey);
            } else {
                itemSection = getConfig().getConfigurationSection("Item." + itemKey);
            }

            if (itemSection == null) {
                getLogger().warning("Missing item section for key: " + itemKey);
                continue;
            }

            List<String> lore = new ArrayList<>();
            if (day - 1 < dayEntries.size()) {
                Object dayEntry = dayEntries.get(day - 1);
                if (dayEntry instanceof ConfigurationSection) {
                    ConfigurationSection daySection = (ConfigurationSection) dayEntry;
                    if (daySection.contains("Lore")) {
                        lore = daySection.getStringList("Lore");
                    }
                }
            }


            if (lore.isEmpty() && !itemKey.equals("DiemDanhBu")) {
                lore = itemSection.getStringList("Lore");
            }


            List<String> translatedLore = new ArrayList<>();
            for (String line : lore) {
                translatedLore.add(color.transalate(line));
            }


            XMaterial xMaterial = XMaterial.matchXMaterial(itemSection.getString("ID")).orElse(XMaterial.BARRIER);
            Material material = xMaterial.parseMaterial();

            String name = color.transalate(itemSection.getString("Name").replace("<date>", String.valueOf(day)));
            boolean glow = itemSection.getBoolean("Glow");

            int itemAmount = day;

            ItemStack item = new ItemStack(material, itemAmount);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(translatedLore);
            if (glow) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);

            gui.setItem(day - 1, item);
        }

        for (int i = 0; i < 3; i++) {
            int daysRequired = (i + 1) * 7;
            String itemKey = getTichLuyItemKey(playerUUID, daysRequired);
            ConfigurationSection itemSection = getConfig().getConfigurationSection("TichLuy." + daysRequired + "ngay.Icon." + itemKey);

            ItemStack item = createItemFromConfig(itemSection, daysRequired, playerUUID);
            gui.setItem(36 + i, item);
        }

        ConfigurationSection ticketSection = getConfig().getConfigurationSection("Item.Ticket");
        if (ticketSection != null) {
            int tickets = playerData.getInt(playerUUID + ".tickets", 0);
            ItemStack ticketItem = createItemFromConfig(ticketSection, tickets, playerUUID);
            gui.setItem(34, ticketItem);
        } else {
            getLogger().warning("Missing 'Item.Ticket' section in config.yml");
        }

        ConfigurationSection thongTinSection = getConfig().getConfigurationSection("Item.ThongTin");
        if (thongTinSection != null) {
            int daysCheckedIn = getDaysCheckedInThisMonth(playerUUID);
            ItemStack thongTinItem = createItemFromConfig(thongTinSection, daysCheckedIn, playerUUID);
            gui.setItem(35, thongTinItem);
        } else {
            getLogger().warning("Missing 'Item.ThongTin' section in config.yml");
        }

        player.openInventory(gui);
    }
    private ItemStack createItemFromConfig(ConfigurationSection itemSection, int replaceValue, String playerUUID) {
        XMaterial xMaterial = XMaterial.matchXMaterial(itemSection.getString("ID")).orElse(XMaterial.BARRIER);
        Material material = xMaterial.parseMaterial();

        List<String> rawlore = itemSection.getStringList("Lore");
        List<String> translatedLore = new ArrayList<>();
        for (String line : rawlore) {
            line = line.replace("<days>", String.valueOf(getDaysCheckedInThisMonth(playerUUID)));
            line = line.replace("<tickets>", String.valueOf(playerData.getInt(playerUUID + ".tickets", 0)));
            line = line.replace("<totaldays>", String.valueOf(playerData.getInt(playerUUID + ".totalDays", 0)));
            translatedLore.add(color.transalate(line));
        }

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color.transalate(itemSection.getString("Name").replace("<date>", String.valueOf(replaceValue))));
        meta.setLore(translatedLore);
        if (itemSection.getBoolean("Glow")) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }







    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(guiTitle.replace("<month>", String.valueOf(LocalDate.now().getMonthValue())))) return;
        if (!(event.getWhoClicked() instanceof Player)) {
            event.getWhoClicked().sendMessage(getMessage("NotPlayer"));
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        LocalDate today = LocalDate.now();
        String playerUUID = player.getUniqueId().toString();


        if (slot < 0 || slot > 44) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        String originalItemKey;
        String specialDayKey = getSpecialDayKey(today, slot + 1);

        if (slot < 31) {
            originalItemKey = (specialDayKey != null) ? getSpecialDayItemKey(playerUUID, specialDayKey) : getItemKeyForDay(slot + 1, playerUUID);
            if (originalItemKey.equals("DiemDanh")) {
                ConfigurationSection rewardSection = (specialDayKey != null)
                        ? getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                        : getConfig().getConfigurationSection("Days." + (slot + 1));

                if (rewardSection != null) {
                    executeCommands(rewardSection.getStringList("Reward"), player);
                }

                markPlayerCheckedIn(playerUUID, slot + 1);

                String messageKey = (specialDayKey != null) ? "SpecialDayDiemDanhThanhCong" : "DiemDanh";
                player.sendMessage(color.transalate(getConfig().getString("Message." + messageKey, "&aBạn đã điểm danh thành công!")
                        .replace("%day%", String.valueOf(slot + 1))
                        .replace("%specialDay%", specialDayKey != null ? specialDayKey : "")));
            } else if (originalItemKey.equals("DiemDanhBu")) {

                if (playerData.getInt(playerUUID + ".tickets", 0) > 0) {
                    ConfigurationSection rewardSection = (specialDayKey != null)
                            ? getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                            : getConfig().getConfigurationSection("Days." + (slot + 1));

                    if (rewardSection != null) {
                        executeCommands(rewardSection.getStringList("Reward"), player);
                    }


                    playerData.set(playerUUID + ".tickets", playerData.getInt(playerUUID + ".tickets", 0) - 1);
                    List<Integer> missedDays = playerData.getIntegerList(playerUUID + ".missedDays");
                    missedDays.remove(Integer.valueOf(slot + 1));
                    playerData.set(playerUUID + ".missedDays", missedDays);


                    markPlayerCheckedIn(playerUUID, slot + 1);

                    savePlayerData();
                    player.sendMessage(getMessage("DiemDanh").replace("%day%", String.valueOf(slot + 1)));
                } else {
                    player.sendMessage(getMessage("NotRequire"));
                }
            } else if (originalItemKey.equals("ChuaDiemDanh")) {
                player.sendMessage(isToday(slot + 1) ? getMessage("Claiming") : getMessage("ChuaDiemDanh").replace("%day%", String.valueOf(slot + 1)));
            } else if (specialDayKey != null && originalItemKey.equals("DaDiemDanh")) {
                player.sendMessage(color.transalate(getConfig().getString("Message.SpecialDayDaDiemDanh", "&cBạn đã điểm danh ngày lễ này rồi!")));
            } else if (originalItemKey.equals("DaDiemDanh")) {
                player.sendMessage(getMessage("DaDiemDanh"));
            } else {
                String dayName = (specialDayKey != null)
                        ? getConfig().getString("SpecialDay." + specialDayKey + ".Icon.NgayDiemDanh.Name", specialDayKey)
                        : String.valueOf(slot + 1);
                player.sendMessage(getMessage("NgayDiemDanh").replace("%day%", dayName));
            }
        } else if (slot >= 36 && slot <= 38) {
            originalItemKey = getTichLuyItemKey(playerUUID, (slot - 36 + 1) * 7);

            if (originalItemKey.equals("NhanQua")) {
                ConfigurationSection rewardSection = getConfig().getConfigurationSection("TichLuy." + (slot - 36 + 1) * 7 + "ngay");
                if (rewardSection != null) {
                    executeCommands(rewardSection.getStringList("Reward"), player);
                    playerData.set(playerUUID + ".tichluy." + (slot - 36 + 1) * 7, true);
                    savePlayerData();
                    player.sendMessage(color.transalate(getConfig().getString("Message.TichLuySuccess", "&aBạn đã nhận quà tích lũy %days% ngày thành công!").replace("%days%", String.valueOf((slot - 36 + 1) * 7))));
                }
            } else if (originalItemKey.equals("DaNhanQua")) {
                player.sendMessage(getMessage("IsClaimed"));
            } else {
                player.sendMessage(getMessage("NotRequire"));
            }
        }

        openDiemDanhGUI(player);
    }
    private void executeCommands(List<String> commands, Player player) {
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
        }
    }

    private String getItemKeyForDay(int day, String playerUUID) {
        LocalDate today = LocalDate.now();
        if (day > today.getDayOfMonth()) {
            return "NgayDiemDanh";
        } else if (playerData.getIntegerList(playerUUID + ".checkedDays").contains(day)) {
            return "DaDiemDanh";
        } else if (playerData.getInt(playerUUID + ".tickets", 0) > 0 && playerData.getIntegerList(playerUUID + ".missedDays").contains(day)) {
            return "DiemDanhBu";
        } else if (day < today.getDayOfMonth()) {
            return "ChuaDiemDanh";
        } else {
            return "DiemDanh";
        }
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
                playerData.set(playerUUID + ".tichluy." + daysRequired, false);
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

    private boolean isToday(int day) {
        LocalDate today = LocalDate.now();
        return day == today.getDayOfMonth();
    }

    private void markPlayerCheckedIn(String playerUUID, int day) {
        LocalDate today = LocalDate.now();
        String specialDayKey = getSpecialDayKey(today, day);

        if (specialDayKey != null) {
            playerData.set(playerUUID + ".specialDays." + specialDayKey, true);
        } else {
            playerData.set(playerUUID + ".lastCheckIn", LocalDate.now().toString());
            int currentMonth = LocalDate.now().getMonthValue();
            int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
            if (currentMonth != lastCheckInMonth) {

                playerData.set(playerUUID + ".checkedDays", new ArrayList<>());
                playerData.set(playerUUID + ".daysCheckedIn", 0);
            }
            List<Integer> checkedDays = playerData.getIntegerList(playerUUID + ".checkedDays");
            checkedDays.add(day);
            playerData.set(playerUUID + ".checkedDays", checkedDays);
        }
        int currentMonth = LocalDate.now().getMonthValue();
        int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
        int daysCheckedIn = playerData.getInt(playerUUID + ".daysCheckedIn", 0);
        if (currentMonth != lastCheckInMonth) {
            daysCheckedIn = 0;
        }
        daysCheckedIn++;

        if (!playerData.getIntegerList(playerUUID + ".missedDays").contains(day)) {
            int totalDays = playerData.getInt(playerUUID + ".totalDays", 0);
            totalDays++;
            playerData.set(playerUUID + ".totalDays", totalDays);
        }

        playerData.set(playerUUID + ".daysCheckedIn", daysCheckedIn);
        playerData.set(playerUUID + ".lastCheckInMonth", currentMonth);

        LocalDate lastCheckInDate = LocalDate.parse(playerData.getString(playerUUID + ".lastCheckIn", "1970-01-01"));
        if (today.getYear() != lastCheckInDate.getYear()) {
            for (String key : playerData.getConfigurationSection(playerUUID + ".specialDays").getKeys(false)) {
                playerData.set(playerUUID + ".specialDays." + key, false);
            }
        }

        savePlayerData();
    }

    public void savePlayerData() {
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save player data to file", e);
        }
    }
    private int getDaysCheckedInThisMonth(String playerUUID) {
        int currentMonth = LocalDate.now().getMonthValue();
        int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
        if (currentMonth == lastCheckInMonth) {
            return playerData.getInt(playerUUID + ".daysCheckedIn", 0);
        } else {
            return 0;
        }
    }

    private String getTichLuyItemKey(String playerUUID, int daysRequired) {
        int daysCheckedIn = getDaysCheckedInThisMonth(playerUUID);
        boolean hasClaimed = playerData.getBoolean(playerUUID + ".tichluy." + daysRequired, false);


        if (daysCheckedIn >= daysRequired && !hasClaimed) {
            return "NhanQua";
        } else if (hasClaimed) {
            return "DaNhanQua";
        } else {
            return "ChuaNhanQua";
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

    private String getSpecialDayKey(LocalDate today, int day) {
        for (String key : getConfig().getConfigurationSection("SpecialDay").getKeys(false)) {
            ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + key + ".Require");
            int specialDayDate = specialDaySection.getInt("Date");
            int specialDayMonth = specialDaySection.getInt("Month");
            if (specialDayMonth < 1 || specialDayMonth > 12) {
                getLogger().warning("Invalid month for special day: " + key);
                continue;
            }
            if (day == specialDayDate && today.getMonthValue() == specialDayMonth) {
                return key;
            }
        }
        return null;
    }

    private String getSpecialDayItemKey(String playerUUID, String specialDayKey) {
        boolean hasCheckedIn = playerData.getBoolean(playerUUID + ".specialDays." + specialDayKey, false);
        ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Require");
        int specialDayDate = specialDaySection.getInt("Date");
        int specialDayMonth = specialDaySection.getInt("Month");

        LocalDate specialDay = LocalDate.of(LocalDate.now().getYear(), specialDayMonth, specialDayDate);

        if (hasCheckedIn) {
            return "DaDiemDanh";
        } else if (LocalDate.now().isEqual(specialDay)) {
            return "DiemDanh";
        } else if (LocalDate.now().isAfter(specialDay)) {
            if (playerData.getInt(playerUUID + ".tickets", 0) > 0) {
                return "DiemDanhBu";
            } else {
                return "ChuaDiemDanh";
            }
        } else {
            return "NgayDiemDanh";
        }
    }
}
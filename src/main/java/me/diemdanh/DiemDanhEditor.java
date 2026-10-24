package me.diemdanh;

import com.cryptomorin.xseries.XMaterial;
import me.diemdanh.holder.DiemDanhEditorHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DiemDanhEditor implements Listener {
    private final DiemDanh plugin;
    private FileConfiguration editorConfig;

    public enum EditType {
        ADD_COMMAND,
        CREATE_SPECIAL_DAY,
        CHANGE_DISPLAY_NAME,
        ADD_LORE_LINE,
        EDIT_LORE_LINE
    }

    public static class EditSession {
        public String configPath;
        public EditType editType;
        public String returnMenu;
        public String title;
        public int lineIndex;
        public String extra;

        public EditSession(String configPath, EditType editType, String returnMenu, String title) {
            this(configPath, editType, returnMenu, title, -1, "");
        }

        public EditSession(String configPath, EditType editType, String returnMenu, String title, int lineIndex, String extra) {
            this.configPath = configPath;
            this.editType = editType;
            this.returnMenu = returnMenu;
            this.title = title;
            this.lineIndex = lineIndex;
            this.extra = extra != null ? extra : "";
        }
    }

    private final Map<UUID, EditSession> activeSessions = new java.util.concurrent.ConcurrentHashMap<>();

    public DiemDanhEditor(DiemDanh plugin) {
        this.plugin = plugin;
        loadEditorConfig();
    }

    public void loadEditorConfig() {
        File file = new File(plugin.getDataFolder(), "editor.yml");
        if (!file.exists()) {
            plugin.saveResource("editor.yml", false);
        }
        this.editorConfig = YamlConfiguration.loadConfiguration(file);
        try (InputStream defStream = plugin.getResource("editor.yml")) {
            if (defStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
                this.editorConfig.setDefaults(defConfig);
            }
        } catch (Exception ignored) {}
    }

    public FileConfiguration getEditorConfig() {
        if (editorConfig == null) {
            loadEditorConfig();
        }
        return editorConfig;
    }

    public List<String> getLoreFromConfig(String configPath) {
        List<String> lores = plugin.getConfig().getStringList(configPath);
        if (!lores.isEmpty()) {
            return new ArrayList<>(lores);
        }

        // Fallback đọc từ Icon nếu chưa tạo trực tiếp path TichLuy.<moc>ngay.Lore
        if (configPath.startsWith("TichLuy.") && configPath.endsWith(".Lore")) {
            String base = configPath.substring(0, configPath.length() - 5);
            lores = plugin.getConfig().getStringList(base + ".Icon.NhanQua.Lore");
            if (lores.isEmpty()) {
                lores = plugin.getConfig().getStringList(base + ".Icon.ChuaNhanQua.Lore");
            }
        }
        // Fallback đọc từ Icon nếu chưa tạo trực tiếp path SpecialDay.<key>.Lore
        else if (configPath.startsWith("SpecialDay.") && configPath.endsWith(".Lore")) {
            String base = configPath.substring(0, configPath.length() - 5);
            lores = plugin.getConfig().getStringList(base + ".Icon.NgayDiemDanh.Lore");
            if (lores.isEmpty()) {
                lores = plugin.getConfig().getStringList(base + ".Icon.DiemDanh.Lore");
            }
        }

        return new ArrayList<>(lores);
    }

    public void saveLoreToConfig(String configPath, List<String> lores) {
        plugin.getConfig().set(configPath, lores);

        // Đồng bộ các trạng thái Icon của TichLuy (ChuaNhanQua, NhanQua, DaNhanQua)
        if (configPath.startsWith("TichLuy.") && configPath.endsWith(".Lore")) {
            String base = configPath.substring(0, configPath.length() - 5);
            plugin.getConfig().set(base + ".Icon.ChuaNhanQua.Lore", lores);
            plugin.getConfig().set(base + ".Icon.NhanQua.Lore", lores);
            plugin.getConfig().set(base + ".Icon.DaNhanQua.Lore", lores);
        }
        // Đồng bộ các trạng thái Icon của SpecialDay
        else if (configPath.startsWith("SpecialDay.") && configPath.endsWith(".Lore")) {
            String base = configPath.substring(0, configPath.length() - 5);
            ConfigurationSection iconSec = plugin.getConfig().getConfigurationSection(base + ".Icon");
            if (iconSec != null) {
                for (String iconKey : iconSec.getKeys(false)) {
                    plugin.getConfig().set(base + ".Icon." + iconKey + ".Lore", lores);
                }
            }
        }

        plugin.saveConfig();
    }

    private ItemStack createConfigItem(String path, Map<String, String> replacements, List<String> rewardList, List<String> loreList) {
        String matStr = getEditorConfig().getString(path + ".ID", "BARRIER");
        XMaterial xMat = XMaterial.matchXMaterial(matStr).orElse(XMaterial.BARRIER);
        Material mat = xMat.parseMaterial();
        if (mat == null) mat = Material.BARRIER;

        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = getEditorConfig().getString(path + ".Name", "");
            if (replacements != null) {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    name = name.replace(entry.getKey(), entry.getValue());
                }
            }
            meta.setDisplayName(ColorUtil.translate(name));

            List<String> rawLore = getEditorConfig().getStringList(path + ".Lore");
            List<String> finalLore = new ArrayList<>();

            for (String line : rawLore) {
                if (line.equalsIgnoreCase("<rewards>")) {
                    if (rewardList == null || rewardList.isEmpty()) {
                        finalLore.add(ColorUtil.translate(getEditorConfig().getString("Icons.NoRewardFormat", " &c(Chưa có lệnh)")));
                    } else {
                        String format = getEditorConfig().getString("Icons.RewardFormat", " &e- /<command>");
                        for (String cmd : rewardList) {
                            finalLore.add(ColorUtil.translate(format.replace("<command>", cmd)));
                        }
                    }
                } else if (line.equalsIgnoreCase("<lores>")) {
                    if (loreList == null || loreList.isEmpty()) {
                        finalLore.add(ColorUtil.translate(getEditorConfig().getString("Icons.NoLoreFormat", " &c(Chưa có dòng mô tả)")));
                    } else {
                        String format = getEditorConfig().getString("Icons.LoreFormat", " &f- <lore>");
                        for (String l : loreList) {
                            finalLore.add(ColorUtil.translate(format.replace("<lore>", l)));
                        }
                    }
                } else {
                    if (replacements != null) {
                        for (Map.Entry<String, String> entry : replacements.entrySet()) {
                            line = line.replace(entry.getKey(), entry.getValue());
                        }
                    }
                    finalLore.add(ColorUtil.translate(line));
                }
            }
            meta.setLore(finalLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void sendMessage(Player player, String messageKey, Map<String, String> replacements) {
        if (getEditorConfig().isList("Messages." + messageKey)) {
            List<String> lines = getEditorConfig().getStringList("Messages." + messageKey);
            for (String line : lines) {
                if (replacements != null) {
                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                        line = line.replace(entry.getKey(), entry.getValue());
                    }
                }
                player.sendMessage(ColorUtil.translate(line));
            }
        } else {
            String msg = getEditorConfig().getString("Messages." + messageKey, "");
            if (replacements != null) {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    msg = msg.replace(entry.getKey(), entry.getValue());
                }
            }
            player.sendMessage(ColorUtil.translate(msg));
        }
    }

    // --- 1. MENU CHÍNH ---
    public void openMainMenu(Player player) {
        String title = ColorUtil.translate(getEditorConfig().getString("Title.MainMenu", "&8[&cDiemDanh Editor&8] &0Menu Chính"));
        Inventory gui = Bukkit.createInventory(new DiemDanhEditorHolder(DiemDanhEditorHolder.MenuType.MAIN), 27, title);

        gui.setItem(11, createConfigItem("Icons.MainMenu.Days", null, null, null));
        gui.setItem(13, createConfigItem("Icons.MainMenu.TichLuy", null, null, null));
        gui.setItem(15, createConfigItem("Icons.MainMenu.SpecialDay", null, null, null));

        player.openInventory(gui);
    }

    // --- 2. MENU EDIT NGÀY THƯỜNG ---
    public void openDaysEditor(Player player) {
        String title = ColorUtil.translate(getEditorConfig().getString("Title.DaysMenu", "&8[&cDiemDanh Editor&8] &0Ngày Thường"));
        Inventory gui = Bukkit.createInventory(new DiemDanhEditorHolder(DiemDanhEditorHolder.MenuType.DAYS), 45, title);

        for (int day = 1; day <= 31; day++) {
            List<String> rewards = plugin.getConfig().getStringList("Days." + day + ".Reward");
            List<String> lores = getLoreFromConfig("Days." + day + ".Lore");
            Map<String, String> rep = new HashMap<>();
            rep.put("<day>", String.valueOf(day));

            gui.setItem(day - 1, createConfigItem("Icons.DaysMenu.DayItem", rep, rewards, lores));
        }

        gui.setItem(44, createConfigItem("Icons.DaysMenu.Back", null, null, null));
        player.openInventory(gui);
    }

    // --- 3. MENU EDIT TÍCH LŨY ---
    public void openTichLuyEditor(Player player) {
        String title = ColorUtil.translate(getEditorConfig().getString("Title.TichLuyMenu", "&8[&cDiemDanh Editor&8] &0Tích Lũy"));
        Inventory gui = Bukkit.createInventory(new DiemDanhEditorHolder(DiemDanhEditorHolder.MenuType.TICHLUY), 27, title);

        int[] mocs = {7, 14, 21};
        int[] slots = {11, 13, 15};

        for (int i = 0; i < mocs.length; i++) {
            int moc = mocs[i];
            List<String> rewards = plugin.getConfig().getStringList("TichLuy." + moc + "ngay.Reward");
            List<String> lores = getLoreFromConfig("TichLuy." + moc + "ngay.Lore");
            Map<String, String> rep = new HashMap<>();
            rep.put("<moc>", String.valueOf(moc));

            gui.setItem(slots[i], createConfigItem("Icons.TichLuyMenu.Item", rep, rewards, lores));
        }

        gui.setItem(26, createConfigItem("Icons.TichLuyMenu.Back", null, null, null));
        player.openInventory(gui);
    }

    // --- 4. MENU EDIT SPECIAL DAY ---
    public void openSpecialDayEditor(Player player) {
        String title = ColorUtil.translate(getEditorConfig().getString("Title.SpecialDayMenu", "&8[&cDiemDanh Editor&8] &0Ngày Lễ"));
        Inventory gui = Bukkit.createInventory(new DiemDanhEditorHolder(DiemDanhEditorHolder.MenuType.SPECIAL), 54, title);

        ConfigurationSection specialSec = plugin.getConfig().getConfigurationSection("SpecialDay");
        int slot = 0;
        if (specialSec != null) {
            for (String key : specialSec.getKeys(false)) {
                if (slot >= 45) break;
                int date = plugin.getConfig().getInt("SpecialDay." + key + ".Require.Date");
                int month = plugin.getConfig().getInt("SpecialDay." + key + ".Require.Month");
                String displayName = plugin.getConfig().getString("SpecialDay." + key + ".Icon.NgayDiemDanh.Name", "&e" + key);
                List<String> rewards = plugin.getConfig().getStringList("SpecialDay." + key + ".Reward");
                List<String> lores = getLoreFromConfig("SpecialDay." + key + ".Lore");

                Map<String, String> rep = new HashMap<>();
                rep.put("<key>", key);
                rep.put("<display_name>", displayName);
                rep.put("<date>", String.valueOf(date));
                rep.put("<month>", String.valueOf(month));

                gui.setItem(slot, createConfigItem("Icons.SpecialDayMenu.SpecialItem", rep, rewards, lores));
                slot++;
            }
        }

        gui.setItem(48, createConfigItem("Icons.SpecialDayMenu.CreateNew", null, null, null));
        gui.setItem(53, createConfigItem("Icons.SpecialDayMenu.Back", null, null, null));

        player.openInventory(gui);
    }

    // --- 5. MENU DANH SÁCH LỆNH (COMMAND EDITOR) ---
    public void openCommandEditor(Player player, String configPath, String title, String returnMenu) {
        String rawTitleFormat = getEditorConfig().getString("Title.CommandEditor", "&8[Editor Lệnh] &0<title>");
        String guiTitle = ColorUtil.translate(rawTitleFormat.replace("<title>", title));
        String extraData = configPath + "::" + returnMenu + "::" + title;
        Inventory gui = Bukkit.createInventory(new DiemDanhEditorHolder(DiemDanhEditorHolder.MenuType.COMMAND, extraData), 54, guiTitle);

        List<String> commands = plugin.getConfig().getStringList(configPath);
        for (int i = 0; i < commands.size(); i++) {
            if (i >= 45) break;
            String cmd = commands.get(i);
            Map<String, String> rep = new HashMap<>();
            rep.put("<index>", String.valueOf(i + 1));
            rep.put("<command>", cmd);

            gui.setItem(i, createConfigItem("Icons.CommandEditor.CmdItem", rep, null, null));
        }

        gui.setItem(49, createConfigItem("Icons.CommandEditor.AddCmd", null, null, null));

        // Nút chuyển sang sửa Lore
        if (returnMenu.equals("DAYS") && title.startsWith("Ngày ")) {
            gui.setItem(50, createConfigItem("Icons.CommandEditor.SwitchLore", null, null, null));
        } else if (returnMenu.equals("TICHLUY") && title.startsWith("Tích Lũy ")) {
            gui.setItem(50, createConfigItem("Icons.CommandEditor.SwitchLore", null, null, null));
        } else if (returnMenu.equals("SPECIAL") && title.startsWith("Lễ ")) {
            String key = title.replace("Lễ ", "").trim();
            String currentDisplayName = plugin.getConfig().getString("SpecialDay." + key + ".Icon.NgayDiemDanh.Name", "&e" + key);
            Map<String, String> rep = new HashMap<>();
            rep.put("<display_name>", currentDisplayName);
            rep.put("<key>", key);

            gui.setItem(50, createConfigItem("Icons.CommandEditor.ChangeNameTag", rep, null, null));
            gui.setItem(51, createConfigItem("Icons.CommandEditor.SwitchLore", null, null, null));
            gui.setItem(52, createConfigItem("Icons.CommandEditor.DeleteSpecialDay", rep, null, null));
        }

        gui.setItem(53, createConfigItem("Icons.CommandEditor.Back", null, null, null));

        player.openInventory(gui);
    }

    // --- 6. MENU CHỈNH SỬA TỪNG DÒNG LORE (LORE EDITOR) ---
    public void openLoreEditor(Player player, String configPath, String title, String returnMenu) {
        String rawTitleFormat = getEditorConfig().getString("Title.LoreEditor", "&8[Editor Lore] &0<title>");
        String guiTitle = ColorUtil.translate(rawTitleFormat.replace("<title>", title));
        String extraData = configPath + "::" + returnMenu + "::" + title;
        Inventory gui = Bukkit.createInventory(new DiemDanhEditorHolder(DiemDanhEditorHolder.MenuType.LORE, extraData), 54, guiTitle);

        List<String> lores = getLoreFromConfig(configPath);
        for (int i = 0; i < lores.size(); i++) {
            if (i >= 45) break;
            String line = lores.get(i);
            Map<String, String> rep = new HashMap<>();
            rep.put("<index>", String.valueOf(i + 1));
            rep.put("<lore_line>", line);

            gui.setItem(i, createConfigItem("Icons.LoreEditor.LoreItem", rep, null, null));
        }

        // Slot 48: Nút chuyển sang sửa Lệnh Thưởng
        if (returnMenu.equals("DAYS") || returnMenu.equals("TICHLUY") || returnMenu.equals("SPECIAL")) {
            gui.setItem(48, createConfigItem("Icons.LoreEditor.SwitchReward", null, null, null));
        }

        // Slot 49: Nút thêm dòng Lore mới
        gui.setItem(49, createConfigItem("Icons.LoreEditor.AddLine", null, null, null));

        // Slot 52: Nút xóa ngày lễ (nếu đang ở menu Special Day)
        if (returnMenu.equals("SPECIAL") && title.startsWith("Lễ ")) {
            String key = title.replace("Lễ ", "").trim();
            Map<String, String> rep = new HashMap<>();
            rep.put("<key>", key);
            gui.setItem(52, createConfigItem("Icons.CommandEditor.DeleteSpecialDay", rep, null, null));
        }

        // Slot 53: Nút quay lại
        gui.setItem(53, createConfigItem("Icons.LoreEditor.Back", null, null, null));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (InventoryUtil.getHolder(event.getInventory()) instanceof DiemDanhEditorHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(InventoryUtil.getHolder(event.getInventory()) instanceof DiemDanhEditorHolder holder)) return;

        event.setCancelled(true);
        int slot = event.getSlot();
        if (slot < 0 || event.getCurrentItem() == null) return;

        switch (holder.getMenuType()) {
            case MAIN -> {
                if (slot == 11) openDaysEditor(player);
                else if (slot == 13) openTichLuyEditor(player);
                else if (slot == 15) openSpecialDayEditor(player);
            }
            case DAYS -> {
                if (slot == 44) {
                    openMainMenu(player);
                } else if (slot >= 0 && slot < 31) {
                    int day = slot + 1;
                    if (event.isRightClick()) {
                        openLoreEditor(player, "Days." + day + ".Lore", "Ngày " + day, "DAYS");
                    } else {
                        openCommandEditor(player, "Days." + day + ".Reward", "Ngày " + day, "DAYS");
                    }
                }
            }
            case TICHLUY -> {
                if (slot == 26) {
                    openMainMenu(player);
                } else if (slot == 11 || slot == 13 || slot == 15) {
                    int moc = (slot == 11) ? 7 : (slot == 13 ? 14 : 21);
                    String pathReward = "TichLuy." + moc + "ngay.Reward";
                    String pathLore = "TichLuy." + moc + "ngay.Lore";
                    String title = "Tích Lũy " + moc + " Ngày";

                    if (event.isRightClick()) {
                        openLoreEditor(player, pathLore, title, "TICHLUY");
                    } else {
                        openCommandEditor(player, pathReward, title, "TICHLUY");
                    }
                }
            }
            case SPECIAL -> {
                if (slot == 53) {
                    openMainMenu(player);
                } else if (slot == 48) {
                    player.closeInventory();
                    activeSessions.put(player.getUniqueId(), new EditSession(null, EditType.CREATE_SPECIAL_DAY, "SPECIAL", "Ngày Lễ"));
                    sendMessage(player, "CreateSpecialPrompt", null);
                } else if (slot < 45) {
                    ConfigurationSection specialSec = plugin.getConfig().getConfigurationSection("SpecialDay");
                    if (specialSec != null) {
                        List<String> keys = new ArrayList<>(specialSec.getKeys(false));
                        if (slot < keys.size()) {
                            String keyName = keys.get(slot);
                            if (event.isRightClick()) {
                                plugin.getConfig().set("SpecialDay." + keyName, null);
                                plugin.saveConfig();
                                Map<String, String> rep = new HashMap<>();
                                rep.put("<key>", keyName);
                                sendMessage(player, "DeleteSpecialSuccess", rep);
                                openSpecialDayEditor(player);
                            } else {
                                openCommandEditor(player, "SpecialDay." + keyName + ".Reward", "Lễ " + keyName, "SPECIAL");
                            }
                        }
                    }
                }
            }
            case COMMAND -> {
                String extra = holder.getExtraData();
                String[] parts = extra.split("::");
                String configPath = parts.length > 0 ? parts[0] : "";
                String returnMenu = parts.length > 1 ? parts[1] : "MAIN";
                String title = parts.length > 2 ? parts[2] : "";

                if (slot == 53) {
                    if (returnMenu.equals("DAYS")) openDaysEditor(player);
                    else if (returnMenu.equals("TICHLUY")) openTichLuyEditor(player);
                    else if (returnMenu.equals("SPECIAL")) openSpecialDayEditor(player);
                    else openMainMenu(player);
                } else if (slot == 50 && returnMenu.equals("DAYS") && title.startsWith("Ngày ")) {
                    String day = title.replace("Ngày ", "");
                    openLoreEditor(player, "Days." + day + ".Lore", title, "DAYS");
                } else if (slot == 50 && returnMenu.equals("TICHLUY") && title.startsWith("Tích Lũy ")) {
                    String moc = title.replace("Tích Lũy ", "").replace(" Ngày", "");
                    openLoreEditor(player, "TichLuy." + moc + "ngay.Lore", title, "TICHLUY");
                } else if (slot == 51 && returnMenu.equals("SPECIAL") && title.startsWith("Lễ ")) {
                    String key = title.replace("Lễ ", "");
                    openLoreEditor(player, "SpecialDay." + key + ".Lore", title, "SPECIAL");
                } else if (slot == 49) {
                    player.closeInventory();
                    activeSessions.put(player.getUniqueId(), new EditSession(configPath, EditType.ADD_COMMAND, returnMenu, title));
                    sendMessage(player, "AddCommandPrompt", null);
                } else if (slot == 50 && returnMenu.equals("SPECIAL") && title.startsWith("Lễ ")) {
                    String key = title.replace("Lễ ", "").trim();
                    player.closeInventory();
                    activeSessions.put(player.getUniqueId(), new EditSession(configPath, EditType.CHANGE_DISPLAY_NAME, "SPECIAL", title, -1, key));
                    sendMessage(player, "ChangeNamePrompt", null);
                } else if (slot == 52 && returnMenu.equals("SPECIAL") && title.startsWith("Lễ ")) {
                    String key = title.replace("Lễ ", "").trim();
                    plugin.getConfig().set("SpecialDay." + key, null);
                    plugin.saveConfig();
                    Map<String, String> rep = new HashMap<>();
                    rep.put("<key>", key);
                    sendMessage(player, "DeleteSpecialSuccess", rep);
                    openSpecialDayEditor(player);
                } else if (slot < 45) {
                    List<String> cmds = plugin.getConfig().getStringList(configPath);
                    if (slot < cmds.size()) {
                        String removed = cmds.remove(slot);
                        plugin.getConfig().set(configPath, cmds);
                        plugin.saveConfig();
                        Map<String, String> rep = new HashMap<>();
                        rep.put("<command>", removed);
                        sendMessage(player, "DeleteCommandSuccess", rep);
                        openCommandEditor(player, configPath, title, returnMenu);
                    }
                }
            }
            case LORE -> {
                String extra = holder.getExtraData();
                String[] parts = extra.split("::");
                String configPath = parts.length > 0 ? parts[0] : "";
                String returnMenu = parts.length > 1 ? parts[1] : "DAYS";
                String title = parts.length > 2 ? parts[2] : "";

                if (slot == 53) {
                    if (returnMenu.equals("DAYS")) openDaysEditor(player);
                    else if (returnMenu.equals("TICHLUY")) openTichLuyEditor(player);
                    else if (returnMenu.equals("SPECIAL")) openSpecialDayEditor(player);
                    else openMainMenu(player);
                } else if (slot == 48) {
                    // Chuyển sang sửa Lệnh Thưởng
                    if (returnMenu.equals("DAYS") && title.startsWith("Ngày ")) {
                        String day = title.replace("Ngày ", "");
                        openCommandEditor(player, "Days." + day + ".Reward", title, "DAYS");
                    } else if (returnMenu.equals("TICHLUY") && title.startsWith("Tích Lũy ")) {
                        String moc = title.replace("Tích Lũy ", "").replace(" Ngày", "");
                        openCommandEditor(player, "TichLuy." + moc + "ngay.Reward", title, "TICHLUY");
                    } else if (returnMenu.equals("SPECIAL") && title.startsWith("Lễ ")) {
                        String key = title.replace("Lễ ", "");
                        openCommandEditor(player, "SpecialDay." + key + ".Reward", title, "SPECIAL");
                    }
                } else if (slot == 49) {
                    // Thêm dòng Lore mới
                    player.closeInventory();
                    activeSessions.put(player.getUniqueId(), new EditSession(configPath, EditType.ADD_LORE_LINE, returnMenu, title));
                    sendMessage(player, "AddLorePrompt", null);
                } else if (slot == 52 && returnMenu.equals("SPECIAL") && title.startsWith("Lễ ")) {
                    String key = title.replace("Lễ ", "").trim();
                    plugin.getConfig().set("SpecialDay." + key, null);
                    plugin.saveConfig();
                    Map<String, String> rep = new HashMap<>();
                    rep.put("<key>", key);
                    sendMessage(player, "DeleteSpecialSuccess", rep);
                    openSpecialDayEditor(player);
                } else if (slot < 45) {
                    List<String> lores = getLoreFromConfig(configPath);
                    if (slot < lores.size()) {
                        if (event.isRightClick()) {
                            // Click Phải: XÓA dòng này
                            String removed = lores.remove(slot);
                            saveLoreToConfig(configPath, lores);
                            Map<String, String> rep = new HashMap<>();
                            rep.put("<lore>", removed);
                            sendMessage(player, "DeleteLoreSuccess", rep);
                            openLoreEditor(player, configPath, title, returnMenu);
                        } else {
                            // Click Trái: SỬA dòng này
                            String currentLine = lores.get(slot);
                            player.closeInventory();
                            activeSessions.put(player.getUniqueId(), new EditSession(configPath, EditType.EDIT_LORE_LINE, returnMenu, title, slot, currentLine));
                            Map<String, String> rep = new HashMap<>();
                            rep.put("<index>", String.valueOf(slot + 1));
                            rep.put("<old_lore>", currentLine);
                            sendMessage(player, "EditLorePrompt", rep);
                        }
                    }
                }
            }
        }
    }

    // --- XỬ LÝ NHẬP CHAT AN TOÀN LUỒNG (THREAD-SAFE) ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        EditSession session = activeSessions.remove(player.getUniqueId());
        String msg = event.getMessage().trim();

        // Đẩy toàn bộ tác vụ sửa config và lưu đĩa về Main Thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("cancel")) {
                sendMessage(player, "Cancel", null);
                reopenMenu(player, session);
                return;
            }

            if (session.editType == EditType.ADD_COMMAND) {
                List<String> cmds = plugin.getConfig().getStringList(session.configPath);
                cmds.add(msg);
                plugin.getConfig().set(session.configPath, cmds);
                plugin.saveConfig();
                Map<String, String> rep = new HashMap<>();
                rep.put("<command>", msg);
                sendMessage(player, "AddCommandSuccess", rep);
                openCommandEditor(player, session.configPath, session.title, session.returnMenu);
            } else if (session.editType == EditType.ADD_LORE_LINE) {
                List<String> lores = getLoreFromConfig(session.configPath);
                lores.add(msg);
                saveLoreToConfig(session.configPath, lores);
                Map<String, String> rep = new HashMap<>();
                rep.put("<lore>", msg);
                sendMessage(player, "AddLoreSuccess", rep);
                openLoreEditor(player, session.configPath, session.title, session.returnMenu);
            } else if (session.editType == EditType.EDIT_LORE_LINE) {
                List<String> lores = getLoreFromConfig(session.configPath);
                if (session.lineIndex >= 0 && session.lineIndex < lores.size()) {
                    lores.set(session.lineIndex, msg);
                    saveLoreToConfig(session.configPath, lores);
                    Map<String, String> rep = new HashMap<>();
                    rep.put("<index>", String.valueOf(session.lineIndex + 1));
                    rep.put("<lore>", msg);
                    sendMessage(player, "EditLoreSuccess", rep);
                }
                openLoreEditor(player, session.configPath, session.title, session.returnMenu);
            } else if (session.editType == EditType.CHANGE_DISPLAY_NAME) {
                String key = (session.extra != null && !session.extra.isEmpty())
                        ? session.extra
                        : session.title.replace("Lễ ", "").trim();

                String namePath = "SpecialDay." + key + ".Icon.NgayDiemDanh.Name";
                plugin.getConfig().set(namePath, msg);

                // Đồng bộ tên hiển thị sang các icon trạng thái khác của ngày lễ nếu có
                ConfigurationSection iconSec = plugin.getConfig().getConfigurationSection("SpecialDay." + key + ".Icon");
                if (iconSec != null) {
                    for (String iconKey : iconSec.getKeys(false)) {
                        if (!iconKey.equals("NgayDiemDanh")) {
                            plugin.getConfig().set("SpecialDay." + key + ".Icon." + iconKey + ".Name", msg);
                        }
                    }
                }

                plugin.saveConfig();
                Map<String, String> rep = new HashMap<>();
                rep.put("<name>", msg);
                sendMessage(player, "ChangeNameSuccess", rep);

                // Mở lại đúng Command Editor với path lệnh thưởng là SpecialDay.<key>.Reward
                String rewardPath = (session.configPath != null && session.configPath.endsWith(".Reward"))
                        ? session.configPath
                        : "SpecialDay." + key + ".Reward";
                openCommandEditor(player, rewardPath, session.title, session.returnMenu);
            } else if (session.editType == EditType.CREATE_SPECIAL_DAY) {
                String[] args = msg.split(" ");
                if (args.length < 3) {
                    sendMessage(player, "CreateSpecialSyntaxError", null);
                    openSpecialDayEditor(player);
                    return;
                }
                try {
                    String key = args[0];
                    int date = Integer.parseInt(args[1]);
                    int month = Integer.parseInt(args[2]);

                    String path = "SpecialDay." + key;
                    plugin.getConfig().set(path + ".Require.Date", date);
                    plugin.getConfig().set(path + ".Require.Month", month);
                    plugin.getConfig().set(path + ".Icon.NgayDiemDanh.ID", "SUNFLOWER");
                    plugin.getConfig().set(path + ".Icon.NgayDiemDanh.Name", "&e" + key);
                    plugin.getConfig().set(path + ".Icon.DiemDanh.ID", "SUNFLOWER");
                    plugin.getConfig().set(path + ".Icon.DiemDanh.Name", "&a" + key + " Điểm Danh");
                    plugin.getConfig().set(path + ".Icon.DiemDanh.Glow", true);
                    plugin.getConfig().set(path + ".Icon.DaDiemDanh.ID", "SUNFLOWER");
                    plugin.getConfig().set(path + ".Icon.DaDiemDanh.Name", "&2" + key + " Đã Điểm Danh");
                    plugin.getConfig().set(path + ".Icon.ChuaDiemDanh.ID", "SUNFLOWER");
                    plugin.getConfig().set(path + ".Icon.ChuaDiemDanh.Name", "&c" + key + " Chưa Điểm Danh");
                    plugin.getConfig().set(path + ".Icon.DiemDanhBu.ID", "SUNFLOWER");
                    plugin.getConfig().set(path + ".Icon.DiemDanhBu.Name", "&b" + key + " Điểm Danh Bù");
                    plugin.getConfig().set(path + ".Reward", new ArrayList<String>());

                    plugin.saveConfig();
                    Map<String, String> rep = new HashMap<>();
                    rep.put("<key>", key);
                    rep.put("<date>", String.valueOf(date));
                    rep.put("<month>", String.valueOf(month));
                    sendMessage(player, "CreateSpecialSuccess", rep);
                } catch (NumberFormatException e) {
                    sendMessage(player, "CreateSpecialNumberError", null);
                }
                openSpecialDayEditor(player);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeSessions.remove(event.getPlayer().getUniqueId());
    }

    private void reopenMenu(Player player, EditSession session) {
        if (session.editType == EditType.ADD_LORE_LINE || session.editType == EditType.EDIT_LORE_LINE) {
            openLoreEditor(player, session.configPath, session.title, session.returnMenu);
        } else if (session.editType == EditType.CREATE_SPECIAL_DAY) {
            openSpecialDayEditor(player);
        } else if (session.returnMenu.equals("SPECIAL")) {
            if (session.configPath != null && session.configPath.startsWith("SpecialDay.")) {
                String[] parts = session.configPath.split("\\.");
                if (parts.length >= 2) {
                    String key = parts[1];
                    openCommandEditor(player, "SpecialDay." + key + ".Reward", "Lễ " + key, "SPECIAL");
                    return;
                }
            }
            openSpecialDayEditor(player);
        } else if (session.returnMenu.equals("DAYS")) {
            if (session.configPath != null && session.configPath.startsWith("Days.")) {
                String[] parts = session.configPath.split("\\.");
                if (parts.length >= 2) {
                    String day = parts[1];
                    openCommandEditor(player, "Days." + day + ".Reward", "Ngày " + day, "DAYS");
                    return;
                }
            }
            openDaysEditor(player);
        } else if (session.returnMenu.equals("TICHLUY")) {
            if (session.configPath != null && session.configPath.startsWith("TichLuy.")) {
                openCommandEditor(player, session.configPath, session.title, "TICHLUY");
                return;
            }
            openTichLuyEditor(player);
        } else {
            openMainMenu(player);
        }
    }
}
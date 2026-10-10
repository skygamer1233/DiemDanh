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
import java.util.*;

public class DiemDanhEditor implements Listener {
    private final DiemDanh plugin;
    private FileConfiguration editorConfig;

    public enum EditType {
        ADD_COMMAND,
        CREATE_SPECIAL_DAY,
        CHANGE_DISPLAY_NAME
    }

    public static class EditSession {
        public String configPath;
        public EditType editType;
        public String returnMenu;
        public String title;

        public EditSession(String configPath, EditType editType, String returnMenu, String title) {
            this.configPath = configPath;
            this.editType = editType;
            this.returnMenu = returnMenu;
            this.title = title;
        }
    }

    private final Map<UUID, EditSession> activeSessions = new HashMap<>();

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
    }

    public FileConfiguration getEditorConfig() {
        if (editorConfig == null) {
            loadEditorConfig();
        }
        return editorConfig;
    }

    private ItemStack createConfigItem(String path, Map<String, String> replacements, List<String> rewardList) {
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

        gui.setItem(11, createConfigItem("Icons.MainMenu.Days", null, null));
        gui.setItem(13, createConfigItem("Icons.MainMenu.TichLuy", null, null));
        gui.setItem(15, createConfigItem("Icons.MainMenu.SpecialDay", null, null));

        player.openInventory(gui);
    }

    // --- 2. MENU EDIT NGÀY THƯỜNG ---
    public void openDaysEditor(Player player) {
        String title = ColorUtil.translate(getEditorConfig().getString("Title.DaysMenu", "&8[&cDiemDanh Editor&8] &0Ngày Thường"));
        Inventory gui = Bukkit.createInventory(new DiemDanhEditorHolder(DiemDanhEditorHolder.MenuType.DAYS), 45, title);

        for (int day = 1; day <= 31; day++) {
            List<String> rewards = plugin.getConfig().getStringList("Days." + day + ".Reward");
            Map<String, String> rep = new HashMap<>();
            rep.put("<day>", String.valueOf(day));

            gui.setItem(day - 1, createConfigItem("Icons.DaysMenu.DayItem", rep, rewards));
        }

        gui.setItem(44, createConfigItem("Icons.DaysMenu.Back", null, null));
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
            Map<String, String> rep = new HashMap<>();
            rep.put("<moc>", String.valueOf(moc));

            gui.setItem(slots[i], createConfigItem("Icons.TichLuyMenu.Item", rep, rewards));
        }

        gui.setItem(26, createConfigItem("Icons.TichLuyMenu.Back", null, null));
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

                Map<String, String> rep = new HashMap<>();
                rep.put("<key>", key);
                rep.put("<display_name>", displayName);
                rep.put("<date>", String.valueOf(date));
                rep.put("<month>", String.valueOf(month));

                gui.setItem(slot, createConfigItem("Icons.SpecialDayMenu.SpecialItem", rep, rewards));
                slot++;
            }
        }

        gui.setItem(48, createConfigItem("Icons.SpecialDayMenu.CreateNew", null, null));
        gui.setItem(53, createConfigItem("Icons.SpecialDayMenu.Back", null, null));

        player.openInventory(gui);
    }

    // --- 5. MENU DANH SÁCH LỆNH ---
    public void openCommandEditor(Player player, String configPath, String title, String returnMenu) {
        String rawTitleFormat = getEditorConfig().getString("Title.CommandEditor", "&8[Editor] &0<title>");
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

            gui.setItem(i, createConfigItem("Icons.CommandEditor.CmdItem", rep, null));
        }

        gui.setItem(49, createConfigItem("Icons.CommandEditor.AddCmd", null, null));

        if (returnMenu.equals("SPECIAL") && title.startsWith("Lễ ")) {
            String key = title.replace("Lễ ", "");
            String currentDisplayName = plugin.getConfig().getString("SpecialDay." + key + ".Icon.NgayDiemDanh.Name", "&e" + key);
            Map<String, String> rep = new HashMap<>();
            rep.put("<display_name>", currentDisplayName);

            gui.setItem(50, createConfigItem("Icons.CommandEditor.ChangeNameTag", rep, null));
        }

        gui.setItem(53, createConfigItem("Icons.CommandEditor.Back", null, null));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DiemDanhEditorHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof DiemDanhEditorHolder holder)) return;

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
                    openCommandEditor(player, "Days." + day + ".Reward", "Ngày " + day, "DAYS");
                }
            }
            case TICHLUY -> {
                if (slot == 26) {
                    openMainMenu(player);
                } else if (slot == 11) openCommandEditor(player, "TichLuy.7ngay.Reward", "Tích Lũy 7 Ngày", "TICHLUY");
                else if (slot == 13) openCommandEditor(player, "TichLuy.14ngay.Reward", "Tích Lũy 14 Ngày", "TICHLUY");
                else if (slot == 15) openCommandEditor(player, "TichLuy.21ngay.Reward", "Tích Lũy 21 Ngày", "TICHLUY");
            }
            case SPECIAL -> {
                if (slot == 53) {
                    openMainMenu(player);
                } else if (slot == 48) {
                    player.closeInventory();
                    activeSessions.put(player.getUniqueId(), new EditSession(null, EditType.CREATE_SPECIAL_DAY, "SPECIAL", "Ngày Lễ"));
                    sendMessage(player, "CreateSpecialPrompt", null);
                } else if (slot < 45) {
                    ItemStack item = event.getCurrentItem();
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        String rawDisplayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                        String keyName = rawDisplayName.replace("Key: ", "").trim();
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
                } else if (slot == 49) {
                    player.closeInventory();
                    activeSessions.put(player.getUniqueId(), new EditSession(configPath, EditType.ADD_COMMAND, returnMenu, title));
                    sendMessage(player, "AddCommandPrompt", null);
                } else if (slot == 50 && returnMenu.equals("SPECIAL") && title.startsWith("Lễ ")) {
                    String key = title.replace("Lễ ", "");
                    player.closeInventory();
                    activeSessions.put(player.getUniqueId(), new EditSession("SpecialDay." + key + ".Icon.NgayDiemDanh.Name", EditType.CHANGE_DISPLAY_NAME, "SPECIAL", title));
                    sendMessage(player, "ChangeNamePrompt", null);
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
                reopenMenu(player, session);
            } else if (session.editType == EditType.CHANGE_DISPLAY_NAME) {
                plugin.getConfig().set(session.configPath, msg);
                plugin.saveConfig();
                Map<String, String> rep = new HashMap<>();
                rep.put("<name>", msg);
                sendMessage(player, "ChangeNameSuccess", rep);
                reopenMenu(player, session);
            } else if (session.editType == EditType.CREATE_SPECIAL_DAY) {
                String[] args = msg.split(" ");
                if (args.length < 3) {
                    sendMessage(player, "CreateSpecialSyntaxError", null);
                    reopenMenu(player, session);
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
                reopenMenu(player, session);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeSessions.remove(event.getPlayer().getUniqueId());
    }

    private void reopenMenu(Player player, EditSession session) {
        if (session.editType == EditType.CREATE_SPECIAL_DAY) {
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
package me.diemdanh;

import me.diemdanh.color;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DiemDanh extends JavaPlugin implements Listener {

    private FileConfiguration playerData;
    private File playerDataFile;

    private String diemDanhMessage;
    private String daDiemDanhMessage;
    private String ngayDiemDanhMessage;
    private String chuaDiemDanhMessage;
    private String notRequireMessage;
    private String notPlayerMessage;
    private String noPermissionMessage;
    private String isClaimedMessage;
    private String claimingMessage;
    private String reloadMessage;
    private String syntaxErrorMessage;
    private String guiTitle; 



    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        diemDanhMessage = color.transalate(getConfig().getString("Message.DiemDanh", "&aBạn đã điểm danh thành công ngày %day%!"));
        daDiemDanhMessage = color.transalate(getConfig().getString("Message.DaDiemDanh", "&cBạn đã điểm danh ngày này rồi!"));
        ngayDiemDanhMessage = color.transalate(getConfig().getString("Message.NgayDiemDanh", "&cNgày %day% chưa tới!"));
        chuaDiemDanhMessage = color.transalate(getConfig().getString("Message.ChuaDiemDanh", "&cBạn đã bỏ lỡ ngày %day%!"));
        notRequireMessage = color.transalate(getConfig().getString("Message.NotRequire", "&cBạn không đủ số ngày yêu cầu!"));
        notPlayerMessage = color.transalate(getConfig().getString("Message.NotPlayer", "&cBạn không phải là người chơi!"));
        noPermissionMessage = color.transalate(getConfig().getString("Message.NoPermission", "&cBạn không có quyền để sử dụng lệnh này!"));
        isClaimedMessage = color.transalate(getConfig().getString("Message.IsClaimed", "&cBạn đã nhận quà này rồi!"));
        claimingMessage = color.transalate(getConfig().getString("Message.Claiming", "&aHôm nay bạn chưa điểm danh, bấm /diemdanh để điểm danh"));
        syntaxErrorMessage = color.transalate(getConfig().getString("Message.SyntaxError", "&cLỗi cú pháp"));
        guiTitle = color.transalate(getConfig().getString("Title", "&a&lĐiểm Danh Tháng "));
        reloadMessage = color.transalate(getConfig().getString("Message.Reload", "&aNạp lại config thành công!"));

        playerDataFile = new File(getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            saveResource("playerdata.yml", false);
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("diemdanh")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(notPlayerMessage);
                return true;
            }
            Player player = (Player) sender;

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("diemdanh.reload")) {
                    player.sendMessage(noPermissionMessage);
                    return true;
                }
                for (String playerUUID : playerData.getKeys(false)) {
                    updatePlayerCheckInData(playerUUID);
                }

                
                File configFile = new File(getDataFolder(), "config.yml");
                if (!configFile.exists()) {
                    saveDefaultConfig();
                    configFile = new File(getDataFolder(), "config.yml");
                }
                try {
                    getConfig().load(configFile); // Tải lại config từ file
                } catch (IOException | InvalidConfigurationException e) {
                    player.sendMessage(color.transalate("&cLỗi khi tải lại config!"));
                    e.printStackTrace();
                    return true;
                }

                
                diemDanhMessage = color.transalate(getConfig().getString("Message.DiemDanh", "&aBạn đã điểm danh thành công ngày %day%!"));
                daDiemDanhMessage = color.transalate(getConfig().getString("Message.DaDiemDanh", "&cBạn đã điểm danh ngày này rồi!"));
                ngayDiemDanhMessage = color.transalate(getConfig().getString("Message.NgayDiemDanh", "&cNgày %day% chưa tới!"));
                chuaDiemDanhMessage = color.transalate(getConfig().getString("Message.ChuaDiemDanh", "&cBạn đã bỏ lỡ ngày %day%!"));
                notRequireMessage = color.transalate(getConfig().getString("Message.NotRequire", "&cBạn không đủ số ngày yêu cầu!"));
                notPlayerMessage = color.transalate(getConfig().getString("Message.NotPlayer", "&cBạn không phải là người chơi!"));
                noPermissionMessage = color.transalate(getConfig().getString("Message.NoPermission", "&cBạn không có quyền để sử dụng lệnh này!"));
                isClaimedMessage = color.transalate(getConfig().getString("Message.IsClaimed", "&cBạn đã nhận quà này rồi!"));
                claimingMessage = color.transalate(getConfig().getString("Message.Claiming", "&aHôm nay bạn chưa điểm danh, bấm /diemdanh để điểm danh"));
                syntaxErrorMessage = color.transalate(getConfig().getString("Message.SyntaxError", "&cLỗi cú pháp"));
                guiTitle = color.transalate(getConfig().getString("Title", "&a&lĐiểm Danh Tháng <month>"));
                reloadMessage = color.transalate(getConfig().getString("Message.Reload", "&aNạp lại config thành công!"));

                
                guiTitle = color.transalate(getConfig().getString("Title", "&a&lĐiểm Danh"));

                player.sendMessage(reloadMessage);
                return true;
            } else if (args.length == 0) { 
                openDiemDanhGUI(player);
                return true;
            } else {
                player.sendMessage(syntaxErrorMessage); 
                return true;
            }
        }
        return false;
    }
    private void updatePlayerCheckInData(String playerUUID) {
        LocalDate lastCheckInDate = LocalDate.parse(playerData.getString(playerUUID + ".lastCheckIn", "1970-01-01"));
        LocalDate today = LocalDate.now();

       
        for (String specialDayKey : getConfig().getConfigurationSection("SpecialDay").getKeys(false)) {
            ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey);
            int specialDayDate = specialDaySection.getInt("Require.Date");
            int specialDayMonth = specialDaySection.getInt("Require.Month");

            
            if (today.isAfter(LocalDate.of(today.getYear(), specialDayMonth, specialDayDate)) &&
                    !hasPlayerCheckedInSpecialDay(playerUUID, specialDayKey)) {
                markPlayerMissedCheckIn(playerUUID, specialDayDate); 
            }
        }

        
        for (int day = 1; day < today.getDayOfMonth(); day++) {
            if (!hasPlayerCheckedInToday(playerUUID, day)) {
                markPlayerMissedCheckIn(playerUUID, day); 
            }
        }
    }

    private boolean hasPlayerCheckedInSpecialDay(String playerUUID, String specialDayKey) {
        return playerData.getBoolean(playerUUID + ".specialDays." + specialDayKey, false);
    }



    private void openDiemDanhGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, guiTitle);
        LocalDate today = LocalDate.now();
        String playerUUID = player.getUniqueId().toString();

        ConfigurationSection daysSection = getConfig().getConfigurationSection("Days");
        if (daysSection == null) {
            getLogger().severe("Missing 'Days' section in config.yml");
            return;
        }
        List<?> dayEntries = new ArrayList<>(daysSection.getValues(false).values());

        // Tạo các ô ngày lễ
        for (String specialDayKey : getConfig().getConfigurationSection("SpecialDay").getKeys(false)) {
            ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey);
            int specialDayDate = specialDaySection.getInt("Require.Date"); 
            int specialDayMonth = specialDaySection.getInt("Require.Month");
            if (today.getMonthValue() == specialDayMonth) {
                String itemKey = getSpecialDayItemKey(playerUUID, specialDayKey);
                ConfigurationSection itemSection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Icon." + itemKey);

                List<String> lore = new ArrayList<>(); 
                if (itemSection != null && itemSection.contains("Lore")) {
                    lore = itemSection.getStringList("Lore");
                }

                
                List<String> translatedLore = new ArrayList<>();
                for (String line : lore) {
                    translatedLore.add(color.transalate(line));
                }

                Material material = Material.getMaterial(itemSection.getString("ID"));
                if (material == null) {
                    getLogger().warning("Invalid Material for special day: " + specialDayKey);
                    material = Material.BARRIER;
                }

                short data = (short) itemSection.getInt("Data");
                String name = color.transalate(itemSection.getString("Name").replace("<date>", String.valueOf(specialDayDate)));
                boolean glow = itemSection.getBoolean("Glow");

                ItemStack item = new ItemStack(material, 1, data);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(name);
                meta.setLore(translatedLore);
                if (glow) {
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);
                gui.setItem(specialDayDate - 1, item); 
            }
        }

        
        for (int day = 1; day <= 31; day++) {
            
            if (gui.getItem(day - 1) != null) {
                continue; 
            }

            String itemKey = getItemKeyForDay(day, playerUUID);
            ConfigurationSection itemSection = getConfig().getConfigurationSection("Item." + itemKey);

            
            List<String> lore = new ArrayList<>();
            if (day - 1 < dayEntries.size()) {
                Object dayEntry = dayEntries.get(day - 1);
                if (dayEntry instanceof ConfigurationSection) {
                    ConfigurationSection daySection = (ConfigurationSection) dayEntry;
                    if (daySection.contains("Lore")) {
                        List<String> rawLore = daySection.getStringList("Lore");
                        for (String line : rawLore) {
                            lore.add(color.transalate(line));
                        }
                    }
                }
            }

            
            if (lore.isEmpty()) {
                List<String> rawLore = itemSection.getStringList("Lore");
                for (String line : rawLore) {
                    lore.add(color.transalate(line));
                }
            }

            
            Material material = Material.getMaterial(itemSection.getString("ID"));
            if (material == null) {
                getLogger().warning("Invalid Material: " + itemSection.getString("ID"));
                material = Material.BARRIER;
            }

            short data = (short) itemSection.getInt("Data");
            String name = color.transalate(itemSection.getString("Name").replace("<date>", String.valueOf(day)));
            boolean glow = itemSection.getBoolean("Glow");

            ItemStack item = new ItemStack(material, 1, data);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(lore);
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

            Material material = Material.getMaterial(itemSection.getString("ID"));
            if (material == null) {
                getLogger().warning("Invalid Material: " + itemSection.getString("ID"));
                material = Material.BARRIER;
            }

            short data = (short) itemSection.getInt("Data");
            String name = color.transalate(itemSection.getString("Name"));
            boolean glow = itemSection.getBoolean("Glow");

            List<String> rawLore = itemSection.getStringList("Lore");
            List<String> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(color.transalate(line));
            }

            ItemStack item = new ItemStack(material, 1, data);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(lore);
            if (glow) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
            gui.setItem(36 + i, item); 
        }

        player.openInventory(gui);
    }











    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(guiTitle)) return; // Kiểm tra title GUI
        if (!(event.getWhoClicked() instanceof Player)) {
            event.getWhoClicked().sendMessage(notPlayerMessage);
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

        
        if (slot < 31) {
            int day = slot + 1;

            
            String originalItemKey = getItemKeyForDay(day, playerUUID);
            String specialDayKey = getSpecialDayKey(today, day);

            
            if (specialDayKey != null) {
                originalItemKey = getSpecialDayItemKey(playerUUID, specialDayKey);
            }

            
            if (originalItemKey.equals("DiemDanh")) {
                ConfigurationSection rewardSection;
                if (specialDayKey != null) { 
                    rewardSection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey);
                } else { 
                    rewardSection = getConfig().getConfigurationSection("Days." + day);
                }

                if (rewardSection != null) {
                    List<String> commands = rewardSection.getStringList("Reward");
                    for (String command : commands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
                    }
                }

                
                markPlayerCheckedIn(playerUUID, day);

                
                if (specialDayKey != null) {
                    player.sendMessage(color.transalate(getConfig().getString("Message.SpecialDaySuccess", "&aBạn đã điểm danh thành công ngày lễ %specialDay%!").replace("%specialDay%", specialDayKey)));
                } else {
                    player.sendMessage(diemDanhMessage.replace("%day%", String.valueOf(day)));
                }
            } else if (originalItemKey.equals("ChuaDiemDanh")) {
                if (isToday(day)) {
                    player.sendMessage(claimingMessage);
                } else {
                    player.sendMessage(chuaDiemDanhMessage.replace("%day%", String.valueOf(day)));
                }
            } else if (originalItemKey.equals("DaDiemDanh")) { 
                player.sendMessage(daDiemDanhMessage); 
            } else {
                if (specialDayKey != null) { 
                    String specialDayName = getConfig().getString("SpecialDay." + specialDayKey + ".Icon.NgayDiemDanh.Name", specialDayKey);
                    player.sendMessage(ngayDiemDanhMessage.replace("%day%", specialDayName));
                } else {
                    player.sendMessage(ngayDiemDanhMessage.replace("%day%", String.valueOf(day)));
                }
            }
        } else if (slot >= 36 && slot <= 38) { // Xử lý tích lũy (ô 36-38)
            int daysRequired = (slot - 36 + 1) * 7;

            
            String originalItemKey = getTichLuyItemKey(playerUUID, daysRequired);

            if (originalItemKey.equals("NhanQua")) {
                ConfigurationSection rewardSection = getConfig().getConfigurationSection("TichLuy." + daysRequired + "ngay");
                if (rewardSection != null) {
                    List<String> commands = rewardSection.getStringList("Reward");
                    for (String command : commands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
                    }
                    playerData.set(playerUUID + ".tichluy." + daysRequired, true); 
                    savePlayerData();
                    player.sendMessage(color.transalate(getConfig().getString("Message.TichLuySuccess", "&aBạn đã nhận quà tích lũy %days% ngày thành công!").replace("%days%", String.valueOf(daysRequired))));
                }
            } else if (originalItemKey.equals("DaNhanQua")) {
                player.sendMessage(isClaimedMessage);
            } else {
                player.sendMessage(notRequireMessage);
            }
        }

        
        openDiemDanhGUI(player);
    }








    private String getItemKeyForDay(int day, String playerUUID) {
        LocalDate today = LocalDate.now();
        if (day > today.getDayOfMonth()) { 
            return "NgayDiemDanh"; 
        } else if (hasPlayerCheckedInToday(playerUUID, day)) {
            return "DaDiemDanh"; 
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

        for (int day = 1; day < today.getDayOfMonth(); day++) {
            if (!hasPlayerCheckedInToday(playerUUID, day)) {
                markPlayerMissedCheckIn(playerUUID, day);
            }
        }
    }
    private boolean isToday(int day) {
        LocalDate today = LocalDate.now();
        return day == today.getDayOfMonth();
    }

    private boolean hasPlayerCheckedInToday(String playerUUID, int day) {
        LocalDate lastCheckInDate = LocalDate.parse(playerData.getString(playerUUID + ".lastCheckIn", "1970-01-01"));
        return lastCheckInDate.getYear() == LocalDate.now().getYear() &&
                lastCheckInDate.getMonth() == LocalDate.now().getMonth() &&
                lastCheckInDate.getDayOfMonth() == day;
    }

    private void markPlayerCheckedIn(String playerUUID, int day) {
        playerData.set(playerUUID + ".lastCheckIn", LocalDate.now().toString());

        
        int currentMonth = LocalDate.now().getMonthValue();
        int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
        int daysCheckedIn = playerData.getInt(playerUUID + ".daysCheckedIn", 0);

        if (currentMonth != lastCheckInMonth) {
            
            daysCheckedIn = 0;
        }

        daysCheckedIn++;
        playerData.set(playerUUID + ".daysCheckedIn", daysCheckedIn);
        playerData.set(playerUUID + ".lastCheckInMonth", currentMonth);
        savePlayerData();
    }

    private void markPlayerMissedCheckIn(String playerUUID, int day) {
        // (Không cần làm gì ở đây, chỉ cần đánh dấu là đã bỏ lỡ điểm danh)
    }

    private void savePlayerData() {
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
    private String getMessage(String key) {
        String message = getConfig().getString("Message." + key, "&cMessage not found: " + key);
        return color.transalate(message); 
    }
    private String getSpecialDayKey(LocalDate today, int day) {
        for (String key : getConfig().getConfigurationSection("SpecialDay").getKeys(false)) {
            ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + key + ".Require"); // Thêm .Require vào đường dẫn
            int specialDayDate = specialDaySection.getInt("Date");
            int specialDayMonth = specialDaySection.getInt("Month");

            // Kiểm tra giá trị tháng có hợp lệ không
            if (specialDayMonth < 1 || specialDayMonth > 12) {
                getLogger().warning("Invalid month for special day: " + key);
                continue; // Bỏ qua ngày lễ nếu tháng không hợp lệ
            }

            if (day == specialDayDate && today.getMonthValue() == specialDayMonth) {
                return key;
            }
        }
        return null;
    }

    private String getSpecialDayItemKey(String playerUUID, String specialDayKey) {
        LocalDate lastCheckInDate = LocalDate.parse(playerData.getString(playerUUID + ".lastCheckIn", "1970-01-01"));
        ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Require");
        int specialDayDate = specialDaySection.getInt("Date");
        int specialDayMonth = specialDaySection.getInt("Month");

        
        if (specialDayMonth < 1 || specialDayMonth > 12) {
            getLogger().warning("Invalid month for special day: " + specialDayKey);
            return "NgayDiemDanh"; // Trả về "NgayDiemDanh" nếu tháng không hợp lệ
        }

        if (lastCheckInDate.getYear() == LocalDate.now().getYear() &&
                lastCheckInDate.getMonthValue() == specialDayMonth &&
                lastCheckInDate.getDayOfMonth() == specialDayDate) {
            return "DaDiemDanh";
        } else if (LocalDate.now().getDayOfMonth() == specialDayDate && LocalDate.now().getMonthValue() == specialDayMonth) {
            return "DiemDanh";
        } else if (LocalDate.now().isAfter(LocalDate.of(LocalDate.now().getYear(), specialDayMonth, specialDayDate))) {
            return "ChuaDiemDanh";
        } else {
            return "NgayDiemDanh";
        }
    }


}

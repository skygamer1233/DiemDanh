package me.diemdanh;

import com.cryptomorin.xseries.XMaterial;
import me.diemdanh.data.PlayerDataManager;
import me.diemdanh.holder.DiemDanhGUIHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class DiemDanhGUI implements Listener {
    private final DiemDanh plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public DiemDanhGUI(DiemDanh plugin) {
        this.plugin = plugin;
    }

    public void openDiemDanhGUI(Player player) {
        LocalDate today = LocalDate.now();
        UUID playerUUID = player.getUniqueId();
        String titleWithMonth = ColorUtil.translate(plugin.guiTitle.replace("<month>", String.valueOf(today.getMonthValue())));

        Inventory gui;
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof DiemDanhGUIHolder
                && player.getOpenInventory().getTopInventory().getSize() == 45) {
            gui = player.getOpenInventory().getTopInventory();
            gui.clear();
        } else {
            gui = Bukkit.createInventory(new DiemDanhGUIHolder(), 45, titleWithMonth);
        }

        ConfigurationSection daysSection = plugin.getConfig().getConfigurationSection("Days");
        if (daysSection == null) {
            plugin.getLogger().severe("Missing 'Days' section in config.yml");
            return;
        }
        List<?> dayEntries = new ArrayList<>(daysSection.getValues(false).values());

        // 1. Render Special Day
        ConfigurationSection specialSection = plugin.getConfig().getConfigurationSection("SpecialDay");
        if (specialSection != null) {
            for (String specialDayKey : specialSection.getKeys(false)) {
                ConfigurationSection specialDaySection = plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey);
                if (specialDaySection == null) continue;
                int specialDayDate = specialDaySection.getInt("Require.Date");
                int specialDayMonth = specialDaySection.getInt("Require.Month");
                if (today.getMonthValue() == specialDayMonth && specialDayDate >= 1 && specialDayDate <= 31) {
                    String itemKey = getSpecialDayItemKey(playerUUID, specialDayKey);
                    ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Icon." + itemKey);
                    if (itemSection != null) {
                        ItemStack item = createItemFromConfig(itemSection, specialDayDate, playerUUID);
                        gui.setItem(specialDayDate - 1, item);
                    }
                }
            }
        }

        // 2. Render Calendar Days động theo tháng hiện tại (28, 29, 30 hoặc 31 ngày)
        int maxDaysInMonth = YearMonth.now().lengthOfMonth();

        for (int day = 1; day <= maxDaysInMonth; day++) {
            if (gui.getItem(day - 1) != null) {
                continue;
            }

            String itemKey = getItemKeyForDay(day, playerUUID);
            ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("Item." + itemKey);

            if (itemSection == null) {
                plugin.getLogger().warning("Missing item section for key: " + itemKey);
                continue;
            }

            List<String> lore = new ArrayList<>();
            if (day - 1 < dayEntries.size()) {
                Object dayEntry = dayEntries.get(day - 1);
                if (dayEntry instanceof ConfigurationSection ds && ds.contains("Lore")) {
                    lore = ds.getStringList("Lore");
                }
            }

            if (lore.isEmpty() && !itemKey.equals("DiemDanhBu")) {
                lore = itemSection.getStringList("Lore");
            }

            List<String> translatedLore = new ArrayList<>();
            for (String line : lore) {
                translatedLore.add(ColorUtil.translate(line));
            }

            XMaterial xMaterial = XMaterial.matchXMaterial(itemSection.getString("ID", "BARRIER")).orElse(XMaterial.BARRIER);
            Material material = xMaterial.parseMaterial();
            if (material == null) material = Material.BARRIER;

            String name = ColorUtil.translate(itemSection.getString("Name", "").replace("<date>", String.valueOf(day)));
            boolean glow = itemSection.getBoolean("Glow");

            ItemStack item = new ItemStack(material, Math.min(day, 64));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(translatedLore);
                if (glow) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);
            }

            gui.setItem(day - 1, item);
        }

        // Các slot ngày vượt quá độ dài tháng (ví dụ ngày 29, 30, 31 trong tháng 2) sẽ được lấp kính trống
        for (int day = maxDaysInMonth + 1; day <= 31; day++) {
            gui.setItem(day - 1, createBlankPane());
        }

        // 3. Render Mốc Tích Lũy (Slots 36, 37, 38)
        for (int i = 0; i < 3; i++) {
            int daysRequired = (i + 1) * 7;
            String itemKey = getTichLuyItemKey(playerUUID, daysRequired);
            ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("TichLuy." + daysRequired + "ngay.Icon." + itemKey);
            if (itemSection != null) {
                ItemStack item = createItemFromConfig(itemSection, daysRequired, playerUUID);
                gui.setItem(36 + i, item);
            }
        }

        // 4. Render Ticket (Slot 34)
        ConfigurationSection ticketSection = plugin.getConfig().getConfigurationSection("Item.Ticket");
        if (ticketSection != null) {
            int tickets = plugin.getPlayerDataManager().getTickets(playerUUID);
            ItemStack ticketItem = createItemFromConfig(ticketSection, tickets, playerUUID);
            gui.setItem(34, ticketItem);
        }

        // 5. Render Thông Tin (Slot 35)
        ConfigurationSection thongTinSection = plugin.getConfig().getConfigurationSection("Item.ThongTin");
        if (thongTinSection != null) {
            int daysCheckedIn = getDaysCheckedInThisMonth(playerUUID);
            ItemStack thongTinItem = createItemFromConfig(thongTinSection, daysCheckedIn, playerUUID);
            gui.setItem(35, thongTinItem);
        }

        if (player.getOpenInventory().getTopInventory() != gui) {
            player.openInventory(gui);
        }
    }

    private ItemStack createBlankPane() {
        Material mat = XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial();
        if (mat == null) mat = Material.BARRIER;
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItemFromConfig(ConfigurationSection itemSection, int replaceValue, UUID playerUUID) {
        if (itemSection == null) return new ItemStack(Material.BARRIER);

        XMaterial xMaterial = XMaterial.matchXMaterial(itemSection.getString("ID", "BARRIER")).orElse(XMaterial.BARRIER);
        Material material = xMaterial.parseMaterial();
        if (material == null) material = Material.BARRIER;

        List<String> rawlore = itemSection.getStringList("Lore");
        List<String> translatedLore = new ArrayList<>();
        PlayerDataManager dataMgr = plugin.getPlayerDataManager();

        for (String line : rawlore) {
            line = line.replace("<days>", String.valueOf(getDaysCheckedInThisMonth(playerUUID)));
            line = line.replace("<tickets>", String.valueOf(dataMgr.getTickets(playerUUID)));
            line = line.replace("<totaldays>", String.valueOf(dataMgr.getTotalDays(playerUUID)));
            translatedLore.add(ColorUtil.translate(line));
        }

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(itemSection.getString("Name", "").replace("<date>", String.valueOf(replaceValue))));
            meta.setLore(translatedLore);
            if (itemSection.getBoolean("Glow")) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DiemDanhGUIHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof DiemDanhGUIHolder)) return;

        event.setCancelled(true);

        int slot = event.getSlot();
        if (slot < 0 || slot > 44) return;

        // Anti-Spam click cooldown (400ms)
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(player.getUniqueId()) && now - cooldowns.get(player.getUniqueId()) < 400) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now);

        LocalDate today = LocalDate.now();
        UUID playerUUID = player.getUniqueId();
        int maxDaysInMonth = YearMonth.now().lengthOfMonth();

        // 1. Click các ô ngày trong tháng (Slot 0 - 30)
        if (slot < 31) {
            int day = slot + 1;
            if (day > maxDaysInMonth) {
                return; // Ngày này không tồn tại trong tháng hiện tại
            }

            String specialDayKey = getSpecialDayKey(today, day);
            String originalItemKey = (specialDayKey != null)
                    ? getSpecialDayItemKey(playerUUID, specialDayKey)
                    : getItemKeyForDay(day, playerUUID);

            if (originalItemKey.equals("DiemDanh")) {
                ConfigurationSection rewardSection = (specialDayKey != null)
                        ? plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                        : plugin.getConfig().getConfigurationSection("Days." + day);

                if (rewardSection != null) {
                    executeCommands(rewardSection.getStringList("Reward"), player);
                }

                markPlayerCheckedIn(playerUUID, day, false);

                String messageKey = (specialDayKey != null) ? "SpecialDayDiemDanhThanhCong" : "DiemDanh";
                player.sendMessage(ColorUtil.translate(plugin.getConfig().getString("Message." + messageKey, plugin.getMessage("DiemDanh"))
                        .replace("%day%", String.valueOf(day))
                        .replace("%specialDay%", specialDayKey != null ? specialDayKey : "")));

            } else if (originalItemKey.equals("DiemDanhBu")) {
                int tickets = plugin.getPlayerDataManager().getTickets(playerUUID);
                if (tickets > 0) {
                    ConfigurationSection rewardSection = (specialDayKey != null)
                            ? plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                            : plugin.getConfig().getConfigurationSection("Days." + day);

                    if (rewardSection != null) {
                        executeCommands(rewardSection.getStringList("Reward"), player);
                    }

                    plugin.getPlayerDataManager().setTickets(playerUUID, tickets - 1);
                    List<Integer> missedDays = plugin.getPlayerDataManager().getMissedDays(playerUUID);
                    missedDays.remove(Integer.valueOf(day));
                    plugin.getPlayerDataManager().setMissedDays(playerUUID, missedDays);

                    markPlayerCheckedIn(playerUUID, day, true);

                    player.sendMessage(plugin.getMessage("DiemDanh").replace("%day%", String.valueOf(day)));
                } else {
                    player.sendMessage(plugin.getMessage("NotRequire"));
                }
            } else if (originalItemKey.equals("ChuaDiemDanh")) {
                player.sendMessage(isToday(day) ? plugin.getMessage("Claiming") : plugin.getMessage("ChuaDiemDanh").replace("%day%", String.valueOf(day)));
            } else if (specialDayKey != null && originalItemKey.equals("DaDiemDanh")) {
                player.sendMessage(ColorUtil.translate(plugin.getConfig().getString("Message.SpecialDayDaDiemDanh", "&cBạn đã điểm danh ngày lễ này rồi!")));
            } else if (originalItemKey.equals("DaDiemDanh")) {
                player.sendMessage(plugin.getMessage("DaDiemDanh"));
            } else {
                String dayName = (specialDayKey != null)
                        ? plugin.getConfig().getString("SpecialDay." + specialDayKey + ".Icon.NgayDiemDanh.Name", specialDayKey)
                        : String.valueOf(day);
                player.sendMessage(plugin.getMessage("NgayDiemDanh").replace("%day%", dayName));
            }
        }
        // 2. Click các mốc Tích Lũy (Slot 36, 37, 38)
        else if (slot >= 36 && slot <= 38) {
            int daysRequired = (slot - 36 + 1) * 7;
            String originalItemKey = getTichLuyItemKey(playerUUID, daysRequired);

            if (originalItemKey.equals("NhanQua")) {
                ConfigurationSection rewardSection = plugin.getConfig().getConfigurationSection("TichLuy." + daysRequired + "ngay");
                if (rewardSection != null) {
                    executeCommands(rewardSection.getStringList("Reward"), player);
                    plugin.getPlayerDataManager().setTichLuyClaimed(playerUUID, daysRequired, true, today.getMonthValue());
                    plugin.getPlayerDataManager().savePlayerDataAsync(playerUUID);
                    player.sendMessage(ColorUtil.translate(plugin.getConfig().getString("Message.TichLuySuccess", "&aBạn đã nhận quà tích lũy %days% ngày thành công!").replace("%days%", String.valueOf(daysRequired))));
                }
            } else if (originalItemKey.equals("DaNhanQua")) {
                player.sendMessage(plugin.getMessage("IsClaimed"));
            } else {
                player.sendMessage(plugin.getMessage("NotRequire"));
            }
        }

        // Cập nhật lại giao diện
        openDiemDanhGUI(player);
    }

    public void executeCommands(List<String> commands, Player player) {
        if (commands == null) return;
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
        }
    }

    private String getItemKeyForDay(int day, UUID playerUUID) {
        LocalDate today = LocalDate.now();
        if (day > today.getDayOfMonth()) {
            return "NgayDiemDanh";
        } else if (plugin.getPlayerDataManager().getCheckedDays(playerUUID).contains(day)) {
            return "DaDiemDanh";
        } else if (plugin.getPlayerDataManager().getTickets(playerUUID) > 0 && plugin.getPlayerDataManager().getMissedDays(playerUUID).contains(day)) {
            return "DiemDanhBu";
        } else if (day < today.getDayOfMonth()) {
            return "ChuaDiemDanh";
        } else {
            return "DiemDanh";
        }
    }

    private boolean isToday(int day) {
        return day == LocalDate.now().getDayOfMonth();
    }

    private void markPlayerCheckedIn(UUID playerUUID, int day, boolean isBu) {
        LocalDate today = LocalDate.now();
        String specialDayKey = getSpecialDayKey(today, day);
        PlayerDataManager dataMgr = plugin.getPlayerDataManager();

        if (specialDayKey != null) {
            dataMgr.setSpecialDayChecked(playerUUID, specialDayKey, true);
        } else {
            dataMgr.setLastCheckIn(playerUUID, today.toString());
            int currentMonth = today.getMonthValue();
            int lastCheckInMonth = dataMgr.getLastCheckInMonth(playerUUID);
            if (currentMonth != lastCheckInMonth) {
                dataMgr.setCheckedDays(playerUUID, new ArrayList<>());
                dataMgr.setDaysCheckedIn(playerUUID, 0);
            }
            List<Integer> checkedDays = dataMgr.getCheckedDays(playerUUID);
            if (!checkedDays.contains(day)) {
                checkedDays.add(day);
            }
            dataMgr.setCheckedDays(playerUUID, checkedDays);
        }

        int currentMonth = today.getMonthValue();
        int lastCheckInMonth = dataMgr.getLastCheckInMonth(playerUUID);
        int daysCheckedIn = dataMgr.getDaysCheckedIn(playerUUID);
        if (currentMonth != lastCheckInMonth) {
            daysCheckedIn = 0;
        }
        daysCheckedIn++;

        int totalDays = dataMgr.getTotalDays(playerUUID) + 1;
        dataMgr.setTotalDays(playerUUID, totalDays);
        dataMgr.setDaysCheckedIn(playerUUID, daysCheckedIn);
        dataMgr.setLastCheckInMonth(playerUUID, currentMonth);

        dataMgr.savePlayerDataAsync(playerUUID);
    }

    public int getDaysCheckedInThisMonth(UUID playerUUID) {
        int currentMonth = LocalDate.now().getMonthValue();
        int lastCheckInMonth = plugin.getPlayerDataManager().getLastCheckInMonth(playerUUID);
        if (currentMonth == lastCheckInMonth) {
            return plugin.getPlayerDataManager().getDaysCheckedIn(playerUUID);
        } else {
            return 0;
        }
    }

    private String getTichLuyItemKey(UUID playerUUID, int daysRequired) {
        int daysCheckedIn = getDaysCheckedInThisMonth(playerUUID);
        int currentMonth = LocalDate.now().getMonthValue();
        int claimedMonth = plugin.getPlayerDataManager().getTichLuyMonth(playerUUID, daysRequired);

        if (currentMonth != claimedMonth) {
            plugin.getPlayerDataManager().setTichLuyClaimed(playerUUID, daysRequired, false, 0);
        }

        boolean hasClaimed = plugin.getPlayerDataManager().isTichLuyClaimed(playerUUID, daysRequired);

        if (daysCheckedIn >= daysRequired && !hasClaimed) {
            return "NhanQua";
        } else if (hasClaimed) {
            return "DaNhanQua";
        } else {
            return "ChuaNhanQua";
        }
    }

    private String getSpecialDayKey(LocalDate today, int day) {
        ConfigurationSection specialSec = plugin.getConfig().getConfigurationSection("SpecialDay");
        if (specialSec == null) return null;

        for (String key : specialSec.getKeys(false)) {
            ConfigurationSection specialDaySection = plugin.getConfig().getConfigurationSection("SpecialDay." + key + ".Require");
            if (specialDaySection == null) continue;
            int specialDayDate = specialDaySection.getInt("Date");
            int specialDayMonth = specialDaySection.getInt("Month");
            if (specialDayMonth < 1 || specialDayMonth > 12) {
                continue;
            }
            if (day == specialDayDate && today.getMonthValue() == specialDayMonth) {
                return key;
            }
        }
        return null;
    }

    private String getSpecialDayItemKey(UUID playerUUID, String specialDayKey) {
        boolean hasCheckedIn = plugin.getPlayerDataManager().isSpecialDayChecked(playerUUID, specialDayKey);
        ConfigurationSection specialDaySection = plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Require");
        if (specialDaySection == null) return "ChuaDiemDanh";

        int specialDayDate = specialDaySection.getInt("Date");
        int specialDayMonth = specialDaySection.getInt("Month");

        LocalDate specialDay = LocalDate.of(LocalDate.now().getYear(), specialDayMonth, specialDayDate);

        if (hasCheckedIn) {
            return "DaDiemDanh";
        } else if (LocalDate.now().isEqual(specialDay)) {
            return "DiemDanh";
        } else if (LocalDate.now().isAfter(specialDay)) {
            if (plugin.getPlayerDataManager().getTickets(playerUUID) > 0) {
                return "DiemDanhBu";
            } else {
                return "ChuaDiemDanh";
            }
        } else {
            return "NgayDiemDanh";
        }
    }
}
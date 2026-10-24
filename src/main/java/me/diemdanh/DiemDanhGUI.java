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
import java.util.concurrent.ConcurrentHashMap;

public class DiemDanhGUI implements Listener {
    private final DiemDanh plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    // Template cache cho các Item.* tĩnh để tránh parse XMaterial và config 31 lần/click
    private static class ItemTemplate {
        final Material material;
        final String nameTemplate;
        final List<String> rawLore;
        final boolean glow;

        ItemTemplate(Material material, String nameTemplate, List<String> rawLore, boolean glow) {
            this.material = material;
            this.nameTemplate = nameTemplate != null ? nameTemplate : "";
            this.rawLore = rawLore != null ? rawLore : Collections.emptyList();
            this.glow = glow;
        }
    }

    private final Map<String, ItemTemplate> itemTemplateCache = new ConcurrentHashMap<>();

    public DiemDanhGUI(DiemDanh plugin) {
        this.plugin = plugin;
    }

    public void clearCooldown(UUID uuid) {
        if (uuid != null) {
            cooldowns.remove(uuid);
        }
    }

    public void clearTemplateCache() {
        itemTemplateCache.clear();
    }

    private ItemTemplate getItemTemplate(String itemKey) {
        return itemTemplateCache.computeIfAbsent(itemKey, key -> {
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection("Item." + key);
            if (sec == null) {
                return new ItemTemplate(Material.BARRIER, "", Collections.emptyList(), false);
            }
            XMaterial xMat = XMaterial.matchXMaterial(sec.getString("ID", "BARRIER")).orElse(XMaterial.BARRIER);
            Material mat = xMat.parseMaterial();
            if (mat == null) mat = Material.BARRIER;
            String name = sec.getString("Name", "");
            List<String> lore = sec.getStringList("Lore");
            boolean glow = sec.getBoolean("Glow", false);
            return new ItemTemplate(mat, name, lore, glow);
        });
    }

    public void openDiemDanhGUI(Player player) {
        LocalDate today = LocalDate.now();
        UUID playerUUID = player.getUniqueId();
        String titleWithMonth = ColorUtil.translate(plugin.guiTitle.replace("<month>", String.valueOf(today.getMonthValue())));

        Inventory gui;
        if (InventoryUtil.getHolder(player.getOpenInventory().getTopInventory()) instanceof DiemDanhGUIHolder
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

        // Tối ưu hóa: Đọc dữ liệu người chơi 1 lần duy nhất ở đầu hàm và tái sử dụng cho toàn bộ 45 ô
        PlayerDataManager dataMgr = plugin.getPlayerDataManager();
        int tickets = dataMgr.getTickets(playerUUID);
        int totalDays = dataMgr.getTotalDays(playerUUID);
        int daysCheckedIn = getDaysCheckedInThisMonth(playerUUID);
        Set<Integer> checkedDays = new HashSet<>(dataMgr.getCheckedDays(playerUUID));
        Set<Integer> missedDays = new HashSet<>(dataMgr.getMissedDays(playerUUID));

        String daysStr = String.valueOf(daysCheckedIn);
        String ticketsStr = String.valueOf(tickets);
        String totalDaysStr = String.valueOf(totalDays);

        // 1. Render Special Day
        ConfigurationSection specialSection = plugin.getConfig().getConfigurationSection("SpecialDay");
        if (specialSection != null) {
            for (String specialDayKey : specialSection.getKeys(false)) {
                ConfigurationSection specialDaySection = plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey);
                if (specialDaySection == null) continue;
                int specialDayDate = specialDaySection.getInt("Require.Date");
                int specialDayMonth = specialDaySection.getInt("Require.Month");
                if (today.getMonthValue() == specialDayMonth && specialDayDate >= 1 && specialDayDate <= 31) {
                    String itemKey = getSpecialDayItemKey(playerUUID, specialDayKey, tickets);
                    ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Icon." + itemKey);
                    if (itemSection == null) {
                        itemSection = plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Icon.NgayDiemDanh");
                    }
                    if (itemSection != null) {
                        ItemStack item = createItemFromConfig(itemSection, specialDayDate, daysStr, ticketsStr, totalDaysStr);
                        gui.setItem(specialDayDate - 1, item);
                    }
                }
            }
        }

        // 2. Render Calendar Days động theo tháng hiện tại (28, 29, 30 hoặc 31 ngày)
        int maxDaysInMonth = YearMonth.now().lengthOfMonth();
        int todayDay = today.getDayOfMonth();

        for (int day = 1; day <= maxDaysInMonth; day++) {
            if (gui.getItem(day - 1) != null) {
                continue;
            }

            String itemKey = getItemKeyForDayFast(day, todayDay, checkedDays, missedDays, tickets);
            ItemTemplate template = getItemTemplate(itemKey);

            List<String> lore = plugin.getConfig().getStringList("Days." + day + ".Lore");
            if (lore.isEmpty() && !itemKey.equals("DiemDanhBu")) {
                lore = template.rawLore;
            }

            List<String> translatedLore = new ArrayList<>(lore.size());
            for (String line : lore) {
                line = line.replace("<days>", daysStr)
                           .replace("<tickets>", ticketsStr)
                           .replace("<totaldays>", totalDaysStr);
                translatedLore.add(ColorUtil.translate(line));
            }

            String name = ColorUtil.translate(template.nameTemplate.replace("<date>", String.valueOf(day)));

            ItemStack item = new ItemStack(template.material, Math.min(day, 64));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(translatedLore);
                if (template.glow) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);
            }

            gui.setItem(day - 1, item);
        }

        // Các slot ngày vượt quá độ dài tháng sẽ được lấp kính trống
        for (int day = maxDaysInMonth + 1; day <= 31; day++) {
            gui.setItem(day - 1, createBlankPane());
        }

        // 3. Render Mốc Tích Lũy (Slots 36, 37, 38)
        for (int i = 0; i < 3; i++) {
            int daysRequired = (i + 1) * 7;
            String itemKey = getTichLuyItemKeyFast(daysCheckedIn, playerUUID, daysRequired);
            ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("TichLuy." + daysRequired + "ngay.Icon." + itemKey);
            if (itemSection != null) {
                ItemStack item = createItemFromConfig(itemSection, daysRequired, daysStr, ticketsStr, totalDaysStr);
                gui.setItem(36 + i, item);
            }
        }

        // 4. Render Ticket (Slot 34)
        ConfigurationSection ticketSection = plugin.getConfig().getConfigurationSection("Item.Ticket");
        if (ticketSection != null) {
            ItemStack ticketItem = createItemFromConfig(ticketSection, tickets, daysStr, ticketsStr, totalDaysStr);
            gui.setItem(34, ticketItem);
        }

        // 5. Render Thông Tin (Slot 35)
        ConfigurationSection thongTinSection = plugin.getConfig().getConfigurationSection("Item.ThongTin");
        if (thongTinSection != null) {
            ItemStack thongTinItem = createItemFromConfig(thongTinSection, daysCheckedIn, daysStr, ticketsStr, totalDaysStr);
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

    private ItemStack createItemFromConfig(ConfigurationSection itemSection, int replaceValue, String daysStr, String ticketsStr, String totalDaysStr) {
        if (itemSection == null) return new ItemStack(Material.BARRIER);

        XMaterial xMaterial = XMaterial.matchXMaterial(itemSection.getString("ID", "BARRIER")).orElse(XMaterial.BARRIER);
        Material material = xMaterial.parseMaterial();
        if (material == null) material = Material.BARRIER;

        List<String> rawlore = itemSection.getStringList("Lore");
        List<String> translatedLore = new ArrayList<>(rawlore.size());

        for (String line : rawlore) {
            line = line.replace("<days>", daysStr)
                       .replace("<tickets>", ticketsStr)
                       .replace("<totaldays>", totalDaysStr);
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
        if (InventoryUtil.getHolder(event.getInventory()) instanceof DiemDanhGUIHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(InventoryUtil.getHolder(event.getInventory()) instanceof DiemDanhGUIHolder)) return;

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

        boolean updated = false;

        // 1. Click các ô ngày trong tháng (Slot 0 - 30)
        if (slot < 31) {
            int day = slot + 1;
            if (day > maxDaysInMonth) {
                return;
            }

            String specialDayKey = getSpecialDayKey(today, day);
            String originalItemKey = (specialDayKey != null)
                    ? getSpecialDayItemKey(playerUUID, specialDayKey, plugin.getPlayerDataManager().getTickets(playerUUID))
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
                updated = true;

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
                    updated = true;
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
                    updated = true;
                }
            } else if (originalItemKey.equals("DaNhanQua")) {
                player.sendMessage(plugin.getMessage("IsClaimed"));
            } else {
                player.sendMessage(plugin.getMessage("NotRequire"));
            }
        }

        // Tối ưu: Chỉ dựng lại toàn bộ giao diện khi trạng thái thực sự thay đổi!
        if (updated) {
            openDiemDanhGUI(player);
        }
    }

    public void executeCommands(List<String> commands, Player player) {
        if (commands == null) return;
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
        }
    }

    public String getItemKeyForDayFast(int day, int todayDay, Set<Integer> checkedDays, Set<Integer> missedDays, int tickets) {
        if (day > todayDay) {
            return "NgayDiemDanh";
        } else if (checkedDays.contains(day)) {
            return "DaDiemDanh";
        } else if (tickets > 0 && missedDays.contains(day)) {
            return "DiemDanhBu";
        } else if (day < todayDay) {
            return "ChuaDiemDanh";
        } else {
            return "DiemDanh";
        }
    }

    public String getItemKeyForDay(int day, UUID playerUUID) {
        LocalDate today = LocalDate.now();
        Set<Integer> checked = new HashSet<>(plugin.getPlayerDataManager().getCheckedDays(playerUUID));
        Set<Integer> missed = new HashSet<>(plugin.getPlayerDataManager().getMissedDays(playerUUID));
        int tickets = plugin.getPlayerDataManager().getTickets(playerUUID);
        return getItemKeyForDayFast(day, today.getDayOfMonth(), checked, missed, tickets);
    }

    public boolean isToday(int day) {
        return day == LocalDate.now().getDayOfMonth();
    }

    public void markPlayerCheckedIn(UUID playerUUID, int day, boolean isBu) {
        LocalDate today = LocalDate.now();
        String specialDayKey = getSpecialDayKey(today, day);
        PlayerDataManager dataMgr = plugin.getPlayerDataManager();

        if (specialDayKey != null) {
            dataMgr.setSpecialDayChecked(playerUUID, specialDayKey, true);
        }

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

        int daysCheckedIn = dataMgr.getDaysCheckedIn(playerUUID) + 1;
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

    public String getTichLuyItemKeyFast(int daysCheckedIn, UUID playerUUID, int daysRequired) {
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

    public String getTichLuyItemKey(UUID playerUUID, int daysRequired) {
        int daysCheckedIn = getDaysCheckedInThisMonth(playerUUID);
        return getTichLuyItemKeyFast(daysCheckedIn, playerUUID, daysRequired);
    }

    public String getSpecialDayKey(LocalDate today, int day) {
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

    public String getSpecialDayItemKey(UUID playerUUID, String specialDayKey, int tickets) {
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
            if (tickets > 0) {
                return "DiemDanhBu";
            } else {
                return "ChuaDiemDanh";
            }
        } else {
            return "NgayDiemDanh";
        }
    }

    public String getSpecialDayItemKey(UUID playerUUID, String specialDayKey) {
        return getSpecialDayItemKey(playerUUID, specialDayKey, plugin.getPlayerDataManager().getTickets(playerUUID));
    }
}
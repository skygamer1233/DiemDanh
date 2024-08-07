package me.diemdanh;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DiemDanhGUI implements Listener {
    private final DiemDanh plugin;

    public DiemDanhGUI(DiemDanh plugin) {
        this.plugin = plugin;
    }

    public void openDiemDanhGUI(Player player) {
        LocalDate today = LocalDate.now();
        String playerUUID = player.getUniqueId().toString();
        String titleWithMonth = plugin.guiTitle.replace("<month>", String.valueOf(today.getMonthValue()));
        Inventory gui = Bukkit.createInventory(null, 45, titleWithMonth);

        ConfigurationSection daysSection = plugin.getConfig().getConfigurationSection("Days");
        if (daysSection == null) {
            plugin.getLogger().severe("Missing 'Days' section in config.yml");
            return;
        }
        List<?> dayEntries = new ArrayList<>(daysSection.getValues(false).values());

        for (String specialDayKey : plugin.getConfig().getConfigurationSection("SpecialDay").getKeys(false)) {
            ConfigurationSection specialDaySection = plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey);
            int specialDayDate = specialDaySection.getInt("Require.Date");
            int specialDayMonth = specialDaySection.getInt("Require.Month");
            if (today.getMonthValue() == specialDayMonth) {
                String itemKey = getSpecialDayItemKey(playerUUID, specialDayKey);
                ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Icon." + itemKey);
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
                itemSection = plugin.getConfig().getConfigurationSection("Item." + itemKey);
            } else {
                itemSection = plugin.getConfig().getConfigurationSection("Item." + itemKey);
            }

            if (itemSection == null) {
                plugin.getLogger().warning("Missing item section for key: " + itemKey);
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
            ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("TichLuy." + daysRequired + "ngay.Icon." + itemKey);

            ItemStack item = createItemFromConfig(itemSection, daysRequired, playerUUID);
            gui.setItem(36 + i, item);
        }

        ConfigurationSection ticketSection = plugin.getConfig().getConfigurationSection("Item.Ticket");
        if (ticketSection != null) {
            int tickets = plugin.playerData.getInt(playerUUID + ".tickets", 0);
            ItemStack ticketItem = createItemFromConfig(ticketSection, tickets, playerUUID);
            gui.setItem(34, ticketItem);
        } else {
            plugin.getLogger().warning("Missing 'Item.Ticket' section in config.yml");
        }

        ConfigurationSection thongTinSection = plugin.getConfig().getConfigurationSection("Item.ThongTin");
        if (thongTinSection != null) {
            int daysCheckedIn = getDaysCheckedInThisMonth(playerUUID);
            ItemStack thongTinItem = createItemFromConfig(thongTinSection, daysCheckedIn, playerUUID);
            gui.setItem(35, thongTinItem);
        } else {
            plugin.getLogger().warning("Missing 'Item.ThongTin' section in config.yml");
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
            line = line.replace("<tickets>", String.valueOf(plugin.playerData.getInt(playerUUID + ".tickets", 0)));
            line = line.replace("<totaldays>", String.valueOf(plugin.playerData.getInt(playerUUID + ".totalDays", 0)));
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
        if (!event.getView().getTitle().equals(plugin.guiTitle.replace("<month>", String.valueOf(LocalDate.now().getMonthValue()))))
            return;
        if (!(event.getWhoClicked() instanceof Player)) {
            event.getWhoClicked().sendMessage(plugin.getMessage("NotPlayer"));
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
                        ? plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                        : plugin.getConfig().getConfigurationSection("Days." + (slot + 1));

                if (rewardSection != null) {
                    executeCommands(rewardSection.getStringList("Reward"), player);
                }

                markPlayerCheckedIn(playerUUID, slot + 1);

                String messageKey = (specialDayKey != null) ? "SpecialDayDiemDanhThanhCong" : "DiemDanh";
                player.sendMessage(color.transalate(plugin.getConfig().getString("Message." + messageKey, plugin.getMessage("DiemDanh"))
                        .replace("%day%", String.valueOf(slot + 1))
                        .replace("%specialDay%", specialDayKey != null ? specialDayKey : "")));
            } else if (originalItemKey.equals("DiemDanhBu")) {
                if (plugin.playerData.getInt(playerUUID + ".tickets", 0) > 0) {
                    ConfigurationSection rewardSection = (specialDayKey != null)
                            ? plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                            : plugin.getConfig().getConfigurationSection("Days." + (slot + 1));

                    if (rewardSection != null) {
                        executeCommands(rewardSection.getStringList("Reward"), player);
                    }

                    plugin.playerData.set(playerUUID + ".tickets", plugin.playerData.getInt(playerUUID + ".tickets", 0) - 1);
                    List<Integer> missedDays = plugin.playerData.getIntegerList(playerUUID + ".missedDays");
                    missedDays.remove(Integer.valueOf(slot + 1));
                    plugin.playerData.set(playerUUID + ".missedDays", missedDays);

                    markPlayerCheckedIn(playerUUID, slot + 1);

                    plugin.savePlayerData();
                    player.sendMessage(plugin.getMessage("DiemDanh").replace("%day%", String.valueOf(slot + 1)));
                } else {
                    player.sendMessage(plugin.getMessage("NotRequire"));
                }
            } else if (originalItemKey.equals("ChuaDiemDanh")) {
                player.sendMessage(isToday(slot + 1) ? plugin.getMessage("Claiming") : plugin.getMessage("ChuaDiemDanh").replace("%day%", String.valueOf(slot + 1)));
            } else if (specialDayKey != null && originalItemKey.equals("DaDiemDanh")) {
                player.sendMessage(color.transalate(plugin.getConfig().getString("Message.SpecialDayDaDiemDanh", "&cBạn đã điểm danh ngày lễ này rồi!")));
            } else if (originalItemKey.equals("DaDiemDanh")) {
                player.sendMessage(plugin.getMessage("DaDiemDanh"));
            } else {
                String dayName = (specialDayKey != null)
                        ? plugin.getConfig().getString("SpecialDay." + specialDayKey + ".Icon.NgayDiemDanh.Name", specialDayKey)
                        : String.valueOf(slot + 1);
                player.sendMessage(plugin.getMessage("NgayDiemDanh").replace("%day%", dayName));
            }
        } else if (slot >= 36 && slot <= 38) {
            originalItemKey = getTichLuyItemKey(playerUUID, (slot - 36 + 1) * 7);

            if (originalItemKey.equals("NhanQua")) {
                ConfigurationSection rewardSection = plugin.getConfig().getConfigurationSection("TichLuy." + (slot - 36 + 1) * 7 + "ngay");
                if (rewardSection != null) {
                    executeCommands(rewardSection.getStringList("Reward"), player);
                    plugin.playerData.set(playerUUID + ".tichluy." + (slot - 36 + 1) * 7 + ".claimed", true);
                    plugin.playerData.set(playerUUID + ".tichluy." + (slot - 36 + 1) * 7 + ".month", today.getMonthValue());
                    plugin.savePlayerData();
                    player.sendMessage(color.transalate(plugin.getConfig().getString("Message.TichLuySuccess", "&aBạn đã nhận quà tích lũy %days% ngày thành công!").replace("%days%", String.valueOf((slot - 36 + 1) * 7))));
                }
            } else if (originalItemKey.equals("DaNhanQua")) {
                player.sendMessage(plugin.getMessage("IsClaimed"));
            } else {
                player.sendMessage(plugin.getMessage("NotRequire"));
            }
        }

        openDiemDanhGUI(player);
    }

    public void executeCommands(List<String> commands, Player player) {
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
        }
    }

    private String getItemKeyForDay(int day, String playerUUID) {
        LocalDate today = LocalDate.now();
        if (day > today.getDayOfMonth()) {
            return "NgayDiemDanh";
        } else if (plugin.playerData.getIntegerList(playerUUID + ".checkedDays").contains(day)) {
            return "DaDiemDanh";
        } else if (plugin.playerData.getInt(playerUUID + ".tickets", 0) > 0 && plugin.playerData.getIntegerList(playerUUID + ".missedDays").contains(day)) {
            return "DiemDanhBu";
        } else if (day < today.getDayOfMonth()) {
            return "ChuaDiemDanh";
        } else {
            return "DiemDanh";
        }
    }

    private boolean isToday(int day) {
        LocalDate today = LocalDate.now();
        return day == today.getDayOfMonth();
    }

    private void markPlayerCheckedIn(String playerUUID, int day) {
        LocalDate today = LocalDate.now();
        String specialDayKey = getSpecialDayKey(today, day);

        if (specialDayKey != null) {
            plugin.playerData.set(playerUUID + ".specialDays." + specialDayKey, true);
        } else {
            plugin.playerData.set(playerUUID + ".lastCheckIn", LocalDate.now().toString());
            int currentMonth = LocalDate.now().getMonthValue();
            int lastCheckInMonth = plugin.playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
            if (currentMonth != lastCheckInMonth) {
                plugin.playerData.set(playerUUID + ".checkedDays", new ArrayList<>());
                plugin.playerData.set(playerUUID + ".daysCheckedIn", 0);
            }
            List<Integer> checkedDays = plugin.playerData.getIntegerList(playerUUID + ".checkedDays");
            checkedDays.add(day);
            plugin.playerData.set(playerUUID + ".checkedDays", checkedDays);
        }
        int currentMonth = LocalDate.now().getMonthValue();
        int lastCheckInMonth = plugin.playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
        int daysCheckedIn = plugin.playerData.getInt(playerUUID + ".daysCheckedIn", 0);
        if (currentMonth != lastCheckInMonth) {
            daysCheckedIn = 0;
        }
        daysCheckedIn++;

        if (!plugin.playerData.getIntegerList(playerUUID + ".missedDays").contains(day)) {
            int totalDays = plugin.playerData.getInt(playerUUID + ".totalDays", 0);
            totalDays++;
            plugin.playerData.set(playerUUID + ".totalDays", totalDays);
        }

        plugin.playerData.set(playerUUID + ".daysCheckedIn", daysCheckedIn);
        plugin.playerData.set(playerUUID + ".lastCheckInMonth", currentMonth);

        LocalDate lastCheckInDate = LocalDate.parse(plugin.playerData.getString(playerUUID + ".lastCheckIn", "1970-01-01"));
        if (today.getYear() != lastCheckInDate.getYear()) {
            for (String key : plugin.playerData.getConfigurationSection(playerUUID + ".specialDays").getKeys(false)) {
                plugin.playerData.set(playerUUID + ".specialDays." + key, false);
            }
        }

        plugin.savePlayerData();
    }

    private int getDaysCheckedInThisMonth(String playerUUID) {
        int currentMonth = LocalDate.now().getMonthValue();
        int lastCheckInMonth = plugin.playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
        if (currentMonth == lastCheckInMonth) {
            return plugin.playerData.getInt(playerUUID + ".daysCheckedIn", 0);
        } else {
            return 0;
        }
    }

    private String getTichLuyItemKey(String playerUUID, int daysRequired) {
        int daysCheckedIn = getDaysCheckedInThisMonth(playerUUID);
        int currentMonth = LocalDate.now().getMonthValue();
        int claimedMonth = plugin.playerData.getInt(playerUUID + ".tichluy." + daysRequired + ".month", 0);


        if (currentMonth != claimedMonth) {
            plugin.playerData.set(playerUUID + ".tichluy." + daysRequired + ".claimed", false);
            plugin.playerData.set(playerUUID + ".tichluy." + daysRequired + ".month", 0);
            plugin.savePlayerData();
        }

        boolean hasClaimed = plugin.playerData.getBoolean(playerUUID + ".tichluy." + daysRequired + ".claimed", false);

        if (daysCheckedIn >= daysRequired && !hasClaimed) {
            return "NhanQua";
        } else if (hasClaimed) {
            return "DaNhanQua";
        } else {
            return "ChuaNhanQua";
        }
    }

    private String getSpecialDayKey(LocalDate today, int day) {
        for (String key : plugin.getConfig().getConfigurationSection("SpecialDay").getKeys(false)) {
            ConfigurationSection specialDaySection = plugin.getConfig().getConfigurationSection("SpecialDay." + key + ".Require");
            int specialDayDate = specialDaySection.getInt("Date");
            int specialDayMonth = specialDaySection.getInt("Month");
            if (specialDayMonth < 1 || specialDayMonth > 12) {
                plugin.getLogger().warning("Invalid month for special day: " + key);
                continue;
            }
            if (day == specialDayDate && today.getMonthValue() == specialDayMonth) {
                return key;
            }
        }
        return null;
    }

    private String getSpecialDayItemKey(String playerUUID, String specialDayKey) {
        boolean hasCheckedIn = plugin.playerData.getBoolean(playerUUID + ".specialDays." + specialDayKey, false);
        ConfigurationSection specialDaySection = plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Require");
        int specialDayDate = specialDaySection.getInt("Date");
        int specialDayMonth = specialDaySection.getInt("Month");

        LocalDate specialDay = LocalDate.of(LocalDate.now().getYear(), specialDayMonth, specialDayDate);

        if (hasCheckedIn) {
            return "DaDiemDanh";
        } else if (LocalDate.now().isEqual(specialDay)) {
            return "DiemDanh";
        } else if (LocalDate.now().isAfter(specialDay)) {
            if (plugin.playerData.getInt(playerUUID + ".tickets", 0) > 0) {
                return "DiemDanhBu";
            } else {
                return "ChuaDiemDanh";
            }
        } else {
            return "NgayDiemDanh";
        }
    }
}

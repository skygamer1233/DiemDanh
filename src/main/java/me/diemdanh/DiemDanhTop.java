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
import org.bukkit.inventory.meta.SkullMeta;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DiemDanhTop {
    private final DiemDanh plugin;

    public DiemDanhTop(DiemDanh plugin) {
        this.plugin = plugin;
    }

    public void openTopDiemDanhGUI(Player player) {
        int currentMonth = LocalDate.now().getMonthValue();
        Map<UUID, Integer> topPlayers = getTopDiemDanhPlayers(currentMonth);

        Inventory gui = Bukkit.createInventory(null, 54, plugin.topGuiTitle.replace("<month>", String.valueOf(currentMonth)));

        int slot = 0;
        ConfigurationSection topSection = plugin.topGuiConfig.getConfigurationSection("TopItem");
        if (topSection == null) {
            plugin.getLogger().warning("Missing 'TopItem' section in topgui.yml");
            return;
        }

        for (Map.Entry<UUID, Integer> entry : topPlayers.entrySet()) {
            ItemStack item = createItemFromConfig(topSection, entry.getKey().toString(), entry.getValue(), slot + 1);
            gui.setItem(slot, item);
            slot++;
        }

        ConfigurationSection nextPageSection = plugin.topGuiConfig.getConfigurationSection("NextPage");
        if (nextPageSection != null) {
            ItemStack nextPageItem = createItemFromConfig(nextPageSection, null, 0, 0);
            gui.setItem(53, nextPageItem);
        } else {
            plugin.getLogger().warning("Missing 'NextPage' section in topgui.yml");
        }

        player.openInventory(gui);
    }

    private Map<UUID, Integer> getTopDiemDanhPlayers(int month) {
        return plugin.playerData.getKeys(false).stream()
            // Lọc người chơi có tháng điểm danh trùng khớp
            .filter(playerUUID -> plugin.playerData.getInt(playerUUID + ".lastCheckInMonth", 0) == month)
            // Chuyển đổi thành Map.Entry với UUID và số ngày điểm danh
            .collect(Collectors.toMap(
                UUID::fromString,
                playerUUID -> plugin.playerData.getInt(playerUUID + ".daysCheckedIn", 0),
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ))
            // Chuyển đổi thành Stream các Entry
            .entrySet().stream()
            // Sắp xếp theo số ngày điểm danh (giảm dần)
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            // Giới hạn 10 người đứng đầu
            .limit(10)
            // Collect kết quả vào LinkedHashMap để giữ thứ tự
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    private ItemStack createItemFromConfig(ConfigurationSection topItemSection, String playerUUID, int daysCheckedIn, int top) {
        XMaterial xMaterial = XMaterial.matchXMaterial(topItemSection.getString("ID")).orElse(XMaterial.BARRIER);
        Material material = xMaterial.parseMaterial();
        int totalDays = playerUUID != null ? plugin.playerData.getInt(playerUUID + ".totalDays", 0) : 0;

        List<String> rawlore = topItemSection.getStringList("Lore");
        List<String> translatedLore = new ArrayList<>();
        for (String line : rawlore) {
            line = line.replace("<days>", String.valueOf(daysCheckedIn));
            line = line.replace("<totaldays>", String.valueOf(totalDays));
            translatedLore.add(color.transalate(line));
        }

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (playerUUID != null && material == XMaterial.PLAYER_HEAD.parseMaterial()) {
            SkullMeta skullMeta = (SkullMeta) meta;
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(playerUUID)));

            String name = topItemSection.getString("Name")
                    .replace("<player_name>", Bukkit.getOfflinePlayer(UUID.fromString(playerUUID)).getName())
                    .replace("<top>", String.valueOf(top));
            skullMeta.setDisplayName(color.transalate(name));

            skullMeta.setLore(translatedLore);
            if (topItemSection.getBoolean("Glow")) {
                skullMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                skullMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(skullMeta);
        } else {
            String name = topItemSection.getString("Name");
            if (name != null) {
                meta.setDisplayName(color.transalate(name));
            }
            meta.setLore(translatedLore);
            if (topItemSection.getBoolean("Glow")) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    public void openTopDiemDanhTongGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, plugin.TotalTitle);

        Map<UUID, Integer> topPlayers = getTopTotalDiemDanhPlayers();

        int slot = 0;
        ConfigurationSection topSection = plugin.topGuiConfig.getConfigurationSection("TopItem");
        if (topSection == null) {
            plugin.getLogger().warning("Missing 'TopItem' section in topgui.yml");
            return;
        }

        for (Map.Entry<UUID, Integer> entry : topPlayers.entrySet()) {
            ItemStack item = createItemFromConfig(topSection, entry.getKey().toString(), entry.getValue(), slot + 1);
            gui.setItem(slot, item);
            slot++;
        }

        ConfigurationSection backSection = plugin.topGuiConfig.getConfigurationSection("BackPage");
        if (backSection != null) {
            ItemStack backItem = createItemFromConfig(backSection, null, 0, 0);
            gui.setItem(45, backItem);
        } else {
            plugin.getLogger().warning("Missing 'BackPage' section in topgui.yml");
        }

        player.openInventory(gui);
    }

    private Map<UUID, Integer> getTopTotalDiemDanhPlayers() {
        return plugin.playerData.getKeys(false).stream()
            .collect(Collectors.toMap(
                UUID::fromString,
                playerUUID -> plugin.playerData.getInt(playerUUID + ".totalDays", 0),
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    public class TopGUIListener implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            String title = event.getView().getTitle();
            if (!title.equals(plugin.topGuiTitle.replace("<month>", String.valueOf(LocalDate.now().getMonthValue()))) 
                && !title.equals(plugin.TotalTitle)) {
                return;
            }

            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            if (title.equals(plugin.topGuiTitle.replace("<month>", String.valueOf(LocalDate.now().getMonthValue())))) {
                ConfigurationSection nextPageSection = plugin.topGuiConfig.getConfigurationSection("NextPage");
                if (nextPageSection != null) {
                    String nextPageItemId = nextPageSection.getString("ID");
                    if (nextPageItemId != null && event.getCurrentItem().getType() == XMaterial.matchXMaterial(nextPageItemId).get().parseMaterial()) {
                        if (event.getSlot() == 53) {
                            openTopDiemDanhTongGUI(player);
                        }
                    }
                }
            } else if (title.equals(plugin.TotalTitle)) {
                ConfigurationSection backPageSection = plugin.topGuiConfig.getConfigurationSection("BackPage");
                if (backPageSection != null) {
                    String backPageItemId = backPageSection.getString("ID");
                    if (backPageItemId != null && event.getCurrentItem().getType() == XMaterial.matchXMaterial(backPageItemId).get().parseMaterial()) {
                        if (event.getSlot() == 45) {
                            openTopDiemDanhGUI(player);
                        }
                    }
                }
            }
        }
    }
}
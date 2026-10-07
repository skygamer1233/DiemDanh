package me.diemdanh;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

        String title = color.transalate(plugin.topGuiTitle.replace("<month>", String.valueOf(currentMonth)));
        Inventory gui = Bukkit.createInventory(null, 54, title);

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
                // Kiểm tra an toàn xem Key có phải là UUID hợp lệ không
                .filter(this::isValidUUID)
                .filter(playerUUID -> plugin.playerData.getInt(playerUUID + ".lastCheckInMonth", 0) == month)
                .collect(Collectors.toMap(
                        UUID::fromString,
                        playerUUID -> plugin.playerData.getInt(playerUUID + ".daysCheckedIn", 0),
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

    private ItemStack createItemFromConfig(ConfigurationSection topItemSection, String playerUUID, int daysCheckedIn, int top) {
        if (topItemSection == null) return new ItemStack(Material.BARRIER);

        XMaterial xMaterial = XMaterial.matchXMaterial(topItemSection.getString("ID", "BARRIER")).orElse(XMaterial.BARRIER);
        Material material = xMaterial.parseMaterial();
        if (material == null) material = Material.BARRIER;

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
        if (meta == null) return item;

        boolean isHead = playerUUID != null && material == XMaterial.PLAYER_HEAD.parseMaterial();

        if (isHead && meta instanceof SkullMeta skullMeta) {
            UUID uuid = UUID.fromString(playerUUID);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            skullMeta.setOwningPlayer(offlinePlayer);

            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            String rawName = topItemSection.getString("Name", "")
                    .replace("<player_name>", playerName)
                    .replace("<top>", String.valueOf(top));
            skullMeta.setDisplayName(color.transalate(rawName));
            skullMeta.setLore(translatedLore);

            if (topItemSection.getBoolean("Glow")) {
                skullMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
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
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    public void openTopDiemDanhTongGUI(Player player) {
        String title = color.transalate(plugin.TotalTitle);
        Inventory gui = Bukkit.createInventory(null, 54, title);

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
                .filter(this::isValidUUID)
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

    private boolean isValidUUID(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public class TopGUIListener implements Listener {
        private final Map<UUID, Long> clickCooldowns = new HashMap<>();

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;

            String title = color.transalate(event.getView().getTitle());
            String monthTopTitle = color.transalate(plugin.topGuiTitle.replace("<month>", String.valueOf(LocalDate.now().getMonthValue())));
            String totalTopTitle = color.transalate(plugin.TotalTitle);

            if (!title.equals(monthTopTitle) && !title.equals(totalTopTitle)) {
                return;
            }

            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            // Chống spam click (Cooldown 500ms)
            long now = System.currentTimeMillis();
            if (clickCooldowns.containsKey(player.getUniqueId()) && now - clickCooldowns.get(player.getUniqueId()) < 500) {
                return;
            }
            clickCooldowns.put(player.getUniqueId(), now);

            if (title.equals(monthTopTitle)) {
                ConfigurationSection nextPageSection = plugin.topGuiConfig.getConfigurationSection("NextPage");
                if (nextPageSection != null) {
                    String nextPageItemId = nextPageSection.getString("ID", "BARRIER");
                    XMaterial xMat = XMaterial.matchXMaterial(nextPageItemId).orElse(XMaterial.BARRIER);
                    Material nextPageMat = xMat.parseMaterial();

                    if (nextPageMat != null && event.getCurrentItem().getType() == nextPageMat) {
                        if (event.getSlot() == 53) {
                            openTopDiemDanhTongGUI(player);
                        }
                    }
                }
            } else if (title.equals(totalTopTitle)) {
                ConfigurationSection backPageSection = plugin.topGuiConfig.getConfigurationSection("BackPage");
                if (backPageSection != null) {
                    String backPageItemId = backPageSection.getString("ID", "BARRIER");
                    XMaterial xMat = XMaterial.matchXMaterial(backPageItemId).orElse(XMaterial.BARRIER);
                    Material backPageMat = xMat.parseMaterial();

                    if (backPageMat != null && event.getCurrentItem().getType() == backPageMat) {
                        if (event.getSlot() == 45) {
                            openTopDiemDanhGUI(player);
                        }
                    }
                }
            }
        }
    }
}
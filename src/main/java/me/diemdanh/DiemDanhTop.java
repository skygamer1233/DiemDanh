package me.diemdanh;

import com.cryptomorin.xseries.XMaterial;
import me.diemdanh.holder.DiemDanhTopHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.util.*;

public class DiemDanhTop {
    private final DiemDanh plugin;
    private volatile List<TopEntry> cachedMonthTop = new ArrayList<>();
    private volatile List<TopEntry> cachedTotalTop = new ArrayList<>();
    private BukkitTask updateTask;

    public static class TopEntry {
        private final UUID uuid;
        private final String name;
        private final int monthDays;
        private final int totalDays;
        private ItemStack cachedMonthItem;
        private ItemStack cachedTotalItem;

        public TopEntry(UUID uuid, String name, int monthDays, int totalDays) {
            this.uuid = uuid;
            this.name = name;
            this.monthDays = monthDays;
            this.totalDays = totalDays;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public int getMonthDays() { return monthDays; }
        public int getTotalDays() { return totalDays; }
        public ItemStack getCachedMonthItem() { return cachedMonthItem; }
        public void setCachedMonthItem(ItemStack item) { this.cachedMonthItem = item; }
        public ItemStack getCachedTotalItem() { return cachedTotalItem; }
        public void setCachedTotalItem(ItemStack item) { this.cachedTotalItem = item; }
    }

    public DiemDanhTop(DiemDanh plugin) {
        this.plugin = plugin;
        startAutoUpdateTask();
    }

    public void startAutoUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        // Chạy lần đầu sau 1s, lặp lại mỗi 5 phút (6000 ticks) bất đồng bộ
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::computeTopCache, 20L, 6000L);
    }

    public void stopAutoUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void refreshCache() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::computeTopCache);
    }

    public List<TopEntry> getCachedMonthTop() {
        return cachedMonthTop;
    }

    public List<TopEntry> getCachedTotalTop() {
        return cachedTotalTop;
    }

    private void computeTopCache() {
        int currentMonth = LocalDate.now().getMonthValue();
        Set<UUID> allUuids = plugin.getPlayerDataManager().getAllTrackedUUIDs();

        List<TopEntry> allEntries = new ArrayList<>();
        for (UUID uuid : allUuids) {
            int lastCheckInMonth = plugin.getPlayerDataManager().getLastCheckInMonth(uuid);
            int monthDays = (lastCheckInMonth == currentMonth) ? plugin.getPlayerDataManager().getDaysCheckedIn(uuid) : 0;
            int totalDays = plugin.getPlayerDataManager().getTotalDays(uuid);
            String name = plugin.getPlayerDataManager().getPlayerName(uuid);

            allEntries.add(new TopEntry(uuid, name, monthDays, totalDays));
        }

        // Top Tháng
        List<TopEntry> newMonthTop = allEntries.stream()
                .filter(e -> e.getMonthDays() > 0)
                .sorted((a, b) -> Integer.compare(b.getMonthDays(), a.getMonthDays()))
                .limit(10)
                .toList();

        // Top Tổng
        List<TopEntry> newTotalTop = allEntries.stream()
                .filter(e -> e.getTotalDays() > 0)
                .sorted((a, b) -> Integer.compare(b.getTotalDays(), a.getTotalDays()))
                .limit(10)
                .toList();

        ConfigurationSection topSection = plugin.topGuiConfig.getConfigurationSection("TopItem");

        // Pre-build item cho Top Tháng
        for (int i = 0; i < newMonthTop.size(); i++) {
            TopEntry entry = newMonthTop.get(i);
            entry.setCachedMonthItem(buildSkullItem(topSection, entry.getUuid(), entry.getName(), entry.getMonthDays(), entry.getTotalDays(), i + 1));
        }

        // Pre-build item cho Top Tổng
        for (int i = 0; i < newTotalTop.size(); i++) {
            TopEntry entry = newTotalTop.get(i);
            entry.setCachedTotalItem(buildSkullItem(topSection, entry.getUuid(), entry.getName(), entry.getMonthDays(), entry.getTotalDays(), i + 1));
        }

        this.cachedMonthTop = newMonthTop;
        this.cachedTotalTop = newTotalTop;
    }

    private ItemStack buildSkullItem(ConfigurationSection topItemSection, UUID uuid, String knownName, int days, int totalDays, int top) {
        if (topItemSection == null) return new ItemStack(Material.BARRIER);

        XMaterial xMat = XMaterial.matchXMaterial(topItemSection.getString("ID", "PLAYER_HEAD")).orElse(XMaterial.PLAYER_HEAD);
        Material material = xMat.parseMaterial();
        if (material == null) material = Material.BARRIER;

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> rawLore = topItemSection.getStringList("Lore");
        List<String> translatedLore = new ArrayList<>();
        for (String line : rawLore) {
            line = line.replace("<days>", String.valueOf(days));
            line = line.replace("<totaldays>", String.valueOf(totalDays));
            translatedLore.add(ColorUtil.translate(line));
        }

        String displayName = knownName;
        if (meta instanceof SkullMeta skullMeta) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            skullMeta.setOwningPlayer(offlinePlayer);
            if (offlinePlayer.getName() != null) {
                displayName = offlinePlayer.getName();
            }
        }

        String rawName = topItemSection.getString("Name", "&aTop <top>: &c<player_name>")
                .replace("<player_name>", displayName)
                .replace("<top>", String.valueOf(top));

        meta.setDisplayName(ColorUtil.translate(rawName));
        meta.setLore(translatedLore);

        if (topItemSection.getBoolean("Glow")) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    public void openTopDiemDanhGUI(Player player) {
        int currentMonth = LocalDate.now().getMonthValue();
        String title = ColorUtil.translate(plugin.topGuiTitle.replace("<month>", String.valueOf(currentMonth)));
        Inventory gui = Bukkit.createInventory(new DiemDanhTopHolder(DiemDanhTopHolder.TopType.MONTH), 54, title);

        List<TopEntry> monthTop = cachedMonthTop;
        for (int i = 0; i < monthTop.size(); i++) {
            ItemStack item = monthTop.get(i).getCachedMonthItem();
            if (item != null) {
                gui.setItem(i, item);
            }
        }

        ConfigurationSection nextPageSection = plugin.topGuiConfig.getConfigurationSection("NextPage");
        if (nextPageSection != null) {
            gui.setItem(53, createSimpleItem(nextPageSection));
        }

        player.openInventory(gui);
    }

    public void openTopDiemDanhTongGUI(Player player) {
        String title = ColorUtil.translate(plugin.TotalTitle);
        Inventory gui = Bukkit.createInventory(new DiemDanhTopHolder(DiemDanhTopHolder.TopType.TOTAL), 54, title);

        List<TopEntry> totalTop = cachedTotalTop;
        for (int i = 0; i < totalTop.size(); i++) {
            ItemStack item = totalTop.get(i).getCachedTotalItem();
            if (item != null) {
                gui.setItem(i, item);
            }
        }

        ConfigurationSection backSection = plugin.topGuiConfig.getConfigurationSection("BackPage");
        if (backSection != null) {
            gui.setItem(45, createSimpleItem(backSection));
        }

        player.openInventory(gui);
    }

    private ItemStack createSimpleItem(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.BARRIER);

        XMaterial xMat = XMaterial.matchXMaterial(section.getString("ID", "BARRIER")).orElse(XMaterial.BARRIER);
        Material mat = xMat.parseMaterial();
        if (mat == null) mat = Material.BARRIER;

        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("Name", "");
            meta.setDisplayName(ColorUtil.translate(name));

            List<String> lore = section.getStringList("Lore");
            List<String> translated = new ArrayList<>();
            for (String l : lore) translated.add(ColorUtil.translate(l));
            meta.setLore(translated);

            if (section.getBoolean("Glow")) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public class TopGUIListener implements Listener {
        private final Map<UUID, Long> clickCooldowns = new HashMap<>();

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (!(event.getInventory().getHolder() instanceof DiemDanhTopHolder topHolder)) return;

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            long now = System.currentTimeMillis();
            if (clickCooldowns.containsKey(player.getUniqueId()) && now - clickCooldowns.get(player.getUniqueId()) < 400) {
                return;
            }
            clickCooldowns.put(player.getUniqueId(), now);

            if (topHolder.getType() == DiemDanhTopHolder.TopType.MONTH) {
                if (event.getSlot() == 53) {
                    openTopDiemDanhTongGUI(player);
                }
            } else if (topHolder.getType() == DiemDanhTopHolder.TopType.TOTAL) {
                if (event.getSlot() == 45) {
                    openTopDiemDanhGUI(player);
                }
            }
        }

        @EventHandler
        public void onInventoryDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof DiemDanhTopHolder) {
                event.setCancelled(true);
            }
        }
    }
}
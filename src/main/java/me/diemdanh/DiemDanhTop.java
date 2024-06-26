    package me.diemdanh;

    import com.cryptomorin.xseries.XMaterial;
    import org.bukkit.Bukkit;
    import org.bukkit.Material;
    import org.bukkit.configuration.ConfigurationSection;
    import org.bukkit.enchantments.Enchantment;
    import org.bukkit.entity.Player;
    import org.bukkit.inventory.Inventory;
    import org.bukkit.inventory.ItemFlag;
    import org.bukkit.inventory.ItemStack;
    import org.bukkit.inventory.meta.ItemMeta;
    import org.bukkit.inventory.meta.SkullMeta;
    import org.bukkit.event.Listener;
    import org.bukkit.event.EventHandler;
    import org.bukkit.event.inventory.InventoryClickEvent;


    import java.time.LocalDate;
    import java.util.*;

    public class DiemDanhTop{
        private final DiemDanh plugin;


        public DiemDanhTop(DiemDanh plugin) {
            this.plugin = plugin;

        }




        public void openTopDiemDanhGUI(Player player) {
            LocalDate today = LocalDate.now();
            String titleWithMonth = plugin.getTopGuiTitle().replace("<month>", String.valueOf(today.getMonthValue()));
            Inventory gui = Bukkit.createInventory(null, 54, titleWithMonth);


            Map<UUID, Integer> topPlayers = getTopDiemDanhPlayers(today.getMonthValue());


            int[] topPositions = {10, 19, 20, 28, 29, 30, 37, 38, 39, 40};

            int index = 0;
            for (Map.Entry<UUID, Integer> entry : topPlayers.entrySet()) {
                UUID playerUUID = entry.getKey();
                int daysCheckedIn = entry.getValue();


                ConfigurationSection topItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.Top.Icon.HavePlayer");
                if (topItemSection == null) {
                    plugin.getLogger().warning("Missing configuration for Top in topgui.yml");
                    return;
                }


                String playerName = playerUUID != null ? Bukkit.getOfflinePlayer(playerUUID).getName() : "Chưa có";

                ItemStack item = createItemFromConfig(topItemSection, playerUUID.toString(), daysCheckedIn, index + 1);
                gui.setItem(topPositions[index], item);

                index++;

            }

            ConfigurationSection noPlayerItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.Top.Icon.NoPlayer");
            if (noPlayerItemSection != null) {
                ItemStack noPlayerItem = createItemFromConfig(noPlayerItemSection, null, 0, 0);
                for (int i = 0; i < topPositions.length; i++) {
                    if (gui.getItem(topPositions[i]) == null) {
                        gui.setItem(topPositions[i], noPlayerItem);
                    }
                }
            } else {
                plugin.getLogger().warning("Missing configuration for NoPlayer in topgui.yml");
            }

            ConfigurationSection fillItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.FillItem");
            if (fillItemSection != null) {
                ItemStack fillItem = createItemFromConfig(fillItemSection, "", 0, 0);
                for (int i = 0; i < gui.getSize(); i++) {
                    if (gui.getItem(i) == null) {
                        gui.setItem(i, fillItem);
                    }
                }
            } else {
                plugin.getLogger().warning("Missing 'FillItem' section in topgui.yml");
            }
            ConfigurationSection nextPageItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.NextPage");
            if (nextPageItemSection != null) {
                ItemStack nextPageItem = createItemFromConfig(nextPageItemSection, null, 0, 0);
                gui.setItem(53, nextPageItem);
            } else {
                plugin.getLogger().warning("Missing 'NextPage' section in topgui.yml");
            }





            player.openInventory(gui);
        }


        private ItemStack createItemFromConfig( ConfigurationSection topItemSection, String playerUUID, int daysCheckedIn, int top) {
            XMaterial xMaterial = XMaterial.matchXMaterial(topItemSection.getString("ID")).orElse(XMaterial.BARRIER);
            Material material = xMaterial.parseMaterial();
            int totalDays = plugin.playerData.getInt(playerUUID + ".totalDays", 0);

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



        private Map<UUID, Integer> getTopDiemDanhPlayers(int month) {
            Map<UUID, Integer> playerCheckIns = new HashMap<>();


            for (String playerUUID : plugin.playerData.getKeys(false)) {
                int lastCheckInMonth = plugin.playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
                if (lastCheckInMonth == month) {
                    int daysCheckedIn = plugin.playerData.getInt(playerUUID + ".daysCheckedIn", 0);
                    playerCheckIns.put(UUID.fromString(playerUUID), daysCheckedIn);
                }
            }


            if (playerCheckIns.isEmpty()) {
                return playerCheckIns;
            }


            List<Map.Entry<UUID, Integer>> sortedEntries = new ArrayList<>(playerCheckIns.entrySet());
            sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));


            int topLimit = Math.min(10, sortedEntries.size());
            Map<UUID, Integer> topPlayers = new LinkedHashMap<>();
            for (int i = 0; i < topLimit; i++) {
                Map.Entry<UUID, Integer> entry = sortedEntries.get(i);
                topPlayers.put(entry.getKey(), entry.getValue());
            }

            return topPlayers;
        }
        public void openTopDiemDanhTongGUI(Player player) {
            String title = color.transalate(plugin.topGuiConfig.getString("TotalTitle"));
            Inventory gui = Bukkit.createInventory(null, 54, title);


            Map<UUID, Integer> topPlayers = getTopDiemDanhTongPlayers();


            int[] topPositions = {10, 19, 20, 28, 29, 30, 37, 38, 39, 40};

            int index = 0;
            for (Map.Entry<UUID, Integer> entry : topPlayers.entrySet()) {
                UUID playerUUID = entry.getKey();
                int totalDays = entry.getValue();


                ConfigurationSection topItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.Total.Icon.HavePlayer");
                if (topItemSection == null) {
                    plugin.getLogger().warning("Missing configuration for Total in topgui.yml");
                    return;
                }


                String playerName = playerUUID != null ? Bukkit.getOfflinePlayer(playerUUID).getName() : "Chưa có";

                ItemStack item = createItemFromConfig(topItemSection, playerUUID.toString(), totalDays, index + 1);
                gui.setItem(topPositions[index], item);

                index++;
            }


            ConfigurationSection noPlayerItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.Total.Icon.NoPlayer");
            if (noPlayerItemSection != null) {
                ItemStack noPlayerItem = createItemFromConfig(noPlayerItemSection, null, 0, 0);
                for (int i = 0; i < topPositions.length; i++) {
                    if (gui.getItem(topPositions[i]) == null) {
                        gui.setItem(topPositions[i], noPlayerItem);
                    }
                }
            } else {
                plugin.getLogger().warning("Missing configuration for NoPlayer in topgui.yml");
            }


            ConfigurationSection fillItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.FillItem");
            if (fillItemSection != null) {
                ItemStack fillItem = createItemFromConfig(fillItemSection, null, 0, 0);
                for (int i = 0; i < gui.getSize(); i++) {
                    if (gui.getItem(i) == null) {
                        gui.setItem(i, fillItem);
                    }
                }
            } else {
                plugin.getLogger().warning("Missing 'FillItem' section in topgui.yml");
            }
            ConfigurationSection nextPageItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.PrevPage");
            if (nextPageItemSection != null) {
                ItemStack nextPageItem = createItemFromConfig(nextPageItemSection, null, 0, 0);
                gui.setItem(53, nextPageItem);
            } else {
                plugin.getLogger().warning("Missing 'PrevPage' section in topgui.yml");
            }


            Bukkit.getPluginManager().registerEvents(new TopGUIListener(), plugin);

            player.openInventory(gui);
        }
        private Map<UUID, Integer> getTopDiemDanhTongPlayers() {
            Map<UUID, Integer> playerCheckIns = new HashMap<>();


            for (String playerUUID : plugin.playerData.getKeys(false)) {
                int totalDays = plugin.playerData.getInt(playerUUID + ".totalDays", 0);
                playerCheckIns.put(UUID.fromString(playerUUID), totalDays);
            }


            if (playerCheckIns.isEmpty()) {
                return playerCheckIns;
            }


            List<Map.Entry<UUID, Integer>> sortedEntries = new ArrayList<>(playerCheckIns.entrySet());
            sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));


            int topLimit = Math.min(10, sortedEntries.size());
            Map<UUID, Integer> topPlayers = new LinkedHashMap<>();
            for (int i = 0; i < topLimit; i++) {
                Map.Entry<UUID, Integer> entry = sortedEntries.get(i);
                topPlayers.put(entry.getKey(), entry.getValue());
            }

            return topPlayers;
        }
        public class TopGUIListener implements Listener {
            @EventHandler
            public void onInventoryClick(InventoryClickEvent event) {
                String title = event.getView().getTitle();
                String topMonthTitle = plugin.getTopGuiTitle().replace("<month>", String.valueOf(LocalDate.now().getMonthValue()));
                String topTotalTitle = color.transalate(plugin.topGuiConfig.getString("TotalTitle"));

                if (!title.equals(topMonthTitle) && !title.equals(topTotalTitle)) {
                    return;
                }

                if (!(event.getWhoClicked() instanceof Player)) {
                    event.getWhoClicked().sendMessage(plugin.getMessage("NotPlayer"));
                    return;
                }

                Player player = (Player) event.getWhoClicked();
                int slot = event.getSlot();

                event.setCancelled(true);

                if (slot == 53) { 
                    if (title.equals(topMonthTitle)) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            openTopDiemDanhTongGUI(player);
                        }, 1L);
                    } else if (title.equals(topTotalTitle)) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            openTopDiemDanhGUI(player);
                        }, 1L);
                    }
                }
            }
        }


    }

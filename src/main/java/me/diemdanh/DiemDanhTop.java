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


                ConfigurationSection topItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.Top" + (index + 1) + ".Icon.HavePlayer");
                if(topItemSection == null) {
                    plugin.getLogger().warning("Missing configuration for Top" + (index + 1) + " in topgui.yml");
                    continue;
                }


                String playerName = playerUUID != null ? Bukkit.getOfflinePlayer(playerUUID).getName() : "Chưa có";

                ItemStack item = createItemFromConfig(topItemSection, playerUUID.toString(), daysCheckedIn);
                ItemMeta meta = item.getItemMeta();


                if (playerUUID != null && item.getType() == XMaterial.PLAYER_HEAD.parseMaterial()) {
                    SkullMeta skullMeta = (SkullMeta) meta;
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(playerUUID.toString())));
                    skullMeta.setDisplayName(color.transalate(topItemSection.getString("Name").replace("<player_name>", playerName)));

                    item.setItemMeta(skullMeta);
                } else {
                    String name = topItemSection.getString("Name");
                    if (name != null) {
                        meta.setDisplayName(color.transalate(name));
                    }
                    meta.setLore(getLoreFromConfig(topItemSection, "Lore"));
                    if (topItemSection.getBoolean("Glow")) {
                        meta.addEnchant(Enchantment.DURABILITY, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    item.setItemMeta(meta);
                }

                if (index < topPositions.length) {
                    gui.setItem(topPositions[index], item);
                }

                index++;
            }

            for (int i = 0; i < topPositions.length; i++) {
                if (gui.getItem(topPositions[i]) == null) {
                    ConfigurationSection topItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.Top" + (i + 1) + ".Icon.NoPlayer");
                    ItemStack item = createItemFromConfig(topItemSection, null, 0);
                    gui.setItem(topPositions[i], item);
                }
            }
            ConfigurationSection fillItemSection = plugin.topGuiConfig.getConfigurationSection("ItemTops.FillItem");
            if (fillItemSection != null) {
                ItemStack fillItem = createItemFromConfig(fillItemSection, null, 0);
                for (int i = 0; i < gui.getSize(); i++) {
                    if (gui.getItem(i) == null) {
                        gui.setItem(i, fillItem);
                    }
                }
            } else {
                plugin.getLogger().warning("Missing 'FillItem' section in topgui.yml");
            }



            player.openInventory(gui);
        }


        private ItemStack createItemFromConfig(ConfigurationSection topItemSection, String playerUUID, int daysCheckedIn) {
            XMaterial xMaterial = XMaterial.matchXMaterial(topItemSection.getString("ID")).orElse(XMaterial.BARRIER);
            Material material = xMaterial.parseMaterial();

            List<String> rawlore = topItemSection.getStringList("Lore");
            List<String> translatedLore = new ArrayList<>();
            for (String line : rawlore) {
                line = line.replace("<days>", String.valueOf(daysCheckedIn));
                translatedLore.add(color.transalate(line));
            }

            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();


            if (playerUUID != null && material == XMaterial.PLAYER_HEAD.parseMaterial()) {
                SkullMeta skullMeta = (SkullMeta) meta;
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(playerUUID))); // Đặt owning player dựa trên UUID
                skullMeta.setDisplayName(color.transalate(topItemSection.getString("Name")
                        .replace("<player_name>", Bukkit.getOfflinePlayer(UUID.fromString(playerUUID)).getName()))); // Đặt displayName cho skullMeta


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
        private List<String> getLoreFromConfig(ConfigurationSection section, String key) {
            List<String> lore = new ArrayList<>();
            if (section.isList(key)) {
                for (Object obj : section.getList(key)) {
                    if (obj instanceof String) {
                        lore.add(color.transalate((String) obj));
                    } else if (obj instanceof List) {
                        for (String nestedLine : (List<String>) obj) {
                            lore.add(color.transalate(nestedLine));
                        }
                    }
                }
            }
            return lore;
        }
        public class TopGUIListener implements Listener {
            @EventHandler
            public void onInventoryClick(InventoryClickEvent event) {
                if (!event.getView().getTitle().equals(plugin.getTopGuiTitle().replace("<month>", String.valueOf(LocalDate.now().getMonthValue())))) return;
                if (!(event.getWhoClicked() instanceof Player)) {
                    event.getWhoClicked().sendMessage(plugin.getMessage("NotPlayer"));
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getSlot();
                if (slot < 55) {
                    event.setCancelled(true);
                    openTopDiemDanhGUI(player);
                }
            }
        }

    }

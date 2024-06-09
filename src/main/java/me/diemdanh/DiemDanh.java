package me.diemdanh;

import me.diemdanh.color;
import com.cryptomorin.xseries.XMaterial;
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
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

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
            Player player = null;
            if (sender instanceof Player) {
                player = (Player) sender;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("diemdanh.reload")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }

                File configFile = new File(getDataFolder(), "config.yml");
                if (!configFile.exists()) {
                    saveDefaultConfig();
                    configFile = new File(getDataFolder(), "config.yml");
                }
                try {
                    getConfig().load(configFile);
                } catch (IOException | InvalidConfigurationException e) {
                    player.sendMessage(color.transalate("&cLỗi khi tải lại config!"));
                    e.printStackTrace();
                    return true;
                }

                playerDataFile = new File(getDataFolder(), "playerdata.yml");
                if (!playerDataFile.exists()) {
                    saveResource("playerdata.yml", false);
                }
                playerData = YamlConfiguration.loadConfiguration(playerDataFile);

                sender.sendMessage(reloadMessage);
                return true;
            } else if (args.length == 3 && args[0].equalsIgnoreCase("giveticket")) {
                if (!sender.hasPermission("diemdanh.giveticket")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }

                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(getMessage("PlayerNotFound"));
                    return true;
                }

                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        sender.sendMessage(getMessage("InvalidTicketAmount"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("InvalidTicketAmount"));
                    return true;
                }

                String targetPlayerUUID = targetPlayer.getUniqueId().toString();
                int currentTickets = playerData.getInt(targetPlayerUUID + ".tickets", 0);
                playerData.set(targetPlayerUUID + ".tickets", currentTickets + amount);
                savePlayerData();

                String giveTicketSuccessMessage = getMessage("GiveTicketSuccess")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%player%", targetPlayer.getName());
                sender.sendMessage(giveTicketSuccessMessage);

                String receiveTicketMessage = getMessage("ReceiveTicket")
                        .replace("%amount%", String.valueOf(amount));
                targetPlayer.sendMessage(receiveTicketMessage);

                return true;

            } else if (args.length == 0 && sender instanceof Player) {
                openDiemDanhGUI((Player) sender);
                return true;

            } else {
                player.sendMessage(syntaxErrorMessage);
                return true;
            }
        }
        return false;
    }



    private void openDiemDanhGUI(Player player) {
        LocalDate today = LocalDate.now();
        String playerUUID = player.getUniqueId().toString();
        String titleWithMonth = guiTitle.replace("<month>", String.valueOf(today.getMonthValue()));
        Inventory gui = Bukkit.createInventory(null, 45, titleWithMonth);

        ConfigurationSection daysSection = getConfig().getConfigurationSection("Days");
        if (daysSection == null) {
            getLogger().severe("Missing 'Days' section in config.yml");
            return;
        }
        List<?> dayEntries = new ArrayList<>(daysSection.getValues(false).values());

        for (String specialDayKey : getConfig().getConfigurationSection("SpecialDay").getKeys(false)) {
            ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey);
            int specialDayDate = specialDaySection.getInt("Require.Date");
            int specialDayMonth = specialDaySection.getInt("Require.Month");
            if (today.getMonthValue() == specialDayMonth) {
                String itemKey = getSpecialDayItemKey(playerUUID, specialDayKey);
                ConfigurationSection itemSection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Icon." + itemKey);
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
                itemSection = getConfig().getConfigurationSection("Item." + itemKey);
            } else {
                itemSection = getConfig().getConfigurationSection("Item." + itemKey);
            }

            if (itemSection == null) {
                getLogger().warning("Missing item section for key: " + itemKey);
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

            ItemStack item = new ItemStack(material, 1);
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
            ConfigurationSection itemSection = getConfig().getConfigurationSection("TichLuy." + daysRequired + "ngay.Icon." + itemKey);

            ItemStack item = createItemFromConfig(itemSection, daysRequired, playerUUID);
            gui.setItem(36 + i, item);
        }

        ConfigurationSection ticketSection = getConfig().getConfigurationSection("Item.Ticket");
        if (ticketSection != null) {
            int tickets = playerData.getInt(playerUUID + ".tickets", 0);
            ItemStack ticketItem = createItemFromConfig(ticketSection, tickets, playerUUID);
            gui.setItem(34, ticketItem);
        } else {
            getLogger().warning("Missing 'Item.Ticket' section in config.yml");
        }

        ConfigurationSection thongTinSection = getConfig().getConfigurationSection("Item.ThongTin");
        if (thongTinSection != null) {
            int daysCheckedIn = getDaysCheckedInThisMonth(playerUUID);
            ItemStack thongTinItem = createItemFromConfig(thongTinSection, daysCheckedIn, playerUUID);
            gui.setItem(35, thongTinItem);
        } else {
            getLogger().warning("Missing 'Item.ThongTin' section in config.yml");
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
            line = line.replace("<tickets>", String.valueOf(playerData.getInt(playerUUID + ".tickets", 0)));
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
        if (!event.getView().getTitle().equals(guiTitle.replace("<month>", String.valueOf(LocalDate.now().getMonthValue())))) return;
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

        String originalItemKey;
        String specialDayKey = getSpecialDayKey(today, slot + 1);

        if (slot < 31) {
            originalItemKey = (specialDayKey != null) ? getSpecialDayItemKey(playerUUID, specialDayKey) : getItemKeyForDay(slot + 1, playerUUID);
            if (originalItemKey.equals("DiemDanh")) {
                ConfigurationSection rewardSection = (specialDayKey != null)
                        ? getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                        : getConfig().getConfigurationSection("Days." + (slot + 1));

                if (rewardSection != null) {
                    executeCommands(rewardSection.getStringList("Reward"), player);
                }

                markPlayerCheckedIn(playerUUID, slot + 1);

                String messageKey = (specialDayKey != null) ? "SpecialDayDiemDanhThanhCong" : "DiemDanh";
                player.sendMessage(color.transalate(getConfig().getString("Message." + messageKey, "&aBạn đã điểm danh thành công!")
                        .replace("%day%", String.valueOf(slot + 1))
                        .replace("%specialDay%", specialDayKey != null ? specialDayKey : "")));
            } else if (originalItemKey.equals("DiemDanhBu")) {

                if (playerData.getInt(playerUUID + ".tickets", 0) > 0) {
                    ConfigurationSection rewardSection = (specialDayKey != null)
                            ? getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                            : getConfig().getConfigurationSection("Days." + (slot + 1));

                    if (rewardSection != null) {
                        executeCommands(rewardSection.getStringList("Reward"), player);
                    }


                    playerData.set(playerUUID + ".tickets", playerData.getInt(playerUUID + ".tickets", 0) - 1);
                    List<Integer> missedDays = playerData.getIntegerList(playerUUID + ".missedDays");
                    missedDays.remove(Integer.valueOf(slot + 1));
                    playerData.set(playerUUID + ".missedDays", missedDays);


                    markPlayerCheckedIn(playerUUID, slot + 1);

                    savePlayerData();
                    player.sendMessage(diemDanhMessage.replace("%day%", String.valueOf(slot + 1)));
                } else {
                    player.sendMessage(notRequireMessage);
                }
            } else if (originalItemKey.equals("ChuaDiemDanh")) {
                player.sendMessage(isToday(slot + 1) ? claimingMessage : chuaDiemDanhMessage.replace("%day%", String.valueOf(slot + 1)));
            } else if (specialDayKey != null && originalItemKey.equals("DaDiemDanh")) {
                player.sendMessage(color.transalate(getConfig().getString("Message.SpecialDayDaDiemDanh", "&cBạn đã điểm danh ngày lễ này rồi!")));
            } else if (originalItemKey.equals("DaDiemDanh")) {
                player.sendMessage(daDiemDanhMessage);
            } else {
                String dayName = (specialDayKey != null)
                        ? getConfig().getString("SpecialDay." + specialDayKey + ".Icon.NgayDiemDanh.Name", specialDayKey)
                        : String.valueOf(slot + 1);
                player.sendMessage(ngayDiemDanhMessage.replace("%day%", dayName));
            }
        } else if (slot >= 36 && slot <= 38) {
            originalItemKey = getTichLuyItemKey(playerUUID, (slot - 36 + 1) * 7);

            if (originalItemKey.equals("NhanQua")) {
                ConfigurationSection rewardSection = getConfig().getConfigurationSection("TichLuy." + (slot - 36 + 1) * 7 + "ngay");
                if (rewardSection != null) {
                    executeCommands(rewardSection.getStringList("Reward"), player);
                    playerData.set(playerUUID + ".tichluy." + (slot - 36 + 1) * 7, true);
                    savePlayerData();
                    player.sendMessage(color.transalate(getConfig().getString("Message.TichLuySuccess", "&aBạn đã nhận quà tích lũy %days% ngày thành công!").replace("%days%", String.valueOf((slot - 36 + 1) * 7))));
                }
            } else if (originalItemKey.equals("DaNhanQua")) {
                player.sendMessage(isClaimedMessage);
            } else {
                player.sendMessage(notRequireMessage);
            }
        }

        openDiemDanhGUI(player);
    }
    private void executeCommands(List<String> commands, Player player) {
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName()));
        }
    }











    private String getItemKeyForDay(int day, String playerUUID) {
        LocalDate today = LocalDate.now();
        if (day > today.getDayOfMonth()) {
            return "NgayDiemDanh";
        } else if (playerData.getIntegerList(playerUUID + ".checkedDays").contains(day)) {
            return "DaDiemDanh";
        } else if (playerData.getInt(playerUUID + ".tickets", 0) > 0 && playerData.getIntegerList(playerUUID + ".missedDays").contains(day)) {
            return "DiemDanhBu";
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

        if (!playerData.contains(playerUUID)) {
            playerData.set(playerUUID + ".name", player.getName());
            playerData.set(playerUUID + ".lastCheckIn", LocalDate.now().toString());
            playerData.set(playerUUID + ".daysCheckedIn", 0);
            playerData.set(playerUUID + ".lastCheckInMonth", LocalDate.now().getMonthValue());
            playerData.set(playerUUID + ".checkedDays", new ArrayList<>());
            playerData.set(playerUUID + ".missedDays", new ArrayList<>());

            ConfigurationSection specialDaysSection = getConfig().getConfigurationSection("SpecialDay");
            if (specialDaysSection != null) {
                for (String specialDayKey : specialDaysSection.getKeys(false)) {
                    playerData.set(playerUUID + ".specialDays." + specialDayKey, false);
                }
            }
            savePlayerData();
        }
        updateMissedDays(playerUUID);

        int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
        if (today.getMonthValue() != lastCheckInMonth) {
            playerData.set(playerUUID + ".checkedDays", new ArrayList<>());
            playerData.set(playerUUID + ".daysCheckedIn", 0);
            playerData.set(playerUUID + ".missedDays", new ArrayList<>());
            for (int daysRequired : new int[]{7, 14, 21}) {
                playerData.set(playerUUID + ".tichluy." + daysRequired, false);
            }
            savePlayerData();
        }

        LocalDate lastCheckInDate = LocalDate.parse(playerData.getString(playerUUID + ".lastCheckIn", "1970-01-01"));
        if (today.getYear() != lastCheckInDate.getYear()) {
            for (String key : playerData.getConfigurationSection(playerUUID + ".specialDays").getKeys(false)) {
                playerData.set(playerUUID + ".specialDays." + key, false);
            }
            savePlayerData();
        }
        List<Integer> checkedDays = playerData.getIntegerList(playerUUID + ".checkedDays");
        if (!checkedDays.contains(today.getDayOfMonth())) {
            TextComponent message = new TextComponent(getMessage("ChuaDiemDanhHomNay"));
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/diemdanh"));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(getMessage("Hover")).create()));
            player.spigot().sendMessage(message);
        }
    }

    private void updateMissedDays(String playerUUID) {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);


        if (currentMonth != lastCheckInMonth) {
            playerData.set(playerUUID + ".checkedDays", new ArrayList<>());
            playerData.set(playerUUID + ".daysCheckedIn", 0);
            playerData.set(playerUUID + ".lastCheckInMonth", currentMonth);
        }

        List<Integer> missedDays = playerData.getIntegerList(playerUUID + ".missedDays");
        List<Integer> checkedDays = playerData.getIntegerList(playerUUID + ".checkedDays");


        for (int day = 1; day < today.getDayOfMonth(); day++) {
            if (!checkedDays.contains(day) && !missedDays.contains(day)) {
                missedDays.add(day);
            }
        }

        playerData.set(playerUUID + ".missedDays", missedDays);
        savePlayerData();
    }

    private boolean isToday(int day) {
        LocalDate today = LocalDate.now();
        return day == today.getDayOfMonth();
    }

    private void markPlayerCheckedIn(String playerUUID, int day) {
        LocalDate today = LocalDate.now();
        String specialDayKey = getSpecialDayKey(today, day);

        if (specialDayKey != null) {
            playerData.set(playerUUID + ".specialDays." + specialDayKey, true);
        } else {
            playerData.set(playerUUID + ".lastCheckIn", LocalDate.now().toString());
            int currentMonth = LocalDate.now().getMonthValue();
            int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
            if (currentMonth != lastCheckInMonth) {

                playerData.set(playerUUID + ".checkedDays", new ArrayList<>());
                playerData.set(playerUUID + ".daysCheckedIn", 0);
            }
            List<Integer> checkedDays = playerData.getIntegerList(playerUUID + ".checkedDays");
            checkedDays.add(day);
            playerData.set(playerUUID + ".checkedDays", checkedDays);
        }
        int currentMonth = LocalDate.now().getMonthValue();
        int lastCheckInMonth = playerData.getInt(playerUUID + ".lastCheckInMonth", 0);
        int daysCheckedIn = playerData.getInt(playerUUID + ".daysCheckedIn", 0);
        if (currentMonth != lastCheckInMonth) {
            daysCheckedIn = 0;
        }
        daysCheckedIn++;
        playerData.set(playerUUID + ".daysCheckedIn", daysCheckedIn);
        playerData.set(playerUUID + ".lastCheckInMonth", currentMonth);

        LocalDate lastCheckInDate = LocalDate.parse(playerData.getString(playerUUID + ".lastCheckIn", "1970-01-01"));
        if (today.getYear() != lastCheckInDate.getYear()) {
            for (String key : playerData.getConfigurationSection(playerUUID + ".specialDays").getKeys(false)) {
                playerData.set(playerUUID + ".specialDays." + key, false);
            }
        }

        savePlayerData();
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
            ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + key + ".Require");
            int specialDayDate = specialDaySection.getInt("Date");
            int specialDayMonth = specialDaySection.getInt("Month");
            if (specialDayMonth < 1 || specialDayMonth > 12) {
                getLogger().warning("Invalid month for special day: " + key);
                continue;
            }
            if (day == specialDayDate && today.getMonthValue() == specialDayMonth) {
                return key;
            }
        }
        return null;
    }

    private String getSpecialDayItemKey(String playerUUID, String specialDayKey) {
        boolean hasCheckedIn = playerData.getBoolean(playerUUID + ".specialDays." + specialDayKey, false);
        ConfigurationSection specialDaySection = getConfig().getConfigurationSection("SpecialDay." + specialDayKey + ".Require");
        int specialDayDate = specialDaySection.getInt("Date");
        int specialDayMonth = specialDaySection.getInt("Month");

        LocalDate specialDay = LocalDate.of(LocalDate.now().getYear(), specialDayMonth, specialDayDate);

        if (hasCheckedIn) {
            return "DaDiemDanh";
        } else if (LocalDate.now().isEqual(specialDay)) {
            return "DiemDanh";
        } else if (LocalDate.now().isAfter(specialDay)) {
            if (playerData.getInt(playerUUID + ".tickets", 0) > 0) {
                return "DiemDanhBu";
            } else {
                return "ChuaDiemDanh";
            }
        } else {
            return "NgayDiemDanh";
        }
    }



}

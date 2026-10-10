package me.diemdanh.data;

import me.diemdanh.DiemDanh;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerDataManager {
    private final DiemDanh plugin;
    private final File dataFolder;
    private final Map<UUID, YamlConfiguration> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(DiemDanh plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        migrateOldPlayerDataFile();
    }

    private void migrateOldPlayerDataFile() {
        File oldFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!oldFile.exists() || oldFile.length() == 0) {
            return;
        }

        plugin.getLogger().info("Dang kiem tra va chuyen doi du lieu tu playerdata.yml cu sang thu muc playerdata/...");
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);
        Set<String> keys = oldConfig.getKeys(false);
        int migratedCount = 0;

        for (String key : keys) {
            try {
                UUID uuid = UUID.fromString(key);
                File singleFile = new File(dataFolder, uuid.toString() + ".yml");
                if (!singleFile.exists()) {
                    YamlConfiguration singleConfig = new YamlConfiguration();
                    ConfigurationSection section = oldConfig.getConfigurationSection(key);
                    if (section != null) {
                        for (String subKey : section.getKeys(true)) {
                            if (!section.isConfigurationSection(subKey)) {
                                singleConfig.set(subKey, section.get(subKey));
                            }
                        }
                    }
                    singleConfig.save(singleFile);
                    migratedCount++;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Loi khi chuyen doi du lieu cho key: " + key, e);
            }
        }

        File backupFile = new File(plugin.getDataFolder(), "playerdata.yml.bak");
        if (oldFile.renameTo(backupFile)) {
            plugin.getLogger().info("Da chuyen doi thanh cong " + migratedCount + " nguoi choi va doi ten file cu thanh playerdata.yml.bak!");
        } else {
            plugin.getLogger().warning("Da chuyen doi " + migratedCount + " nguoi choi nhung khong the doi ten playerdata.yml!");
        }
    }

    public synchronized YamlConfiguration getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            File file = new File(dataFolder, id.toString() + ".yml");
            if (file.exists()) {
                return YamlConfiguration.loadConfiguration(file);
            } else {
                return new YamlConfiguration();
            }
        });
    }

    public void savePlayerDataAsync(UUID uuid) {
        YamlConfiguration config = cache.get(uuid);
        if (config == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                savePlayerDataSync(uuid);
            }
        });
    }

    public synchronized void savePlayerDataSync(UUID uuid) {
        YamlConfiguration config = cache.get(uuid);
        if (config == null) return;

        File file = new File(dataFolder, uuid.toString() + ".yml");
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Khong the luu player data cho " + uuid, e);
        }
    }

    public synchronized void saveAllSync() {
        for (UUID uuid : cache.keySet()) {
            savePlayerDataSync(uuid);
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayerDataSync(uuid);
        cache.remove(uuid);
    }

    public Set<UUID> getAllTrackedUUIDs() {
        Set<UUID> uuids = new HashSet<>(cache.keySet());
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String rawUuid = name.substring(0, name.length() - 4);
                try {
                    uuids.add(UUID.fromString(rawUuid));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return uuids;
    }

    public boolean hasPlayerData(UUID uuid) {
        if (cache.containsKey(uuid)) return true;
        File file = new File(dataFolder, uuid.toString() + ".yml");
        return file.exists();
    }

    public void initPlayer(UUID uuid, String name) {
        YamlConfiguration data = getPlayerData(uuid);
        data.set("name", name);
        data.set("lastCheckIn", LocalDate.now().toString());
        data.set("daysCheckedIn", 0);
        data.set("totalDays", 0);
        data.set("lastCheckInMonth", LocalDate.now().getMonthValue());
        data.set("checkedDays", new ArrayList<Integer>());
        data.set("missedDays", new ArrayList<Integer>());
        data.set("tickets", 0);

        ConfigurationSection specialDaysSection = plugin.getConfig().getConfigurationSection("SpecialDay");
        if (specialDaysSection != null) {
            for (String specialDayKey : specialDaysSection.getKeys(false)) {
                data.set("specialDays." + specialDayKey, false);
            }
        }
        savePlayerDataAsync(uuid);
    }

    public String getPlayerName(UUID uuid) {
        return getPlayerData(uuid).getString("name", "Unknown");
    }

    public void setPlayerName(UUID uuid, String name) {
        getPlayerData(uuid).set("name", name);
    }

    public int getTickets(UUID uuid) {
        return getPlayerData(uuid).getInt("tickets", 0);
    }

    public void setTickets(UUID uuid, int tickets) {
        getPlayerData(uuid).set("tickets", tickets);
    }

    public void addTickets(UUID uuid, int amount) {
        setTickets(uuid, getTickets(uuid) + amount);
    }

    public int getDaysCheckedIn(UUID uuid) {
        return getPlayerData(uuid).getInt("daysCheckedIn", 0);
    }

    public void setDaysCheckedIn(UUID uuid, int days) {
        getPlayerData(uuid).set("daysCheckedIn", days);
    }

    public int getTotalDays(UUID uuid) {
        return getPlayerData(uuid).getInt("totalDays", 0);
    }

    public void setTotalDays(UUID uuid, int totalDays) {
        getPlayerData(uuid).set("totalDays", totalDays);
    }

    public int getLastCheckInMonth(UUID uuid) {
        return getPlayerData(uuid).getInt("lastCheckInMonth", 0);
    }

    public void setLastCheckInMonth(UUID uuid, int month) {
        getPlayerData(uuid).set("lastCheckInMonth", month);
    }

    public String getLastCheckIn(UUID uuid) {
        return getPlayerData(uuid).getString("lastCheckIn", "1970-01-01");
    }

    public void setLastCheckIn(UUID uuid, String date) {
        getPlayerData(uuid).set("lastCheckIn", date);
    }

    public List<Integer> getCheckedDays(UUID uuid) {
        return getPlayerData(uuid).getIntegerList("checkedDays");
    }

    public void setCheckedDays(UUID uuid, List<Integer> checkedDays) {
        getPlayerData(uuid).set("checkedDays", checkedDays);
    }

    public List<Integer> getMissedDays(UUID uuid) {
        return getPlayerData(uuid).getIntegerList("missedDays");
    }

    public void setMissedDays(UUID uuid, List<Integer> missedDays) {
        getPlayerData(uuid).set("missedDays", missedDays);
    }

    public boolean isSpecialDayChecked(UUID uuid, String specialDayKey) {
        return getPlayerData(uuid).getBoolean("specialDays." + specialDayKey, false);
    }

    public void setSpecialDayChecked(UUID uuid, String specialDayKey, boolean checked) {
        getPlayerData(uuid).set("specialDays." + specialDayKey, checked);
    }

    public void resetSpecialDays(UUID uuid) {
        ConfigurationSection specialSec = getPlayerData(uuid).getConfigurationSection("specialDays");
        if (specialSec != null) {
            for (String key : specialSec.getKeys(false)) {
                getPlayerData(uuid).set("specialDays." + key, false);
            }
        }
    }

    public boolean isTichLuyClaimed(UUID uuid, int daysRequired) {
        return getPlayerData(uuid).getBoolean("tichluy." + daysRequired + ".claimed", false);
    }

    public int getTichLuyMonth(UUID uuid, int daysRequired) {
        return getPlayerData(uuid).getInt("tichluy." + daysRequired + ".month", 0);
    }

    public void setTichLuyClaimed(UUID uuid, int daysRequired, boolean claimed, int month) {
        getPlayerData(uuid).set("tichluy." + daysRequired + ".claimed", claimed);
        getPlayerData(uuid).set("tichluy." + daysRequired + ".month", month);
    }
}

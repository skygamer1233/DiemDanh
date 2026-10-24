package me.diemdanh.data;

import me.diemdanh.DiemDanh;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class PlayerDataManager {
    private final DiemDanh plugin;
    private final File dataFolder;

    // Cache trong RAM chỉ chứa người chơi đang online hoặc vừa login (giải phóng khi quit)
    private final Map<UUID, YamlConfiguration> cache = new ConcurrentHashMap<>();

    // Thread đơn đảm bảo các tác vụ ghi file được xử lý theo đúng thứ tự FIFO, không chặn Main Thread
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DiemDanh-IO-Thread");
        t.setDaemon(true);
        return t;
    });

    // Index siêu nhẹ trong RAM phục vụ Leaderboard & PAPI cho người chơi offline (chỉ tốn vài KB RAM)
    public static class PlayerTopRecord {
        private final String name;
        private final int lastCheckInMonth;
        private final int daysCheckedIn;
        private final int totalDays;

        public PlayerTopRecord(String name, int lastCheckInMonth, int daysCheckedIn, int totalDays) {
            this.name = (name != null && !name.isEmpty()) ? name : "Unknown";
            this.lastCheckInMonth = lastCheckInMonth;
            this.daysCheckedIn = daysCheckedIn;
            this.totalDays = totalDays;
        }

        public String getName() { return name; }
        public int getLastCheckInMonth() { return lastCheckInMonth; }
        public int getDaysCheckedIn() { return daysCheckedIn; }
        public int getTotalDays() { return totalDays; }
    }

    private final Map<UUID, PlayerTopRecord> topIndex = new ConcurrentHashMap<>();

    public PlayerDataManager(DiemDanh plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        migrateOldPlayerDataFile();
        loadTopIndex();
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

    /**
     * Khởi tạo index nhẹ cho Leaderboard & PAPI offline.
     * Đọc trực tiếp dữ liệu từ file một lần khi bật server, KHÔNG nạp YamlConfiguration vào cache RAM.
     */
    public void loadTopIndex() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String fname = file.getName();
            try {
                UUID uuid = UUID.fromString(fname.substring(0, fname.length() - 4));
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String name = config.getString("name", "Unknown");
                int month = config.getInt("lastCheckInMonth", 0);
                int days = config.getInt("daysCheckedIn", 0);
                int total = config.getInt("totalDays", 0);
                topIndex.put(uuid, new PlayerTopRecord(name, month, days, total));
            } catch (Exception ignored) {}
        }
    }

    public Map<UUID, PlayerTopRecord> getTopIndex() {
        return topIndex;
    }

    public PlayerTopRecord getTopRecord(UUID uuid) {
        return topIndex.get(uuid);
    }

    public boolean isCached(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public YamlConfiguration getPlayerDataIfCached(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Lấy dữ liệu người chơi. Không dùng synchronized toàn cục.
     * ConcurrentHashMap.computeIfAbsent đảm bảo thread-safe và atomic.
     */
    public YamlConfiguration getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            File file = new File(dataFolder, id.toString() + ".yml");
            if (file.exists()) {
                return YamlConfiguration.loadConfiguration(file);
            } else {
                return new YamlConfiguration();
            }
        });
    }

    /**
     * Đọc xem trước (peek) dữ liệu từ file mà KHÔNG lưu vào RAM cache.
     */
    public YamlConfiguration peekPlayerData(UUID uuid) {
        YamlConfiguration cached = cache.get(uuid);
        if (cached != null) return cached;
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        return null;
    }

    /**
     * Lưu dữ liệu bất đồng bộ.
     * Chụp snapshot chuỗi YAML trên luồng gọi (Main thread) để tránh Race Condition,
     * sau đó đẩy chuỗi sang I/O Single-Thread Executor để ghi đĩa mà không chặn Main Thread.
     */
    public void savePlayerDataAsync(UUID uuid) {
        YamlConfiguration config = cache.get(uuid);
        if (config == null) return;

        // Cập nhật index nhẹ
        String name = config.getString("name", "Unknown");
        int month = config.getInt("lastCheckInMonth", 0);
        int days = config.getInt("daysCheckedIn", 0);
        int total = config.getInt("totalDays", 0);
        topIndex.put(uuid, new PlayerTopRecord(name, month, days, total));

        // Chụp snapshot YAML trên Main Thread
        String data;
        synchronized (config) {
            data = config.saveToString();
        }

        File file = new File(dataFolder, uuid.toString() + ".yml");
        ioExecutor.execute(() -> {
            try {
                Files.writeString(file.toPath(), data, StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Khong the luu player data cho " + uuid, e);
            }
        });
    }

    public void savePlayerDataSync(UUID uuid) {
        YamlConfiguration config = cache.get(uuid);
        if (config == null) return;

        String name = config.getString("name", "Unknown");
        int month = config.getInt("lastCheckInMonth", 0);
        int days = config.getInt("daysCheckedIn", 0);
        int total = config.getInt("totalDays", 0);
        topIndex.put(uuid, new PlayerTopRecord(name, month, days, total));

        String data;
        synchronized (config) {
            data = config.saveToString();
        }

        File file = new File(dataFolder, uuid.toString() + ".yml");
        try {
            Files.writeString(file.toPath(), data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Khong the luu player data cho " + uuid, e);
        }
    }

    /**
     * Giải phóng người chơi khỏi RAM khi thoát game.
     * Đẩy việc ghi đĩa vào hàng đợi I/O ngầm và xóa khỏi cache ngay lập tức.
     */
    public void unloadPlayer(UUID uuid) {
        savePlayerDataAsync(uuid);
        cache.remove(uuid);
    }

    /**
     * Dừng và lưu toàn bộ dữ liệu an toàn khi tắt server.
     */
    public void shutdown() {
        for (UUID uuid : cache.keySet()) {
            YamlConfiguration config = cache.get(uuid);
            if (config != null) {
                String data;
                synchronized (config) {
                    data = config.saveToString();
                }
                File file = new File(dataFolder, uuid.toString() + ".yml");
                ioExecutor.execute(() -> {
                    try {
                        Files.writeString(file.toPath(), data, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Khong the luu player data cho " + uuid, e);
                    }
                });
            }
        }
        cache.clear();

        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void saveAllSync() {
        shutdown();
    }

    public Set<UUID> getAllTrackedUUIDs() {
        Set<UUID> uuids = new HashSet<>(topIndex.keySet());
        uuids.addAll(cache.keySet());
        return uuids;
    }

    public boolean hasPlayerData(UUID uuid) {
        if (cache.containsKey(uuid) || topIndex.containsKey(uuid)) return true;
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

        topIndex.put(uuid, new PlayerTopRecord(name, LocalDate.now().getMonthValue(), 0, 0));
        savePlayerDataAsync(uuid);
    }

    public String getPlayerName(UUID uuid) {
        YamlConfiguration cached = cache.get(uuid);
        if (cached != null) {
            return cached.getString("name", "Unknown");
        }
        PlayerTopRecord rec = topIndex.get(uuid);
        return rec != null ? rec.getName() : "Unknown";
    }

    public void setPlayerName(UUID uuid, String name) {
        getPlayerData(uuid).set("name", name);
        updateTopIndexOnly(uuid, name, null, null, null);
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
        YamlConfiguration cached = cache.get(uuid);
        if (cached != null) {
            return cached.getInt("daysCheckedIn", 0);
        }
        PlayerTopRecord rec = topIndex.get(uuid);
        return rec != null ? rec.getDaysCheckedIn() : 0;
    }

    public void setDaysCheckedIn(UUID uuid, int days) {
        getPlayerData(uuid).set("daysCheckedIn", days);
        updateTopIndexOnly(uuid, null, null, days, null);
    }

    public int getTotalDays(UUID uuid) {
        YamlConfiguration cached = cache.get(uuid);
        if (cached != null) {
            return cached.getInt("totalDays", 0);
        }
        PlayerTopRecord rec = topIndex.get(uuid);
        return rec != null ? rec.getTotalDays() : 0;
    }

    public void setTotalDays(UUID uuid, int totalDays) {
        getPlayerData(uuid).set("totalDays", totalDays);
        updateTopIndexOnly(uuid, null, null, null, totalDays);
    }

    public int getLastCheckInMonth(UUID uuid) {
        YamlConfiguration cached = cache.get(uuid);
        if (cached != null) {
            return cached.getInt("lastCheckInMonth", 0);
        }
        PlayerTopRecord rec = topIndex.get(uuid);
        return rec != null ? rec.getLastCheckInMonth() : 0;
    }

    public void setLastCheckInMonth(UUID uuid, int month) {
        getPlayerData(uuid).set("lastCheckInMonth", month);
        updateTopIndexOnly(uuid, null, month, null, null);
    }

    private void updateTopIndexOnly(UUID uuid, String name, Integer month, Integer days, Integer total) {
        PlayerTopRecord old = topIndex.get(uuid);
        String finalName = name != null ? name : (old != null ? old.getName() : "Unknown");
        int finalMonth = month != null ? month : (old != null ? old.getLastCheckInMonth() : 0);
        int finalDays = days != null ? days : (old != null ? old.getDaysCheckedIn() : 0);
        int finalTotal = total != null ? total : (old != null ? old.getTotalDays() : 0);
        topIndex.put(uuid, new PlayerTopRecord(finalName, finalMonth, finalDays, finalTotal));
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

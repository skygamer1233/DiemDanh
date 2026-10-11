package me.diemdanh.bedrock;

import me.diemdanh.ColorUtil;
import me.diemdanh.DiemDanh;
import me.diemdanh.DiemDanhTop;
import me.diemdanh.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FloodgateBedrockService implements BedrockService {

    private final DiemDanh plugin;
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();

    public FloodgateBedrockService(DiemDanh plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getFormConfig() {
        return plugin.getBedrockFormConfig();
    }

    private String getFormString(String path, String def) {
        String val = getFormConfig().getString(path, def);
        return ColorUtil.translate(val != null ? val : def);
    }

    private List<String> getFormStringList(String path, List<String> def) {
        List<String> list = getFormConfig().getStringList(path);
        if (list == null || list.isEmpty()) {
            return def;
        }
        return list;
    }

    @Override
    public boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean checkCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        long cooldownMs = getFormConfig().getLong("Settings.ClickCooldownMillis", 350L);
        Long last = clickCooldowns.get(uuid);
        if (last != null && now - last < cooldownMs) {
            return true;
        }
        clickCooldowns.put(uuid, now);
        return false;
    }

    @Override
    public void openMainMenu(Player player) {
        if (player == null || !player.isOnline()) return;

        LocalDate today = LocalDate.now();
        UUID uuid = player.getUniqueId();
        PlayerDataManager dataMgr = plugin.getPlayerDataManager();

        int currentDay = today.getDayOfMonth();
        int monthDays = plugin.getDiemDanhGUI().getDaysCheckedInThisMonth(uuid);
        int totalDays = dataMgr.getTotalDays(uuid);
        int tickets = dataMgr.getTickets(uuid);
        List<Integer> missedDays = dataMgr.getMissedDays(uuid);

        String specialKey = plugin.getDiemDanhGUI().getSpecialDayKey(today, currentDay);
        String dayStatusKey = (specialKey != null)
                ? plugin.getDiemDanhGUI().getSpecialDayItemKey(uuid, specialKey)
                : plugin.getDiemDanhGUI().getItemKeyForDay(currentDay, uuid);

        boolean isTodayChecked = dayStatusKey.equals("DaDiemDanh");

        // Status string
        String todayStatusStr = isTodayChecked
                ? getFormString("MainMenu.Status.CheckedIn", "&a✔ Đã điểm danh")
                : getFormString("MainMenu.Status.NotCheckedIn", "&c✖ Chưa điểm danh");
        if (specialKey != null) {
            String suffix = getFormString("MainMenu.Status.SpecialDaySuffix", " &6(Sự kiện: %special_day%)");
            todayStatusStr += suffix.replace("%special_day%", specialKey);
        }

        // Build content from diemdanhform.yml
        List<String> rawContent = getFormStringList("MainMenu.Content", Arrays.asList(
                "&7--------------------------------",
                "&e&lXin chào: &f%player%",
                "&a&lHôm nay: &fNgày %day% tháng %month%/%year%",
                "&b&lTích lũy tháng: &f%month_days% ngày",
                "&6&lTổng điểm danh: &f%total_days% ngày",
                "&e&lVé điểm danh bù: &f%tickets% vé",
                "&d&lTrạng thái hôm nay: %today_status%",
                "&7--------------------------------"
        ));

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < rawContent.size(); i++) {
            String line = rawContent.get(i)
                    .replace("%player%", player.getName())
                    .replace("%day%", String.valueOf(currentDay))
                    .replace("%month%", String.valueOf(today.getMonthValue()))
                    .replace("%year%", String.valueOf(today.getYear()))
                    .replace("%month_days%", String.valueOf(monthDays))
                    .replace("%total_days%", String.valueOf(totalDays))
                    .replace("%tickets%", String.valueOf(tickets))
                    .replace("%today_status%", todayStatusStr);
            content.append(ColorUtil.translate(line));
            if (i < rawContent.size() - 1) {
                content.append("\n");
            }
        }

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(getFormString("MainMenu.Title", "&a&lĐIỂM DANH HÀNG NGÀY"))
                .content(content.toString());

        // Buttons
        if (isTodayChecked) {
            builder.button(getFormString("MainMenu.Buttons.AlreadyCheckedIn", "&7&l✔ Đã Điểm Danh Hôm Nay"));
        } else {
            builder.button(getFormString("MainMenu.Buttons.TodayCheckIn", "&a&l✔ Điểm Danh Hôm Nay"));
        }

        builder.button(getFormString("MainMenu.Buttons.MissedDays", "&e&l✎ Điểm Danh Bù (%count% ngày)")
                .replace("%count%", String.valueOf(missedDays.size())));

        builder.button(getFormString("MainMenu.Buttons.Milestones", "&6&l🎁 Quà Mốc Tích Lũy"));
        builder.button(getFormString("MainMenu.Buttons.Leaderboard", "&b&l🏆 Bảng Xếp Hạng"));
        builder.button(getFormString("MainMenu.Buttons.Calendar", "&d&l📅 Chi Tiết Lịch Tháng"));
        builder.button(getFormString("MainMenu.Buttons.ChestGui", "&e&l📦 Mở Giao Diện Rương (Java)"));
        builder.button(getFormString("MainMenu.Buttons.Close", "&c&l✖ Đóng"));

        builder.validResultHandler(response -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (checkCooldown(uuid)) return;

                int buttonId = response.clickedButtonId();
                switch (buttonId) {
                    case 0 -> handleTodayCheckIn(player);
                    case 1 -> openMissedDaysMenu(player);
                    case 2 -> openMilestoneMenu(player);
                    case 3 -> openLeaderboardMenu(player);
                    case 4 -> openCalendarDetailMenu(player);
                    case 5 -> plugin.getDiemDanhGUI().openDiemDanhGUI(player);
                    default -> {}
                }
            });
        });

        FloodgateApi.getInstance().sendForm(uuid, builder.build());
    }

    private void handleTodayCheckIn(Player player) {
        LocalDate today = LocalDate.now();
        UUID uuid = player.getUniqueId();
        int day = today.getDayOfMonth();

        String specialDayKey = plugin.getDiemDanhGUI().getSpecialDayKey(today, day);
        String originalItemKey = (specialDayKey != null)
                ? plugin.getDiemDanhGUI().getSpecialDayItemKey(uuid, specialDayKey)
                : plugin.getDiemDanhGUI().getItemKeyForDay(day, uuid);

        if (originalItemKey.equals("DiemDanh")) {
            ConfigurationSection rewardSection = (specialDayKey != null)
                    ? plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                    : plugin.getConfig().getConfigurationSection("Days." + day);

            if (rewardSection != null) {
                plugin.getDiemDanhGUI().executeCommands(rewardSection.getStringList("Reward"), player);
            }

            plugin.getDiemDanhGUI().markPlayerCheckedIn(uuid, day, false);

            String messageKey = (specialDayKey != null) ? "SpecialDayDiemDanhThanhCong" : "DiemDanh";
            player.sendMessage(ColorUtil.translate(plugin.getConfig().getString("Message." + messageKey, plugin.getMessage("DiemDanh"))
                    .replace("%day%", String.valueOf(day))
                    .replace("%specialDay%", specialDayKey != null ? specialDayKey : "")));

            openMainMenu(player);

        } else if (originalItemKey.equals("DaDiemDanh")) {
            if (specialDayKey != null) {
                player.sendMessage(ColorUtil.translate(plugin.getConfig().getString("Message.SpecialDayDaDiemDanh", "&cBạn đã điểm danh ngày lễ này rồi!")));
            } else {
                player.sendMessage(plugin.getMessage("DaDiemDanh"));
            }
            openMainMenu(player);
        } else {
            player.sendMessage(plugin.getMessage("NgayDiemDanh").replace("%day%", String.valueOf(day)));
            openMainMenu(player);
        }
    }

    @Override
    public void openMissedDaysMenu(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        PlayerDataManager dataMgr = plugin.getPlayerDataManager();
        int tickets = dataMgr.getTickets(uuid);
        List<Integer> missedDays = new ArrayList<>(dataMgr.getMissedDays(uuid));
        Collections.sort(missedDays);

        StringBuilder content = new StringBuilder();

        // Header
        List<String> header = getFormStringList("MissedDaysMenu.Header", Arrays.asList(
                "&7--------------------------------",
                "&e&lVé điểm danh bù hiện có: &a%tickets% vé"
        ));
        for (String h : header) {
            content.append(ColorUtil.translate(h.replace("%tickets%", String.valueOf(tickets)))).append("\n");
        }

        if (missedDays.isEmpty()) {
            content.append(getFormString("MissedDaysMenu.NoMissedDays", "&aBạn không có ngày nào bị bỏ lỡ trong tháng này!")).append("\n");
        } else if (tickets <= 0) {
            List<String> noTickets = getFormStringList("MissedDaysMenu.NoTicketsWarning", Arrays.asList(
                    "&cBạn không có đủ vé điểm danh bù để bù ngày!",
                    "&7(Nhận vé từ quản trị viên hoặc sự kiện)"
            ));
            for (String nt : noTickets) {
                content.append(ColorUtil.translate(nt)).append("\n");
            }
        } else {
            content.append(getFormString("MissedDaysMenu.HasMissedDays", "&fChọn một ngày bên dưới để tiến hành điểm danh bù (Tốn 1 vé):")).append("\n");
        }

        // Footer
        List<String> footer = getFormStringList("MissedDaysMenu.Footer", Collections.singletonList("&7--------------------------------"));
        for (int i = 0; i < footer.size(); i++) {
            content.append(ColorUtil.translate(footer.get(i)));
            if (i < footer.size() - 1) content.append("\n");
        }

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(getFormString("MissedDaysMenu.Title", "&e&lĐIỂM DANH BÙ"))
                .content(content.toString());

        String buttonFormat = getFormString("MissedDaysMenu.ButtonFormat", "&eNgày %day% &7(Bấm để bù)");
        List<Integer> buttonDayMap = new ArrayList<>();
        for (int day : missedDays) {
            builder.button(buttonFormat.replace("%day%", String.valueOf(day)));
            buttonDayMap.add(day);
        }

        builder.button(getFormString("MissedDaysMenu.BackButton", "&c&l◀ Quay Lại"));

        builder.validResultHandler(response -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (checkCooldown(uuid)) return;

                int buttonId = response.clickedButtonId();
                if (buttonId < buttonDayMap.size()) {
                    int targetDay = buttonDayMap.get(buttonId);
                    handleMissedDayCheckIn(player, targetDay);
                } else {
                    openMainMenu(player);
                }
            });
        });

        FloodgateApi.getInstance().sendForm(uuid, builder.build());
    }

    private void handleMissedDayCheckIn(Player player, int day) {
        UUID uuid = player.getUniqueId();
        PlayerDataManager dataMgr = plugin.getPlayerDataManager();
        int tickets = dataMgr.getTickets(uuid);
        List<Integer> missedDays = new ArrayList<>(dataMgr.getMissedDays(uuid));

        if (tickets > 0 && missedDays.contains(day)) {
            LocalDate today = LocalDate.now();
            String specialDayKey = plugin.getDiemDanhGUI().getSpecialDayKey(today, day);
            ConfigurationSection rewardSection = (specialDayKey != null)
                    ? plugin.getConfig().getConfigurationSection("SpecialDay." + specialDayKey)
                    : plugin.getConfig().getConfigurationSection("Days." + day);

            if (rewardSection != null) {
                plugin.getDiemDanhGUI().executeCommands(rewardSection.getStringList("Reward"), player);
            }

            dataMgr.setTickets(uuid, tickets - 1);
            missedDays.remove(Integer.valueOf(day));
            dataMgr.setMissedDays(uuid, missedDays);

            plugin.getDiemDanhGUI().markPlayerCheckedIn(uuid, day, true);

            player.sendMessage(plugin.getMessage("DiemDanh").replace("%day%", String.valueOf(day)));
            openMissedDaysMenu(player);
        } else {
            player.sendMessage(plugin.getMessage("NotRequire"));
            openMissedDaysMenu(player);
        }
    }

    @Override
    public void openMilestoneMenu(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        int monthDays = plugin.getDiemDanhGUI().getDaysCheckedInThisMonth(uuid);

        List<String> rawContent = getFormStringList("MilestoneMenu.Content", Arrays.asList(
                "&7--------------------------------",
                "&a&lSố ngày tích lũy tháng này: &e%month_days% ngày",
                "&fĐạt các mốc tích lũy bên dưới để nhận phần thưởng đặc biệt!",
                "&7--------------------------------"
        ));

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < rawContent.size(); i++) {
            content.append(ColorUtil.translate(rawContent.get(i).replace("%month_days%", String.valueOf(monthDays))));
            if (i < rawContent.size() - 1) content.append("\n");
        }

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(getFormString("MilestoneMenu.Title", "&6&lQUÀ MỐC TÍCH LŨY"))
                .content(content.toString());

        int[] milestones = {7, 14, 21};
        for (int m : milestones) {
            String status = plugin.getDiemDanhGUI().getTichLuyItemKey(uuid, m);
            String btnText;
            if (status.equals("NhanQua")) {
                btnText = getFormString("MilestoneMenu.Buttons.Claimable", "&a&l🎁 Mốc %days% Ngày &f[BẤM ĐỂ NHẬN]");
            } else if (status.equals("DaNhanQua")) {
                btnText = getFormString("MilestoneMenu.Buttons.Claimed", "&7&l✔ Mốc %days% Ngày &8[ĐÃ NHẬN]");
            } else {
                btnText = getFormString("MilestoneMenu.Buttons.Locked", "&c&l🔒 Mốc %days% Ngày &7[%month_days%/%days% Ngày]");
            }
            btnText = btnText.replace("%days%", String.valueOf(m)).replace("%month_days%", String.valueOf(monthDays));
            builder.button(btnText);
        }

        builder.button(getFormString("MilestoneMenu.Buttons.BackButton", "&c&l◀ Quay Lại"));

        builder.validResultHandler(response -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (checkCooldown(uuid)) return;

                int buttonId = response.clickedButtonId();
                if (buttonId >= 0 && buttonId < milestones.length) {
                    int milestone = milestones[buttonId];
                    handleMilestoneClaim(player, milestone);
                } else {
                    openMainMenu(player);
                }
            });
        });

        FloodgateApi.getInstance().sendForm(uuid, builder.build());
    }

    private void handleMilestoneClaim(Player player, int daysRequired) {
        UUID uuid = player.getUniqueId();
        String originalItemKey = plugin.getDiemDanhGUI().getTichLuyItemKey(uuid, daysRequired);
        LocalDate today = LocalDate.now();

        if (originalItemKey.equals("NhanQua")) {
            ConfigurationSection rewardSection = plugin.getConfig().getConfigurationSection("TichLuy." + daysRequired + "ngay");
            if (rewardSection != null) {
                plugin.getDiemDanhGUI().executeCommands(rewardSection.getStringList("Reward"), player);
                plugin.getPlayerDataManager().setTichLuyClaimed(uuid, daysRequired, true, today.getMonthValue());
                plugin.getPlayerDataManager().savePlayerDataAsync(uuid);
                player.sendMessage(ColorUtil.translate(plugin.getConfig().getString("Message.TichLuySuccess", "&aBạn đã nhận quà tích lũy %days% ngày thành công!").replace("%days%", String.valueOf(daysRequired))));
            }
        } else if (originalItemKey.equals("DaNhanQua")) {
            player.sendMessage(plugin.getMessage("IsClaimed"));
        } else {
            player.sendMessage(plugin.getMessage("NotRequire"));
        }

        openMilestoneMenu(player);
    }

    @Override
    public void openLeaderboardMenu(Player player) {
        if (player == null || !player.isOnline()) return;

        DiemDanhTop top = plugin.getDiemDanhTop();
        List<DiemDanhTop.TopEntry> monthTop = top.getCachedMonthTop();
        List<DiemDanhTop.TopEntry> totalTop = top.getCachedTotalTop();

        String entryFormat = getFormString("LeaderboardMenu.EntryFormat", "%medal%&e%player%&7: &a%days% ngày");
        String top1Medal = getFormString("LeaderboardMenu.Medals.Top1", "&6#1. ");
        String top2Medal = getFormString("LeaderboardMenu.Medals.Top2", "&f#2. ");
        String top3Medal = getFormString("LeaderboardMenu.Medals.Top3", "&c#3. ");
        String defaultMedal = getFormString("LeaderboardMenu.Medals.Default", "&7#%rank%. ");

        StringBuilder content = new StringBuilder();
        content.append(getFormString("LeaderboardMenu.MonthHeader", "&e&l=== BẢNG XẾP HẠNG THÁNG ===&r")).append("\n");

        if (monthTop.isEmpty()) {
            content.append(getFormString("LeaderboardMenu.MonthEmpty", "&7Chưa có dữ liệu xếp hạng tháng.")).append("\n");
        } else {
            int limit = Math.min(10, monthTop.size());
            for (int i = 0; i < limit; i++) {
                DiemDanhTop.TopEntry entry = monthTop.get(i);
                String medal = switch (i) {
                    case 0 -> top1Medal;
                    case 1 -> top2Medal;
                    case 2 -> top3Medal;
                    default -> defaultMedal.replace("%rank%", String.valueOf(i + 1));
                };
                String line = entryFormat
                        .replace("%medal%", medal)
                        .replace("%player%", entry.getName())
                        .replace("%days%", String.valueOf(entry.getMonthDays()));
                content.append(ColorUtil.translate(line)).append("\n");
            }
        }

        content.append("\n").append(getFormString("LeaderboardMenu.TotalHeader", "&6&l=== BẢNG XẾP HẠNG TỔNG ===&r")).append("\n");
        if (totalTop.isEmpty()) {
            content.append(getFormString("LeaderboardMenu.TotalEmpty", "&7Chưa có dữ liệu xếp hạng tổng.")).append("\n");
        } else {
            int limit = Math.min(10, totalTop.size());
            for (int i = 0; i < limit; i++) {
                DiemDanhTop.TopEntry entry = totalTop.get(i);
                String medal = switch (i) {
                    case 0 -> top1Medal;
                    case 1 -> top2Medal;
                    case 2 -> top3Medal;
                    default -> defaultMedal.replace("%rank%", String.valueOf(i + 1));
                };
                String line = entryFormat
                        .replace("%medal%", medal)
                        .replace("%player%", entry.getName())
                        .replace("%days%", String.valueOf(entry.getTotalDays()));
                content.append(ColorUtil.translate(line)).append("\n");
            }
        }

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(getFormString("LeaderboardMenu.Title", "&b&lBẢNG XẾP HẠNG"))
                .content(content.toString())
                .button(getFormString("LeaderboardMenu.Buttons.Refresh", "&a&l🔄 Làm Mới"))
                .button(getFormString("LeaderboardMenu.Buttons.BackButton", "&c&l◀ Quay Lại"));

        builder.validResultHandler(response -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (checkCooldown(player.getUniqueId())) return;

                int buttonId = response.clickedButtonId();
                if (buttonId == 0) {
                    top.refreshCache();
                    openLeaderboardMenu(player);
                } else {
                    openMainMenu(player);
                }
            });
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }

    @Override
    public void openCalendarDetailMenu(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        LocalDate today = LocalDate.now();
        int todayDay = today.getDayOfMonth();
        int maxDays = YearMonth.now().lengthOfMonth();

        PlayerDataManager dataMgr = plugin.getPlayerDataManager();
        List<Integer> checkedDays = dataMgr.getCheckedDays(uuid);
        List<Integer> missedDays = dataMgr.getMissedDays(uuid);

        String header = getFormString("CalendarDetailMenu.Header", "&7Chi tiết lịch điểm danh tháng %month%/%year%:\n")
                .replace("%month%", String.valueOf(today.getMonthValue()))
                .replace("%year%", String.valueOf(today.getYear()));

        StringBuilder content = new StringBuilder(header);

        String fmtChecked = getFormString("CalendarDetailMenu.Format.CheckedIn", "&fNgày %day%: &a✔ Đã điểm danh");
        String fmtToday = getFormString("CalendarDetailMenu.Format.Today", "&fNgày %day%: &e⭐ Hôm nay (Chưa điểm danh)");
        String fmtMissed = getFormString("CalendarDetailMenu.Format.Missed", "&fNgày %day%: &c✖ Bỏ lỡ (Có thể điểm danh bù)");
        String fmtLocked = getFormString("CalendarDetailMenu.Format.Locked", "&fNgày %day%: &8🔒 Chưa tới ngày");
        String fmtDefault = getFormString("CalendarDetailMenu.Format.Default", "&fNgày %day%: &7- Chưa điểm danh");
        String suffixSpecial = getFormString("CalendarDetailMenu.Format.SpecialDaySuffix", " &6[Sự kiện: %special_day%]");

        for (int day = 1; day <= maxDays; day++) {
            String dayStr = (day < 10 ? "0" + day : String.valueOf(day));
            String line;

            if (checkedDays.contains(day)) {
                line = fmtChecked.replace("%day%", dayStr);
            } else if (day == todayDay) {
                line = fmtToday.replace("%day%", dayStr);
            } else if (missedDays.contains(day)) {
                line = fmtMissed.replace("%day%", dayStr);
            } else if (day > todayDay) {
                line = fmtLocked.replace("%day%", dayStr);
            } else {
                line = fmtDefault.replace("%day%", dayStr);
            }

            String specialKey = plugin.getDiemDanhGUI().getSpecialDayKey(today, day);
            if (specialKey != null) {
                line += suffixSpecial.replace("%special_day%", specialKey);
            }

            content.append(ColorUtil.translate(line)).append("\n");
        }

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(getFormString("CalendarDetailMenu.Title", "&d&lCHI TIẾT LỊCH THÁNG"))
                .content(content.toString())
                .button(getFormString("CalendarDetailMenu.BackButton", "&c&l◀ Quay Lại"));

        builder.validResultHandler(response -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (checkCooldown(uuid)) return;
                openMainMenu(player);
            });
        });

        FloodgateApi.getInstance().sendForm(uuid, builder.build());
    }
}

package me.diemdanh.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.diemdanh.DiemDanh;
import me.diemdanh.DiemDanhTop;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class DiemDanhExpansion extends PlaceholderExpansion {
    private final DiemDanh plugin;

    public DiemDanhExpansion(DiemDanh plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "diemdanh";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SkyGamer";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        // --- 1. Placeholders Top Bảng Xếp Hạng ---
        if (params.toLowerCase().startsWith("top_month_")) {
            String[] parts = params.split("_");
            if (parts.length >= 4) {
                try {
                    int rank = Integer.parseInt(parts[2]);
                    String type = parts[3].toLowerCase();
                    List<DiemDanhTop.TopEntry> topList = plugin.getDiemDanhTop().getCachedMonthTop();
                    if (rank >= 1 && rank <= topList.size()) {
                        DiemDanhTop.TopEntry entry = topList.get(rank - 1);
                        if (type.equals("name")) return entry.getName();
                        if (type.equals("days")) return String.valueOf(entry.getMonthDays());
                    } else {
                        if (type.equals("name")) return "Chưa có";
                        if (type.equals("days")) return "0";
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (params.toLowerCase().startsWith("top_total_")) {
            String[] parts = params.split("_");
            if (parts.length >= 4) {
                try {
                    int rank = Integer.parseInt(parts[2]);
                    String type = parts[3].toLowerCase();
                    List<DiemDanhTop.TopEntry> topList = plugin.getDiemDanhTop().getCachedTotalTop();
                    if (rank >= 1 && rank <= topList.size()) {
                        DiemDanhTop.TopEntry entry = topList.get(rank - 1);
                        if (type.equals("name")) return entry.getName();
                        if (type.equals("days")) return String.valueOf(entry.getTotalDays());
                    } else {
                        if (type.equals("name")) return "Chưa có";
                        if (type.equals("days")) return "0";
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // --- 2. Placeholders của Người Chơi ---
        if (offlinePlayer == null) {
            return "";
        }

        UUID uuid = offlinePlayer.getUniqueId();
        LocalDate today = LocalDate.now();

        if (params.equalsIgnoreCase("days") || params.equalsIgnoreCase("month_days")) {
            int lastCheckInMonth = plugin.getPlayerDataManager().getLastCheckInMonth(uuid);
            return String.valueOf(lastCheckInMonth == today.getMonthValue() ? plugin.getPlayerDataManager().getDaysCheckedIn(uuid) : 0);
        }

        if (params.equalsIgnoreCase("totaldays") || params.equalsIgnoreCase("total_days")) {
            return String.valueOf(plugin.getPlayerDataManager().getTotalDays(uuid));
        }

        if (params.equalsIgnoreCase("tickets") || params.equalsIgnoreCase("ticket")) {
            return String.valueOf(plugin.getPlayerDataManager().getTickets(uuid));
        }

        if (params.equalsIgnoreCase("is_checked_today")) {
            List<Integer> checkedDays = plugin.getPlayerDataManager().getCheckedDays(uuid);
            return checkedDays.contains(today.getDayOfMonth()) ? "true" : "false";
        }

        if (params.equalsIgnoreCase("checked_today")) {
            List<Integer> checkedDays = plugin.getPlayerDataManager().getCheckedDays(uuid);
            return checkedDays.contains(today.getDayOfMonth()) ? "&aĐã điểm danh" : "&cChưa điểm danh";
        }

        return null;
    }
}

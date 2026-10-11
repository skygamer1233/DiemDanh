package me.diemdanh.bedrock;

import me.diemdanh.DiemDanh;
import org.bukkit.entity.Player;

public class DummyBedrockService implements BedrockService {
    private final DiemDanh plugin;

    public DummyBedrockService(DiemDanh plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isBedrockPlayer(Player player) {
        return false;
    }

    @Override
    public void openMainMenu(Player player) {
        plugin.getDiemDanhGUI().openDiemDanhGUI(player);
    }

    @Override
    public void openMissedDaysMenu(Player player) {
        plugin.getDiemDanhGUI().openDiemDanhGUI(player);
    }

    @Override
    public void openMilestoneMenu(Player player) {
        plugin.getDiemDanhGUI().openDiemDanhGUI(player);
    }

    @Override
    public void openLeaderboardMenu(Player player) {
        plugin.getDiemDanhTop().openTopDiemDanhGUI(player);
    }

    @Override
    public void openCalendarDetailMenu(Player player) {
        plugin.getDiemDanhGUI().openDiemDanhGUI(player);
    }
}

package me.diemdanh.bedrock;

import org.bukkit.entity.Player;

public interface BedrockService {
    boolean isBedrockPlayer(Player player);
    void openMainMenu(Player player);
    void openMissedDaysMenu(Player player);
    void openMilestoneMenu(Player player);
    void openLeaderboardMenu(Player player);
    void openCalendarDetailMenu(Player player);
    default void clearCooldown(java.util.UUID uuid) {}
}

package me.diemdanh.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class DiemDanhEditorHolder implements InventoryHolder {
    public enum MenuType {
        MAIN,
        DAYS,
        TICHLUY,
        SPECIAL,
        COMMAND
    }

    private final MenuType menuType;
    private final String extraData;

    public DiemDanhEditorHolder(MenuType menuType) {
        this(menuType, "");
    }

    public DiemDanhEditorHolder(MenuType menuType, String extraData) {
        this.menuType = menuType;
        this.extraData = extraData != null ? extraData : "";
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public String getExtraData() {
        return extraData;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}

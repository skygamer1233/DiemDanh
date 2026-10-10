package me.diemdanh.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class DiemDanhTopHolder implements InventoryHolder {
    public enum TopType {
        MONTH,
        TOTAL
    }

    private final TopType type;

    public DiemDanhTopHolder(TopType type) {
        this.type = type;
    }

    public TopType getType() {
        return type;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}

package me.diemdanh;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class InventoryUtil {
    private static MethodHandle GET_HOLDER_METHOD;

    static {
        try {
            Method m = Inventory.class.getMethod("getHolder", boolean.class);
            GET_HOLDER_METHOD = MethodHandles.lookup().unreflect(m);
        } catch (Throwable ignored) {
            GET_HOLDER_METHOD = null;
        }
    }

    /**
     * Lấy InventoryHolder an toàn và tối ưu hiệu năng.
     * Trên Paper/Folia: gọi getHolder(false) để tránh tạo BlockState snapshot tốn tài nguyên.
     * Trên Spigot tiêu chuẩn: fallback về getHolder() mặc định.
     */
    public static InventoryHolder getHolder(Inventory inventory) {
        if (inventory == null) return null;
        if (GET_HOLDER_METHOD != null) {
            try {
                return (InventoryHolder) GET_HOLDER_METHOD.invokeExact(inventory, false);
            } catch (Throwable ignored) {}
        }
        return inventory.getHolder();
    }
}

package com.example.addon.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class RocketManager {
    private int previousSlot = -1;

    public void reset() {
        previousSlot = -1;
    }

    public boolean hasRocket(MinecraftClient mc) {
        return findRocketSlot(mc) != -1;
    }

    public int findRocketSlot(MinecraftClient mc) {
        if (mc.player == null) return -1;

        for (int slot = 0; slot < 9; slot++) {
            if (
                mc.player.getInventory()
                    .getStack(slot)
                    .isOf(Items.FIREWORK_ROCKET)
            ) {
                return slot;
            }
        }

        return -1;
    }

    public void rememberSelectedSlot(MinecraftClient mc) {
        if (mc.player == null) return;

        previousSlot =
            mc.player.getInventory().getSelectedSlot();
    }

    public void restoreSelectedSlot(MinecraftClient mc) {
        if (mc.player == null) {
            previousSlot = -1;
            return;
        }

        if (previousSlot >= 0 && previousSlot < 9) {
            mc.player.getInventory()
                .setSelectedSlot(previousSlot);
        }

        previousSlot = -1;
    }

    public boolean selectRocket(MinecraftClient mc) {
        if (mc.player == null) return false;

        int rocketSlot = findRocketSlot(mc);

        if (rocketSlot == -1) return false;

        mc.player.getInventory()
            .setSelectedSlot(rocketSlot);

        return true;
    }

    public boolean boost(MinecraftClient mc) {
        if (
            mc.player == null
                || mc.interactionManager == null
        ) {
            return false;
        }

        if (!mc.player.isGliding()) {
            return false;
        }

        int rocketSlot = findRocketSlot(mc);

        if (rocketSlot == -1) {
            return false;
        }

        rememberSelectedSlot(mc);

        try {
            mc.player.getInventory()
                .setSelectedSlot(rocketSlot);

            mc.interactionManager.interactItem(
                mc.player,
                Hand.MAIN_HAND
            );

            return true;
        } finally {
            restoreSelectedSlot(mc);
        }
    }

    public int getPreviousSlot() {
        return previousSlot;
    }
}

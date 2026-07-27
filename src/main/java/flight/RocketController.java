package com.example.addon.flight;

import com.example.addon.inventory.RocketManager;
import net.minecraft.client.MinecraftClient;

public class RocketController {
    private static final double MAX_SAFE_SPEED = 38.0;
    private static final double HARD_TURN_LIMIT = 45.0;
    private static final double CLOSE_DISTANCE = 40.0;

    private int boostCooldown;

    public void reset() {
        boostCooldown = 0;
    }

    public boolean tick(
        MinecraftClient mc,
        FlightAnalyzer analyzer,
        RocketManager rocketManager,
        boolean autoBoostEnabled,
        int boostThreshold,
        int cooldownTicks
    ) {
        updateCooldown();

        if (!autoBoostEnabled) {
            return false;
        }

        if (mc.player == null) {
            return false;
        }

        if (!mc.player.isGliding()) {
            return false;
        }

        if (!ready()) {
            return false;
        }

        if (!rocketManager.hasRocket(mc)) {
            return false;
        }

        double speed =
            analyzer.getHorizontalSpeed() * 20.0;

        double headingError =
            Math.abs(
                analyzer.getHeadingError()
            );

        double distance =
            analyzer.getDistanceToTarget();

        if (speed >= MAX_SAFE_SPEED) {
            return false;
        }

        if (headingError >= HARD_TURN_LIMIT) {
            return false;
        }

        if (
            distance > 0.0
                && distance <= CLOSE_DISTANCE
        ) {
            return false;
        }

        if (
            !analyzer.shouldBoost(
                boostThreshold
            )
        ) {
            return false;
        }

        boolean boosted =
            rocketManager.boost(mc);

        if (boosted) {
            triggerCooldown(
                cooldownTicks
            );
        }

        return boosted;
    }

    private void updateCooldown() {
        if (boostCooldown > 0) {
            boostCooldown--;
        }
    }

    public boolean ready() {
        return boostCooldown <= 0;
    }

    public void triggerCooldown(
        int ticks
    ) {
        boostCooldown =
            Math.max(0, ticks);
    }

    public int getCooldown() {
        return boostCooldown;
    }
}

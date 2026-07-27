package com.example.addon.flight;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

public class TakeoffController {
    private static final int MIN_AIRBORNE_TICKS = 2;
    private static final int DEPLOY_RETRY_DELAY = 5;
    private static final int TAKEOFF_TIMEOUT_TICKS = 100;

    private int takeoffTicks;
    private int airborneTicks;
    private int deployCooldown;

    public void reset() {
        takeoffTicks = 0;
        airborneTicks = 0;
        deployCooldown = 0;
    }

    public void tick(
        MinecraftClient mc,
        boolean autoTakeoffEnabled
    ) {
        if (
            mc.player == null
                || !autoTakeoffEnabled
        ) {
            releaseJump(mc);
            reset();
            return;
        }

        if (deployCooldown > 0) {
            deployCooldown--;
        }

        if (mc.player.isGliding()) {
            releaseJump(mc);
            reset();
            return;
        }

        takeoffTicks++;

        if (takeoffTicks > TAKEOFF_TIMEOUT_TICKS) {
            releaseJump(mc);
            reset();
            return;
        }

        if (mc.player.isOnGround()) {
            airborneTicks = 0;
            mc.options.jumpKey.setPressed(true);
            return;
        }

        releaseJump(mc);
        airborneTicks++;

        boolean readyToDeploy =
            airborneTicks >= MIN_AIRBORNE_TICKS
                && mc.player.getVelocity().y < 0.0
                && deployCooldown <= 0;

        if (!readyToDeploy) {
            return;
        }

        mc.player.networkHandler.sendPacket(
            new ClientCommandC2SPacket(
                mc.player,
                ClientCommandC2SPacket.Mode.START_FALL_FLYING
            )
        );

        deployCooldown = DEPLOY_RETRY_DELAY;
    }

    public void releaseJump(
        MinecraftClient mc
    ) {
        mc.options.jumpKey.setPressed(false);
    }

    public int getTakeoffTicks() {
        return takeoffTicks;
    }

    public int getDeployCooldown() {
        return deployCooldown;
    }
}

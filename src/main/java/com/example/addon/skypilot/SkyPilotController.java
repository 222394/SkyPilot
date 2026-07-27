package com.example.addon.skypilot;

import com.example.addon.flight.FlightAnalyzer;
import com.example.addon.flight.RocketController;
import com.example.addon.flight.TakeoffController;
import com.example.addon.inventory.RocketManager;
import com.example.addon.navigation.NavigationController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public class SkyPilotController {
    private static final double DEFAULT_CLIMB_BUFFER = 60.0;

    private static final double CLIMB_PITCH = 38.0;
    private static final double CLIMB_MAX_SPEED = 24.0;
    private static final int CLIMB_MIN_COOLDOWN = 30;

    private static final double CRUISE_ALTITUDE_BAND = 24.0;

    private static final double CRUISE_LOW_SPEED = 24.0;
    private static final double CRUISE_CLIMB_SPEED = 28.0;
    private static final double CRUISE_HIGH_SPEED = 36.0;

    private static final float CRUISE_GLIDE_PITCH = 1.5f;
    private static final float CRUISE_DIVE_PITCH = 4.0f;
    private static final float CRUISE_CLIMB_PITCH = -4.5f;
    private static final float CRUISE_HIGH_ENERGY_PITCH = -2.5f;
    private static final float CRUISE_DESCENT_PITCH = 3.5f;

    private static final float RECOVERY_ACCELERATION_PITCH = 0.5f;
    private static final float RECOVERY_CLIMB_PITCH = -8.0f;

    private static final float CRUISE_PITCH_DEADZONE = 0.30f;
    private static final float CRUISE_MIN_PITCH_STEP = 0.06f;

    private static final double APPROACH_DISTANCE = 100.0;
    private static final double LANDING_DISTANCE = 35.0;
    private static final double APPROACH_DESCENT = 12.0;
    private static final double LANDING_DESCENT = 30.0;

    private FlightState state = FlightState.IDLE;
    private boolean recoveringAltitude;

    public FlightState getState() {
        return state;
    }

    public boolean isFlying() {
        return state == FlightState.CLIMB
            || state == FlightState.CRUISE
            || state == FlightState.BOOST
            || state == FlightState.APPROACH
            || state == FlightState.LANDING;
    }

    public void tick(
        MinecraftClient mc,
        NavigationController navigation,
        TakeoffController takeoff,
        FlightAnalyzer analyzer,
        RocketController rocket,
        RocketManager rocketManager,
        boolean autoTakeoffEnabled,
        boolean autoBoostEnabled,
        int targetX,
        int targetZ,
        double cruiseAltitude,
        double yawSpeed,
        double pitchSpeed,
        double altitudeDeadzone,
        double maximumPitch,
        double minimumBoostSpeed,
        int boostThreshold,
        int boostCooldownTicks
    ) {
        tick(
            mc,
            navigation,
            takeoff,
            analyzer,
            rocket,
            rocketManager,
            autoTakeoffEnabled,
            autoBoostEnabled,
            targetX,
            targetZ,
            cruiseAltitude,
            DEFAULT_CLIMB_BUFFER,
            yawSpeed,
            pitchSpeed,
            altitudeDeadzone,
            maximumPitch,
            minimumBoostSpeed,
            boostThreshold,
            boostCooldownTicks,
            false
        );
    }

    public void tick(
        MinecraftClient mc,
        NavigationController navigation,
        TakeoffController takeoff,
        FlightAnalyzer analyzer,
        RocketController rocket,
        RocketManager rocketManager,
        boolean autoTakeoffEnabled,
        boolean autoBoostEnabled,
        int targetX,
        int targetZ,
        double cruiseAltitude,
        double climbBuffer,
        double yawSpeed,
        double pitchSpeed,
        double altitudeDeadzone,
        double maximumPitch,
        double minimumBoostSpeed,
        int boostThreshold,
        int boostCooldownTicks
    ) {
        tick(
            mc,
            navigation,
            takeoff,
            analyzer,
            rocket,
            rocketManager,
            autoTakeoffEnabled,
            autoBoostEnabled,
            targetX,
            targetZ,
            cruiseAltitude,
            climbBuffer,
            yawSpeed,
            pitchSpeed,
            altitudeDeadzone,
            maximumPitch,
            minimumBoostSpeed,
            boostThreshold,
            boostCooldownTicks,
            false
        );
    }

    public void tick(
        MinecraftClient mc,
        NavigationController navigation,
        TakeoffController takeoff,
        FlightAnalyzer analyzer,
        RocketController rocket,
        RocketManager rocketManager,
        boolean autoTakeoffEnabled,
        boolean autoBoostEnabled,
        int targetX,
        int targetZ,
        double cruiseAltitude,
        double climbBuffer,
        double yawSpeed,
        double pitchSpeed,
        double altitudeDeadzone,
        double maximumPitch,
        double minimumBoostSpeed,
        int boostThreshold,
        int boostCooldownTicks,
        boolean externalCruiseControl
    ) {
        if (mc.player == null) {
            state = FlightState.IDLE;
            recoveringAltitude = false;
            analyzer.reset();
            takeoff.reset();
            rocket.reset();
            return;
        }

        analyzer.update(
            mc.player,
            targetX,
            targetZ,
            cruiseAltitude,
            minimumBoostSpeed
        );

        if (state == FlightState.COMPLETE) {
            takeoff.releaseJump(mc);
            rocket.reset();
            return;
        }

        if (
            mc.player.isOnGround()
                && (
                state == FlightState.APPROACH
                    || state == FlightState.LANDING
            )
        ) {
            completeLanding(
                mc,
                takeoff,
                rocket
            );

            return;
        }

        if (!mc.player.isGliding()) {
            if (
                state == FlightState.APPROACH
                    || state == FlightState.LANDING
            ) {
                state = FlightState.LANDING;

                takeoff.tick(
                    mc,
                    true
                );

                rocket.reset();
                return;
            }

            handleTakeoff(
                mc,
                takeoff,
                rocket,
                autoTakeoffEnabled
            );

            return;
        }

        takeoff.releaseJump(mc);

        double distance =
            analyzer.getDistanceToTarget();

        boolean landingSequenceActive =
            state == FlightState.APPROACH
                || state == FlightState.LANDING;

        if (
            landingSequenceActive
                || distance <= APPROACH_DISTANCE
        ) {
            recoveringAltitude = false;

            navigation.updateYaw(
                mc.player,
                targetX,
                targetZ,
                yawSpeed
            );

            if (externalCruiseControl) {
                rocket.reset();
                state = FlightState.CRUISE;
                return;
            }

            handleLandingSequence(
                mc,
                navigation,
                rocket,
                distance,
                altitudeDeadzone,
                maximumPitch,
                pitchSpeed
            );

            return;
        }

        if (
            state == FlightState.IDLE
                || state == FlightState.TAKEOFF
                || state == FlightState.DEPLOY
        ) {
            state = FlightState.CLIMB;
        }

        double climbTargetAltitude =
            cruiseAltitude
                + Math.max(
                0.0,
                climbBuffer
            );

        if (
            state == FlightState.CLIMB
                && mc.player.getY()
                < climbTargetAltitude
        ) {
            if (externalCruiseControl) {
                navigation.updateYaw(
                    mc.player,
                    targetX,
                    targetZ,
                    yawSpeed
                );

                rocket.reset();
                state = FlightState.CRUISE;
                return;
            }

            navigation.updateClimbPitch(
                mc.player,
                CLIMB_PITCH,
                pitchSpeed
            );

            rocket.tick(
                mc,
                analyzer,
                rocketManager,
                false,
                boostThreshold,
                boostCooldownTicks
            );

            handleClimbBoost(
                mc,
                analyzer,
                rocket,
                rocketManager,
                autoBoostEnabled,
                boostCooldownTicks
            );

            return;
        }

        if (state == FlightState.CLIMB) {
            state = FlightState.CRUISE;
        }

        navigation.updateYaw(
            mc.player,
            targetX,
            targetZ,
            yawSpeed
        );

        if (externalCruiseControl) {
            recoveringAltitude = false;
            rocket.reset();
            state = FlightState.CRUISE;
            return;
        }

        updateEnergyCruisePitch(
            mc.player,
            analyzer,
            cruiseAltitude,
            pitchSpeed
        );

        boolean boosted =
            rocket.tick(
                mc,
                analyzer,
                rocketManager,
                autoBoostEnabled,
                boostThreshold,
                boostCooldownTicks
            );

        state = boosted
            ? FlightState.BOOST
            : FlightState.CRUISE;
    }

    private void updateEnergyCruisePitch(
        ClientPlayerEntity player,
        FlightAnalyzer analyzer,
        double cruiseAltitude,
        double pitchSpeed
    ) {
        double altitude =
            player.getY();

        double lowerAltitude =
            cruiseAltitude
                - CRUISE_ALTITUDE_BAND;

        double upperAltitude =
            cruiseAltitude
                + CRUISE_ALTITUDE_BAND;

        double speed =
            analyzer.getHorizontalSpeed()
                * 20.0;

        double verticalSpeed =
            player.getVelocity().y
                * 20.0;

        if (altitude < lowerAltitude) {
            recoveringAltitude = true;
        } else if (
            recoveringAltitude
                && altitude >= cruiseAltitude
        ) {
            recoveringAltitude = false;
        }

        float targetPitch;

        if (recoveringAltitude) {
            if (speed >= CRUISE_CLIMB_SPEED) {
                targetPitch =
                    RECOVERY_CLIMB_PITCH;
            } else {
                targetPitch =
                    RECOVERY_ACCELERATION_PITCH;
            }
        } else if (altitude > upperAltitude) {
            targetPitch =
                CRUISE_DESCENT_PITCH;
        } else if (speed >= CRUISE_HIGH_SPEED) {
            targetPitch =
                CRUISE_HIGH_ENERGY_PITCH;
        } else if (speed <= CRUISE_LOW_SPEED) {
            targetPitch =
                CRUISE_DIVE_PITCH;
        } else {
            double energyRatio =
                (
                    speed
                        - CRUISE_LOW_SPEED
                )
                    / (
                    CRUISE_HIGH_SPEED
                        - CRUISE_LOW_SPEED
                );

            targetPitch =
                (float) MathHelper.lerp(
                    energyRatio,
                    CRUISE_GLIDE_PITCH,
                    CRUISE_CLIMB_PITCH
                );
        }

        if (
            !recoveringAltitude
                && verticalSpeed > 4.0
                && targetPitch < 0.0f
        ) {
            targetPitch =
                Math.max(
                    targetPitch,
                    -1.0f
                );
        }

        if (
            !recoveringAltitude
                && verticalSpeed < -8.0
                && targetPitch > 0.0f
                && altitude <= upperAltitude
        ) {
            targetPitch =
                Math.min(
                    targetPitch,
                    1.0f
                );
        }

        moveCruisePitchToward(
            player,
            targetPitch,
            pitchSpeed
        );
    }

    private void moveCruisePitchToward(
        ClientPlayerEntity player,
        float targetPitch,
        double pitchSpeed
    ) {
        float currentPitch =
            player.getPitch();

        float pitchDifference =
            targetPitch
                - currentPitch;

        if (
            Math.abs(pitchDifference)
                <= CRUISE_PITCH_DEADZONE
        ) {
            return;
        }

        float maximumStep =
            (float) Math.max(
                0.0,
                pitchSpeed
            );

        float proportionalStep =
            Math.abs(pitchDifference)
                * 0.12f;

        float allowedStep =
            Math.min(
                maximumStep,
                Math.max(
                    CRUISE_MIN_PITCH_STEP,
                    proportionalStep
                )
            );

        float pitchStep =
            MathHelper.clamp(
                pitchDifference,
                -allowedStep,
                allowedStep
            );

        player.setPitch(
            MathHelper.clamp(
                currentPitch
                    + pitchStep,
                -90.0f,
                90.0f
            )
        );
    }

    private void handleClimbBoost(
        MinecraftClient mc,
        FlightAnalyzer analyzer,
        RocketController rocket,
        RocketManager rocketManager,
        boolean autoBoostEnabled,
        int boostCooldownTicks
    ) {
        if (!autoBoostEnabled) {
            return;
        }

        if (!rocket.ready()) {
            return;
        }

        if (!rocketManager.hasRocket(mc)) {
            return;
        }

        double speed =
            analyzer.getHorizontalSpeed()
                * 20.0;

        if (speed >= CLIMB_MAX_SPEED) {
            return;
        }

        boolean boosted =
            rocketManager.boost(mc);

        if (!boosted) {
            return;
        }

        int climbCooldown =
            Math.max(
                CLIMB_MIN_COOLDOWN,
                boostCooldownTicks
            );

        rocket.triggerCooldown(
            climbCooldown
        );
    }

    private void handleLandingSequence(
        MinecraftClient mc,
        NavigationController navigation,
        RocketController rocket,
        double distance,
        double altitudeDeadzone,
        double maximumPitch,
        double pitchSpeed
    ) {
        rocket.reset();

        double targetAltitude;

        if (
            state == FlightState.LANDING
                || distance <= LANDING_DISTANCE
        ) {
            state = FlightState.LANDING;

            targetAltitude =
                mc.player.getY()
                    - LANDING_DESCENT;
        } else {
            state = FlightState.APPROACH;

            targetAltitude =
                mc.player.getY()
                    - APPROACH_DESCENT;
        }

        navigation.updatePitch(
            mc.player,
            targetAltitude,
            altitudeDeadzone,
            maximumPitch,
            pitchSpeed
        );
    }

    private void completeLanding(
        MinecraftClient mc,
        TakeoffController takeoff,
        RocketController rocket
    ) {
        state = FlightState.COMPLETE;
        recoveringAltitude = false;

        takeoff.releaseJump(mc);
        takeoff.reset();
        rocket.reset();
    }

    private void handleTakeoff(
        MinecraftClient mc,
        TakeoffController takeoff,
        RocketController rocket,
        boolean autoTakeoffEnabled
    ) {
        rocket.reset();
        recoveringAltitude = false;

        if (!autoTakeoffEnabled) {
            state = FlightState.IDLE;

            takeoff.tick(
                mc,
                false
            );

            return;
        }

        state = mc.player.isOnGround()
            ? FlightState.TAKEOFF
            : FlightState.DEPLOY;

        takeoff.tick(
            mc,
            true
        );
    }

    public void reset() {
        state = FlightState.IDLE;
        recoveringAltitude = false;
    }
}

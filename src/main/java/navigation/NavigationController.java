package com.example.addon.navigation;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public class NavigationController {
    private static final float YAW_DEADZONE = 0.5f;
    private static final float PITCH_DEADZONE = 0.35f;

    private static final double CRUISE_CORRECTION_DISTANCE = 140.0;

    private static final float CRUISE_GLIDE_PITCH = 1.5f;
    private static final float CRUISE_MAX_CLIMB_PITCH = 7.0f;
    private static final float CRUISE_MAX_DESCENT_PITCH = 4.0f;

    public void updateYaw(
        ClientPlayerEntity player,
        int targetX,
        int targetZ,
        double yawSpeed
    ) {
        double deltaX =
            targetX - player.getX();

        double deltaZ =
            targetZ - player.getZ();

        float targetYaw =
            (float) (
                Math.toDegrees(
                    Math.atan2(deltaZ, deltaX)
                ) - 90.0
            );

        float currentYaw =
            player.getYaw();

        float yawDifference =
            MathHelper.wrapDegrees(
                targetYaw - currentYaw
            );

        if (
            Math.abs(yawDifference)
                <= YAW_DEADZONE
        ) {
            return;
        }

        float maximumStep =
            (float) Math.max(
                0.0,
                yawSpeed
            );

        float scaledStep =
            Math.abs(yawDifference) * 0.20f;

        float allowedStep =
            Math.min(
                maximumStep,
                Math.max(
                    0.15f,
                    scaledStep
                )
            );

        float yawStep =
            MathHelper.clamp(
                yawDifference,
                -allowedStep,
                allowedStep
            );

        float newYaw =
            MathHelper.wrapDegrees(
                currentYaw + yawStep
            );

        player.setYaw(newYaw);
        player.setHeadYaw(newYaw);
    }

    public void updateClimbPitch(
        ClientPlayerEntity player,
        double climbPitch,
        double pitchSpeed
    ) {
        float targetPitch =
            (float) -Math.abs(climbPitch);

        movePitchToward(
            player,
            targetPitch,
            pitchSpeed
        );
    }

    public void updateCruisePitch(
        ClientPlayerEntity player,
        double cruiseAltitude,
        double altitudeDeadzone,
        double pitchSpeed
    ) {
        double altitudeDifference =
            cruiseAltitude - player.getY();

        double cruiseDeadzone =
            Math.max(
                altitudeDeadzone,
                8.0
            );

        float targetPitch;

        if (
            Math.abs(altitudeDifference)
                <= cruiseDeadzone
        ) {
            targetPitch =
                CRUISE_GLIDE_PITCH;
        } else if (altitudeDifference > 0.0) {
            double climbStrength =
                MathHelper.clamp(
                    (
                        altitudeDifference
                            - cruiseDeadzone
                    ) / CRUISE_CORRECTION_DISTANCE,
                    0.0,
                    1.0
                );

            targetPitch =
                (float) MathHelper.lerp(
                    climbStrength,
                    CRUISE_GLIDE_PITCH,
                    -CRUISE_MAX_CLIMB_PITCH
                );
        } else {
            double descentStrength =
                MathHelper.clamp(
                    (
                        -altitudeDifference
                            - cruiseDeadzone
                    ) / CRUISE_CORRECTION_DISTANCE,
                    0.0,
                    1.0
                );

            targetPitch =
                (float) MathHelper.lerp(
                    descentStrength,
                    CRUISE_GLIDE_PITCH,
                    CRUISE_MAX_DESCENT_PITCH
                );
        }

        movePitchToward(
            player,
            targetPitch,
            pitchSpeed
        );
    }

    public void updatePitch(
        ClientPlayerEntity player,
        double cruiseAltitude,
        double altitudeDeadzone,
        double maximumPitch,
        double pitchSpeed
    ) {
        double altitudeDifference =
            cruiseAltitude - player.getY();

        float targetPitch;

        if (
            Math.abs(altitudeDifference)
                <= altitudeDeadzone
        ) {
            targetPitch = 0.0f;
        } else {
            double normalizedDifference =
                altitudeDifference / 50.0;

            targetPitch =
                (float) MathHelper.clamp(
                    -normalizedDifference
                        * maximumPitch,
                    -maximumPitch,
                    maximumPitch
                );
        }

        movePitchToward(
            player,
            targetPitch,
            pitchSpeed
        );
    }

    private void movePitchToward(
        ClientPlayerEntity player,
        float targetPitch,
        double pitchSpeed
    ) {
        float currentPitch =
            player.getPitch();

        float pitchDifference =
            targetPitch - currentPitch;

        if (
            Math.abs(pitchDifference)
                <= PITCH_DEADZONE
        ) {
            return;
        }

        float maximumStep =
            (float) Math.max(
                0.0,
                pitchSpeed
            );

        float scaledStep =
            Math.abs(pitchDifference) * 0.20f;

        float allowedStep =
            Math.min(
                maximumStep,
                Math.max(
                    0.08f,
                    scaledStep
                )
            );

        float pitchStep =
            MathHelper.clamp(
                pitchDifference,
                -allowedStep,
                allowedStep
            );

        float newPitch =
            MathHelper.clamp(
                currentPitch + pitchStep,
                -90.0f,
                90.0f
            );

        player.setPitch(newPitch);
    }
}

package com.example.addon.flight;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FlightAnalyzer {
    private double horizontalSpeed;
    private double verticalSpeed;
    private double altitudeError;
    private double distanceToTarget;
    private double headingError;
    private int boostScore;

    public void reset() {
        horizontalSpeed = 0.0;
        verticalSpeed = 0.0;
        altitudeError = 0.0;
        distanceToTarget = 0.0;
        headingError = 0.0;
        boostScore = 0;
    }

    public void update(
        ClientPlayerEntity player,
        int targetX,
        int targetZ,
        double cruiseAltitude,
        double minimumBoostSpeed
    ) {
        Vec3d velocity = player.getVelocity();

        horizontalSpeed = Math.sqrt(
            velocity.x * velocity.x
                + velocity.z * velocity.z
        );

        verticalSpeed = velocity.y;
        altitudeError = cruiseAltitude - player.getY();

        double deltaX = targetX - player.getX();
        double deltaZ = targetZ - player.getZ();

        distanceToTarget = Math.sqrt(
            deltaX * deltaX
                + deltaZ * deltaZ
        );

        double targetYaw =
            Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;

        headingError = Math.abs(
            MathHelper.wrapDegrees(
                (float) targetYaw - player.getYaw()
            )
        );

        boostScore = calculateBoostScore(
            minimumBoostSpeed
        );
    }

    private int calculateBoostScore(
        double minimumBoostSpeed
    ) {
        int score = 0;

        if (horizontalSpeed < minimumBoostSpeed * 0.5) {
            score += 50;
        } else if (horizontalSpeed < minimumBoostSpeed) {
            score += 35;
        } else if (
            horizontalSpeed < minimumBoostSpeed * 1.25
        ) {
            score += 15;
        }

        if (verticalSpeed < -0.75) {
            score += 30;
        } else if (verticalSpeed < -0.25) {
            score += 15;
        }

        if (altitudeError > 40.0) {
            score += 30;
        } else if (altitudeError > 15.0) {
            score += 15;
        }

        if (headingError > 90.0) {
            score -= 30;
        } else if (headingError > 45.0) {
            score -= 15;
        }

        if (distanceToTarget < 50.0) {
            score -= 100;
        } else if (distanceToTarget < 150.0) {
            score -= 40;
        }

        return MathHelper.clamp(score, 0, 100);
    }

    public boolean shouldBoost(
        int boostThreshold
    ) {
        return boostScore >= boostThreshold;
    }

    public double getHorizontalSpeed() {
        return horizontalSpeed;
    }

    public double getVerticalSpeed() {
        return verticalSpeed;
    }

    public double getAltitudeError() {
        return altitudeError;
    }

    public double getDistanceToTarget() {
        return distanceToTarget;
    }

    public double getHeadingError() {
        return headingError;
    }

    public int getBoostScore() {
        return boostScore;
    }
}

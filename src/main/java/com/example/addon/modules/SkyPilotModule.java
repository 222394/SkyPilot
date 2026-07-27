package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.flight.FlightAnalyzer;
import com.example.addon.flight.RocketController;
import com.example.addon.flight.TakeoffController;
import com.example.addon.inventory.RocketManager;
import com.example.addon.navigation.NavigationController;
import com.example.addon.skypilot.FlightState;
import com.example.addon.skypilot.SkyPilotController;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.orbit.EventHandler;

public class SkyPilotModule extends Module {
    private static final double HANDOFF_SAFETY_MARGIN = 25.0;

    public enum HudMode {
        Off,
        Compact,
        Full
    }

    public static SkyPilotModule INSTANCE;

    private final SettingGroup sgGeneral =
        settings.getDefaultGroup();

    private final SettingGroup sgElytraFly =
        settings.createGroup("ElytraFly Integration");

    private final SettingGroup sgRocket =
        settings.createGroup("Rocket Boost");

    private final SettingGroup sgHud =
        settings.createGroup("HUD");

    private final SkyPilotController controller =
        new SkyPilotController();

    private final NavigationController navigation =
        new NavigationController();

    private final TakeoffController takeoff =
        new TakeoffController();

    private final FlightAnalyzer analyzer =
        new FlightAnalyzer();

    private final RocketController rocket =
        new RocketController();

    private final RocketManager rocketManager =
        new RocketManager();

    private ElytraFly elytraFly;

    private boolean elytraFlyOwned;

    private boolean handoffTracking;
    private double handoffStartX;
    private double handoffStartZ;

    public final Setting<Integer> targetX = sgGeneral.add(
        new IntSetting.Builder()
            .name("target-x")
            .description("Destination X coordinate.")
            .defaultValue(0)
            .build()
    );

    public final Setting<Integer> targetZ = sgGeneral.add(
        new IntSetting.Builder()
            .name("target-z")
            .description("Destination Z coordinate.")
            .defaultValue(0)
            .build()
    );

    public final Setting<Double> cruiseAltitude = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("cruise-altitude")
            .description(
                "Altitude SkyPilot will try to maintain."
            )
            .defaultValue(250)
            .min(100)
            .sliderRange(100, 400)
            .build()
    );

    public final Setting<Double> climbBuffer = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("climb-buffer")
            .description(
                "Extra altitude SkyPilot gains before "
                    + "beginning navigation."
            )
            .defaultValue(60.0)
            .min(0.0)
            .sliderRange(0.0, 150.0)
            .build()
    );

    private final Setting<Boolean> autoTakeoff =
        sgGeneral.add(
            new BoolSetting.Builder()
                .name("auto-takeoff")
                .description(
                    "Automatically jumps and attempts "
                        + "to deploy the Elytra."
                )
                .defaultValue(false)
                .build()
        );

    private final Setting<Double> yawSpeed =
        sgGeneral.add(
            new DoubleSetting.Builder()
                .name("yaw-speed")
                .description(
                    "Maximum horizontal turning speed per tick."
                )
                .defaultValue(3.0)
                .min(0.1)
                .sliderRange(0.1, 15.0)
                .build()
        );

    private final Setting<Double> pitchSpeed =
        sgGeneral.add(
            new DoubleSetting.Builder()
                .name("pitch-speed")
                .description(
                    "Maximum vertical turning speed per tick."
                )
                .defaultValue(1.5)
                .min(0.1)
                .sliderRange(0.1, 10.0)
                .build()
        );

    private final Setting<Double> altitudeDeadzone =
        sgGeneral.add(
            new DoubleSetting.Builder()
                .name("altitude-deadzone")
                .description(
                    "How far from cruise altitude SkyPilot "
                        + "can drift before correcting."
                )
                .defaultValue(5.0)
                .min(0.0)
                .sliderRange(0.0, 30.0)
                .build()
        );

    private final Setting<Double> maximumPitch =
        sgGeneral.add(
            new DoubleSetting.Builder()
                .name("maximum-pitch")
                .description(
                    "Maximum pitch SkyPilot uses while "
                        + "correcting altitude."
                )
                .defaultValue(20.0)
                .min(1.0)
                .sliderRange(1.0, 45.0)
                .build()
        );

    private final Setting<Boolean> useElytraFly =
        sgElytraFly.add(
            new BoolSetting.Builder()
                .name("use-elytra-fly")
                .description(
                    "Hands stable long-distance cruise over "
                        + "to Meteor ElytraFly, then disables "
                        + "it before SkyPilot begins approach."
                )
                .defaultValue(false)
                .build()
        );

    private final Setting<Double> elytraFlyHandoffDistance =
        sgElytraFly.add(
            new DoubleSetting.Builder()
                .name("handoff-distance")
                .description(
                    "Distance SkyPilot glides after climbing "
                        + "before enabling ElytraFly."
                )
                .defaultValue(40.0)
                .min(0.0)
                .sliderRange(0.0, 500.0)
                .visible(useElytraFly::get)
                .build()
        );

    private final Setting<Double> elytraFlyReturnDistance =
        sgElytraFly.add(
            new DoubleSetting.Builder()
                .name("return-distance")
                .description(
                    "Distance from the destination where "
                        + "ElytraFly is disabled and SkyPilot "
                        + "takes control again."
                )
                .defaultValue(150.0)
                .min(110.0)
                .sliderRange(110.0, 500.0)
                .visible(useElytraFly::get)
                .build()
        );

    private final Setting<Boolean> autoBoost =
        sgRocket.add(
            new BoolSetting.Builder()
                .name("auto-boost")
                .description(
                    "Automatically uses firework rockets "
                        + "when a boost is needed."
                )
                .defaultValue(false)
                .build()
        );

    private final Setting<Double> minimumBoostSpeed =
        sgRocket.add(
            new DoubleSetting.Builder()
                .name("minimum-boost-speed")
                .description(
                    "Reference speed used by the smart "
                        + "boost scoring system."
                )
                .defaultValue(1.2)
                .min(0.1)
                .sliderRange(0.1, 3.0)
                .build()
        );

    private final Setting<Integer> boostThreshold =
        sgRocket.add(
            new IntSetting.Builder()
                .name("boost-threshold")
                .description(
                    "Minimum flight-analysis score required "
                        + "to use a rocket."
                )
                .defaultValue(50)
                .min(0)
                .sliderRange(0, 100)
                .build()
        );

    private final Setting<Integer> boostCooldown =
        sgRocket.add(
            new IntSetting.Builder()
                .name("boost-cooldown")
                .description(
                    "Minimum number of ticks between boosts."
                )
                .defaultValue(40)
                .min(1)
                .sliderRange(1, 200)
                .build()
        );

    private final Setting<HudMode> hudMode =
        sgHud.add(
            new EnumSetting.Builder<HudMode>()
                .name("hud-mode")
                .description(
                    "Controls how much flight information "
                        + "the SkyPilot HUD displays."
                )
                .defaultValue(HudMode.Compact)
                .build()
        );

    public SkyPilotModule() {
        super(
            AddonTemplate.CATEGORY,
            "sky-pilot",
            "Automatically takes off, navigates and "
                + "maintains intelligent powered Elytra flight."
        );

        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        controller.reset();
        takeoff.reset();
        analyzer.reset();
        rocket.reset();
        rocketManager.reset();

        resetElytraFlyHandoff();

        elytraFly =
            Modules.get().get(ElytraFly.class);

        info(
            "Steering toward X "
                + targetX.get()
                + ", Z "
                + targetZ.get()
                + " at altitude "
                + cruiseAltitude.get()
                + "."
        );
    }

    @Override
    public void onDeactivate() {
        takeoff.releaseJump(mc);

        disableOwnedElytraFly();

        takeoff.reset();
        analyzer.reset();
        rocket.reset();
        rocketManager.reset();
        controller.reset();

        resetElytraFlyHandoff();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) {
            disableOwnedElytraFly();
            resetElytraFlyHandoff();

            tickSkyPilot(false);
            return;
        }

        if (!useElytraFly.get()) {
            disableOwnedElytraFly();
            resetElytraFlyHandoff();

            tickSkyPilot(false);
            return;
        }

        if (elytraFly == null) {
            elytraFly =
                Modules.get().get(ElytraFly.class);
        }

        double distanceToTarget =
            getDistanceToTarget();

        if (elytraFlyOwned) {
            if (
                elytraFly == null
                    || !elytraFly.isActive()
            ) {
                elytraFlyOwned = false;
                resetElytraFlyHandoff();

                tickSkyPilot(false);
                return;
            }

            if (
                distanceToTarget
                    <= elytraFlyReturnDistance.get()
                    || !mc.player.isGliding()
            ) {
                disableOwnedElytraFly();
                resetElytraFlyHandoff();

                tickSkyPilot(false);
                return;
            }

            tickSkyPilot(true);
            return;
        }

        tickSkyPilot(false);

        if (!canBeginElytraFlyHandoff(distanceToTarget)) {
            resetElytraFlyHandoff();
            return;
        }

        if (!handoffTracking) {
            handoffTracking = true;

            handoffStartX =
                mc.player.getX();

            handoffStartZ =
                mc.player.getZ();

            info(
                "Stable cruise reached. Preparing "
                    + "ElytraFly handoff."
            );

            return;
        }

        double distanceSinceCruiseStarted =
            getHorizontalDistance(
                handoffStartX,
                handoffStartZ,
                mc.player.getX(),
                mc.player.getZ()
            );

        if (
            distanceSinceCruiseStarted
                < elytraFlyHandoffDistance.get()
        ) {
            return;
        }

        enableElytraFly();
    }

    private void tickSkyPilot(
        boolean externalCruiseControl
    ) {
        controller.tick(
            mc,
            navigation,
            takeoff,
            analyzer,
            rocket,
            rocketManager,
            autoTakeoff.get(),
            autoBoost.get(),
            targetX.get(),
            targetZ.get(),
            cruiseAltitude.get(),
            climbBuffer.get(),
            yawSpeed.get(),
            pitchSpeed.get(),
            altitudeDeadzone.get(),
            maximumPitch.get(),
            minimumBoostSpeed.get(),
            boostThreshold.get(),
            boostCooldown.get(),
            externalCruiseControl
        );
    }

    private boolean canBeginElytraFlyHandoff(
        double distanceToTarget
    ) {
        if (elytraFly == null) {
            return false;
        }

        if (elytraFly.isActive()) {
            return false;
        }

        if (!mc.player.isGliding()) {
            return false;
        }

        FlightState state =
            controller.getState();

        boolean stableCruise =
            state == FlightState.CRUISE
                || state == FlightState.BOOST;

        if (!stableCruise) {
            return false;
        }

        double requiredTargetDistance =
            elytraFlyReturnDistance.get()
                + HANDOFF_SAFETY_MARGIN;

        return distanceToTarget
            > requiredTargetDistance;
    }

    private void enableElytraFly() {
        if (elytraFly == null) {
            warning(
                "Meteor ElytraFly could not be found."
            );

            return;
        }

        if (elytraFly.isActive()) {
            return;
        }

        elytraFly.enable();
        elytraFlyOwned = true;

        rocket.reset();
        resetElytraFlyHandoff();

        info(
            "Cruise control handed to ElytraFly."
        );
    }

    private void disableOwnedElytraFly() {
        if (!elytraFlyOwned) {
            return;
        }

        if (
            elytraFly != null
                && elytraFly.isActive()
        ) {
            elytraFly.disable();
        }

        elytraFlyOwned = false;

        info(
            "ElytraFly disabled. SkyPilot has control."
        );
    }

    private void resetElytraFlyHandoff() {
        handoffTracking = false;
        handoffStartX = 0.0;
        handoffStartZ = 0.0;
    }

    private double getDistanceToTarget() {
        return getHorizontalDistance(
            mc.player.getX(),
            mc.player.getZ(),
            targetX.get(),
            targetZ.get()
        );
    }

    private double getHorizontalDistance(
        double startX,
        double startZ,
        double endX,
        double endZ
    ) {
        double deltaX =
            endX - startX;

        double deltaZ =
            endZ - startZ;

        return Math.sqrt(
            deltaX * deltaX
                + deltaZ * deltaZ
        );
    }

    public boolean shouldUseElytraFly() {
        return useElytraFly.get();
    }

    public boolean isElytraFlyControlling() {
        return elytraFlyOwned;
    }

    public HudMode getHudMode() {
        return hudMode.get();
    }

    public FlightAnalyzer getAnalyzer() {
        return analyzer;
    }

    public SkyPilotController getController() {
        return controller;
    }

    public RocketController getRocketController() {
        return rocket;
    }
}

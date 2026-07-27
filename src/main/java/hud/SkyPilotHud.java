package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import com.example.addon.flight.FlightAnalyzer;
import com.example.addon.modules.SkyPilotModule;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SkyPilotHud extends HudElement {
    public static final HudElementInfo<SkyPilotHud> INFO =
        new HudElementInfo<>(
            AddonTemplate.HUD_GROUP,
            "sky-pilot",
            "Displays live SkyPilot flight information.",
            SkyPilotHud::new
        );

    private static final String TITLE_TEXT =
        "S K Y P I L O T";

    private static final double ELYTRA_BAR_WIDTH =
        34.0;

    private static final double ELYTRA_BAR_HEIGHT =
        4.0;

    private static final double ELYTRA_BAR_GAP =
        5.0;

    private static final Color BACKGROUND =
        new Color(12, 16, 24, 220);

    private static final Color BORDER =
        new Color(75, 190, 255, 220);

    private static final Color TITLE =
        new Color(100, 220, 255, 255);

    private static final Color TITLE_SHADOW =
        new Color(25, 90, 120, 255);

    private static final Color LABEL =
        new Color(185, 195, 210, 255);

    private static final Color VALUE =
        new Color(245, 248, 255, 255);

    private static final Color MUTED =
        new Color(145, 155, 170, 255);

    private static final Color BAR_BACKGROUND =
        new Color(55, 62, 75, 255);

    private static final Color GREEN =
        new Color(90, 230, 130, 255);

    private static final Color YELLOW =
        new Color(255, 215, 80, 255);

    private static final Color ORANGE =
        new Color(255, 155, 65, 255);

    private static final Color RED =
        new Color(255, 90, 90, 255);

    private static final Color BLUE =
        new Color(90, 175, 255, 255);

    private static final Color PURPLE =
        new Color(190, 125, 255, 255);

    private final MinecraftClient mc =
        MinecraftClient.getInstance();

    public SkyPilotHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        SkyPilotModule module =
            SkyPilotModule.INSTANCE;

        if (
            module == null
                || module.getHudMode()
                == SkyPilotModule.HudMode.Off
        ) {
            setSize(0, 0);
            return;
        }

        List<HudRow> rows =
            createRows(module);

        double padding = 8.0;
        double borderSize = 1.0;
        double columnGap = 18.0;

        double lineHeight =
            renderer.textHeight(true);

        double titleHeight =
            lineHeight + 4.0;

        double spacerHeight =
            lineHeight * 0.65;

        double titleWidth =
            renderer.textWidth(
                TITLE_TEXT,
                true
            );

        double labelWidth = 0.0;
        double valueWidth = 0.0;
        double rowsHeight = 0.0;

        for (HudRow row : rows) {
            if (row.spacer) {
                rowsHeight += spacerHeight;
                continue;
            }

            labelWidth = Math.max(
                labelWidth,
                renderer.textWidth(
                    row.label,
                    true
                )
            );

            double rowValueWidth;

            if (row.elytraBar) {
                rowValueWidth =
                    ELYTRA_BAR_WIDTH
                        + ELYTRA_BAR_GAP
                        + renderer.textWidth(
                        row.value,
                        true
                    );
            }
            else {
                rowValueWidth =
                    renderer.textWidth(
                        row.value,
                        true
                    );
            }

            valueWidth = Math.max(
                valueWidth,
                rowValueWidth
            );

            rowsHeight += lineHeight;
        }

        double contentWidth =
            Math.max(
                titleWidth,
                labelWidth
                    + columnGap
                    + valueWidth
            );

        double contentHeight =
            titleHeight
                + rowsHeight;

        setSize(
            contentWidth
                + padding * 2.0,
            contentHeight
                + padding * 2.0
        );

        renderer.quad(
            x,
            y,
            getWidth(),
            getHeight(),
            BACKGROUND
        );

        drawBorder(
            renderer,
            borderSize
        );

        double titleX =
            x
                + (
                getWidth()
                    - titleWidth
            ) / 2.0;

        double titleY =
            y + padding;

        drawBoldTitle(
            renderer,
            titleX,
            titleY
        );

        double textX =
            x + padding;

        double textY =
            titleY + titleHeight;

        for (HudRow row : rows) {
            if (row.spacer) {
                textY += spacerHeight;
                continue;
            }

            renderer.text(
                row.label,
                textX,
                textY,
                LABEL,
                true
            );

            if (row.elytraBar) {
                drawElytraBarRow(
                    renderer,
                    row,
                    textY,
                    padding,
                    lineHeight
                );
            }
            else {
                double renderedValueWidth =
                    renderer.textWidth(
                        row.value,
                        true
                    );

                double valueX =
                    x
                        + getWidth()
                        - padding
                        - renderedValueWidth;

                renderer.text(
                    row.value,
                    valueX,
                    textY,
                    row.valueColor,
                    true
                );
            }

            textY += lineHeight;
        }
    }

    private void drawElytraBarRow(
        HudRenderer renderer,
        HudRow row,
        double textY,
        double padding,
        double lineHeight
    ) {
        double percentageWidth =
            renderer.textWidth(
                row.value,
                true
            );

        double percentageX =
            x
                + getWidth()
                - padding
                - percentageWidth;

        double barX =
            percentageX
                - ELYTRA_BAR_GAP
                - ELYTRA_BAR_WIDTH;

        double barY =
            textY
                + (
                lineHeight
                    - ELYTRA_BAR_HEIGHT
            ) / 2.0;

        renderer.quad(
            barX,
            barY,
            ELYTRA_BAR_WIDTH,
            ELYTRA_BAR_HEIGHT,
            BAR_BACKGROUND
        );

        double fillWidth =
            ELYTRA_BAR_WIDTH
                * clamp(
                row.elytraPercent,
                0.0,
                1.0
            );

        if (fillWidth > 0.0) {
            renderer.quad(
                barX,
                barY,
                fillWidth,
                ELYTRA_BAR_HEIGHT,
                row.valueColor
            );
        }

        renderer.text(
            row.value,
            percentageX,
            textY,
            row.valueColor,
            true
        );
    }

    private void drawBorder(
        HudRenderer renderer,
        double borderSize
    ) {
        renderer.quad(
            x,
            y,
            getWidth(),
            borderSize,
            BORDER
        );

        renderer.quad(
            x,
            y
                + getHeight()
                - borderSize,
            getWidth(),
            borderSize,
            BORDER
        );

        renderer.quad(
            x,
            y,
            borderSize,
            getHeight(),
            BORDER
        );

        renderer.quad(
            x
                + getWidth()
                - borderSize,
            y,
            borderSize,
            getHeight(),
            BORDER
        );
    }

    private void drawBoldTitle(
        HudRenderer renderer,
        double titleX,
        double titleY
    ) {
        renderer.text(
            TITLE_TEXT,
            titleX + 1.0,
            titleY + 1.0,
            TITLE_SHADOW,
            true
        );

        renderer.text(
            TITLE_TEXT,
            titleX + 0.5,
            titleY,
            TITLE,
            true
        );

        renderer.text(
            TITLE_TEXT,
            titleX,
            titleY,
            TITLE,
            true
        );
    }

    private List<HudRow> createRows(
        SkyPilotModule module
    ) {
        List<HudRow> rows =
            new ArrayList<>();

        if (!module.isActive()) {
            rows.add(
                row(
                    "Status",
                    "DISABLED",
                    RED
                )
            );

            return rows;
        }

        FlightAnalyzer analyzer =
            module.getAnalyzer();

        String state =
            String.valueOf(
                module
                    .getController()
                    .getState()
            );

        double horizontalSpeed =
            analyzer
                .getHorizontalSpeed()
                * 20.0;

        rows.add(
            row(
                "State",
                state,
                stateColor(state)
            )
        );

        rows.add(
            row(
                "Speed",
                format(horizontalSpeed)
                    + " b/s",
                BLUE
            )
        );

        int boostScore =
            analyzer.getBoostScore();

        rows.add(
            row(
                "Boost",
                boostScore > 0
                    ? String.valueOf(
                    boostScore
                )
                    : "--",
                boostScore > 0
                    ? YELLOW
                    : MUTED
            )
        );

        if (
            module.getHudMode()
                == SkyPilotModule.HudMode.Compact
        ) {
            return rows;
        }

        rows.add(spacer());

        double altitude =
            mc.player == null
                ? 0.0
                : mc.player.getY();

        rows.add(
            row(
                "Altitude",
                format(altitude),
                VALUE
            )
        );

        rows.add(
            row(
                "Target",
                format(
                    module
                        .cruiseAltitude
                        .get()
                ),
                PURPLE
            )
        );

        double verticalSpeed =
            analyzer
                .getVerticalSpeed()
                * 20.0;

        rows.add(
            row(
                "V Speed",
                signed(verticalSpeed)
                    + " b/s",
                verticalSpeedColor(
                    verticalSpeed
                )
            )
        );

        double distance =
            analyzer
                .getDistanceToTarget();

        rows.add(
            row(
                "Distance",
                formatDistance(distance),
                distanceColor(distance)
            )
        );

        double headingError =
            analyzer.getHeadingError();

        rows.add(
            row(
                "Heading",
                format(headingError)
                    + "°",
                headingColor(
                    headingError
                )
            )
        );

        int cooldown =
            module
                .getRocketController()
                .getCooldown();

        rows.add(
            row(
                "Rocket",
                cooldown <= 0
                    ? "READY"
                    : String.valueOf(
                    cooldown
                ),
                cooldown <= 0
                    ? GREEN
                    : YELLOW
            )
        );

        ElytraHealth elytraHealth =
            getElytraHealth();

        if (elytraHealth.equipped) {
            rows.add(
                elytraRow(
                    "Elytra",
                    elytraHealth.percentage
                )
            );
        }
        else {
            rows.add(
                row(
                    "Elytra",
                    "NO ELYTRA",
                    RED
                )
            );
        }

        rows.add(spacer());

        double etaSeconds =
            calculateEtaSeconds(
                distance,
                horizontalSpeed,
                state
            );

        rows.add(
            row(
                "ETA",
                formatEta(etaSeconds),
                etaColor(etaSeconds)
            )
        );

        return rows;
    }

    private ElytraHealth getElytraHealth() {
        if (mc.player == null) {
            return new ElytraHealth(
                false,
                0.0
            );
        }

        ItemStack chestStack =
            mc.player.getEquippedStack(
                EquipmentSlot.CHEST
            );

        if (
            chestStack.isEmpty()
                || !chestStack.isOf(
                Items.ELYTRA
            )
        ) {
            return new ElytraHealth(
                false,
                0.0
            );
        }

        int maxDamage =
            chestStack.getMaxDamage();

        if (maxDamage <= 0) {
            return new ElytraHealth(
                true,
                1.0
            );
        }

        int remainingDurability =
            maxDamage
                - chestStack.getDamage();

        double percentage =
            remainingDurability
                / (double) maxDamage;

        return new ElytraHealth(
            true,
            clamp(
                percentage,
                0.0,
                1.0
            )
        );
    }

    private HudRow elytraRow(
        String label,
        double percentage
    ) {
        int roundedPercentage =
            (int) Math.round(
                percentage * 100.0
            );

        return new HudRow(
            label,
            roundedPercentage + "%",
            elytraColor(percentage),
            false,
            true,
            percentage
        );
    }

    private Color elytraColor(
        double percentage
    ) {
        if (percentage >= 0.75) {
            return GREEN;
        }

        if (percentage >= 0.40) {
            return YELLOW;
        }

        if (percentage >= 0.15) {
            return ORANGE;
        }

        return RED;
    }

    private double calculateEtaSeconds(
        double distance,
        double horizontalSpeed,
        String state
    ) {
        if (
            state.equalsIgnoreCase(
                "COMPLETE"
            )
        ) {
            return 0.0;
        }

        if (
            distance <= 0.0
                || horizontalSpeed < 0.5
        ) {
            return -1.0;
        }

        return distance
            / horizontalSpeed;
    }

    private String formatEta(
        double etaSeconds
    ) {
        if (etaSeconds < 0.0) {
            return "--";
        }

        long totalSeconds =
            Math.max(
                0L,
                Math.round(etaSeconds)
            );

        long hours =
            totalSeconds / 3600L;

        long minutes =
            (
                totalSeconds % 3600L
            ) / 60L;

        long seconds =
            totalSeconds % 60L;

        if (hours > 0L) {
            return String.format(
                Locale.US,
                "%dh %02dm",
                hours,
                minutes
            );
        }

        if (minutes > 0L) {
            return String.format(
                Locale.US,
                "%dm %02ds",
                minutes,
                seconds
            );
        }

        return String.format(
            Locale.US,
            "%ds",
            seconds
        );
    }

    private String formatDistance(
        double value
    ) {
        double absoluteValue =
            Math.abs(value);

        if (
            absoluteValue
                >= 1_000_000.0
        ) {
            return String.format(
                Locale.US,
                "%.2fM",
                value
                    / 1_000_000.0
            );
        }

        if (
            absoluteValue
                >= 1_000.0
        ) {
            return String.format(
                Locale.US,
                "%.1fK",
                value
                    / 1_000.0
            );
        }

        return format(value);
    }

    private Color stateColor(
        String state
    ) {
        return switch (
            state.toUpperCase(
                Locale.ROOT
            )
            ) {
            case "TAKEOFF",
                 "DEPLOY",
                 "CLIMB" ->
                PURPLE;

            case "CRUISE" ->
                BLUE;

            case "BOOST" ->
                YELLOW;

            case "APPROACH" ->
                ORANGE;

            case "LANDING" ->
                RED;

            case "COMPLETE" ->
                GREEN;

            case "IDLE" ->
                MUTED;

            default ->
                VALUE;
        };
    }

    private Color verticalSpeedColor(
        double verticalSpeed
    ) {
        if (verticalSpeed > 0.5) {
            return GREEN;
        }

        if (verticalSpeed < -0.5) {
            return ORANGE;
        }

        return MUTED;
    }

    private Color distanceColor(
        double distance
    ) {
        if (distance <= 25.0) {
            return GREEN;
        }

        if (distance <= 100.0) {
            return YELLOW;
        }

        return VALUE;
    }

    private Color headingColor(
        double headingError
    ) {
        double error =
            Math.abs(headingError);

        if (error <= 2.0) {
            return GREEN;
        }

        if (error <= 10.0) {
            return YELLOW;
        }

        return RED;
    }

    private Color etaColor(
        double etaSeconds
    ) {
        if (etaSeconds < 0.0) {
            return MUTED;
        }

        if (etaSeconds <= 30.0) {
            return GREEN;
        }

        if (etaSeconds <= 120.0) {
            return YELLOW;
        }

        return BLUE;
    }

    private HudRow row(
        String label,
        String value,
        Color valueColor
    ) {
        return new HudRow(
            label,
            value,
            valueColor,
            false,
            false,
            0.0
        );
    }

    private HudRow spacer() {
        return new HudRow(
            "",
            "",
            MUTED,
            true,
            false,
            0.0
        );
    }

    private double clamp(
        double value,
        double minimum,
        double maximum
    ) {
        return Math.max(
            minimum,
            Math.min(
                maximum,
                value
            )
        );
    }

    private String format(
        double value
    ) {
        return String.format(
            Locale.US,
            "%.1f",
            value
        );
    }

    private String signed(
        double value
    ) {
        return String.format(
            Locale.US,
            "%+.1f",
            value
        );
    }

    private static class ElytraHealth {
        private final boolean equipped;
        private final double percentage;

        private ElytraHealth(
            boolean equipped,
            double percentage
        ) {
            this.equipped = equipped;
            this.percentage = percentage;
        }
    }

    private static class HudRow {
        private final String label;
        private final String value;
        private final Color valueColor;
        private final boolean spacer;
        private final boolean elytraBar;
        private final double elytraPercent;

        private HudRow(
            String label,
            String value,
            Color valueColor,
            boolean spacer,
            boolean elytraBar,
            double elytraPercent
        ) {
            this.label = label;
            this.value = value;
            this.valueColor =
                valueColor;
            this.spacer = spacer;
            this.elytraBar =
                elytraBar;
            this.elytraPercent =
                elytraPercent;
        }
    }
}

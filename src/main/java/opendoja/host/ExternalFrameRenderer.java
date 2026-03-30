package opendoja.host;

import com.nttdocomo.ui.Frame;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ExternalFrameRenderer {
    private static final int SIDE_WIDTH = 2;
    private static final int TOP_HEIGHT = 18;
    private static final int BOTTOM_HEIGHT = 20;
    private static final int HORIZONTAL_INSET = 2;
    private static final int VERTICAL_INSET = 2;
    private static final int STATUS_GROUP_GAP = 4;
    private static final int[] SOFT_KEYS = {
            Frame.SOFT_KEY_1,
            Frame.SOFT_KEY_2
    };

    private final List<HostOverlayRenderer> overlays = new CopyOnWriteArrayList<>();
    private final StatusBarIcons statusBarIcons;
    private final IAppliType iAppliType;
    private volatile boolean enabled;

    public ExternalFrameRenderer(boolean enabled, String statusBarIconDevice, IAppliType iAppliType) {
        this.enabled = enabled;
        this.statusBarIcons = StatusBarIcons.load(statusBarIconDevice);
        this.iAppliType = iAppliType == null ? IAppliType.I_APPLI : iAppliType;
    }

    boolean enabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void addOverlay(HostOverlayRenderer overlay) {
        overlays.add(Objects.requireNonNull(overlay, "overlay"));
    }

    void removeOverlay(HostOverlayRenderer overlay) {
        overlays.remove(overlay);
    }

    public ExternalFrameLayout layoutFor(int viewportWidth, int viewportHeight, int scale) {
        int clampedScale = Math.max(1, scale);
        if (!enabled) {
            Rectangle screenArea = new Rectangle(0, 0, viewportWidth, viewportHeight);
            Rectangle drawArea = new Rectangle(0, 0, viewportWidth, viewportHeight);
            return new ExternalFrameLayout(false, clampedScale, screenArea, drawArea, new Rectangle(), new Rectangle(),
                    new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(),
                    new Dimension(viewportWidth * clampedScale, viewportHeight * clampedScale));
        }

        int outerWidth = viewportWidth + SIDE_WIDTH * 2;
        int outerHeight = viewportHeight + TOP_HEIGHT + BOTTOM_HEIGHT;

        Rectangle screenArea = new Rectangle(SIDE_WIDTH, TOP_HEIGHT, viewportWidth, viewportHeight);
        Rectangle drawArea = new Rectangle(screenArea);
        Rectangle topBar = new Rectangle(0, 0, outerWidth, TOP_HEIGHT);
        Rectangle bottomBar = new Rectangle(0, TOP_HEIGHT + viewportHeight, outerWidth, BOTTOM_HEIGHT);
        Rectangle leftConnector = new Rectangle(0, TOP_HEIGHT, SIDE_WIDTH, viewportHeight);
        Rectangle rightConnector = new Rectangle(SIDE_WIDTH + viewportWidth, TOP_HEIGHT, SIDE_WIDTH, viewportHeight);

        Rectangle statusArea = new Rectangle(HORIZONTAL_INSET, VERTICAL_INSET,
                Math.max(0, outerWidth - HORIZONTAL_INSET * 2),
                Math.max(0, TOP_HEIGHT - VERTICAL_INSET * 2));
        Rectangle softKeyArea = new Rectangle(HORIZONTAL_INSET, bottomBar.y + VERTICAL_INSET,
                Math.max(0, outerWidth - HORIZONTAL_INSET * 2),
                Math.max(0, BOTTOM_HEIGHT - VERTICAL_INSET * 2));

        return new ExternalFrameLayout(true, clampedScale, screenArea, drawArea, topBar, bottomBar,
                leftConnector, rightConnector, statusArea, softKeyArea,
                new Dimension(outerWidth * clampedScale, outerHeight * clampedScale));
    }

    public void paint(Graphics2D graphics, Frame frame, BufferedImage drawImage, int viewportWidth, int viewportHeight, int scale) {
        ExternalFrameLayout layout = layoutFor(viewportWidth, viewportHeight, scale);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.scale(layout.scale(), layout.scale());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, layout.preferredSize().width / layout.scale(), layout.preferredSize().height / layout.scale());
            if (layout.enabled()) {
                paintChrome(g, layout, frame);
            }
            paintDrawArea(g, layout.screenArea(), layout.drawArea(), drawImage);
            paintOverlays(g, layout, frame, drawImage);
        } finally {
            g.dispose();
        }
    }

    private void paintChrome(Graphics2D g, ExternalFrameLayout layout, Frame frame) {
        Color outer = new Color(0xD7D8DB);

        g.setColor(outer);
        g.fillRect(0, 0, layout.topBar().width, layout.topBar().height);
        g.fillRect(0, layout.bottomBar().y, layout.bottomBar().width, layout.bottomBar().height);
        g.fillRect(layout.leftConnector().x, layout.leftConnector().y, layout.leftConnector().width, layout.leftConnector().height);
        g.fillRect(layout.rightConnector().x, layout.rightConnector().y, layout.rightConnector().width, layout.rightConnector().height);

        paintStatusIcons(g, layout.statusArea());
        paintSoftKeys(g, layout.softKeyArea(), frame);
    }

    private void paintDrawArea(Graphics2D g, Rectangle screenArea, Rectangle drawArea, BufferedImage drawImage) {
        g.setColor(Color.BLACK);
        g.fillRect(screenArea.x, screenArea.y, screenArea.width, screenArea.height);
        if (drawImage != null) {
            g.drawImage(drawImage, drawArea.x, drawArea.y, drawArea.width, drawArea.height, null);
        }
    }

    private void paintSoftKeys(Graphics2D g, Rectangle softKeyArea, Frame frame) {
        if (softKeyArea.width <= 0 || softKeyArea.height <= 0) {
            return;
        }

        if (frame == null || !frame.isSoftLabelVisible()) {
            return;
        }

        com.nttdocomo.ui.Font font = softKeyFontForHeight(softKeyArea.height);
        int argbColor = 0xFF1F2229;

        for (int i = 0; i < SOFT_KEYS.length; i++) {
            Rectangle section = new Rectangle(softKeyArea);
            String label = frame.getSoftLabel(SOFT_KEYS[i]);
            if (label == null || label.isBlank()) {
                continue;
            }
            paintSoftKeyLabel(g, font, argbColor, label, section, i);
        }
    }

    private void paintSoftKeyLabel(Graphics2D g, com.nttdocomo.ui.Font font, int argbColor, String label,
                                   Rectangle section, int alignmentIndex) {
        Graphics2D textGraphics = (Graphics2D) g.create();
        try {
            textGraphics.clipRect(section.x, section.y, section.width, section.height);
            int textWidth = font.stringWidth(label);
            int textY = section.y + ((section.height - font.getHeight()) / 2) + font.getAscent();
            int horizontalPadding = Math.max(4, section.height / 4);
            int textX = switch (alignmentIndex) {
                case 0 -> section.x + horizontalPadding;
                default -> section.x + Math.max(horizontalPadding, section.width - textWidth - horizontalPadding);
            };
            font.drawHostString(textGraphics, label, textX, textY, argbColor);
        } finally {
            textGraphics.dispose();
        }
    }

    private com.nttdocomo.ui.Font softKeyFontForHeight(int availableHeight) {
        int[] supportedSizes = com.nttdocomo.ui.Font.getSupportedFontSizes();
        int target = java.lang.Math.max(1, availableHeight - 2);
        int selected = supportedSizes[0];
        for (int size : supportedSizes) {
            selected = size;
            if (size >= target) {
                break;
            }
        }
        return com.nttdocomo.ui.Font.getFont(com.nttdocomo.ui.Font.FACE_SYSTEM | com.nttdocomo.ui.Font.STYLE_PLAIN, selected);
    }

    private void paintStatusIcons(Graphics2D g, Rectangle statusArea) {
        if (statusArea.width <= 0 || statusArea.height <= 0 || statusBarIcons.isEmpty()) {
            return;
        }

        int left = statusArea.x;
        int right = statusArea.x + statusArea.width;

        paintStatusIconRight(g, statusArea, right, batteryStatusIcon());
        left = paintCoverageStatus(g, statusArea, left);
        if (left > statusArea.x) {
            left += STATUS_GROUP_GAP;
        }
        paintStatusIconLeft(g, statusArea, left, iAppliStatusIcon(), false);
    }

    private int paintCoverageStatus(Graphics2D g, Rectangle statusArea, int left) {
        if (!hasInternetConnectivity()) {
            return paintStatusIconLeft(g, statusArea, left, statusBarIcons.noSignal(), false);
        }
        int next = paintStatusIconLeft(g, statusArea, left, statusBarIcons.signalAntenna(), false);
        return paintStatusIconLeft(g, statusArea, next, signalStrengthIcon(), true);
    }

    private BufferedImage signalStrengthIcon() {
        return switch (resolveSignalStrength()) {
            case 1 -> statusBarIcons.signal1();
            case 2 -> statusBarIcons.signal2();
            default -> statusBarIcons.signal3();
        };
    }

    private BufferedImage iAppliStatusIcon() {
        return iAppliType == IAppliType.I_APPLI_DX ? statusBarIcons.iAppliDx() : statusBarIcons.iAppli();
    }

    private BufferedImage batteryStatusIcon() {
        return switch (resolveBatteryLevel()) {
            case 0 -> statusBarIcons.battery0();
            case 1 -> statusBarIcons.battery1();
            case 2 -> statusBarIcons.battery2();
            default -> statusBarIcons.battery3();
        };
    }

    private int resolveSignalStrength() {
        // TODO: Map host Wi-Fi strength into the handset signal levels.
        return 3;
    }

    private boolean hasInternetConnectivity() {
        // TODO: Replace this stub with host connectivity detection and use no_signal when offline.
        return true;
    }

    private int resolveBatteryLevel() {
        // TODO: Detect host battery percentage when available and map it into 0-3.
        return 3;
    }

    private int paintStatusIconLeft(Graphics2D g, Rectangle statusArea, int left, BufferedImage icon, boolean gapBefore) {
        ScaledStatusIcon scaled = ScaledStatusIcon.scale(icon, statusArea.height);
        if (scaled == null) {
            return left;
        }
        int drawX = left;
        if (drawX >= statusArea.x + statusArea.width) {
            return statusArea.x + statusArea.width;
        }
        int drawY = statusArea.y + java.lang.Math.max(0, (statusArea.height - scaled.height()) / 2);
        g.drawImage(scaled.image(), drawX, drawY, scaled.width(), scaled.height(), null);
        return drawX + scaled.width();
    }

    private void paintStatusIconRight(Graphics2D g, Rectangle statusArea, int right, BufferedImage icon) {
        ScaledStatusIcon scaled = ScaledStatusIcon.scale(icon, statusArea.height);
        if (scaled == null) {
            return;
        }
        int drawX = java.lang.Math.max(statusArea.x, right - scaled.width());
        int drawY = statusArea.y + java.lang.Math.max(0, (statusArea.height - scaled.height()) / 2);
        g.drawImage(scaled.image(), drawX, drawY, scaled.width(), scaled.height(), null);
    }

    private void paintOverlays(Graphics2D g, ExternalFrameLayout layout, Frame frame, BufferedImage drawImage) {
        for (HostOverlayRenderer overlay : overlays) {
            overlay.paint(g, layout, frame, drawImage);
        }
    }

    private record ScaledStatusIcon(BufferedImage image, int width, int height) {
        private static ScaledStatusIcon scale(BufferedImage image, int maxHeight) {
            if (image == null || maxHeight <= 0) {
                return null;
            }
            int targetHeight = java.lang.Math.min(maxHeight, image.getHeight());
            if (targetHeight <= 0) {
                return null;
            }
            int targetWidth = java.lang.Math.max(1,
                    (int) java.lang.Math.round(image.getWidth() * (targetHeight / (double) image.getHeight())));
            return new ScaledStatusIcon(image, targetWidth, targetHeight);
        }
    }

    private record StatusBarIcons(
            BufferedImage signalAntenna,
            BufferedImage signal1,
            BufferedImage signal2,
            BufferedImage signal3,
            BufferedImage noSignal,
            BufferedImage iAppli,
            BufferedImage iAppliDx,
            BufferedImage battery0,
            BufferedImage battery1,
            BufferedImage battery2,
            BufferedImage battery3) {
        private static final String RESOURCE_ROOT = "opendoja/images/icons/";

        private static StatusBarIcons load(String requestedFamily) {
            String family = normalizeFamily(requestedFamily);
            StatusBarIcons icons = loadFamily(family);
            if (icons != null) {
                return icons;
            }
            if (!LaunchConfig.DEFAULT_STATUS_BAR_ICON_DEVICE.equals(family)) {
                OpenDoJaLog.warn(ExternalFrameRenderer.class,
                        () -> "Status bar icon family '" + family + "' not found, falling back to '"
                                + LaunchConfig.DEFAULT_STATUS_BAR_ICON_DEVICE + "'");
                icons = loadFamily(LaunchConfig.DEFAULT_STATUS_BAR_ICON_DEVICE);
                if (icons != null) {
                    return icons;
                }
            }
            OpenDoJaLog.warn(ExternalFrameRenderer.class, "Status bar icons unavailable; leaving the top bar blank");
            return new StatusBarIcons(null, null, null, null, null, null, null, null, null, null, null);
        }

        private static StatusBarIcons loadFamily(String family) {
            try {
                BufferedImage signalAntenna = loadImage(family, "signal_antenna.png");
                BufferedImage signal1 = loadImage(family, "signal_1.png");
                BufferedImage signal2 = loadImage(family, "signal_2.png");
                BufferedImage signal3 = loadImage(family, "signal_3.png");
                BufferedImage noSignal = loadImage(family, "no_signal.png");
                BufferedImage iAppli = loadImage(family, "i-appli.png");
                BufferedImage iAppliDx = loadImage(family, "i-appli_dx.png", "i-apply_dx.png");
                BufferedImage battery0 = loadImage(family, "battery_0.png");
                BufferedImage battery1 = loadImage(family, "battery_1.png");
                BufferedImage battery2 = loadImage(family, "battery_2.png");
                BufferedImage battery3 = loadImage(family, "battery_3.png");
                if (signalAntenna == null || signal1 == null || signal2 == null || signal3 == null || noSignal == null
                        || iAppli == null || iAppliDx == null || battery0 == null || battery1 == null
                        || battery2 == null || battery3 == null) {
                    return null;
                }
                return new StatusBarIcons(signalAntenna, signal1, signal2, signal3, noSignal,
                        iAppli, iAppliDx, battery0, battery1, battery2, battery3);
            } catch (IOException e) {
                OpenDoJaLog.warn(ExternalFrameRenderer.class,
                        "Failed to load status bar icon family '" + family + "'", e);
                return null;
            }
        }

        private static String normalizeFamily(String requestedFamily) {
            if (requestedFamily == null || requestedFamily.isBlank()) {
                return LaunchConfig.DEFAULT_STATUS_BAR_ICON_DEVICE;
            }
            return requestedFamily.trim();
        }

        private static BufferedImage loadImage(String family, String... names) throws IOException {
            for (String name : names) {
                String resourcePath = RESOURCE_ROOT + family + "/" + name;
                try (InputStream in = ExternalFrameRenderer.class.getClassLoader().getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        continue;
                    }
                    BufferedImage image = ImageIO.read(in);
                    if (image != null) {
                        return image;
                    }
                }
            }
            return null;
        }

        private boolean isEmpty() {
            return signalAntenna == null && signal1 == null && signal2 == null && signal3 == null
                    && noSignal == null && iAppli == null && iAppliDx == null
                    && battery0 == null && battery1 == null && battery2 == null && battery3 == null;
        }
    }
}

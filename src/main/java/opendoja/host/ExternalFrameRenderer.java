package opendoja.host;

import com.nttdocomo.ui.Frame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

final class ExternalFrameRenderer {
    private static final int[] SOFT_KEYS = {
            Frame.SOFT_KEY_1,
            Frame.SOFT_KEY_2
    };

    private final List<HostOverlayRenderer> overlays = new CopyOnWriteArrayList<>();
    private volatile boolean enabled;

    ExternalFrameRenderer(boolean enabled) {
        this.enabled = enabled;
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

    ExternalFrameLayout layoutFor(int viewportWidth, int viewportHeight, int scale) {
        int clampedScale = Math.max(1, scale);
        if (!enabled) {
            Rectangle drawArea = new Rectangle(0, 0, viewportWidth, viewportHeight);
            return new ExternalFrameLayout(false, clampedScale, drawArea, new Rectangle(), new Rectangle(),
                    new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(),
                    new Dimension(viewportWidth * clampedScale, viewportHeight * clampedScale));
        }

        int minDimension = Math.max(1, Math.min(viewportWidth, viewportHeight));
        int sideWidth = Math.max(2, Math.round(viewportWidth * 0.015f));
        int topHeight = Math.max(8, Math.round(viewportHeight * 0.075f));
        int bottomHeight = Math.max(12, Math.round(viewportHeight * 0.095f));
        int outerWidth = viewportWidth + sideWidth * 2;
        int outerHeight = viewportHeight + topHeight + bottomHeight;

        Rectangle drawArea = new Rectangle(sideWidth, topHeight, viewportWidth, viewportHeight);
        Rectangle topBar = new Rectangle(0, 0, outerWidth, topHeight);
        Rectangle bottomBar = new Rectangle(0, topHeight + viewportHeight, outerWidth, bottomHeight);
        Rectangle leftConnector = new Rectangle(0, topHeight, sideWidth, viewportHeight);
        Rectangle rightConnector = new Rectangle(sideWidth + viewportWidth, topHeight, sideWidth, viewportHeight);

        int horizontalInset = Math.max(3, Math.round(minDimension * 0.018f));
        int verticalInset = Math.max(1, Math.round(minDimension * 0.01f));
        Rectangle statusArea = new Rectangle(horizontalInset, verticalInset,
                Math.max(0, outerWidth - horizontalInset * 2),
                Math.max(0, topHeight - verticalInset * 2));
        Rectangle softKeyArea = new Rectangle(horizontalInset, bottomBar.y + verticalInset,
                Math.max(0, outerWidth - horizontalInset * 2),
                Math.max(0, bottomHeight - verticalInset * 2));

        return new ExternalFrameLayout(true, clampedScale, drawArea, topBar, bottomBar,
                leftConnector, rightConnector, statusArea, softKeyArea,
                new Dimension(outerWidth * clampedScale, outerHeight * clampedScale));
    }

    void paint(Graphics2D graphics, Frame frame, BufferedImage drawImage, int viewportWidth, int viewportHeight, int scale) {
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
            paintDrawArea(g, layout.drawArea(), drawImage);
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

        paintSoftKeys(g, layout.softKeyArea(), frame);
    }

    private void paintDrawArea(Graphics2D g, Rectangle drawArea, BufferedImage drawImage) {
        g.setColor(Color.BLACK);
        g.fillRect(drawArea.x, drawArea.y, drawArea.width, drawArea.height);
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

        int fontSize = Math.max(9, softKeyArea.height - 6);
        Font font = new Font(Font.MONOSPACED, Font.BOLD, fontSize);
        g.setFont(font);
        g.setColor(new Color(0x1F2229));

        for (int i = 0; i < SOFT_KEYS.length; i++) {
            Rectangle section = new Rectangle(softKeyArea);
            String label = frame.getSoftLabel(SOFT_KEYS[i]);
            if (label == null || label.isBlank()) {
                continue;
            }
            paintSoftKeyLabel(g, label, section, i);
        }
    }

    private void paintSoftKeyLabel(Graphics2D g, String label, Rectangle section, int alignmentIndex) {
        Graphics2D textGraphics = (Graphics2D) g.create();
        try {
            textGraphics.clipRect(section.x, section.y, section.width, section.height);
            FontMetrics metrics = textGraphics.getFontMetrics();
            int textWidth = metrics.stringWidth(label);
            int textY = section.y + ((section.height - metrics.getHeight()) / 2) + metrics.getAscent();
            int horizontalPadding = Math.max(4, section.height / 4);
            int textX = switch (alignmentIndex) {
                case 0 -> section.x + horizontalPadding;
                default -> section.x + Math.max(horizontalPadding, section.width - textWidth - horizontalPadding);
            };
            textGraphics.drawString(label, textX, textY);
        } finally {
            textGraphics.dispose();
        }
    }

    private void paintOverlays(Graphics2D g, ExternalFrameLayout layout, Frame frame, BufferedImage drawImage) {
        for (HostOverlayRenderer overlay : overlays) {
            overlay.paint(g, layout, frame, drawImage);
        }
    }
}

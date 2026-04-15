package opendoja.host;

import com.nttdocomo.ui.Frame;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface HostOverlayRenderer {
    void paint(Graphics2D graphics, ExternalFrameLayout layout, Frame frame, BufferedImage drawImage);
}

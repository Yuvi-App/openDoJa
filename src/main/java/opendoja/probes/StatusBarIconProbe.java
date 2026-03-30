package opendoja.probes;

import com.nttdocomo.ui.Frame;
import opendoja.host.ExternalFrameLayout;
import opendoja.host.ExternalFrameRenderer;
import opendoja.host.JamLauncher;
import opendoja.host.LaunchConfig;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StatusBarIconProbe {
    private static final int HOST_SCALE = 1;

    private StatusBarIconProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: StatusBarIconProbe <jam-path> <output-png>");
        }
        Path jamPath = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);
        LaunchConfig config = JamLauncher.buildLaunchConfig(jamPath, false);

        ExternalFrameRenderer renderer = new ExternalFrameRenderer(
                true,
                config.statusBarIconDevice(),
                config.iAppliType());
        ExternalFrameLayout layout = renderer.layoutFor(config.width(), config.height(), HOST_SCALE);
        BufferedImage image = new BufferedImage(layout.preferredSize().width, layout.preferredSize().height,
                BufferedImage.TYPE_INT_ARGB);

        Frame frame = new Frame() {
        };
        frame.setSoftLabel(Frame.SOFT_KEY_1, "Menu");
        frame.setSoftLabel(Frame.SOFT_KEY_2, "Back");

        Graphics2D graphics = image.createGraphics();
        try {
            renderer.paint(graphics, frame, null, config.width(), config.height(), HOST_SCALE);
        } finally {
            graphics.dispose();
        }

        writeImage(image, outputPath);
        System.out.println("iAppliType=" + config.iAppliType());
        System.out.println("iconDevice=" + config.statusBarIconDevice());
        System.out.println("topBarNonBackgroundPixels=" + countTopBarNonBackgroundPixels(image));
        System.out.println("output=" + outputPath.toAbsolutePath());
    }

    private static void writeImage(BufferedImage image, Path outputPath) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(image, "png", outputPath.toFile());
    }

    private static int countTopBarNonBackgroundPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < 18 && y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != 0xFFD7D8DB) {
                    count++;
                }
            }
        }
        return count;
    }
}

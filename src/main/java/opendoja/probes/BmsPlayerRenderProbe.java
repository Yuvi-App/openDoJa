package opendoja.probes;

import com.nttdocomo.ui.MediaImage;
import com.nttdocomo.ui.MediaManager;
import com.nttdocomo.ui.Graphics;
import com.nttdocomo.ui.Image;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class BmsPlayerRenderProbe {
    private BmsPlayerRenderProbe() {
    }

    public static void main(String[] args) throws Exception {
        Class<?> canClass = Class.forName("can");
        Constructor<?> canConstructor = canClass.getDeclaredConstructor();
        canConstructor.setAccessible(true);
        Object can = canConstructor.newInstance();

        Field imgField = canClass.getDeclaredField("img");
        imgField.setAccessible(true);
        Image[] img = (Image[]) imgField.get(can);
        img[0] = loadImage("resource:///0.gif");
        img[11] = loadImage("resource:///11.gif");
        img[26] = loadImage("resource:///26.gif");

        Class<?> zipUtilClass = Class.forName("ZipUtil");
        Constructor<?> zipUtilConstructor = zipUtilClass.getDeclaredConstructor(int.class, int.class);
        zipUtilConstructor.setAccessible(true);
        Object zipUtil = zipUtilConstructor.newInstance(0, 0);
        Field zu = canClass.getDeclaredField("zu");
        zu.setAccessible(true);
        zu.set(can, zipUtil);

        Method makeSubScreen = canClass.getDeclaredMethod("makeSubScreen");
        makeSubScreen.setAccessible(true);
        makeSubScreen.invoke(can);

        Field subScreenField = canClass.getDeclaredField("subScreen");
        subScreenField.setAccessible(true);
        Image subScreen = (Image) subScreenField.get(can);
        Graphics graphics = subScreen.getGraphics();

        int transparent = 0;
        int translucent = 0;
        int opaque = 0;
        int minAlpha = 255;
        int maxAlpha = 0;
        for (int y = 0; y < subScreen.getHeight(); y++) {
            for (int x = 0; x < subScreen.getWidth(); x++) {
                int alpha = (graphics.getRGBPixel(x, y) >>> 24) & 0xFF;
                minAlpha = Math.min(minAlpha, alpha);
                maxAlpha = Math.max(maxAlpha, alpha);
                if (alpha == 0) {
                    transparent++;
                } else if (alpha == 255) {
                    opaque++;
                } else {
                    translucent++;
                }
            }
        }

        System.out.printf(
                "subScreen alpha min=%d max=%d transparent=%d translucent=%d opaque=%d%n",
                minAlpha,
                maxAlpha,
                transparent,
                translucent,
                opaque);
    }

    private static Image loadImage(String name) throws Exception {
        MediaImage image = MediaManager.getImage(name);
        image.use();
        return image.getImage();
    }
}

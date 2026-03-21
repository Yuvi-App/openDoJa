package opendoja.demo;

import com.nttdocomo.ui.Font;
import com.nttdocomo.ui.Graphics;
import com.nttdocomo.ui.Image;

public final class BitmapFontProbe {
    private BitmapFontProbe() {
    }

    public static void main(String[] args) {
        Font font12 = Font.getFont(Font.FACE_SYSTEM | Font.STYLE_PLAIN, 12);
        Font font20 = Font.getFont(Font.FACE_SYSTEM | Font.STYLE_PLAIN, 20);

        System.out.printf("font12Class=%s%n", font12.getClass().getName());
        System.out.printf("font20Class=%s%n", font20.getClass().getName());
        System.out.printf("sizes=%s%n", java.util.Arrays.toString(Font.getSupportedFontSizes()));
        System.out.printf("asciiWidth=%d%n", font12.stringWidth("ABC"));
        System.out.printf("halfKanaWidth=%d%n", font12.stringWidth("\uFF71\uFF72\uFF73"));
        System.out.printf("fullWidthWidth=%d%n", font12.stringWidth("\u6F22\u5B57"));
        System.out.printf("questBracketWidth=%d%n", font12.stringWidth("\u300A\u300B"));
        System.out.printf("spaceWidth=%d%n", font12.stringWidth("A A") - font12.stringWidth("AA"));
        System.out.printf("fallbackWidth=%d questionWidth=%d%n", font12.stringWidth("\uFFFD"), font12.stringWidth("?"));
        System.out.printf("asciiPixels=%d%n", renderPixels(font20, "ABC"));
        System.out.printf("halfKanaPixels=%d%n", renderPixels(font20, "\uFF71\uFF72\uFF73"));
        System.out.printf("fullWidthPixels=%d%n", renderPixels(font20, "\u6F22\u5B57"));
        System.out.printf("questBracketPixels=%d%n", renderPixels(font20, "\u300A\u300B"));
        System.out.printf("spacePixels=%d%n", renderPixels(font20, "   "));
        System.out.printf("questionPixels=%d fallbackPixels=%d%n", renderPixels(font20, "?"), renderPixels(font20, "\uFFFD"));
        System.out.printf("pictogramPixels=%d%n", renderPixels(font20, "\uE63E"));
    }

    private static int renderPixels(Font font, String text) {
        Image image = Image.createImage(256, 64);
        Graphics graphics = image.getGraphics();
        graphics.setFont(font);
        graphics.setColor(0xFFFFFFFF);
        graphics.drawString(text, 0, font.getAscent());
        int litPixels = 0;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 256; x++) {
                if ((graphics.getPixel(x, y) >>> 24) != 0) {
                    litPixels++;
                }
            }
        }
        return litPixels;
    }
}

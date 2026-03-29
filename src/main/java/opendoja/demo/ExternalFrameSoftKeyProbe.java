package opendoja.demo;

import com.nttdocomo.ui.Canvas;
import com.nttdocomo.ui.Display;
import com.nttdocomo.ui.Frame;
import com.nttdocomo.ui.Graphics;
import opendoja.host.DoJaRuntime;
import opendoja.host.LaunchConfig;

import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public final class ExternalFrameSoftKeyProbe {
    private ExternalFrameSoftKeyProbe() {
    }

    public static void main(String[] args) throws Exception {
        runProbe(null, 1, 182, 244, 176, 208);
        runProbe("2", 2, 364, 488, 352, 416);
    }

    private static void runProbe(String configuredScale, int expectedHostScale,
                                 int expectedEnabledWidth, int expectedEnabledHeight,
                                 int expectedDisabledWidth, int expectedDisabledHeight) throws Exception {
        LaunchConfig.Builder builder = LaunchConfig.builder(ProbeApplication.class)
                .viewport(176, 208)
                .title("ExternalFrameSoftKeyProbe");
        if (configuredScale != null) {
            builder.parameter("opendoja.hostScale", configuredScale);
        }
        LaunchConfig config = builder.build();
        DoJaRuntime.prepareLaunch(config);
        DoJaRuntime runtime = DoJaRuntime.bootstrap(config);
        try {
            ProbeCanvas canvas = new ProbeCanvas();
            canvas.setSoftLabel(Frame.SOFT_KEY_1, "Left");
            canvas.setSoftLabel(Frame.SOFT_KEY_2, "Right");
            Display.setCurrent(canvas);

            Dimension enabledSize = hostPreferredSize(runtime);
            runtime.setExternalFrameEnabled(false);
            Dimension disabledSize = hostPreferredSize(runtime);
            runtime.setExternalFrameEnabled(true);
            Dimension reenabledSize = hostPreferredSize(runtime);

            if (enabledSize.width != expectedEnabledWidth || enabledSize.height != expectedEnabledHeight) {
                throw new IllegalStateException("Unexpected enabled size for scale " + expectedHostScale + ": " + enabledSize);
            }
            if (disabledSize.width != expectedDisabledWidth || disabledSize.height != expectedDisabledHeight) {
                throw new IllegalStateException("Unexpected disabled size for scale " + expectedHostScale + ": " + disabledSize);
            }
            if (!enabledSize.equals(reenabledSize)) {
                throw new IllegalStateException("External frame toggle did not restore the original host viewport");
            }
            if (runtime.hostScale() != expectedHostScale) {
                throw new IllegalStateException("Unexpected runtime host scale " + runtime.hostScale() + " for requested " + expectedHostScale);
            }

            verifyHostKeyMapping();

            runtime.dispatchHostSoftKey(Frame.SOFT_KEY_1, Display.KEY_PRESSED_EVENT);
            runtime.dispatchHostSoftKey(Frame.SOFT_KEY_1, Display.KEY_RELEASED_EVENT);
            runtime.dispatchHostSoftKey(Frame.SOFT_KEY_2, Display.KEY_PRESSED_EVENT);
            runtime.dispatchHostSoftKey(Frame.SOFT_KEY_2, Display.KEY_RELEASED_EVENT);
            flushEdt();

            List<String> expectedEvents = List.of(
                    "0:21",
                    "1:21",
                    "0:22",
                    "1:22"
            );
            if (!expectedEvents.equals(canvas.events())) {
                throw new IllegalStateException("Unexpected soft-key dispatch sequence: " + canvas.events());
            }

            System.out.println("configuredHostScale=" + (configuredScale == null ? "<default>" : configuredScale));
            System.out.println("resolvedHostScale=" + expectedHostScale);
            System.out.println("enabledSize=" + enabledSize.width + "x" + enabledSize.height);
            System.out.println("disabledSize=" + disabledSize.width + "x" + disabledSize.height);
            System.out.println("hostKeyMap=A:" + Frame.SOFT_KEY_1 + ",S:" + Frame.SOFT_KEY_2 + ",D:-1");
            System.out.println("softKeyEvents=" + canvas.events());
        } finally {
            runtime.shutdown();
        }
    }

    private static Dimension hostPreferredSize(DoJaRuntime runtime) throws Exception {
        flushEdt();
        Field hostPanelField = DoJaRuntime.class.getDeclaredField("hostPanel");
        hostPanelField.setAccessible(true);
        Object hostPanel = hostPanelField.get(runtime);
        return ((java.awt.Component) hostPanel).getPreferredSize();
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    private static void verifyHostKeyMapping() throws Exception {
        Method mapHostSoftKey = DoJaRuntime.class.getDeclaredMethod("mapHostSoftKey", int.class);
        mapHostSoftKey.setAccessible(true);
        int left = (int) mapHostSoftKey.invoke(null, KeyEvent.VK_A);
        int right = (int) mapHostSoftKey.invoke(null, KeyEvent.VK_S);
        int disabled = (int) mapHostSoftKey.invoke(null, KeyEvent.VK_D);
        if (left != Frame.SOFT_KEY_1 || right != Frame.SOFT_KEY_2 || disabled != -1) {
            throw new IllegalStateException("Unexpected host key mapping A=" + left + " S=" + right + " D=" + disabled);
        }
    }

    public static final class ProbeApplication extends com.nttdocomo.ui.IApplication {
        @Override
        public void start() {
        }
    }

    static final class ProbeCanvas extends Canvas {
        private final List<String> events = new ArrayList<>();

        @Override
        public void paint(Graphics g) {
            g.clearRect(0, 0, Display.getWidth(), Display.getHeight());
        }

        @Override
        public void processEvent(int type, int param) {
            events.add(type + ":" + param);
        }

        List<String> events() {
            return List.copyOf(events);
        }
    }
}

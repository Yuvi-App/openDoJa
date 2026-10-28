package opendoja.probes;

import com.nttdocomo.ui.Display;
import opendoja.host.*;
import javax.swing.SwingUtilities;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Checks the desktop path, native handset path, polling, holds, and STAR layout boundaries. */
public final class StarDirectionalInputProbe {
    private static final int UP = Display.KEY_UP, RIGHT = Display.KEY_RIGHT,
            DOWN = Display.KEY_DOWN, LEFT = Display.KEY_LEFT;
    private static final int[] DIRECTIONS = {UP, RIGHT, DOWN, LEFT};
    private static final HostControlAction[] ACTIONS = {HostControlAction.MOVE_UP, HostControlAction.MOVE_RIGHT,
            HostControlAction.MOVE_DOWN, HostControlAction.MOVE_LEFT};
    private static ProbeCanvas canvas;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        String property = OpenDoJaLaunchArgs.INPUT_STAR_DIRECTION_ROTATION;
        String previous = System.getProperty(property);
        try {
            System.setProperty(property, "auto");
            verifyLayout("P01A", 854, 480, true, new int[]{RIGHT, DOWN, LEFT, UP});
            verifyLayout("P-03C", 854, 480, true, new int[]{RIGHT, DOWN, LEFT, UP});
            verifyLayout("P01A", 240, 427, true, DIRECTIONS);
            verifyLayout("P01A", 480, 480, true, DIRECTIONS);
            verifyLayout("N-01A", 427, 240, true, DIRECTIONS);
            verifyLayout("appli", 854, 480, true, DIRECTIONS);
            verifyLayout("P01A", 854, 480, false, DIRECTIONS);
            System.setProperty(property, "none");
            verifyLayout("P01A", 854, 480, true, DIRECTIONS);
            System.setProperty(property, "left");
            verifyLayout("N-01A", 427, 240, true, new int[]{LEFT, UP, RIGHT, DOWN});
            System.setProperty(property, "right");
            verifyLayout("N-01A", 427, 240, true, new int[]{RIGHT, DOWN, LEFT, UP});
            System.setProperty(property, "upside-down");
            verifyLayout("N-01A", 427, 240, true, new int[]{DOWN, LEFT, UP, RIGHT});
            // Even an explicit STAR override must leave regular DoJa input untouched.
            verifyLayout("P01A", 854, 480, false, DIRECTIONS);
            System.setProperty(property, "auto");
            verifyHolds();
        } finally {
            if (previous == null) System.clearProperty(property); else System.setProperty(property, previous);
            if (DoJaRuntime.current() != null) DoJaRuntime.current().shutdown();
        }
        System.out.println("star-directional-input-probe-ok");
    }

    private static void launch(String device, int width, int height, boolean star) {
        var builder = LaunchConfig.builder(ProbeApp.class).viewport(width, height).parameter("TargetDevice", device);
        if (star) builder.parameter("AppType", "FullApp");
        DesktopLauncher.launch(builder.build());
    }

    private static void verifyLayout(String device, int width, int height, boolean star, int[] expected) throws Exception {
        launch(device, width, height, star);
        try {
            for (int i = 0; i < DIRECTIONS.length; i++) {
                mapped(ACTIONS[i], true); expectEvent(0, expected[i]);
                check(canvas.getKeypadState() == 1 << expected[i], "Polling matches mapped direction: " + device);
                mapped(ACTIONS[i], false); expectEvent(1, expected[i]);
                check(canvas.getKeypadState() == 0, "Mapped direction releases: " + device);
            }
            // Number keys retain phone positions; these are action buttons in many STAR titles.
            mapped(HostControlAction.DIGIT_2, true); expectEvent(0, Display.KEY_2);
            mapped(HostControlAction.DIGIT_2, false); expectEvent(1, Display.KEY_2);
            DoJaRuntime.current().dispatchSyntheticKey(RIGHT, 0); expectEvent(0, RIGHT);
            DoJaRuntime.current().dispatchSyntheticKey(RIGHT, 1); expectEvent(1, RIGHT);
        } finally { DoJaRuntime.current().shutdown(); }
    }

    private static void verifyHolds() throws Exception {
        launch("P01A", 854, 480, true);
        try {
            var runtime = DoJaRuntime.current();
            mapped(HostControlAction.MOVE_UP, true); expectEvent(0, RIGHT);
            mapped(HostControlAction.MOVE_RIGHT, true); expectEvent(0, DOWN);
            check(canvas.getKeypadState() == ((1 << RIGHT) | (1 << DOWN)), "Overlapping directions stay held");
            System.setProperty(OpenDoJaLaunchArgs.INPUT_STAR_DIRECTION_ROTATION, "none");
            mapped(HostControlAction.MOVE_UP, false); expectEvent(1, RIGHT);
            check(canvas.getKeypadState() == 1 << DOWN, "Layout change preserves the other held key");
            // Both host keys now select the same handset key. Releasing one must retain the other.
            mapped(HostControlAction.MOVE_DOWN, true);
            mapped(HostControlAction.MOVE_RIGHT, false);
            Thread.sleep(60);
            check(canvas.getKeypadState() == 1 << DOWN, "Overlapping mappings retain the shared handset hold");
            mapped(HostControlAction.MOVE_DOWN, false); expectEvent(1, DOWN);
            check(canvas.getKeypadState() == 0, "Every mapped key releases after layout changes");
            System.setProperty(OpenDoJaLaunchArgs.INPUT_STAR_DIRECTION_ROTATION, "auto");
            mapped(HostControlAction.MOVE_RIGHT, true); expectEvent(0, DOWN);
            // A desktop auto-repeat release/press pair must keep the original mapped hold.
            mapped(HostControlAction.MOVE_RIGHT, false);
            mapped(HostControlAction.MOVE_RIGHT, true);
            Thread.sleep(60);
            check(canvas.events.isEmpty() && canvas.getKeypadState() == 1 << DOWN,
                    "Auto-repeat retains the rotated hold without duplicate edges");
            Object panel = field(runtime, "hostPanel");
            Object adapter = field(panel, "keyInputAdapter");
            SwingUtilities.invokeAndWait(() -> ((DesktopKeyInputAdapter) adapter).releaseAll());
            expectEvent(1, DOWN);
            check(canvas.getKeypadState() == 0, "Focus-loss releaseAll clears rotated keys");
        } finally { DoJaRuntime.current().shutdown(); }
    }

    private static void mapped(HostControlAction action, boolean pressed) throws Exception {
        Object panel = field(DoJaRuntime.current(), "hostPanel");
        var dispatch = panel.getClass().getDeclaredMethod("dispatchMappedAction", HostControlAction.class, boolean.class);
        dispatch.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try { dispatch.invoke(panel, action, pressed); } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
    }
    private static Object field(Object value, String name) throws Exception {
        var field = value.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(value);
    }
    private static void expectEvent(int type, int key) throws Exception {
        String actual = canvas.events.poll(2, TimeUnit.SECONDS);
        check((type + ":" + key).equals(actual), "Expected " + type + ":" + key + ", got " + actual);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static final class ProbeApp extends com.nttdocomo.ui.IApplication {
        @Override public void start() { canvas = new ProbeCanvas(); Display.setCurrent(canvas); }
    }
    private static final class ProbeCanvas extends com.nttdocomo.ui.Canvas {
        final LinkedBlockingQueue<String> events = new LinkedBlockingQueue<>();
        @Override public void paint(com.nttdocomo.ui.Graphics graphics) { }
        @Override public void processEvent(int type, int param) {
            if (type == 0 || type == 1) events.add(type + ":" + param);
        }
    }
}

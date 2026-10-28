package com.docomostar.device.gesturereader;

/** Uses the existing emulated gesture lifecycle and neutral readings. */
public final class RollReader extends GestureReader {
    private static final java.util.Map<Integer, RollReader> READERS = new java.util.HashMap<>();
    private final com.nttdocomo.device.gesturereader.RollReader host;
    private RollReader(com.nttdocomo.device.gesturereader.RollReader host) { this.host = host; }
    public static synchronized RollReader getRollReader(int id) {
        try {
            var host = com.nttdocomo.device.gesturereader.RollReader.getRollReader(id);
            return READERS.computeIfAbsent(id, ignored -> new RollReader(host));
        } catch (com.nttdocomo.lang.UnsupportedOperationException failure) {
            var translated = new com.docomostar.lang.UnsupportedOperationException(failure.getMessage());
            translated.initCause(failure);
            throw translated;
        }
    }
    @Override public void start() { host.start(); }
    @Override public void stop() { host.stop(); }
    public void recenter() { host.recenter(); }
    public RollData getRollData() { return new RollData(host.getRollData()); }
}

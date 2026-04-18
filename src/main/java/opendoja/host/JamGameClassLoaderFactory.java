package opendoja.host;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class JamGameClassLoaderFactory {
    private JamGameClassLoaderFactory() {
    }

    static URLClassLoader create(Path gameJarPath, ClassLoader parent) {
        if (gameJarPath == null) {
            throw new IllegalArgumentException("gameJarPath must not be null");
        }
        try {
            String encodedFileUrl = gameJarPath.toAbsolutePath().normalize().toUri().toASCIIString().replace("!", "%21");
            return new StarAwareUrlClassLoader(new URL[]{new URL(encodedFileUrl)}, parent);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not create class loader for " + gameJarPath, e);
        }
    }

    private static final class StarAwareUrlClassLoader extends URLClassLoader {
        private static final String[][] STAR_REWRITE_RULES = {
                {"com/docomostar/StarApplication", "com/nttdocomo/star/StarApplication"},
                {"com/docomostar/StarApplicationManager", "com/nttdocomo/star/StarApplicationManager"},
                {"com/docomostar/StarEventListener", "com/nttdocomo/star/StarEventListener"},
                {"com/docomostar/StarEventObject", "com/nttdocomo/star/StarEventObject"},
                {"com/docomostar/CalledByDTVEvent", "com/nttdocomo/star/CalledByDTVEvent"},
                {"com/docomostar/FelicaAdhocEvent", "com/nttdocomo/star/FelicaAdhocEvent"},
                {"com/docomostar/media/", "com/nttdocomo/ui/"},
                {"com/docomostar/media/MediaManager", "com/nttdocomo/ui/MediaManager"},
                {"com/docomostar/media/MediaImage", "com/nttdocomo/ui/MediaImage"},
                {"com/docomostar/media/MediaSound", "com/nttdocomo/ui/MediaSound"},
                {"com/docomostar/system/PhoneSystem", "com/nttdocomo/ui/PhoneSystem"},
                {"com/docomostar/opt/system/PhoneSystem2", "com/nttdocomo/opt/ui/PhoneSystem2"},
                {"com/docomostar/system/Contents", "com/nttdocomo/star/system/Contents"},
                {"com/docomostar/system/ContentsException", "com/nttdocomo/star/system/ContentsException"},
                {"com/docomostar/system/Invitation", "com/nttdocomo/star/system/Invitation"},
                {"com/docomostar/system/InvitationParam", "com/nttdocomo/star/system/InvitationParam"},
                {"com/docomostar/system/Launcher", "com/nttdocomo/star/system/Launcher"},
                {"com.docomostar.StarApplication", "com.nttdocomo.star.StarApplication"},
                {"com.docomostar.StarApplicationManager", "com.nttdocomo.star.StarApplicationManager"},
                {"com.docomostar.StarEventListener", "com.nttdocomo.star.StarEventListener"},
                {"com.docomostar.StarEventObject", "com.nttdocomo.star.StarEventObject"},
                {"com.docomostar.CalledByDTVEvent", "com.nttdocomo.star.CalledByDTVEvent"},
                {"com.docomostar.FelicaAdhocEvent", "com.nttdocomo.star.FelicaAdhocEvent"},
                {"com.docomostar.media.", "com.nttdocomo.ui."},
                {"com.docomostar.media.MediaManager", "com.nttdocomo.ui.MediaManager"},
                {"com.docomostar.media.MediaImage", "com.nttdocomo.ui.MediaImage"},
                {"com.docomostar.media.MediaSound", "com.nttdocomo.ui.MediaSound"},
                {"com.docomostar.system.PhoneSystem", "com.nttdocomo.ui.PhoneSystem"},
                {"com.docomostar.opt.system.PhoneSystem2", "com.nttdocomo.opt.ui.PhoneSystem2"},
                {"com.docomostar.system.Contents", "com.nttdocomo.star.system.Contents"},
                {"com.docomostar.system.ContentsException", "com.nttdocomo.star.system.ContentsException"},
                {"com.docomostar.system.Invitation", "com.nttdocomo.star.system.Invitation"},
                {"com.docomostar.system.InvitationParam", "com.nttdocomo.star.system.InvitationParam"},
                {"com.docomostar.system.Launcher", "com.nttdocomo.star.system.Launcher"},
                {"com/docomostar/", "com/nttdocomo/"},
                {"com.docomostar.", "com.nttdocomo."}
        };

        static {
            registerAsParallelCapable();
        }

        private StarAwareUrlClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String resourceName = name.replace('.', '/').concat(".class");
            URL resource = findResource(resourceName);
            if (resource == null) {
                throw new ClassNotFoundException(name);
            }
            try (InputStream in = resource.openStream()) {
                byte[] bytecode = in.readAllBytes();
                byte[] rewritten = rewriteStarReferences(bytecode);
                return defineClass(name, rewritten, 0, rewritten.length);
            } catch (IOException exception) {
                throw new ClassNotFoundException("Could not load class " + name, exception);
            }
        }

        private static byte[] rewriteStarReferences(byte[] bytecode) throws IOException {
            try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(bytecode));
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream(bytecode.length + 256);
                 java.io.DataOutputStream out = new java.io.DataOutputStream(buffer)) {
                out.writeInt(in.readInt());
                out.writeShort(in.readUnsignedShort());
                out.writeShort(in.readUnsignedShort());
                int constantPoolCount = in.readUnsignedShort();
                out.writeShort(constantPoolCount);
                for (int index = 1; index < constantPoolCount; index++) {
                    int tag = in.readUnsignedByte();
                    out.writeByte(tag);
                    switch (tag) {
                        case 1 -> {
                            int length = in.readUnsignedShort();
                            byte[] raw = in.readNBytes(length);
                            String value = new String(raw, StandardCharsets.UTF_8);
                            String rewritten = rewriteStarString(value);
                            byte[] encoded = rewritten.getBytes(StandardCharsets.UTF_8);
                            out.writeShort(encoded.length);
                            out.write(encoded);
                        }
                        case 3, 4 -> out.writeInt(in.readInt());
                        case 5, 6 -> {
                            out.writeLong(in.readLong());
                            index++;
                        }
                        case 7, 8, 16, 19, 20 -> out.writeShort(in.readUnsignedShort());
                        case 9, 10, 11, 12, 17, 18 -> {
                            out.writeShort(in.readUnsignedShort());
                            out.writeShort(in.readUnsignedShort());
                        }
                        case 15 -> {
                            out.writeByte(in.readUnsignedByte());
                            out.writeShort(in.readUnsignedShort());
                        }
                        default -> throw new IOException("Unsupported class-file constant-pool tag: " + tag);
                    }
                }
                out.write(in.readAllBytes());
                out.flush();
                return buffer.toByteArray();
            }
        }

        private static String rewriteStarString(String value) {
            String rewritten = value;
            for (String[] rule : STAR_REWRITE_RULES) {
                rewritten = rewritten.replace(rule[0], rule[1]);
            }
            return rewritten;
        }
    }
}

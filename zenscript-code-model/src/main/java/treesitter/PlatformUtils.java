package treesitter;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.CodeSource;

public class PlatformUtils {

    static  {
//        PlatformUtils.addLibSearchPath();
        NativeLibrary.addSearchPath("tree-sitter-zenscript", "E:\\plugins\\zenscript-intelli-sense\\tree-sitter-zenscript\\build\\Debug");
    }

    public static TreeSitterLibrary loadLibrary() {
        final String baseLibName = "tree-sitter-zenscript";
        return Native.load(baseLibName, TreeSitterLibrary.class);
    }
    public static NativeLibrary loadNative() {
        final String baseLibName = "tree-sitter-zenscript";
        return NativeLibrary.getInstance(baseLibName);
    }


    private static void addLibSearchPath() {
        CodeSource codeSource = PlatformUtils.class.getProtectionDomain().getCodeSource();
        Path jarFile = null;
        try {
            jarFile = Path.of(codeSource.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Path libPath = jarFile.getParent().resolve("lib");

        libPath = libPath.resolve(getLibDir());


        NativeLibrary.addSearchPath(
                "tree-sitter-zenscript",
                libPath.toAbsolutePath().toString()
        );

    }


    private static String getLibDir() {
        if (Platform.isMac()) {
            if(Platform.isARM()) {
                return "macos-arm64";
            }
            return "macos-x64";
        }

        String systemName;
        if (Platform.isWindows()) {
            systemName = "win";
        } else if (Platform.isLinux()) {
            systemName = "linux";
        } else {
            // TODO not crash
            throw new RuntimeException("unsupported operating system!");
        }

        if(Platform.isARM()) {
            if(Platform.is64Bit()) {
                return systemName + "-arm64";
            }
            return systemName + "-arm";
        }

        if(Platform.is64Bit()) {
            return systemName + "-x64";
        }
        return systemName + "-x86";


    }
}

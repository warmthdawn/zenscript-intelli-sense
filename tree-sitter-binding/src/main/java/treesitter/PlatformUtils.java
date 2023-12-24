package treesitter;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PlatformUtils {


    public static TreeSitterLibrary loadLibrary() {
        final String baseLibName = "tree-sitter-zenscript";
        Path libPath = Paths.get(System.getProperty("user.dir"), "lib");

        libPath = libPath.resolve(getLibDir());


        NativeLibrary.addSearchPath(
                "tree-sitter-zenscript",
                libPath.toAbsolutePath().toString()
        );

        return Native.load(baseLibName, TreeSitterLibrary.class);

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

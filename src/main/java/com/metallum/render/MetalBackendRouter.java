package com.metallum.render;

import com.metallum.Metallum;
import com.mojang.renderpearl.api.device.GpuBackend;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Selects the Metal backend variant for the host without changing Minecraft's API option. */
@Environment(EnvType.CLIENT)
public final class MetalBackendRouter {
    enum Backend {
        IOS,
        MACOS_ARM64,
        MACOS_X86_64
    }

    private MetalBackendRouter() {
    }

    public static GpuBackend create() {
        HostPlatform.Kind platform = HostPlatform.current();
        boolean rosetta = HostPlatform.isRosetta();
        boolean allowRosetta = Boolean.getBoolean("metallum.allowRosetta");
        Metallum.LOGGER.info(
                "Metallum host platform: {}, arch={}, rosetta={}",
                HostPlatform.describe(platform),
                System.getProperty("os.arch", ""),
                rosetta
        );

        Backend backendKind = selectBackend(platform, rosetta, allowRosetta);
        GpuBackend backend = switch (backendKind) {
            case IOS -> new IOSMetalBackend();
            case MACOS_ARM64 -> new MetalBackend();
            case MACOS_X86_64 -> new MacOsIntelMetalBackend();
        };
        Metallum.LOGGER.info("Metallum selected backend: {}", backend.getName());
        return backend;
    }

    static Backend selectBackend(final HostPlatform.Kind platform, final boolean rosetta, final boolean allowRosetta) {
        if (HostPlatform.rejectRosetta(platform, rosetta, allowRosetta)) {
            throw new UnsupportedOperationException(
                    "Metallum detected an x86_64 process running under Rosetta on Apple Silicon. "
                            + "Use a native arm64 launcher instead. "
                            + "For diagnostics only, set -Dmetallum.allowRosetta=true to continue.");
        }
        return switch (platform) {
            case IOS -> Backend.IOS;
            case MACOS_ARM64 -> Backend.MACOS_ARM64;
            case MACOS_X86_64 -> Backend.MACOS_X86_64;
            case UNSUPPORTED -> throw new UnsupportedOperationException(
                    "Metallum does not support host platform: " + HostPlatform.describe(platform));
        };
    }
}

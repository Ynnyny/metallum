package com.metallum.render;

import com.metallum.Metallum;
import com.mojang.renderpearl.api.device.GpuBackend;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Selects the Metal backend variant for the host without changing Minecraft's API option. */
@Environment(EnvType.CLIENT)
public final class MetalBackendRouter {
    private MetalBackendRouter() {
    }

    public static GpuBackend create() {
        HostPlatform.Kind platform = HostPlatform.current();
        Metallum.LOGGER.info("Metallum host platform: {}", HostPlatform.describe(platform));
        return switch (platform) {
            case IOS -> new IOSMetalBackend();
            case MACOS_ARM64 -> new MetalBackend();
            case MACOS_X86_64 -> throw new UnsupportedOperationException(
                    "Metallum macOS x86_64 Metal provider is reserved for Phase 2");
            case UNSUPPORTED -> throw new UnsupportedOperationException(
                    "Metallum does not support host platform: " + HostPlatform.describe(platform));
        };
    }
}

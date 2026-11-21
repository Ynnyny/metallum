package com.metallum.render;

import com.mojang.renderpearl.api.device.DeviceFeatures;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.DeviceLimits;
import com.mojang.renderpearl.api.device.DeviceType;
import com.mojang.renderpearl.api.device.HintsAndWorkarounds;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import io.github.kokodio.metaljvm.metal.MTLDevice;
import io.github.kokodio.metaljvm.metal.MTLLanguageVersion;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Set;

@Environment(EnvType.CLIENT)
final class MetalCapabilities {
    static final int SPIRV_CROSS_MSL_2_0 = 0x020000;
    static final int SPIRV_CROSS_MSL_3_2 = 0x030200;
    static final int SPIRV_CROSS_MSL_4_0 = 0x040000;
    static final int APPLE_MSL_2_0 = 0x020000;
    static final int APPLE_MSL_3_2 = 0x030002;
    static final int APPLE_MSL_4_0 = 0x040000;
    static final int MAX_TEXTURE_SIZE = 16384;
    static final int MAX_ANISOTROPY = 16;
    static final int APPLE_MIN_UNIFORM_OFFSET_ALIGNMENT = 16;
    static final int MACOS_INTEL_MIN_UNIFORM_OFFSET_ALIGNMENT = 256;
    static final int INTEL_MAX_DRAW_COUNT = 65535;

    private MetalCapabilities() {
    }

    static int spirvCrossMslVersion(final HostPlatform.Kind kind) {
        return switch (kind) {
            case IOS -> SPIRV_CROSS_MSL_2_0;
            case MACOS_X86_64 -> SPIRV_CROSS_MSL_3_2;
            case MACOS_ARM64 -> SPIRV_CROSS_MSL_4_0;
            case UNSUPPORTED -> SPIRV_CROSS_MSL_4_0;
        };
    }

    static int spirvCrossMslVersion() {
        return spirvCrossMslVersion(HostPlatform.current());
    }

    static int appleMslVersion(final HostPlatform.Kind kind) {
        return switch (kind) {
            case IOS -> APPLE_MSL_2_0;
            case MACOS_X86_64 -> APPLE_MSL_3_2;
            case MACOS_ARM64 -> APPLE_MSL_4_0;
            case UNSUPPORTED -> APPLE_MSL_4_0;
        };
    }

    static MTLLanguageVersion appleLanguageVersion() {
        return switch (HostPlatform.current()) {
            case IOS -> MTLLanguageVersion.Version2_0;
            case MACOS_X86_64 -> MTLLanguageVersion.Version3_2;
            case MACOS_ARM64, UNSUPPORTED -> null;
        };
    }

    static int minUniformOffsetAlignment(final HostPlatform.Kind kind) {
        return kind == HostPlatform.Kind.MACOS_X86_64
                ? MACOS_INTEL_MIN_UNIFORM_OFFSET_ALIGNMENT
                : APPLE_MIN_UNIFORM_OFFSET_ALIGNMENT;
    }

    static DeviceLimits deviceLimits(final HostPlatform.Kind kind, final long maxMemoryAllocationSize) {
        int drawCount = kind == HostPlatform.Kind.MACOS_X86_64 ? INTEL_MAX_DRAW_COUNT : Integer.MAX_VALUE;
        return new DeviceLimits(
                MAX_ANISOTROPY,
                minUniformOffsetAlignment(kind),
                MAX_TEXTURE_SIZE,
                maxMemoryAllocationSize,
                drawCount,
                ColorTargetState.MAX_COLOR_TARGETS,
                drawCount
        );
    }

    static DeviceFeatures deviceFeatures(final HostPlatform.Kind kind) {
        if (kind == HostPlatform.Kind.MACOS_X86_64) {
            return new DeviceFeatures(false, true, true, true, true, true, false, true);
        }
        return new DeviceFeatures(true, true, true, true, true, true, true, true);
    }

    static HintsAndWorkarounds hintsAndWorkarounds(final HostPlatform.Kind kind) {
        return new HintsAndWorkarounds(false, false, true, false);
    }

    static DeviceInfo deviceInfo(final MTLDevice device, final String deviceName) {
        HostPlatform.Kind kind = HostPlatform.current();
        String vendorName = kind == HostPlatform.Kind.MACOS_X86_64 ? "Intel" : "Apple";
        String osVersion = System.getProperty("os.version", "").trim();
        String driverDescription = (kind == HostPlatform.Kind.IOS ? "iOS " : "macOS ") + osVersion;
        long maxMemoryAllocationSize = device.maxBufferLength();
        if (device.supportsRecommendedMaxWorkingSetSize()) {
            maxMemoryAllocationSize = Math.min(maxMemoryAllocationSize, device.recommendedMaxWorkingSetSize());
        }
        return new DeviceInfo(
                deviceName,
                vendorName,
                driverDescription,
                true,
                "Metal",
                1.0F,
                deviceLimits(kind, maxMemoryAllocationSize),
                deviceFeatures(kind),
                Set.of(),
                hintsAndWorkarounds(kind),
                DeviceType.INTEGRATED
        );
    }
}

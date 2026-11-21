package com.metallum.render;

import com.mojang.renderpearl.api.device.DeviceFeatures;
import com.mojang.renderpearl.api.device.DeviceLimits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetalCapabilitiesTest {
    @Test
    void usesConservativeIntelFeatures() {
        DeviceFeatures intel = MetalCapabilities.deviceFeatures(HostPlatform.Kind.MACOS_X86_64);
        assertFalse(intel.wireframeFillMode());
        assertTrue(intel.shaderDrawParameters());
        assertTrue(intel.multiDrawDirectInterleaved());
        assertTrue(intel.multiDrawDirectSeparate());
        assertTrue(intel.multiDrawIndirect());
        assertTrue(intel.drawIndirect());
        assertFalse(intel.nonZeroFirstInstance());
        assertTrue(intel.persistentMapping());

        DeviceFeatures apple = MetalCapabilities.deviceFeatures(HostPlatform.Kind.MACOS_ARM64);
        assertTrue(apple.wireframeFillMode());
        assertTrue(apple.nonZeroFirstInstance());
    }

    @Test
    void capsIntelDrawCounts() {
        DeviceLimits intel = MetalCapabilities.deviceLimits(HostPlatform.Kind.MACOS_X86_64, 1024L);
        assertEquals(MetalCapabilities.INTEL_MAX_DRAW_COUNT, intel.maxMultiDrawDirectInterleavedDrawCount());
        assertEquals(MetalCapabilities.INTEL_MAX_DRAW_COUNT, intel.maxDrawIndirectDrawCount());
        assertEquals(1024L, intel.maxMemoryAllocationSize());

        DeviceLimits apple = MetalCapabilities.deviceLimits(HostPlatform.Kind.MACOS_ARM64, 2048L);
        assertEquals(Integer.MAX_VALUE, apple.maxMultiDrawDirectInterleavedDrawCount());
        assertEquals(Integer.MAX_VALUE, apple.maxDrawIndirectDrawCount());
    }

    @Test
    void usesPlatformUniformOffsetAlignment() {
        assertEquals(256, MetalCapabilities.minUniformOffsetAlignment(HostPlatform.Kind.MACOS_X86_64));
        assertEquals(16, MetalCapabilities.minUniformOffsetAlignment(HostPlatform.Kind.MACOS_ARM64));
        assertEquals(16, MetalCapabilities.minUniformOffsetAlignment(HostPlatform.Kind.IOS));
        assertEquals(256, MetalCapabilities.deviceLimits(HostPlatform.Kind.MACOS_X86_64, 1024L).minUniformOffsetAlignment());
        assertEquals(16, MetalCapabilities.deviceLimits(HostPlatform.Kind.MACOS_ARM64, 1024L).minUniformOffsetAlignment());
    }

    @Test
    void selectsMslVersionsByPlatform() {
        assertEquals(0x020000, MetalCapabilities.spirvCrossMslVersion(HostPlatform.Kind.IOS));
        assertEquals(0x030200, MetalCapabilities.spirvCrossMslVersion(HostPlatform.Kind.MACOS_X86_64));
        assertEquals(0x040000, MetalCapabilities.spirvCrossMslVersion(HostPlatform.Kind.MACOS_ARM64));
        assertEquals(0x020000, MetalCapabilities.appleMslVersion(HostPlatform.Kind.IOS));
        assertEquals(0x030002, MetalCapabilities.appleMslVersion(HostPlatform.Kind.MACOS_X86_64));
        assertEquals(0x040000, MetalCapabilities.appleMslVersion(HostPlatform.Kind.MACOS_ARM64));
    }
}

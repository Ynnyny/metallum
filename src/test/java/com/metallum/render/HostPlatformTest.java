package com.metallum.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostPlatformTest {
    @Test
    void detectsIosByName() {
        assertEquals(HostPlatform.Kind.IOS, HostPlatform.detect("iOS", "aarch64", () -> false));
        assertEquals(HostPlatform.Kind.IOS, HostPlatform.detect("iPhone OS", "arm64", () -> false));
    }

    @Test
    void detectsIosByUIKitOnDarwinArm64() {
        assertEquals(HostPlatform.Kind.IOS, HostPlatform.detect("Darwin", "aarch64", () -> true));
    }

    @Test
    void detectsMacOsArm64() {
        assertEquals(HostPlatform.Kind.MACOS_ARM64, HostPlatform.detect("Mac OS X", "aarch64", () -> false));
        assertEquals(HostPlatform.Kind.MACOS_ARM64, HostPlatform.detect("Darwin", "arm64", () -> false));
    }

    @Test
    void detectsMacOsX86_64AndRosettaPolicy() {
        assertEquals(HostPlatform.Kind.MACOS_X86_64, HostPlatform.detect("Mac OS X", "x86_64", () -> true));
        assertEquals(HostPlatform.Kind.MACOS_X86_64, HostPlatform.detect("Darwin", "amd64", () -> false));
        assertEquals(HostPlatform.Kind.MACOS_X86_64, HostPlatform.detect("Darwin", "x86_64", () -> false));
    }

    @Test
    void rejectsRosettaUnlessExplicitlyAllowed() {
        assertTrue(HostPlatform.rejectRosetta(HostPlatform.Kind.MACOS_X86_64, true, false));
        assertFalse(HostPlatform.rejectRosetta(HostPlatform.Kind.MACOS_X86_64, true, true));
        assertFalse(HostPlatform.rejectRosetta(HostPlatform.Kind.MACOS_X86_64, false, false));
        assertFalse(HostPlatform.rejectRosetta(HostPlatform.Kind.MACOS_ARM64, true, false));
        assertFalse(HostPlatform.rejectRosetta(HostPlatform.Kind.IOS, true, false));
    }

    @Test
    void prefersLauncherPlatformHint() {
        assertEquals(HostPlatform.Kind.IOS,
                HostPlatform.detect("Mac OS X", "aarch64", "ios-arm64", () -> false));
        assertEquals(HostPlatform.Kind.MACOS_ARM64,
                HostPlatform.detect("Mac OS X", "aarch64", "macos-arm64", () -> true));
        assertEquals(HostPlatform.Kind.MACOS_X86_64,
                HostPlatform.detect("Mac OS X", "x86_64", "macos-x86_64", () -> false));
        assertEquals(HostPlatform.Kind.UNSUPPORTED,
                HostPlatform.detect("Mac OS X", "aarch64", "unknown", () -> false));
    }

    @Test
    void rejectsOtherPlatformsAndUnknownArchitectures() {
        assertEquals(HostPlatform.Kind.UNSUPPORTED, HostPlatform.detect("Linux", "aarch64", () -> true));
        assertEquals(HostPlatform.Kind.UNSUPPORTED, HostPlatform.detect("Windows 11", "amd64", () -> false));
        assertEquals(HostPlatform.Kind.UNSUPPORTED, HostPlatform.detect("Mac OS X", "unknown", () -> false));
    }
}

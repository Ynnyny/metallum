package com.metallum.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetalBackendRouterTest {
    @Test
    void selectsNativeBackends() {
        assertEquals(MetalBackendRouter.Backend.IOS,
                MetalBackendRouter.selectBackend(HostPlatform.Kind.IOS, false, false));
        assertEquals(MetalBackendRouter.Backend.MACOS_ARM64,
                MetalBackendRouter.selectBackend(HostPlatform.Kind.MACOS_ARM64, false, false));
        assertEquals(MetalBackendRouter.Backend.MACOS_X86_64,
                MetalBackendRouter.selectBackend(HostPlatform.Kind.MACOS_X86_64, false, false));
    }

    @Test
    void rejectsRosettaWithoutOverride() {
        assertThrows(UnsupportedOperationException.class,
                () -> MetalBackendRouter.selectBackend(HostPlatform.Kind.MACOS_X86_64, true, false));
        assertEquals(MetalBackendRouter.Backend.MACOS_X86_64,
                MetalBackendRouter.selectBackend(HostPlatform.Kind.MACOS_X86_64, true, true));
    }

    @Test
    void rejectsUnsupportedPlatform() {
        assertThrows(UnsupportedOperationException.class,
                () -> MetalBackendRouter.selectBackend(HostPlatform.Kind.UNSUPPORTED, false, false));
    }
}

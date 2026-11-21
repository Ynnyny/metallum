package com.metallum.render;

import com.metallum.Metallum;
import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.NonNull;

/** Direct iOS Metal backend. The SDL UIKit host view supplies the retained CAMetalLayer. */
@Environment(EnvType.CLIENT)
public final class IOSMetalBackend extends MetalBackend {
    @Override
    public @NonNull String getName() {
        return "Metal (iOS)";
    }

    @Override
    public @NonNull GpuDevice createDevice(final @NonNull GpuDebugOptions debugOptions) throws BackendCreationException {
        Metallum.LOGGER.info("Creating direct iOS Metal device");
        return super.createDevice(debugOptions);
    }
}

package com.metallum.render;

import com.metallum.Metallum;
import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.NonNull;

/** Native Metal backend for macOS on Intel GPUs (Metal 3, MSL 3.2). */
@Environment(EnvType.CLIENT)
public final class MacOsIntelMetalBackend extends MetalBackend {
    @Override
    public @NonNull String getName() {
        return "Metal (macOS Intel)";
    }

    @Override
    public @NonNull GpuDevice createDevice(final @NonNull GpuDebugOptions debugOptions) throws BackendCreationException {
        Metallum.LOGGER.info("Creating native Metal device for macOS x86_64 (Intel)");
        return super.createDevice(debugOptions);
    }
}

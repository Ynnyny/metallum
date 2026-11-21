package com.metallum.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class IntelProviderSmokeTest {
    @BeforeEach
    void requireIntelHost() {
        assumeTrue(HostPlatform.isMacOSX86_64(), "Intel provider tests require macOS x86_64");
    }

    private static final String VERTEX_SHADER = """
            #version 450
            void main() {
                vec2 p = vec2(float((gl_VertexIndex << 1) & 2), float(gl_VertexIndex & 2));
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 450
            layout(location = 0) out vec4 fragColor;
            void main() {
                fragColor = vec4(0.0, 1.0, 0.0, 1.0);
            }
            """;

    @Test
    void createsIntelDeviceClearsAndReadsBack() throws Exception {
        MacOsIntelMetalBackend backend = new MacOsIntelMetalBackend();
        assertEquals("Metal (macOS Intel)", backend.getName());

        GpuDevice device = backend.createDevice(new GpuDebugOptions(0, false, false, false));
        try {
            DeviceInfo info = device.getDeviceInfo();
            assertEquals("Intel", info.vendorName());
            assertEquals("Metal", info.backendName());
            assertTrue(info.name().contains("Intel"), info.name());

            GpuTexture texture = device.createTexture(
                    "intel-smoke",
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC,
                    GpuFormat.RGBA8_UNORM,
                    64,
                    64,
                    1,
                    1
            );
            GpuTextureView view = device.createTextureView(texture);
            CommandEncoder encoder = device.createCommandEncoder();
            try (RenderPass pass = encoder.createRenderPass(() -> "intel-smoke-clear", view, Optional.of(new Vector4f(1.0F, 0.0F, 0.0F, 1.0F)))) {
            }
            GpuFence clearFence = encoder.createFence();
            encoder.submit();
            clearFence.awaitCompletion(GpuFence.NO_TIMEOUT);

            GpuBuffer readback = device.createBuffer(
                    () -> "intel-smoke-readback",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ,
                    64L * 64L * 4L
            );
            encoder.copyTextureToBuffer(texture, readback, 0L, () -> {
            }, 0);
            GpuFence readbackFence = encoder.createFence();
            encoder.submit();
            readbackFence.awaitCompletion(GpuFence.NO_TIMEOUT);

            try (GpuBufferSlice.MappedView mapped = readback.map(true, false)) {
                ByteBuffer pixels = mapped.data();
                assertEquals((byte) 255, pixels.get(0));
                assertEquals((byte) 0, pixels.get(1));
                assertEquals((byte) 0, pixels.get(2));
                assertEquals((byte) 255, pixels.get(3));
            }

            readback.close();
            view.close();
            texture.close();
        } finally {
            device.close();
        }
    }

    @Test
    void uploadsCopiesAndReadsBack() throws Exception {
        MacOsIntelMetalBackend backend = new MacOsIntelMetalBackend();
        GpuDevice device = backend.createDevice(new GpuDebugOptions(0, false, false, false));
        try {
            GpuTexture source = device.createTexture(
                    "intel-smoke-upload-source",
                    GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.RGBA8_UNORM,
                    4,
                    4,
                    1,
                    1
            );
            GpuTexture destination = device.createTexture(
                    "intel-smoke-upload-destination",
                    GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.RGBA8_UNORM,
                    4,
                    4,
                    1,
                    1
            );

            ByteBuffer pixels = ByteBuffer.allocateDirect(4 * 4 * 4);
            for (int i = 0; i < 4 * 4; i++) {
                pixels.put((byte) 0);
                pixels.put((byte) 0);
                pixels.put((byte) 255);
                pixels.put((byte) 255);
            }
            pixels.flip();

            GpuBuffer upload = device.createBuffer(() -> "intel-smoke-upload", GpuBuffer.USAGE_COPY_SRC, pixels);
            GpuBuffer readback = device.createBuffer(
                    () -> "intel-smoke-upload-readback",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ,
                    4L * 4L * 4L
            );

            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyBufferToTexture(upload.slice(), 0, 0, 4, 4, source, 0, 0, 4, 4, 0, 0);
            encoder.copyTextureToTexture(source, destination, 0, 0, 0, 0, 0, 4, 4);
            encoder.copyTextureToBuffer(destination, readback, 0L, () -> {
            }, 0);
            GpuFence fence = encoder.createFence();
            encoder.submit();
            fence.awaitCompletion(GpuFence.NO_TIMEOUT);

            try (GpuBufferSlice.MappedView mapped = readback.map(true, false)) {
                ByteBuffer result = mapped.data();
                assertEquals((byte) 0, result.get(0));
                assertEquals((byte) 0, result.get(1));
                assertEquals((byte) 255, result.get(2));
                assertEquals((byte) 255, result.get(3));
            }

            readback.close();
            upload.close();
            destination.close();
            source.close();
        } finally {
            device.close();
        }
    }

    @Test
    void drawsGreenTriangleThroughProvider() throws Exception {
        MacOsIntelMetalBackend backend = new MacOsIntelMetalBackend();
        GpuDevice device = backend.createDevice(new GpuDebugOptions(0, false, false, false));
        Field renderDevice = RenderSystem.class.getDeclaredField("DEVICE");
        renderDevice.setAccessible(true);
        renderDevice.set(null, device);

        GpuTexture texture = null;
        GpuTextureView view = null;
        GpuBuffer readback = null;
        CompiledRenderPipeline pipeline = null;
        try {
            RenderPipeline renderPipeline = RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath("metallum", "intel_smoke_draw"))
                    .withVertexShader("intel_smoke_draw/vertex")
                    .withFragmentShader("intel_smoke_draw/fragment")
                    .withColorTargetState(new ColorTargetState(
                            Optional.empty(),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_ALL
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build();

            ShaderSource source = new ShaderSource() {
                @Override
                public String getShader(final Identifier id, final ShaderType type) {
                    return type == ShaderType.VERTEX ? VERTEX_SHADER : FRAGMENT_SHADER;
                }

                @Override
                public CachedIncludeSource getInclude(final Identifier id) {
                    return null;
                }

                @Override
                public void close() {
                }
            };

            CompletableFuture<CompiledRenderPipeline.Pending> future =
                    device.compilePipeline(renderPipeline, source, Runnable::run);
            pipeline = future.join().finishCompile();
            if (pipeline == null) {
                throw new AssertionError("pipeline compilation returned null");
            }

            texture = device.createTexture(
                    "intel-smoke-draw",
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC,
                    GpuFormat.RGBA8_UNORM,
                    64,
                    64,
                    1,
                    1
            );
            view = device.createTextureView(texture);

            CommandEncoder encoder = device.createCommandEncoder();
            try (RenderPass pass = encoder.createRenderPass(
                    () -> "intel-smoke-draw-pass",
                    view,
                    Optional.of(new Vector4f(1.0F, 0.0F, 0.0F, 1.0F))
            )) {
                pass.setPipeline(pipeline);
                pass.draw(3, 1, 0, 0);
            }

            readback = device.createBuffer(
                    () -> "intel-smoke-draw-readback",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ,
                    64L * 64L * 4L
            );
            encoder.copyTextureToBuffer(texture, readback, 0L, () -> {
            }, 0);
            GpuFence fence = encoder.createFence();
            encoder.submit();
            fence.awaitCompletion(GpuFence.NO_TIMEOUT);

            try (GpuBufferSlice.MappedView mapped = readback.map(true, false)) {
                ByteBuffer pixels = mapped.data();
                int offset = ((64 / 2) * 64 + (64 / 2)) * 4;
                assertEquals((byte) 0, pixels.get(offset));
                assertEquals((byte) 255, pixels.get(offset + 1));
                assertEquals((byte) 0, pixels.get(offset + 2));
                assertEquals((byte) 255, pixels.get(offset + 3));
            }
        } finally {
            if (pipeline != null) pipeline.close();
            if (readback != null) readback.close();
            if (view != null) view.close();
            if (texture != null) texture.close();
            renderDevice.set(null, null);
            device.close();
        }
    }
}

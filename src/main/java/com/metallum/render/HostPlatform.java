package com.metallum.render;

import io.github.kokodio.metaljvm.objc.ObjC;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/** Runtime host detection used by the backend router and iOS compatibility guards. */
public final class HostPlatform {
    public enum Kind {
        IOS,
        MACOS_ARM64,
        MACOS_X86_64,
        UNSUPPORTED
    }

    private static final Kind CURRENT = detect(
            System.getProperty("os.name", ""),
            System.getProperty("os.arch", ""),
            System.getProperty("metallum.platform", ""),
            HostPlatform::hasUIKit
    );
    private static final boolean ROSETTA = detectRosetta();

    private HostPlatform() {
    }

    public static Kind current() {
        return CURRENT;
    }

    public static boolean isIOS() {
        return CURRENT == Kind.IOS;
    }

    public static boolean isMacOS() {
        return CURRENT == Kind.MACOS_ARM64 || CURRENT == Kind.MACOS_X86_64;
    }

    public static boolean isMacOSX86_64() {
        return CURRENT == Kind.MACOS_X86_64;
    }

    /** True when this x86_64 process is running under Apple's Rosetta translation layer. */
    public static boolean isRosetta() {
        return ROSETTA;
    }

    /**
     * The Intel provider must not silently initialize on Rosetta: the native GPU path is
     * significantly slower and may expose a reduced feature set. An explicit opt-in is
     * available for diagnostics via {@code -Dmetallum.allowRosetta=true}.
     */
    static boolean rejectRosetta(final Kind kind, final boolean rosetta, final boolean allowRosetta) {
        return kind == Kind.MACOS_X86_64 && rosetta && !allowRosetta;
    }

    public static String describe(final Kind kind) {
        return switch (kind) {
            case IOS -> "iOS";
            case MACOS_ARM64 -> "macOS arm64";
            case MACOS_X86_64 -> "macOS x86_64";
            case UNSUPPORTED -> "unsupported";
        };
    }

    static Kind detect(final String osName, final String osArch, final BooleanSupplier uiKitAvailable) {
        return detect(osName, osArch, "", uiKitAvailable);
    }

    static Kind detect(
            final String osName,
            final String osArch,
            final String platformHint,
            final BooleanSupplier uiKitAvailable
    ) {
        String hint = platformHint.toLowerCase(Locale.ROOT);
        switch (hint) {
            case "ios":
            case "ios-arm64":
                return Kind.IOS;
            case "macos-arm64":
                return Kind.MACOS_ARM64;
            case "macos-x86_64":
                return Kind.MACOS_X86_64;
            case "":
                break;
            default:
                return Kind.UNSUPPORTED;
        }

        String name = osName.toLowerCase(Locale.ROOT);
        String arch = osArch.toLowerCase(Locale.ROOT);

        if (name.contains("ios") || name.contains("iphone") || name.contains("ipad")) {
            return Kind.IOS;
        }
        if (!name.contains("mac") && !name.contains("darwin")) {
            return Kind.UNSUPPORTED;
        }

        boolean arm64 = arch.contains("aarch64") || arch.contains("arm64");
        if (arm64) {
            return uiKitAvailable.getAsBoolean() ? Kind.IOS : Kind.MACOS_ARM64;
        }
        if (arch.equals("x86_64") || arch.equals("amd64") || arch.equals("x86")) {
            return Kind.MACOS_X86_64;
        }
        return Kind.UNSUPPORTED;
    }

    private static boolean detectRosetta() {
        try {
            Linker linker = Linker.nativeLinker();
            MethodHandle sysctlbyname = linker.downcallHandle(
                    linker.defaultLookup().find("sysctlbyname").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG)
            );
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment name = arena.allocateFrom("sysctl.proc_translated");
                MemorySegment value = arena.allocate(JAVA_INT);
                MemorySegment size = arena.allocate(JAVA_LONG);
                size.set(JAVA_LONG, 0L, JAVA_INT.byteSize());
                int result = (int) sysctlbyname.invokeExact(
                        name,
                        value,
                        size,
                        MemorySegment.NULL,
                        0L
                );
                return result == 0 && value.get(JAVA_INT, 0L) == 1;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasUIKit() {
        try {
            ObjC.framework("UIKit");
            ObjC.clazz("UIDevice");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

package com.metallum.render;

import io.github.kokodio.metaljvm.objc.ObjC;

import java.util.Locale;
import java.util.function.BooleanSupplier;

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

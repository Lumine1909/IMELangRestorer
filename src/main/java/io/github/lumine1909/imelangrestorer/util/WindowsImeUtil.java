package io.github.lumine1909.imelangrestorer.util;

import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.Platform;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class WindowsImeUtil {

    private static final int IME_CMODE_NATIVE = 1024 | 1;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup IMM32;

    private static final MethodHandle IMM_GET_CONTEXT;
    private static final MethodHandle IMM_RELEASE_CONTEXT;
    private static final MethodHandle IMM_GET_CONVERSION_STATUS;
    private static final MethodHandle IMM_SET_CONVERSION_STATUS;

    static {
        if (Platform.get() == Platform.WINDOWS) {
            IMM32 = SymbolLookup.libraryLookup("imm32", Arena.ofShared());
            IMM_GET_CONTEXT = LINKER.downcallHandle(
                IMM32.findOrThrow("ImmGetContext"),
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );
            IMM_RELEASE_CONTEXT = LINKER.downcallHandle(
                IMM32.findOrThrow("ImmReleaseContext"),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );
            IMM_GET_CONVERSION_STATUS = LINKER.downcallHandle(
                IMM32.findOrThrow("ImmGetConversionStatus"),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );
            IMM_SET_CONVERSION_STATUS = LINKER.downcallHandle(
                IMM32.findOrThrow("ImmSetConversionStatus"),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
            );
        } else {
            IMM32 = null;
            IMM_GET_CONTEXT = null;
            IMM_RELEASE_CONTEXT = null;
            IMM_GET_CONVERSION_STATUS = null;
            IMM_SET_CONVERSION_STATUS = null;
        }
    }

    private WindowsImeUtil() {
    }

    public static boolean isNative(long glfwWindow) {
        if (IMM32 == null) {
            return false;
        }

        long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
        if (hwnd == 0L) {
            return false;
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment hwndSegment = MemorySegment.ofAddress(hwnd).reinterpret(0);
            MemorySegment himc = (MemorySegment) IMM_GET_CONTEXT.invokeExact(hwndSegment);
            try {
                if (himc.equals(MemorySegment.NULL)) {
                    return false;
                }
                MemorySegment conversion = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment sentence = arena.allocate(ValueLayout.JAVA_INT);
                int result = (int) IMM_GET_CONVERSION_STATUS.invokeExact(himc, conversion, sentence);
                if (result == 0) {
                    return false;
                }
                int conversionMode = conversion.get(ValueLayout.JAVA_INT, 0);
                return (conversionMode & IME_CMODE_NATIVE) != 0;
            } finally {
                int _ = (int) IMM_RELEASE_CONTEXT.invokeExact(hwndSegment, himc);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setNative(long glfwWindow, boolean nativeMode) {
        if (IMM32 == null) {
            return;
        }

        long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
        if (hwnd == 0L) {
            return;
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment hwndSegment = MemorySegment.ofAddress(hwnd).reinterpret(0);
            MemorySegment himc = (MemorySegment) IMM_GET_CONTEXT.invokeExact(hwndSegment);
            try {
                if (himc.equals(MemorySegment.NULL)) {
                    return;
                }
                MemorySegment conversion = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment sentence = arena.allocate(ValueLayout.JAVA_INT);
                int result = (int) IMM_GET_CONVERSION_STATUS.invokeExact(himc, conversion, sentence);
                if (result == 0) {
                    return;
                }
                int conversionMode = conversion.get(ValueLayout.JAVA_INT, 0);
                int sentenceMode = sentence.get(ValueLayout.JAVA_INT, 0);
                if (nativeMode) {
                    conversionMode |= IME_CMODE_NATIVE;
                } else {
                    conversionMode &= ~IME_CMODE_NATIVE;
                }
                int _ = (int) IMM_SET_CONVERSION_STATUS.invokeExact(himc, conversionMode, sentenceMode);
            } finally {
                int _ = (int) IMM_RELEASE_CONTEXT.invokeExact(hwndSegment, himc);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
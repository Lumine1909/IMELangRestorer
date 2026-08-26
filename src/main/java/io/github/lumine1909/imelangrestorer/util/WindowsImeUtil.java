package io.github.lumine1909.imelangrestorer.util;

import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.system.SharedLibrary;

import java.nio.IntBuffer;

import static org.lwjgl.system.Checks.CHECKS;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.Library.loadNative;

import static org.lwjgl.system.MemoryUtil.*;

public final class WindowsImeUtil {

    private static final SharedLibrary IMM32;

    private static final long IMM_GET_CONTEXT;
    private static final long IMM_RELEASE_CONTEXT;
    private static final long IMM_GET_CONVERSION_STATUS;
    private static final long IMM_SET_CONVERSION_STATUS;

    private static final int IME_CMODE_NATIVE = 0x0001;

    static {
        if (Platform.get() == Platform.WINDOWS) {
            IMM32 = loadNative("imm32", "imm32");
            IMM_GET_CONTEXT = IMM32.getFunctionAddress("ImmGetContext");
            IMM_RELEASE_CONTEXT = IMM32.getFunctionAddress("ImmReleaseContext");
            IMM_GET_CONVERSION_STATUS = IMM32.getFunctionAddress("ImmGetConversionStatus");
            IMM_SET_CONVERSION_STATUS = IMM32.getFunctionAddress("ImmSetConversionStatus");
        } else {
            IMM32 = null;
            IMM_GET_CONTEXT = 0L;
            IMM_RELEASE_CONTEXT = 0L;
            IMM_GET_CONVERSION_STATUS = 0L;
            IMM_SET_CONVERSION_STATUS = 0L;
        }
    }

    private WindowsImeUtil() {
    }

    public static boolean isNative(final long glfwWindow) {
        if (IMM32 == null) {
            return false;
        }

        final long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
        if (hwnd == 0L) {
            return false;
        }

        if (CHECKS) {
            check(IMM_GET_CONTEXT);
            check(IMM_GET_CONVERSION_STATUS);
            check(IMM_RELEASE_CONTEXT);
        }

        // HIMC ImmGetContext(HWND hWnd)
        final long himc = JNI.callPP(
            hwnd,
            IMM_GET_CONTEXT
        );

        if (himc == 0L) {
            return false;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer conversion = stack.mallocInt(1);
            IntBuffer sentence = stack.mallocInt(1);

            // BOOL ImmGetConversionStatus(HIMC hIMC, LPDWORD lpfdwConversion, LPDWORD lpfdwSentence);
            int result = JNI.callPPPI(himc, memAddress(conversion), memAddress(sentence), IMM_GET_CONVERSION_STATUS);
            if (result == 0) {
                return false;
            }

            return (conversion.get(0) & IME_CMODE_NATIVE) != 0;
        } finally {
            // BOOL ImmReleaseContext(HWND hWnd, HIMC hIMC)
            JNI.callPPI(hwnd, himc, IMM_RELEASE_CONTEXT);
        }
    }


    public static void setNative(final long glfwWindow, final boolean nativeMode) {
        if (IMM32 == null) {
            return;
        }

        final long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
        if (hwnd == 0L) {
            return;
        }

        if (CHECKS) {
            check(IMM_GET_CONTEXT);
            check(IMM_GET_CONVERSION_STATUS);
            check(IMM_SET_CONVERSION_STATUS);
            check(IMM_RELEASE_CONTEXT);
        }

        // HIMC ImmGetContext(HWND hWnd)
        final long himc = JNI.callPP(hwnd, IMM_GET_CONTEXT);

        if (himc == 0L) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer conversion = stack.mallocInt(1);
            IntBuffer sentence = stack.mallocInt(1);

            // BOOL ImmGetConversionStatus(HIMC hIMC, LPDWORD lpfdwConversion, LPDWORD lpfdwSentence);
            int result = JNI.callPPPI(himc, memAddress(conversion), memAddress(sentence), IMM_GET_CONVERSION_STATUS);
            if (result == 0) {
                return;
            }

            int conversionMode = conversion.get(0);
            int sentenceMode = sentence.get(0);
            if (nativeMode) {
                conversionMode |= IME_CMODE_NATIVE;
            } else {
                conversionMode &= ~IME_CMODE_NATIVE;
            }

            // BOOL ImmSetConversionStatus(HIMC hIMC, DWORD fdwConversion, DWORD fdwSentence);
            JNI.callPPPI(himc, conversionMode, sentenceMode, IMM_SET_CONVERSION_STATUS);
        } finally {
            // BOOL ImmReleaseContext(HWND hWnd, HIMC hIMC)
            JNI.callPPI(hwnd, himc, IMM_RELEASE_CONTEXT);
        }
    }
}
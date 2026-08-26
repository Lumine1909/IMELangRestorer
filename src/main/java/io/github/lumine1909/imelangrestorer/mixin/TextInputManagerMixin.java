package io.github.lumine1909.imelangrestorer.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.TextInputManager;
import io.github.lumine1909.imelangrestorer.util.WindowsImeUtil;
import org.spongepowered.asm.mixin.*;

import com.mojang.blaze3d.platform.Window;

@Mixin(TextInputManager.class)
public abstract class TextInputManagerMixin {

    @Shadow
    @Final
    private Window window;

    @Unique
    private boolean cachedNative = false;

    @Unique
    private boolean hasCachedNative = false;

    @WrapMethod(method = "setIMEInputMode")
    private void onSetIMEInputMode(boolean value, Operation<Void> original) {
        if (value) {
            original.call(true);
            if (hasCachedNative) {
                WindowsImeUtil.setNative(window.handle(), cachedNative);
                hasCachedNative = false;
            }
        } else {
            if (!hasCachedNative) {
                cachedNative = WindowsImeUtil.isNative(window.handle());
                hasCachedNative = true;
            }
            original.call(false);
        }
    }
}
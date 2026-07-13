package com.pg85.otg.shared.mixin;

import com.pg85.otg.shared.i18n.OTGTranslations;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public abstract class LanguageMixin {

    @Inject(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void otg$getOrDefault(String key, CallbackInfoReturnable<String> cir) {
        String value = OTGTranslations.get(key);
        if (value != null) {
            cir.setReturnValue(value);
        }
    }
}

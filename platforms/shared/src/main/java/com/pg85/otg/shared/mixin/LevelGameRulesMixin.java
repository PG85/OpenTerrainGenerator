package com.pg85.otg.shared.mixin;

import com.pg85.otg.shared.gamerules.GameRuleManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelGameRulesMixin {

    @Inject(method = "getGameRules", at = @At("HEAD"), cancellable = true)
    private void otg$getGameRules(CallbackInfoReturnable<GameRules> cir) {
        if ((Object) this instanceof ServerLevel serverLevel) {
            GameRules custom = GameRuleManager.getGameRules(serverLevel.dimension());
            if (custom != null) {
                cir.setReturnValue(custom);
            }
        }
    }
}

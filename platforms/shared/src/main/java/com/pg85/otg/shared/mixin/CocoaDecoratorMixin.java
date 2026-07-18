package com.pg85.otg.shared.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.treedecorators.CocoaDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CocoaDecorator.class)
public class CocoaDecoratorMixin {
    @Inject(method = "place",
            at = @At("HEAD"),
            cancellable = true)
    private void checkLogs(TreeDecorator.Context context, CallbackInfo ci) {
        List<BlockPos> list = context.logs();
        if (list.isEmpty()) {
            ci.cancel();
        }
    }
}

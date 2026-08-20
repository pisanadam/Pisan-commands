package dev.khaoscube.pisancommands.mixin;

import dev.khaoscube.pisancommands.PisanCommandsMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class ModernMinecartMixin {
    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void pisan$overrideMaxSpeed(ServerLevel level, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(PisanCommandsMod.CONFIG.getMinecartSpeed() / 20.0);
    }
}

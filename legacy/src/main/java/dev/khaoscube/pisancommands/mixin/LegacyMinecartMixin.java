package dev.khaoscube.pisancommands.mixin;

import dev.khaoscube.pisancommands.PisanCommandsMod;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class LegacyMinecartMixin {
    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true, require = 0)
    private void pisan$overrideMaxSpeed(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(PisanCommandsMod.CONFIG.getMinecartSpeed() / 20.0);
    }
}

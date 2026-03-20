package com.townsfolk.signs.mixin;

import com.townsfolk.signs.TownsfolkSignHandler;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallSignBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignBlock.class)
public abstract class WallSignBlockMixin {
    
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void townsfolk$onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!(state.getBlock() instanceof WallSignBlock)) {
            return;
        }
        
        if (player.isSneaking()) {
            return;
        }
        
        Direction facing = state.get(Properties.HORIZONTAL_FACING);
        
        if (!world.isClient) {
            TownsfolkSignHandler.handleSignInteraction(player, pos, facing);
        }
        
        cir.setReturnValue(ActionResult.success(world.isClient));
    }
}

package com.townsfolk.signs.mixin;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SignBlockEntity.class)
public interface SignBlockEntityMixin {
    @Invoker("setText")
    boolean invokeSetText(SignText text, boolean front);
}

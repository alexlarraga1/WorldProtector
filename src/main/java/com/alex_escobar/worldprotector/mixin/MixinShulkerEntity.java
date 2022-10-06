package com.alex_escobar.worldprotector.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

@Mixin(Shulker.class)
public abstract class MixinShulkerEntity  extends AbstractGolem implements Enemy {

    protected MixinShulkerEntity(EntityType<? extends AbstractGolem> type, Level worldIn) {
        super(type, worldIn);
    }

    @Nullable
    @Override
    public Entity changeDimension(ServerLevel server) {
        if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(this, server.dimension())) {
            return null;
        }
        return super.changeDimension(server);
    }
}

package net.minecraft.world.phys.shapes;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface CollisionContext {
    static CollisionContext empty() {
        return EntityCollisionContext.EMPTY;
    }

    static CollisionContext of(Entity pEntity) {
        Objects.requireNonNull(pEntity);

        return (CollisionContext)(switch (pEntity) {
            case AbstractMinecart abstractminecart -> AbstractMinecart.useExperimentalMovement(abstractminecart.level())
            ? new MinecartCollisionContext(abstractminecart, false)
            : new EntityCollisionContext(pEntity, false);
            default -> new EntityCollisionContext(pEntity, false);
        });
    }

    static CollisionContext of(Entity pEntity, boolean pCanStandOnFluid) {
        return new EntityCollisionContext(pEntity, pCanStandOnFluid);
    }

    boolean isDescending();

    boolean isAbove(VoxelShape pShape, BlockPos pPos, boolean pCanAscend);

    boolean isHoldingItem(Item pItem);

    boolean canStandOnFluid(FluidState pFluid1, FluidState pFluid2);

    VoxelShape getCollisionShape(BlockState pState, CollisionGetter pCollisionGetter, BlockPos pPos);
}
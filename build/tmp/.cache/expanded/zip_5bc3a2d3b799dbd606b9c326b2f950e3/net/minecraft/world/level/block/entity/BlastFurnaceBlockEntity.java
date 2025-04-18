package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class BlastFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    public BlastFurnaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.BLAST_FURNACE, pPos, pBlockState, RecipeType.BLASTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.blast_furnace");
    }

    @Override
    protected int getBurnDuration(FuelValues p_366938_, ItemStack p_58852_) {
        return super.getBurnDuration(p_366938_, p_58852_) / 2;
    }

    @Override
    protected AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
        return new BlastFurnaceMenu(pId, pPlayer, this, this.dataAccess);
    }
}
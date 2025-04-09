package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SwordItem extends Item {
    public SwordItem(ToolMaterial pMaterial, float pAttackDamage, float pAttackSpeed, Item.Properties pProperties) {
        super(pMaterial.applySwordProperties(pProperties, pAttackDamage, pAttackSpeed));
    }

    @Override
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return !pPlayer.isCreative();
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack p_342189_, LivingEntity p_344347_, LivingEntity p_343888_) {
        p_342189_.hurtAndBreak(1, p_343888_, EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction toolAction) {
       return net.minecraftforge.common.ToolActions.DEFAULT_SWORD_ACTIONS.contains(toolAction);
    }
}

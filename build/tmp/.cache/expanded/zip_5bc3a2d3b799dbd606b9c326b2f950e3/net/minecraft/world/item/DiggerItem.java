package net.minecraft.world.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

public class DiggerItem extends Item {
    public DiggerItem(ToolMaterial pMaterial, TagKey<Block> pMineableBlocks, float pAttackDamage, float pAttackSpeed, Item.Properties pProperties) {
        super(pMaterial.applyToolProperties(pProperties, pMineableBlocks, pAttackDamage, pAttackSpeed));
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack p_345276_, LivingEntity p_342379_, LivingEntity p_342949_) {
        p_345276_.hurtAndBreak(2, p_342949_, EquipmentSlot.MAINHAND);
    }
}
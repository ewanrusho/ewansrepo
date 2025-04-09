package net.minecraft.world.item;

import net.minecraft.tags.BlockTags;

public class PickaxeItem extends DiggerItem {
    public PickaxeItem(ToolMaterial pMaterial, float pAttackDamage, float pAttackSpeed, Item.Properties pProperties) {
        super(pMaterial, BlockTags.MINEABLE_WITH_PICKAXE, pAttackDamage, pAttackSpeed, pProperties);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction toolAction) {
        return net.minecraftforge.common.ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(toolAction);
    }
}

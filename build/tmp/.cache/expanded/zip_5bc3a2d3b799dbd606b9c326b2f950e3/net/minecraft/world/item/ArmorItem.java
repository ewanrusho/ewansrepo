package net.minecraft.world.item;

import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public class ArmorItem extends Item {
    public ArmorItem(ArmorMaterial pMaterial, ArmorType pArmorType, Item.Properties pProperties) {
        super(pMaterial.humanoidProperties(pProperties, pArmorType));
    }
}
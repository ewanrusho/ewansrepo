package net.minecraft.world.item.equipment;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record ArmorMaterial(
    int durability,
    Map<ArmorType, Integer> defense,
    int enchantmentValue,
    Holder<SoundEvent> equipSound,
    float toughness,
    float knockbackResistance,
    TagKey<Item> repairIngredient,
    ResourceKey<EquipmentAsset> assetId
) {
    public Item.Properties humanoidProperties(Item.Properties pProperties, ArmorType pArmorType) {
        return pProperties.durability(pArmorType.getDurability(this.durability))
            .attributes(this.createAttributes(pArmorType))
            .enchantable(this.enchantmentValue)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(pArmorType.getSlot()).setEquipSound(this.equipSound).setAsset(this.assetId).build())
            .repairable(this.repairIngredient);
    }

    public Item.Properties animalProperties(Item.Properties pProperties, HolderSet<EntityType<?>> pAllowedEntities) {
        return pProperties.durability(ArmorType.BODY.getDurability(this.durability))
            .attributes(this.createAttributes(ArmorType.BODY))
            .repairable(this.repairIngredient)
            .component(
                DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.BODY).setEquipSound(this.equipSound).setAsset(this.assetId).setAllowedEntities(pAllowedEntities).build()
            );
    }

    public Item.Properties animalProperties(Item.Properties pProperties, Holder<SoundEvent> pEquipSound, boolean pDamageOnHurt, HolderSet<EntityType<?>> pAllowedEntities) {
        if (pDamageOnHurt) {
            pProperties = pProperties.durability(ArmorType.BODY.getDurability(this.durability)).repairable(this.repairIngredient);
        }

        return pProperties.attributes(this.createAttributes(ArmorType.BODY))
            .component(
                DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.BODY).setEquipSound(pEquipSound).setAsset(this.assetId).setAllowedEntities(pAllowedEntities).setDamageOnHurt(pDamageOnHurt).build()
            );
    }

    private ItemAttributeModifiers createAttributes(ArmorType pArmorType) {
        int i = this.defense.getOrDefault(pArmorType, 0);
        ItemAttributeModifiers.Builder itemattributemodifiers$builder = ItemAttributeModifiers.builder();
        EquipmentSlotGroup equipmentslotgroup = EquipmentSlotGroup.bySlot(pArmorType.getSlot());
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("armor." + pArmorType.getName());
        itemattributemodifiers$builder.add(
            Attributes.ARMOR, new AttributeModifier(resourcelocation, (double)i, AttributeModifier.Operation.ADD_VALUE), equipmentslotgroup
        );
        itemattributemodifiers$builder.add(
            Attributes.ARMOR_TOUGHNESS, new AttributeModifier(resourcelocation, (double)this.toughness, AttributeModifier.Operation.ADD_VALUE), equipmentslotgroup
        );
        if (this.knockbackResistance > 0.0F) {
            itemattributemodifiers$builder.add(
                Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(resourcelocation, (double)this.knockbackResistance, AttributeModifier.Operation.ADD_VALUE), equipmentslotgroup
            );
        }

        return itemattributemodifiers$builder.build();
    }
}
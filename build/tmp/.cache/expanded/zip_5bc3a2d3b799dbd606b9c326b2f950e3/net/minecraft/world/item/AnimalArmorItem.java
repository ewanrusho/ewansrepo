package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.equipment.ArmorMaterial;

public class AnimalArmorItem extends Item {
    private final AnimalArmorItem.BodyType bodyType;

    public AnimalArmorItem(ArmorMaterial pMaterial, AnimalArmorItem.BodyType pBodyType, Item.Properties pProperties) {
        super(pMaterial.animalProperties(pProperties, pBodyType.allowedEntities));
        this.bodyType = pBodyType;
    }

    public AnimalArmorItem(
        ArmorMaterial pMaterial, AnimalArmorItem.BodyType pBodyType, Holder<SoundEvent> pEquipSound, boolean pDamageOnHurt, Item.Properties pProperties
    ) {
        super(pMaterial.animalProperties(pProperties, pEquipSound, pDamageOnHurt, pBodyType.allowedEntities));
        this.bodyType = pBodyType;
    }

    @Override
    public SoundEvent getBreakingSound() {
        return this.bodyType.breakingSound;
    }

    public static enum BodyType implements net.minecraftforge.common.IExtensibleEnum {
        EQUESTRIAN(SoundEvents.ITEM_BREAK, EntityType.HORSE),
        CANINE(SoundEvents.WOLF_ARMOR_BREAK, EntityType.WOLF);

        final SoundEvent breakingSound;
        final HolderSet<EntityType<?>> allowedEntities;

        private BodyType(final SoundEvent pBreakingSound, final EntityType<?>... pAllowedEntities) {
            this.breakingSound = pBreakingSound;
            this.allowedEntities = HolderSet.direct(EntityType::builtInRegistryHolder, pAllowedEntities);
        }

        public static BodyType create(String name, SoundEvent sound, EntityType<?>... types) {
            throw new IllegalStateException("Enum not extended");
        }
    }
}

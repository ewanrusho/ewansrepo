package net.minecraft.world.item.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public record Equippable(
    EquipmentSlot slot,
    Holder<SoundEvent> equipSound,
    Optional<ResourceKey<EquipmentAsset>> assetId,
    Optional<ResourceLocation> cameraOverlay,
    Optional<HolderSet<EntityType<?>>> allowedEntities,
    boolean dispensable,
    boolean swappable,
    boolean damageOnHurt
) {
    public static final Codec<Equippable> CODEC = RecordCodecBuilder.create(
        p_362866_ -> p_362866_.group(
                    EquipmentSlot.CODEC.fieldOf("slot").forGetter(Equippable::slot),
                    SoundEvent.CODEC.optionalFieldOf("equip_sound", SoundEvents.ARMOR_EQUIP_GENERIC).forGetter(Equippable::equipSound),
                    ResourceKey.codec(EquipmentAssets.ROOT_ID).optionalFieldOf("asset_id").forGetter(Equippable::assetId),
                    ResourceLocation.CODEC.optionalFieldOf("camera_overlay").forGetter(Equippable::cameraOverlay),
                    RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("allowed_entities").forGetter(Equippable::allowedEntities),
                    Codec.BOOL.optionalFieldOf("dispensable", Boolean.valueOf(true)).forGetter(Equippable::dispensable),
                    Codec.BOOL.optionalFieldOf("swappable", Boolean.valueOf(true)).forGetter(Equippable::swappable),
                    Codec.BOOL.optionalFieldOf("damage_on_hurt", Boolean.valueOf(true)).forGetter(Equippable::damageOnHurt)
                )
                .apply(p_362866_, Equippable::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Equippable> STREAM_CODEC = StreamCodec.composite(
        EquipmentSlot.STREAM_CODEC,
        Equippable::slot,
        SoundEvent.STREAM_CODEC,
        Equippable::equipSound,
        ResourceKey.streamCodec(EquipmentAssets.ROOT_ID).apply(ByteBufCodecs::optional),
        Equippable::assetId,
        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional),
        Equippable::cameraOverlay,
        ByteBufCodecs.holderSet(Registries.ENTITY_TYPE).apply(ByteBufCodecs::optional),
        Equippable::allowedEntities,
        ByteBufCodecs.BOOL,
        Equippable::dispensable,
        ByteBufCodecs.BOOL,
        Equippable::swappable,
        ByteBufCodecs.BOOL,
        Equippable::damageOnHurt,
        Equippable::new
    );

    public static Equippable llamaSwag(DyeColor pColor) {
        return builder(EquipmentSlot.BODY)
            .setEquipSound(SoundEvents.LLAMA_SWAG)
            .setAsset(EquipmentAssets.CARPETS.get(pColor))
            .setAllowedEntities(EntityType.LLAMA, EntityType.TRADER_LLAMA)
            .build();
    }

    public static Equippable.Builder builder(EquipmentSlot pSlot) {
        return new Equippable.Builder(pSlot);
    }

    public InteractionResult swapWithEquipmentSlot(ItemStack pStack, Player pPlayer) {
        if (!pPlayer.canUseSlot(this.slot)) {
            return InteractionResult.PASS;
        } else {
            ItemStack itemstack = pPlayer.getItemBySlot(this.slot);
            if ((!EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || pPlayer.isCreative())
                && !ItemStack.isSameItemSameComponents(pStack, itemstack)) {
                if (!pPlayer.level().isClientSide()) {
                    pPlayer.awardStat(Stats.ITEM_USED.get(pStack.getItem()));
                }

                if (pStack.getCount() <= 1) {
                    ItemStack itemstack3 = itemstack.isEmpty() ? pStack : itemstack.copyAndClear();
                    ItemStack itemstack4 = pPlayer.isCreative() ? pStack.copy() : pStack.copyAndClear();
                    pPlayer.setItemSlot(this.slot, itemstack4);
                    return InteractionResult.SUCCESS.heldItemTransformedTo(itemstack3);
                } else {
                    ItemStack itemstack1 = itemstack.copyAndClear();
                    ItemStack itemstack2 = pStack.consumeAndReturn(1, pPlayer);
                    pPlayer.setItemSlot(this.slot, itemstack2);
                    if (!pPlayer.getInventory().add(itemstack1)) {
                        pPlayer.drop(itemstack1, false);
                    }

                    return InteractionResult.SUCCESS.heldItemTransformedTo(pStack);
                }
            } else {
                return InteractionResult.FAIL;
            }
        }
    }

    public boolean canBeEquippedBy(EntityType<?> pEntityType) {
        return this.allowedEntities.isEmpty() || this.allowedEntities.get().contains(pEntityType.builtInRegistryHolder());
    }

    public static class Builder {
        private final EquipmentSlot slot;
        private Holder<SoundEvent> equipSound = SoundEvents.ARMOR_EQUIP_GENERIC;
        private Optional<ResourceKey<EquipmentAsset>> assetId = Optional.empty();
        private Optional<ResourceLocation> cameraOverlay = Optional.empty();
        private Optional<HolderSet<EntityType<?>>> allowedEntities = Optional.empty();
        private boolean dispensable = true;
        private boolean swappable = true;
        private boolean damageOnHurt = true;

        Builder(EquipmentSlot pSlot) {
            this.slot = pSlot;
        }

        public Equippable.Builder setEquipSound(Holder<SoundEvent> pEquipSound) {
            this.equipSound = pEquipSound;
            return this;
        }

        public Equippable.Builder setAsset(ResourceKey<EquipmentAsset> pAsset) {
            this.assetId = Optional.of(pAsset);
            return this;
        }

        public Equippable.Builder setCameraOverlay(ResourceLocation pCameraOverlay) {
            this.cameraOverlay = Optional.of(pCameraOverlay);
            return this;
        }

        public Equippable.Builder setAllowedEntities(EntityType<?>... pAllowedEntities) {
            return this.setAllowedEntities(HolderSet.direct(EntityType::builtInRegistryHolder, pAllowedEntities));
        }

        public Equippable.Builder setAllowedEntities(HolderSet<EntityType<?>> pAllowedEntities) {
            this.allowedEntities = Optional.of(pAllowedEntities);
            return this;
        }

        public Equippable.Builder setDispensable(boolean pDispensable) {
            this.dispensable = pDispensable;
            return this;
        }

        public Equippable.Builder setSwappable(boolean pSwappable) {
            this.swappable = pSwappable;
            return this;
        }

        public Equippable.Builder setDamageOnHurt(boolean pDamageOnHurt) {
            this.damageOnHurt = pDamageOnHurt;
            return this;
        }

        public Equippable build() {
            return new Equippable(
                this.slot, this.equipSound, this.assetId, this.cameraOverlay, this.allowedEntities, this.dispensable, this.swappable, this.damageOnHurt
            );
        }
    }
}
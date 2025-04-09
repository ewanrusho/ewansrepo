package net.minecraft.world.item;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class Item implements FeatureElement, ItemLike, net.minecraftforge.common.extensions.IForgeItem {
    public static final Codec<Holder<Item>> CODEC = BuiltInRegistries.ITEM
        .holderByNameCodec()
        .validate(
            p_361655_ -> p_361655_.is(Items.AIR.builtInRegistryHolder())
                    ? DataResult.error(() -> "Item must not be minecraft:air")
                    : DataResult.success(p_361655_)
        );
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Holder<Item>> STREAM_CODEC =
        net.minecraft.network.codec.ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.ITEM);
    public static final Map<Block, Item> BY_BLOCK = net.minecraftforge.registries.GameData.ItemCallbacks.getBlockItemMap();
    public static final ResourceLocation BASE_ATTACK_DAMAGE_ID = ResourceLocation.withDefaultNamespace("base_attack_damage");
    public static final ResourceLocation BASE_ATTACK_SPEED_ID = ResourceLocation.withDefaultNamespace("base_attack_speed");
    public static final int DEFAULT_MAX_STACK_SIZE = 64;
    public static final int ABSOLUTE_MAX_STACK_SIZE = 99;
    public static final int MAX_BAR_WIDTH = 13;
    private final Holder.Reference<Item> builtInRegistryHolder = BuiltInRegistries.ITEM.createIntrusiveHolder(this);
    private final DataComponentMap components;
    @Nullable
    private final Item craftingRemainingItem;
    protected final String descriptionId;
    private final FeatureFlagSet requiredFeatures;

    public static int getId(Item pItem) {
        return pItem == null ? 0 : BuiltInRegistries.ITEM.getId(pItem);
    }

    public static Item byId(int pId) {
        return BuiltInRegistries.ITEM.byId(pId);
    }

    @Deprecated
    public static Item byBlock(Block pBlock) {
        return BY_BLOCK.getOrDefault(pBlock, Items.AIR);
    }

    public Item(Item.Properties pProperties) {
        this.descriptionId = pProperties.effectiveDescriptionId();
        this.components = pProperties.buildAndValidateComponents(Component.translatable(this.descriptionId), pProperties.effectiveModel());
        this.craftingRemainingItem = pProperties.craftingRemainingItem;
        this.requiredFeatures = pProperties.requiredFeatures;
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            String s = this.getClass().getSimpleName();
            if (!s.endsWith("Item")) {
                LOGGER.error("Item classes should end with Item and {} doesn't.", s);
            }
        }
        initClient();
    }

    @Deprecated
    public Holder.Reference<Item> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    @Nullable
    private DataComponentMap builtComponents = null;

    public DataComponentMap components() {
        if (builtComponents == null) {
            builtComponents = net.minecraftforge.common.ForgeHooks.gatherItemComponents(this, components);
        }

        return builtComponents;
    }

    public int getDefaultMaxStackSize() {
        return builtComponents == null ? this.components.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) : this.builtComponents.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pRemainingUseDuration) {
    }

    /** @deprecated Forge: {@link net.minecraftforge.common.extensions.IForgeItem#onDestroyed(ItemEntity, DamageSource) Use damage source sensitive version} */
    public void onDestroyed(ItemEntity pItemEntity) {
    }

    public void verifyComponentsAfterLoad(ItemStack pStack) {
    }

    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return true;
    }

    @Override
    public Item asItem() {
        return this;
    }

    public InteractionResult useOn(UseOnContext pContext) {
        return InteractionResult.PASS;
    }

    public float getDestroySpeed(ItemStack pStack, BlockState pState) {
        Tool tool = pStack.get(DataComponents.TOOL);
        return tool != null ? tool.getMiningSpeed(pState) : 1.0F;
    }

    public InteractionResult use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        Consumable consumable = itemstack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            return consumable.startConsuming(pPlayer, itemstack, pHand);
        } else {
            Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
            return (InteractionResult)(equippable != null && equippable.swappable() ? equippable.swapWithEquipmentSlot(itemstack, pPlayer) : InteractionResult.PASS);
        }
    }

    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        Consumable consumable = pStack.get(DataComponents.CONSUMABLE);
        return consumable != null ? consumable.onConsume(pLevel, pLivingEntity, pStack) : pStack;
    }

    public boolean isBarVisible(ItemStack pStack) {
        return pStack.isDamaged();
    }

    public int getBarWidth(ItemStack pStack) {
        return Mth.clamp(Math.round(13.0F - (float)pStack.getDamageValue() * 13.0F / (float)pStack.getMaxDamage()), 0, 13);
    }

    public int getBarColor(ItemStack pStack) {
        int i = pStack.getMaxDamage();
        float f = Math.max(0.0F, ((float)i - (float)pStack.getDamageValue()) / (float)i);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    public boolean overrideStackedOnOther(ItemStack pStack, Slot pSlot, ClickAction pAction, Player pPlayer) {
        return false;
    }

    public boolean overrideOtherStackedOnMe(ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess) {
        return false;
    }

    public float getAttackDamageBonus(Entity pTarget, float pDamage, DamageSource pDamageSource) {
        return 0.0F;
    }

    @Nullable
    public DamageSource getDamageSource(LivingEntity pEntity) {
        return null;
    }

    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        return false;
    }

    public void postHurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
    }

    public boolean mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pMiningEntity) {
        Tool tool = pStack.get(DataComponents.TOOL);
        if (tool == null) {
            return false;
        } else {
            if (!pLevel.isClientSide && pState.getDestroySpeed(pLevel, pPos) != 0.0F && tool.damagePerBlock() > 0) {
                pStack.hurtAndBreak(tool.damagePerBlock(), pMiningEntity, EquipmentSlot.MAINHAND);
            }

            return true;
        }
    }

    public boolean isCorrectToolForDrops(ItemStack pStack, BlockState pState) {
        Tool tool = pStack.get(DataComponents.TOOL);
        return tool != null && tool.isCorrectForDrops(pState);
    }

    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        return InteractionResult.PASS;
    }

    @Override
    public String toString() {
        return BuiltInRegistries.ITEM.wrapAsHolder(this).getRegisteredName();
    }

    /** Forge: use {@link ItemStack#getCraftingRemainder()}. */
    @Deprecated
    public final ItemStack getCraftingRemainder() {
        return this.craftingRemainingItem == null ? ItemStack.EMPTY : new ItemStack(this.craftingRemainingItem);
    }

    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
    }

    public void onCraftedBy(ItemStack pStack, Level pLevel, Player pPlayer) {
        this.onCraftedPostProcess(pStack, pLevel);
    }

    public void onCraftedPostProcess(ItemStack pStack, Level pLevel) {
    }

    public ItemUseAnimation getUseAnimation(ItemStack pStack) {
        Consumable consumable = pStack.get(DataComponents.CONSUMABLE);
        return consumable != null ? consumable.animation() : ItemUseAnimation.NONE;
    }

    public int getUseDuration(ItemStack pStack, LivingEntity pEntity) {
        Consumable consumable = pStack.get(DataComponents.CONSUMABLE);
        return consumable != null ? consumable.consumeTicks() : 0;
    }

    public boolean releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pEntity, int pTimeLeft) {
        return false;
    }

    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
    }

    public Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
        return Optional.empty();
    }

    @VisibleForTesting
    public final String getDescriptionId() {
        return this.descriptionId;
    }

    public final Component getName() {
        return this.components.getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
    }

    public Component getName(ItemStack pStack) {
        return pStack.getComponents().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
    }

    public boolean isFoil(ItemStack pStack) {
        return pStack.isEnchanted();
    }

    protected static BlockHitResult getPlayerPOVHitResult(Level pLevel, Player pPlayer, ClipContext.Fluid pFluidMode) {
        Vec3 vec3 = pPlayer.getEyePosition();
        Vec3 vec31 = vec3.add(pPlayer.calculateViewVector(pPlayer.getXRot(), pPlayer.getYRot()).scale(pPlayer.blockInteractionRange()));
        return pLevel.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, pFluidMode, pPlayer));
    }

    public boolean useOnRelease(ItemStack pStack) {
        return false;
    }

    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    public SoundEvent getBreakingSound() {
        return SoundEvents.ITEM_BREAK;
    }

    public boolean canFitInsideContainerItems() {
        return true;
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    public boolean shouldPrintOpWarning(ItemStack pStack, @Nullable Player pPlayer) {
        return false;
    }

    private Object renderProperties;

    /*
       DO NOT CALL, IT WILL DISAPPEAR IN THE FUTURE
       Call RenderProperties.get instead
     */
    public Object getRenderPropertiesInternal() {
        return renderProperties;
    }

    private void initClient() {
        // Minecraft instance isn't available in datagen, so don't call initializeClient if in datagen
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT && !net.minecraftforge.fml.loading.FMLLoader.getLaunchHandler().isData()) {
            initializeClient(properties -> {
                if (properties == this) {
                    throw new IllegalStateException("Don't extend IItemRenderProperties in your item, use an anonymous class instead.");
                }
                this.renderProperties = properties;
            });
        }
    }

    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) { }

    public static class Properties {
        private static final DependantName<Item, String> BLOCK_DESCRIPTION_ID = p_367498_ -> Util.makeDescriptionId("block", p_367498_.location());
        private static final DependantName<Item, String> ITEM_DESCRIPTION_ID = p_367603_ -> Util.makeDescriptionId("item", p_367603_.location());
        private final DataComponentMap.Builder components = DataComponentMap.builder().addAll(DataComponents.COMMON_ITEM_COMPONENTS);
        @Nullable
        Item craftingRemainingItem;
        FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
        @Nullable
        private ResourceKey<Item> id;
        private DependantName<Item, String> descriptionId = ITEM_DESCRIPTION_ID;
        private DependantName<Item, ResourceLocation> model = ResourceKey::location;

        public Item.Properties food(FoodProperties pFood) {
            return this.food(pFood, Consumables.DEFAULT_FOOD);
        }

        public Item.Properties food(FoodProperties pFood, Consumable pConsumable) {
            return this.component(DataComponents.FOOD, pFood).component(DataComponents.CONSUMABLE, pConsumable);
        }

        public Item.Properties usingConvertsTo(Item pUsingConvertsTo) {
            return this.component(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStack(pUsingConvertsTo)));
        }

        public Item.Properties useCooldown(float pUseCooldown) {
            return this.component(DataComponents.USE_COOLDOWN, new UseCooldown(pUseCooldown));
        }

        public Item.Properties stacksTo(int pMaxStackSize) {
            return this.component(DataComponents.MAX_STACK_SIZE, pMaxStackSize);
        }

        public Item.Properties durability(int pMaxDamage) {
            this.component(DataComponents.MAX_DAMAGE, pMaxDamage);
            this.component(DataComponents.MAX_STACK_SIZE, 1);
            this.component(DataComponents.DAMAGE, 0);
            return this;
        }

        public Item.Properties craftRemainder(Item pCraftingRemainingItem) {
            this.craftingRemainingItem = pCraftingRemainingItem;
            return this;
        }

        public Item.Properties rarity(Rarity pRarity) {
            return this.component(DataComponents.RARITY, pRarity);
        }

        public Item.Properties fireResistant() {
            return this.component(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
        }

        public Item.Properties jukeboxPlayable(ResourceKey<JukeboxSong> pSong) {
            return this.component(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(pSong), true));
        }

        public Item.Properties enchantable(int pEnchantmentValue) {
            return this.component(DataComponents.ENCHANTABLE, new Enchantable(pEnchantmentValue));
        }

        public Item.Properties repairable(Item pRepairItem) {
            return this.component(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(pRepairItem.builtInRegistryHolder())));
        }

        public Item.Properties repairable(TagKey<Item> pRepairItems) {
            HolderGetter<Item> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ITEM);
            return this.component(DataComponents.REPAIRABLE, new Repairable(holdergetter.getOrThrow(pRepairItems)));
        }

        public Item.Properties equippable(EquipmentSlot pSlot) {
            return this.component(DataComponents.EQUIPPABLE, Equippable.builder(pSlot).build());
        }

        public Item.Properties equippableUnswappable(EquipmentSlot pSlot) {
            return this.component(DataComponents.EQUIPPABLE, Equippable.builder(pSlot).setSwappable(false).build());
        }

        public Item.Properties requiredFeatures(FeatureFlag... pRequiredFeatures) {
            this.requiredFeatures = FeatureFlags.REGISTRY.subset(pRequiredFeatures);
            return this;
        }

        public Item.Properties setId(ResourceKey<Item> pId) {
            this.id = pId;
            return this;
        }

        public Item.Properties overrideDescription(String pDescription) {
            this.descriptionId = DependantName.fixed(pDescription);
            return this;
        }

        public Item.Properties useBlockDescriptionPrefix() {
            this.descriptionId = BLOCK_DESCRIPTION_ID;
            return this;
        }

        public Item.Properties useItemDescriptionPrefix() {
            this.descriptionId = ITEM_DESCRIPTION_ID;
            return this;
        }

        protected String effectiveDescriptionId() {
            return this.descriptionId.get(Objects.requireNonNull(this.id, "Item id not set"));
        }

        public ResourceLocation effectiveModel() {
            return this.model.get(Objects.requireNonNull(this.id, "Item id not set"));
        }

        public <T> Item.Properties component(DataComponentType<T> pComponent, T pValue) {
            this.components.set(pComponent, pValue);
            return this;
        }

        public Item.Properties attributes(ItemAttributeModifiers pAttributes) {
            return this.component(DataComponents.ATTRIBUTE_MODIFIERS, pAttributes);
        }

        DataComponentMap buildAndValidateComponents(Component pItemName, ResourceLocation pItemModel) {
            DataComponentMap datacomponentmap = this.components
                .set(DataComponents.ITEM_NAME, pItemName)
                .set(DataComponents.ITEM_MODEL, pItemModel)
                .build();
            if (datacomponentmap.has(DataComponents.DAMAGE) && datacomponentmap.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
                throw new IllegalStateException("Item cannot have both durability and be stackable");
            } else {
                return datacomponentmap;
            }
        }
    }

    public interface TooltipContext {
        Item.TooltipContext EMPTY = new Item.TooltipContext() {
            @Nullable
            @Override
            public HolderLookup.Provider registries() {
                return null;
            }

            @Override
            public float tickRate() {
                return 20.0F;
            }

            @Nullable
            @Override
            public MapItemSavedData mapData(MapId p_334227_) {
                return null;
            }
        };

        @Nullable
        HolderLookup.Provider registries();

        float tickRate();

        @Nullable
        MapItemSavedData mapData(MapId pMapId);

        @Nullable
        default Level level() {
            return null;
        }

        static Item.TooltipContext of(@Nullable final Level pLevel) {
            return pLevel == null ? EMPTY : new Item.TooltipContext() {
                @Override
                public HolderLookup.Provider registries() {
                    return pLevel.registryAccess();
                }

                @Override
                public float tickRate() {
                    return pLevel.tickRateManager().tickrate();
                }

                @Override
                public MapItemSavedData mapData(MapId p_330171_) {
                    return pLevel.getMapData(p_330171_);
                }

                @Override
                public Level level() {
                    return pLevel;
                }
            };
        }

        static Item.TooltipContext of(final HolderLookup.Provider pRegistries) {
            return new Item.TooltipContext() {
                @Override
                public HolderLookup.Provider registries() {
                    return pRegistries;
                }

                @Override
                public float tickRate() {
                    return 20.0F;
                }

                @Nullable
                @Override
                public MapItemSavedData mapData(MapId p_332386_) {
                    return null;
                }
            };
        }
    }
}

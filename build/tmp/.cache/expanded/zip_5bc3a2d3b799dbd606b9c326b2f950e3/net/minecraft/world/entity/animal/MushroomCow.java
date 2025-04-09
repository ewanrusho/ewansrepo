package net.minecraft.world.entity.animal;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class MushroomCow extends Cow implements Shearable, VariantHolder<MushroomCow.Variant> {
    private static final EntityDataAccessor<String> DATA_TYPE = SynchedEntityData.defineId(MushroomCow.class, EntityDataSerializers.STRING);
    private static final int MUTATE_CHANCE = 1024;
    private static final String TAG_STEW_EFFECTS = "stew_effects";
    @Nullable
    private SuspiciousStewEffects stewEffects;
    @Nullable
    private UUID lastLightningBoltUUID;

    public MushroomCow(EntityType<? extends MushroomCow> p_28914_, Level p_28915_) {
        super(p_28914_, p_28915_);
    }

    @Override
    public float getWalkTargetValue(BlockPos pPos, LevelReader pLevel) {
        return pLevel.getBlockState(pPos.below()).is(Blocks.MYCELIUM) ? 10.0F : pLevel.getPathfindingCostFromLightLevels(pPos);
    }

    public static boolean checkMushroomSpawnRules(
        EntityType<MushroomCow> pEntityType, LevelAccessor pLevel, EntitySpawnReason pSpawnReason, BlockPos pPos, RandomSource pRandom
    ) {
        return pLevel.getBlockState(pPos.below()).is(BlockTags.MOOSHROOMS_SPAWNABLE_ON) && isBrightEnoughToSpawn(pLevel, pPos);
    }

    @Override
    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
        UUID uuid = pLightning.getUUID();
        if (!uuid.equals(this.lastLightningBoltUUID)) {
            this.setVariant(this.getVariant() == MushroomCow.Variant.RED ? MushroomCow.Variant.BROWN : MushroomCow.Variant.RED);
            this.lastLightningBoltUUID = uuid;
            this.playSound(SoundEvents.MOOSHROOM_CONVERT, 2.0F, 1.0F);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_336015_) {
        super.defineSynchedData(p_336015_);
        p_336015_.define(DATA_TYPE, MushroomCow.Variant.RED.type);
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (itemstack.is(Items.BOWL) && !this.isBaby()) {
            boolean flag = false;
            ItemStack itemstack2;
            if (this.stewEffects != null) {
                flag = true;
                itemstack2 = new ItemStack(Items.SUSPICIOUS_STEW);
                itemstack2.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, this.stewEffects);
                this.stewEffects = null;
            } else {
                itemstack2 = new ItemStack(Items.MUSHROOM_STEW);
            }

            ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, pPlayer, itemstack2, false);
            pPlayer.setItemInHand(pHand, itemstack1);
            SoundEvent soundevent;
            if (flag) {
                soundevent = SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY;
            } else {
                soundevent = SoundEvents.MOOSHROOM_MILK;
            }

            this.playSound(soundevent, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        } else if (false && itemstack.is(Items.SHEARS) && this.readyForShearing()) {
            if (this.level() instanceof ServerLevel serverlevel) {
                this.shear(serverlevel, SoundSource.PLAYERS, itemstack);
                this.gameEvent(GameEvent.SHEAR, pPlayer);
                itemstack.hurtAndBreak(1, pPlayer, getSlotForHand(pHand));
            }

            return InteractionResult.SUCCESS;
        } else if (this.getVariant() == MushroomCow.Variant.BROWN) {
            Optional<SuspiciousStewEffects> optional = this.getEffectsFromItemStack(itemstack);
            if (optional.isEmpty()) {
                return super.mobInteract(pPlayer, pHand);
            } else {
                if (this.stewEffects != null) {
                    for (int i = 0; i < 2; i++) {
                        this.level()
                            .addParticle(
                                ParticleTypes.SMOKE,
                                this.getX() + this.random.nextDouble() / 2.0,
                                this.getY(0.5),
                                this.getZ() + this.random.nextDouble() / 2.0,
                                0.0,
                                this.random.nextDouble() / 5.0,
                                0.0
                            );
                    }
                } else {
                    itemstack.consume(1, pPlayer);

                    for (int j = 0; j < 4; j++) {
                        this.level()
                            .addParticle(
                                ParticleTypes.EFFECT,
                                this.getX() + this.random.nextDouble() / 2.0,
                                this.getY(0.5),
                                this.getZ() + this.random.nextDouble() / 2.0,
                                0.0,
                                this.random.nextDouble() / 5.0,
                                0.0
                            );
                    }

                    this.stewEffects = optional.get();
                    this.playSound(SoundEvents.MOOSHROOM_EAT, 2.0F, 1.0F);
                }

                return InteractionResult.SUCCESS;
            }
        } else {
            return super.mobInteract(pPlayer, pHand);
        }
    }

    @Override
    public void shear(ServerLevel p_369641_, SoundSource p_28924_, ItemStack p_364876_) {
        for (var stack : shearInternal(p_369641_, p_28924_, p_364876_)) {
            for (int i = 0; i < stack.getCount(); i++) {
                this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(1.0D), this.getZ(), stack.copyWithCount(1)));
            }
        }
    }

    private java.util.List<ItemStack> shearInternal(ServerLevel p_369641_, SoundSource p_28924_, ItemStack p_364876_) {
        var ret = new java.util.ArrayList<ItemStack>();
        if (!net.minecraftforge.event.ForgeEventFactory.canLivingConvert(this, EntityType.COW, time -> {}))
            return ret;
        p_369641_.playSound(null, this, SoundEvents.MOOSHROOM_SHEAR, p_28924_, 1.0F, 1.0F);
        this.convertTo(EntityType.COW, ConversionParams.single(this, false, false), p_375107_ -> {
            p_369641_.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            this.dropFromShearingLootTable(p_369641_, BuiltInLootTables.SHEAR_MOOSHROOM, p_364876_, (p_375108_, p_375109_) -> {
                ret.add(p_375109_);
            });
            net.minecraftforge.event.ForgeEventFactory.onLivingConvert(this, p_375107_);
        });
        return ret;
    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && !this.isBaby();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putString("Type", this.getVariant().getSerializedName());
        if (this.stewEffects != null) {
            SuspiciousStewEffects.CODEC.encodeStart(NbtOps.INSTANCE, this.stewEffects).ifSuccess(p_296800_ -> pCompound.put("stew_effects", p_296800_));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setVariant(MushroomCow.Variant.byName(pCompound.getString("Type")));
        if (pCompound.contains("stew_effects", 9)) {
            SuspiciousStewEffects.CODEC.parse(NbtOps.INSTANCE, pCompound.get("stew_effects")).ifSuccess(p_326976_ -> this.stewEffects = p_326976_);
        }
    }

    private Optional<SuspiciousStewEffects> getEffectsFromItemStack(ItemStack pStack) {
        SuspiciousEffectHolder suspiciouseffectholder = SuspiciousEffectHolder.tryGet(pStack.getItem());
        return suspiciouseffectholder != null ? Optional.of(suspiciouseffectholder.getSuspiciousEffects()) : Optional.empty();
    }

    public void setVariant(MushroomCow.Variant p_363768_) {
        this.entityData.set(DATA_TYPE, p_363768_.type);
    }

    public MushroomCow.Variant getVariant() {
        return MushroomCow.Variant.byName(this.entityData.get(DATA_TYPE));
    }

    @Nullable
    public MushroomCow getBreedOffspring(ServerLevel p_148942_, AgeableMob p_148943_) {
        MushroomCow mushroomcow = EntityType.MOOSHROOM.create(p_148942_, EntitySpawnReason.BREEDING);
        if (mushroomcow != null) {
            mushroomcow.setVariant(this.getOffspringVariant((MushroomCow)p_148943_));
        }

        return mushroomcow;
    }

    private MushroomCow.Variant getOffspringVariant(MushroomCow pPartner) {
        MushroomCow.Variant mushroomcow$variant = this.getVariant();
        MushroomCow.Variant mushroomcow$variant1 = pPartner.getVariant();
        MushroomCow.Variant mushroomcow$variant2;
        if (mushroomcow$variant == mushroomcow$variant1 && this.random.nextInt(1024) == 0) {
            mushroomcow$variant2 = mushroomcow$variant == MushroomCow.Variant.BROWN ? MushroomCow.Variant.RED : MushroomCow.Variant.BROWN;
        } else {
            mushroomcow$variant2 = this.random.nextBoolean() ? mushroomcow$variant : mushroomcow$variant1;
        }

        return mushroomcow$variant2;
    }

    @Override
    public java.util.List<ItemStack> onSheared(@org.jetbrains.annotations.Nullable Player player, @org.jetbrains.annotations.NotNull ItemStack item, Level world, BlockPos pos, int fortune) {
        if (world instanceof ServerLevel server) {
            this.gameEvent(GameEvent.SHEAR, player);
            return shearInternal(server, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, item);
        }
        return java.util.Collections.emptyList();
    }

    public static enum Variant implements StringRepresentable {
        RED("red", Blocks.RED_MUSHROOM.defaultBlockState()),
        BROWN("brown", Blocks.BROWN_MUSHROOM.defaultBlockState());

        public static final StringRepresentable.EnumCodec<MushroomCow.Variant> CODEC = StringRepresentable.fromEnum(MushroomCow.Variant::values);
        final String type;
        private final BlockState blockState;

        private Variant(final String pType, final BlockState pBlockState) {
            this.type = pType;
            this.blockState = pBlockState;
        }

        public BlockState getBlockState() {
            return this.blockState;
        }

        @Override
        public String getSerializedName() {
            return this.type;
        }

        static MushroomCow.Variant byName(String pName) {
            return CODEC.byName(pName, RED);
        }
    }
}

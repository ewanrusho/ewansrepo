package net.minecraft.world.item;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;

public class MobBucketItem extends BucketItem {
    private static final MapCodec<TropicalFish.Variant> VARIANT_FIELD_CODEC = TropicalFish.Variant.CODEC.fieldOf("BucketVariantTag");
    private final java.util.function.Supplier<? extends EntityType<? extends Mob>> entityTypeSupplier;
    private final java.util.function.Supplier<? extends SoundEvent> emptySoundSupplier;

    @Deprecated
    public MobBucketItem(EntityType<? extends Mob> pType, Fluid pContent, SoundEvent pEmptySound, Item.Properties pProperties) {
        this(() -> pType, () -> pContent, () -> pEmptySound, pProperties);
    }

    public MobBucketItem(java.util.function.Supplier<? extends EntityType<? extends Mob>> entitySupplier, java.util.function.Supplier<? extends Fluid> fluidSupplier, java.util.function.Supplier<? extends SoundEvent> soundSupplier, Item.Properties properties) {
        super(fluidSupplier, properties);
        this.emptySoundSupplier = soundSupplier;
        this.entityTypeSupplier = entitySupplier;
    }

    @Override
    public void checkExtraContent(@Nullable Player p_151146_, Level p_151147_, ItemStack p_151148_, BlockPos p_151149_) {
        if (p_151147_ instanceof ServerLevel) {
            this.spawn((ServerLevel)p_151147_, p_151148_, p_151149_);
            p_151147_.gameEvent(p_151146_, GameEvent.ENTITY_PLACE, p_151149_);
        }
    }

    @Override
    protected void playEmptySound(@Nullable Player p_151151_, LevelAccessor p_151152_, BlockPos p_151153_) {
        p_151152_.playSound(p_151151_, p_151153_, this.getEmptySound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private void spawn(ServerLevel pServerLevel, ItemStack pBucketedMobStack, BlockPos pPos) {
        Mob mob = this.getFishType().create(pServerLevel, EntityType.createDefaultStackConfig(pServerLevel, pBucketedMobStack, null), pPos, EntitySpawnReason.BUCKET, true, false);
        if (mob instanceof Bucketable bucketable) {
            CustomData customdata = pBucketedMobStack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
            bucketable.loadFromBucketTag(customdata.copyTag());
            bucketable.setFromBucket(true);
        }

        if (mob != null) {
            pServerLevel.addFreshEntityWithPassengers(mob);
            mob.playAmbientSound();
        }
    }

    @Override
    public void appendHoverText(ItemStack p_151155_, Item.TooltipContext p_329546_, List<Component> p_151157_, TooltipFlag p_151158_) {
        if (this.getFishType() == EntityType.TROPICAL_FISH) {
            CustomData customdata = p_151155_.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
            if (customdata.isEmpty()) {
                return;
            }

            Optional<TropicalFish.Variant> optional = customdata.read(VARIANT_FIELD_CODEC).result();
            if (optional.isPresent()) {
                TropicalFish.Variant tropicalfish$variant = optional.get();
                ChatFormatting[] achatformatting = new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY};
                String s = "color.minecraft." + tropicalfish$variant.baseColor();
                String s1 = "color.minecraft." + tropicalfish$variant.patternColor();
                int i = TropicalFish.COMMON_VARIANTS.indexOf(tropicalfish$variant);
                if (i != -1) {
                    p_151157_.add(Component.translatable(TropicalFish.getPredefinedName(i)).withStyle(achatformatting));
                    return;
                }

                p_151157_.add(tropicalfish$variant.pattern().displayName().plainCopy().withStyle(achatformatting));
                MutableComponent mutablecomponent = Component.translatable(s);
                if (!s.equals(s1)) {
                    mutablecomponent.append(", ").append(Component.translatable(s1));
                }

                mutablecomponent.withStyle(achatformatting);
                p_151157_.add(mutablecomponent);
            }
        }
    }

    protected EntityType<? extends Mob> getFishType() {
        return entityTypeSupplier.get();
    }

    protected SoundEvent getEmptySound() {
        return emptySoundSupplier.get();
    }
}

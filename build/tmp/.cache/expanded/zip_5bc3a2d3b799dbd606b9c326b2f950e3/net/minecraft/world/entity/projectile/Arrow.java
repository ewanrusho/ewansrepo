package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

public class Arrow extends AbstractArrow {
    private static final int EXPOSED_POTION_DECAY_TIME = 600;
    private static final int NO_EFFECT_COLOR = -1;
    private static final EntityDataAccessor<Integer> ID_EFFECT_COLOR = SynchedEntityData.defineId(Arrow.class, EntityDataSerializers.INT);
    private static final byte EVENT_POTION_PUFF = 0;

    public Arrow(EntityType<? extends Arrow> p_36858_, Level p_36859_) {
        super(p_36858_, p_36859_);
    }

    public Arrow(Level pLevel, double pX, double pY, double pZ, ItemStack pPickupItemStack, @Nullable ItemStack pFiredFromWeapon) {
        super(EntityType.ARROW, pX, pY, pZ, pLevel, pPickupItemStack, pFiredFromWeapon);
        this.updateColor();
    }

    public Arrow(Level pLevel, LivingEntity pOwner, ItemStack pPickupItemStack, @Nullable ItemStack pFiredFromWeapon) {
        super(EntityType.ARROW, pOwner, pLevel, pPickupItemStack, pFiredFromWeapon);
        this.updateColor();
    }

    private PotionContents getPotionContents() {
        return this.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    private void setPotionContents(PotionContents pPotionContents) {
        this.getPickupItemStackOrigin().set(DataComponents.POTION_CONTENTS, pPotionContents);
        this.updateColor();
    }

    @Override
    protected void setPickupItemStack(ItemStack p_332340_) {
        super.setPickupItemStack(p_332340_);
        this.updateColor();
    }

    private void updateColor() {
        PotionContents potioncontents = this.getPotionContents();
        this.entityData.set(ID_EFFECT_COLOR, potioncontents.equals(PotionContents.EMPTY) ? -1 : potioncontents.getColor());
    }

    public void addEffect(MobEffectInstance pEffectInstance) {
        this.setPotionContents(this.getPotionContents().withEffectAdded(pEffectInstance));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_331799_) {
        super.defineSynchedData(p_331799_);
        p_331799_.define(ID_EFFECT_COLOR, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.isInGround()) {
                if (this.inGroundTime % 5 == 0) {
                    this.makeParticle(1);
                }
            } else {
                this.makeParticle(2);
            }
        } else if (this.isInGround() && this.inGroundTime != 0 && !this.getPotionContents().equals(PotionContents.EMPTY) && this.inGroundTime >= 600) {
            this.level().broadcastEntityEvent(this, (byte)0);
            this.setPickupItemStack(new ItemStack(Items.ARROW));
        }
    }

    private void makeParticle(int pParticleAmount) {
        int i = this.getColor();
        if (i != -1 && pParticleAmount > 0) {
            for (int j = 0; j < pParticleAmount; j++) {
                this.level()
                    .addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, i), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0.0, 0.0, 0.0);
            }
        }
    }

    public int getColor() {
        return this.entityData.get(ID_EFFECT_COLOR);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity pLiving) {
        super.doPostHurtEffects(pLiving);
        Entity entity = this.getEffectSource();
        PotionContents potioncontents = this.getPotionContents();
        if (potioncontents.potion().isPresent()) {
            for (MobEffectInstance mobeffectinstance : potioncontents.potion().get().value().getEffects()) {
                pLiving.addEffect(
                    new MobEffectInstance(
                        mobeffectinstance.getEffect(),
                        Math.max(mobeffectinstance.mapDuration(p_268168_ -> p_268168_ / 8), 1),
                        mobeffectinstance.getAmplifier(),
                        mobeffectinstance.isAmbient(),
                        mobeffectinstance.isVisible()
                    ),
                    entity
                );
            }
        }

        for (MobEffectInstance mobeffectinstance1 : potioncontents.customEffects()) {
            pLiving.addEffect(mobeffectinstance1, entity);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    public void handleEntityEvent(byte p_36869_) {
        if (p_36869_ == 0) {
            int i = this.getColor();
            if (i != -1) {
                float f = (float)(i >> 16 & 0xFF) / 255.0F;
                float f1 = (float)(i >> 8 & 0xFF) / 255.0F;
                float f2 = (float)(i >> 0 & 0xFF) / 255.0F;

                for (int j = 0; j < 20; j++) {
                    this.level()
                        .addParticle(
                            ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, f, f1, f2),
                            this.getRandomX(0.5),
                            this.getRandomY(),
                            this.getRandomZ(0.5),
                            0.0,
                            0.0,
                            0.0
                        );
                }
            }
        } else {
            super.handleEntityEvent(p_36869_);
        }
    }
}
package net.minecraft.world.level.block.entity;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class CampfireBlockEntity extends BlockEntity implements Clearable {
    private static final int BURN_COOL_SPEED = 2;
    private static final int NUM_SLOTS = 4;
    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private final int[] cookingProgress = new int[4];
    private final int[] cookingTime = new int[4];

    public CampfireBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.CAMPFIRE, pPos, pBlockState);
    }

    public static void cookTick(
        ServerLevel pLevel,
        BlockPos pPos,
        BlockState pState,
        CampfireBlockEntity pCampfire,
        RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> pCheck
    ) {
        boolean flag = false;

        for (int i = 0; i < pCampfire.items.size(); i++) {
            ItemStack itemstack = pCampfire.items.get(i);
            if (!itemstack.isEmpty()) {
                flag = true;
                pCampfire.cookingProgress[i]++;
                if (pCampfire.cookingProgress[i] >= pCampfire.cookingTime[i]) {
                    SingleRecipeInput singlerecipeinput = new SingleRecipeInput(itemstack);
                    ItemStack itemstack1 = pCheck.getRecipeFor(singlerecipeinput, pLevel)
                        .map(p_360490_ -> p_360490_.value().assemble(singlerecipeinput, pLevel.registryAccess()))
                        .orElse(itemstack);
                    if (itemstack1.isItemEnabled(pLevel.enabledFeatures())) {
                        Containers.dropItemStack(pLevel, (double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ(), itemstack1);
                        pCampfire.items.set(i, ItemStack.EMPTY);
                        pLevel.sendBlockUpdated(pPos, pState, pState, 3);
                        pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pState));
                    }
                }
            }
        }

        if (flag) {
            setChanged(pLevel, pPos, pState);
        }
    }

    public static void cooldownTick(Level pLevel, BlockPos pPos, BlockState pState, CampfireBlockEntity pBlockEntity) {
        boolean flag = false;

        for (int i = 0; i < pBlockEntity.items.size(); i++) {
            if (pBlockEntity.cookingProgress[i] > 0) {
                flag = true;
                pBlockEntity.cookingProgress[i] = Mth.clamp(pBlockEntity.cookingProgress[i] - 2, 0, pBlockEntity.cookingTime[i]);
            }
        }

        if (flag) {
            setChanged(pLevel, pPos, pState);
        }
    }

    public static void particleTick(Level pLevel, BlockPos pPos, BlockState pState, CampfireBlockEntity pBlockEntity) {
        RandomSource randomsource = pLevel.random;
        if (randomsource.nextFloat() < 0.11F) {
            for (int i = 0; i < randomsource.nextInt(2) + 2; i++) {
                CampfireBlock.makeParticles(pLevel, pPos, pState.getValue(CampfireBlock.SIGNAL_FIRE), false);
            }
        }

        int l = pState.getValue(CampfireBlock.FACING).get2DDataValue();

        for (int j = 0; j < pBlockEntity.items.size(); j++) {
            if (!pBlockEntity.items.get(j).isEmpty() && randomsource.nextFloat() < 0.2F) {
                Direction direction = Direction.from2DDataValue(Math.floorMod(j + l, 4));
                float f = 0.3125F;
                double d0 = (double)pPos.getX()
                    + 0.5
                    - (double)((float)direction.getStepX() * 0.3125F)
                    + (double)((float)direction.getClockWise().getStepX() * 0.3125F);
                double d1 = (double)pPos.getY() + 0.5;
                double d2 = (double)pPos.getZ()
                    + 0.5
                    - (double)((float)direction.getStepZ() * 0.3125F)
                    + (double)((float)direction.getClockWise().getStepZ() * 0.3125F);

                for (int k = 0; k < 4; k++) {
                    pLevel.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 5.0E-4, 0.0);
                }
            }
        }
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void loadAdditional(CompoundTag p_333177_, HolderLookup.Provider p_333564_) {
        super.loadAdditional(p_333177_, p_333564_);
        this.items.clear();
        ContainerHelper.loadAllItems(p_333177_, this.items, p_333564_);
        if (p_333177_.contains("CookingTimes", 11)) {
            int[] aint = p_333177_.getIntArray("CookingTimes");
            System.arraycopy(aint, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, aint.length));
        }

        if (p_333177_.contains("CookingTotalTimes", 11)) {
            int[] aint1 = p_333177_.getIntArray("CookingTotalTimes");
            System.arraycopy(aint1, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, aint1.length));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag p_187486_, HolderLookup.Provider p_336279_) {
        super.saveAdditional(p_187486_, p_336279_);
        ContainerHelper.saveAllItems(p_187486_, this.items, true, p_336279_);
        p_187486_.putIntArray("CookingTimes", this.cookingProgress);
        p_187486_.putIntArray("CookingTotalTimes", this.cookingTime);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_329092_) {
        CompoundTag compoundtag = new CompoundTag();
        ContainerHelper.saveAllItems(compoundtag, this.items, true, p_329092_);
        return compoundtag;
    }

    public boolean placeFood(ServerLevel pLevel, @Nullable LivingEntity pEntity, ItemStack pStack) {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (itemstack.isEmpty()) {
                Optional<RecipeHolder<CampfireCookingRecipe>> optional = pLevel.recipeAccess()
                    .getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(pStack), pLevel);
                if (optional.isEmpty()) {
                    return false;
                }

                this.cookingTime[i] = optional.get().value().cookingTime();
                this.cookingProgress[i] = 0;
                this.items.set(i, pStack.consumeAndReturn(1, pEntity));
                pLevel.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(pEntity, this.getBlockState()));
                this.markUpdated();
                return true;
            }
        }

        return false;
    }

    private void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    public void dowse() {
        if (this.level != null) {
            this.markUpdated();
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput p_333862_) {
        super.applyImplicitComponents(p_333862_);
        p_333862_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_333455_) {
        super.collectImplicitComponents(p_333455_);
        p_333455_.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag p_331425_) {
        p_331425_.remove("Items");
    }
}
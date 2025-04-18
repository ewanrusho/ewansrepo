package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class JigsawBlockEntity extends BlockEntity {
    public static final String TARGET = "target";
    public static final String POOL = "pool";
    public static final String JOINT = "joint";
    public static final String PLACEMENT_PRIORITY = "placement_priority";
    public static final String SELECTION_PRIORITY = "selection_priority";
    public static final String NAME = "name";
    public static final String FINAL_STATE = "final_state";
    private ResourceLocation name = ResourceLocation.withDefaultNamespace("empty");
    private ResourceLocation target = ResourceLocation.withDefaultNamespace("empty");
    private ResourceKey<StructureTemplatePool> pool = ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.withDefaultNamespace("empty"));
    private JigsawBlockEntity.JointType joint = JigsawBlockEntity.JointType.ROLLABLE;
    private String finalState = "minecraft:air";
    private int placementPriority;
    private int selectionPriority;

    public JigsawBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.JIGSAW, pPos, pBlockState);
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public ResourceLocation getTarget() {
        return this.target;
    }

    public ResourceKey<StructureTemplatePool> getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public JigsawBlockEntity.JointType getJoint() {
        return this.joint;
    }

    public int getPlacementPriority() {
        return this.placementPriority;
    }

    public int getSelectionPriority() {
        return this.selectionPriority;
    }

    public void setName(ResourceLocation pName) {
        this.name = pName;
    }

    public void setTarget(ResourceLocation pTarget) {
        this.target = pTarget;
    }

    public void setPool(ResourceKey<StructureTemplatePool> pPool) {
        this.pool = pPool;
    }

    public void setFinalState(String pFinalState) {
        this.finalState = pFinalState;
    }

    public void setJoint(JigsawBlockEntity.JointType pJoint) {
        this.joint = pJoint;
    }

    public void setPlacementPriority(int pPlacementPriority) {
        this.placementPriority = pPlacementPriority;
    }

    public void setSelectionPriority(int pSelectionPriority) {
        this.selectionPriority = pSelectionPriority;
    }

    @Override
    protected void saveAdditional(CompoundTag p_187504_, HolderLookup.Provider p_335581_) {
        super.saveAdditional(p_187504_, p_335581_);
        p_187504_.putString("name", this.name.toString());
        p_187504_.putString("target", this.target.toString());
        p_187504_.putString("pool", this.pool.location().toString());
        p_187504_.putString("final_state", this.finalState);
        p_187504_.putString("joint", this.joint.getSerializedName());
        p_187504_.putInt("placement_priority", this.placementPriority);
        p_187504_.putInt("selection_priority", this.selectionPriority);
    }

    @Override
    protected void loadAdditional(CompoundTag p_331375_, HolderLookup.Provider p_332374_) {
        super.loadAdditional(p_331375_, p_332374_);
        this.name = ResourceLocation.parse(p_331375_.getString("name"));
        this.target = ResourceLocation.parse(p_331375_.getString("target"));
        this.pool = ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.parse(p_331375_.getString("pool")));
        this.finalState = p_331375_.getString("final_state");
        this.joint = StructureTemplate.getJointType(p_331375_, this.getBlockState());
        this.placementPriority = p_331375_.getInt("placement_priority");
        this.selectionPriority = p_331375_.getInt("selection_priority");
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_333585_) {
        return this.saveCustomOnly(p_333585_);
    }

    public void generate(ServerLevel pLevel, int pMaxDepth, boolean pKeepJigsaws) {
        BlockPos blockpos = this.getBlockPos().relative(this.getBlockState().getValue(JigsawBlock.ORIENTATION).front());
        Registry<StructureTemplatePool> registry = pLevel.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        Holder<StructureTemplatePool> holder = registry.getOrThrow(this.pool);
        JigsawPlacement.generateJigsaw(pLevel, holder, this.target, pMaxDepth, blockpos, pKeepJigsaws);
    }

    public static enum JointType implements StringRepresentable {
        ROLLABLE("rollable"),
        ALIGNED("aligned");

        public static final StringRepresentable.EnumCodec<JigsawBlockEntity.JointType> CODEC = StringRepresentable.fromEnum(
            JigsawBlockEntity.JointType::values
        );
        private final String name;

        private JointType(final String pName) {
            this.name = pName;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Component getTranslatedName() {
            return Component.translatable("jigsaw_block.joint." + this.name);
        }
    }
}
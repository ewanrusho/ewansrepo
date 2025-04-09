package net.minecraft.client.resources.model;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class DelegateBakedModel implements BakedModel {
    protected final BakedModel parent;

    public DelegateBakedModel(BakedModel pParent) {
        this.parent = pParent;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState p_369776_, @Nullable Direction p_365054_, RandomSource p_367405_) {
        return this.parent.getQuads(p_369776_, p_365054_, p_367405_);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, net.minecraftforge.client.model.data.ModelData extraData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return this.parent.getQuads(state, side, rand, extraData, renderType);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.parent.useAmbientOcclusion();
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state, net.minecraft.client.renderer.RenderType renderType) {
       return this.parent.useAmbientOcclusion(state, renderType);
    }

    @Override
    public boolean isGui3d() {
        return this.parent.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.parent.usesBlockLight();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.parent.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.parent.getTransforms();
    }
}

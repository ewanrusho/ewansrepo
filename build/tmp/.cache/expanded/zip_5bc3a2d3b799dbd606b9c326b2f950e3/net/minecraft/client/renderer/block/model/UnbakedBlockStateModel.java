package net.minecraft.client.renderer.block.model;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface UnbakedBlockStateModel extends ResolvableModel {
    BakedModel bake(ModelBaker pBaker);

    Object visualEqualityGroup(BlockState pState);
}
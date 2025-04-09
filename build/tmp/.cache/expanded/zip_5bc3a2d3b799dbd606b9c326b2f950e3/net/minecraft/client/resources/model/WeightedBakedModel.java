package net.minecraft.client.resources.model;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeightedBakedModel extends DelegateBakedModel {
    private final SimpleWeightedRandomList<BakedModel> list;

    public WeightedBakedModel(SimpleWeightedRandomList<BakedModel> pList) {
        super(pList.unwrap().getFirst().data());
        this.list = pList;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState p_235058_, @Nullable Direction p_235059_, RandomSource p_235060_) {
        return this.list.getRandomValue(p_235060_).map(p_358058_ -> p_358058_.getQuads(p_235058_, p_235059_, p_235060_)).orElse(Collections.emptyList());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState p_235058_, @Nullable Direction p_235059_, RandomSource p_235060_, net.minecraftforge.client.model.data.ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return this.list.getRandomValue(p_235060_).map(p_358058_ -> p_358058_.getQuads(p_235058_, p_235059_, p_235060_, modelData, renderType)).orElse(Collections.emptyList());
    }
}

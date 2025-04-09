package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockModelWrapper implements ItemModel {
    public final BakedModel model;
    private final List<ItemTintSource> tints;

    BlockModelWrapper(BakedModel pModel, List<ItemTintSource> pTints) {
        this.model = pModel;
        this.tints = pTints;
    }

    @Override
    public void update(
        ItemStackRenderState p_377049_,
        ItemStack p_378482_,
        ItemModelResolver p_377214_,
        ItemDisplayContext p_375691_,
        @Nullable ClientLevel p_376532_,
        @Nullable LivingEntity p_376906_,
        int p_377340_
    ) {
        ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = p_377049_.newLayer();
        if (p_378482_.hasFoil()) {
            itemstackrenderstate$layerrenderstate.setFoilType(
                hasSpecialAnimatedTexture(p_378482_) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD
            );
        }

        int i = this.tints.size();
        int[] aint = itemstackrenderstate$layerrenderstate.prepareTintLayers(i);

        for (int j = 0; j < i; j++) {
            aint[j] = this.tints.get(j).calculate(p_378482_, p_376532_, p_376906_);
        }

        // TODO: [Rendering] Figure out how to add multiple passes
        /*
            - RenderType rendertype = ItemBlockRenderTypes.getRenderType(p_378482_);
            + for (var model : pModel.getRenderPasses(p_378482_, true)) {
            + for (var rendertype : model.getRenderTypes(p_378482_, true)) {
         */
        RenderType rendertype = ItemBlockRenderTypes.getRenderType(p_378482_);
        itemstackrenderstate$layerrenderstate.setupBlockModel(this.model, rendertype);
    }

    private static boolean hasSpecialAnimatedTexture(ItemStack pStack) {
        return pStack.is(ItemTags.COMPASSES) || pStack.is(Items.CLOCK);
    }

    @OnlyIn(Dist.CLIENT)
    public static record Unbaked(ResourceLocation model, List<ItemTintSource> tints) implements ItemModel.Unbaked {
        public static final MapCodec<BlockModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_376987_ -> p_376987_.group(
                        ResourceLocation.CODEC.fieldOf("model").forGetter(BlockModelWrapper.Unbaked::model),
                        ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(BlockModelWrapper.Unbaked::tints)
                    )
                    .apply(p_376987_, BlockModelWrapper.Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_375708_) {
            p_375708_.resolve(this.model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_375857_) {
            BakedModel bakedmodel = p_375857_.bake(this.model);
            return new BlockModelWrapper(bakedmodel, this.tints);
        }

        @Override
        public MapCodec<BlockModelWrapper.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}

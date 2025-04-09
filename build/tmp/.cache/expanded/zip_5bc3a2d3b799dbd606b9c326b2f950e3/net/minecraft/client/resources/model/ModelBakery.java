package net.minecraft.client.resources.model;

import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelBakery {
    public static final Material FIRE_0 = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/fire_0"));
    public static final Material FIRE_1 = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/fire_1"));
    public static final Material LAVA_FLOW = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/lava_flow"));
    public static final Material WATER_FLOW = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/water_flow"));
    public static final Material WATER_OVERLAY = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/water_overlay"));
    public static final Material BANNER_BASE = new Material(Sheets.BANNER_SHEET, ResourceLocation.withDefaultNamespace("entity/banner_base"));
    public static final Material SHIELD_BASE = new Material(Sheets.SHIELD_SHEET, ResourceLocation.withDefaultNamespace("entity/shield_base"));
    public static final Material NO_PATTERN_SHIELD = new Material(Sheets.SHIELD_SHEET, ResourceLocation.withDefaultNamespace("entity/shield_base_nopattern"));
    public static final int DESTROY_STAGE_COUNT = 10;
    public static final List<ResourceLocation> DESTROY_STAGES = IntStream.range(0, 10)
        .mapToObj(p_340955_ -> ResourceLocation.withDefaultNamespace("block/destroy_stage_" + p_340955_))
        .collect(Collectors.toList());
    public static final List<ResourceLocation> BREAKING_LOCATIONS = DESTROY_STAGES.stream()
        .map(p_340960_ -> p_340960_.withPath(p_340956_ -> "textures/" + p_340956_ + ".png"))
        .collect(Collectors.toList());
    public static final List<RenderType> DESTROY_TYPES = BREAKING_LOCATIONS.stream().map(RenderType::crumbling).collect(Collectors.toList());
    static final Logger LOGGER = LogUtils.getLogger();
    private final EntityModelSet entityModelSet;
    final Map<ModelBakery.BakedCacheKey, BakedModel> bakedCache = new HashMap<>();
    private final Map<ModelResourceLocation, UnbakedBlockStateModel> unbakedBlockStateModels;
    private final Map<ResourceLocation, ClientItem> clientInfos;
    final Map<ResourceLocation, UnbakedModel> unbakedPlainModels;
    final UnbakedModel missingModel;

    public ModelBakery(
        EntityModelSet pEntityModelSet,
        Map<ModelResourceLocation, UnbakedBlockStateModel> pUnbakedBlockStateModels,
        Map<ResourceLocation, ClientItem> pUnbakedItemStackModels,
        Map<ResourceLocation, UnbakedModel> pUnbakedPlainModels,
        UnbakedModel pMissingModel
    ) {
        this.entityModelSet = pEntityModelSet;
        this.unbakedBlockStateModels = pUnbakedBlockStateModels;
        this.clientInfos = pUnbakedItemStackModels;
        this.unbakedPlainModels = pUnbakedPlainModels;
        this.missingModel = pMissingModel;
    }

    public ModelBakery.BakingResult bakeModels(ModelBakery.TextureGetter pTextureGetter) {
        BakedModel bakedmodel = UnbakedModel.bakeWithTopModelValues(this.missingModel, new ModelBakery.ModelBakerImpl(pTextureGetter, () -> "missing"), BlockModelRotation.X0_Y0);
        Map<ModelResourceLocation, BakedModel> map = new HashMap<>(this.unbakedBlockStateModels.size());
        this.unbakedBlockStateModels.forEach((p_374710_, p_374711_) -> {
            try {
                BakedModel bakedmodel1 = p_374711_.bake(new ModelBakery.ModelBakerImpl(pTextureGetter, p_374710_::toString));
                map.put(p_374710_, bakedmodel1);
            } catch (Exception exception) {
                LOGGER.warn("Unable to bake model: '{}': {}", p_374710_, exception);
            }
        });
        ItemModel itemmodel = new MissingItemModel(bakedmodel);
        Map<ResourceLocation, ItemModel> map1 = new HashMap<>(this.clientInfos.size());
        Map<ResourceLocation, ClientItem.Properties> map2 = new HashMap<>(this.clientInfos.size());
        this.clientInfos.forEach((p_374705_, p_374706_) -> {
            ModelDebugName modeldebugname = () -> p_374705_ + "#inventory";
            ModelBakery.ModelBakerImpl modelbakery$modelbakerimpl = new ModelBakery.ModelBakerImpl(pTextureGetter, modeldebugname);
            ItemModel.BakingContext itemmodel$bakingcontext = new ItemModel.BakingContext(modelbakery$modelbakerimpl, this.entityModelSet, itemmodel);

            try {
                ItemModel itemmodel1 = p_374706_.model().bake(itemmodel$bakingcontext);
                map1.put(p_374705_, itemmodel1);
                if (!p_374706_.properties().equals(ClientItem.Properties.DEFAULT)) {
                    map2.put(p_374705_, p_374706_.properties());
                }
            } catch (Exception exception) {
                LOGGER.warn("Unable to bake item model: '{}'", p_374705_, exception);
            }
        });
        return new ModelBakery.BakingResult(bakedmodel, map, itemmodel, map1, map2);
    }

    @OnlyIn(Dist.CLIENT)
    static record BakedCacheKey(ResourceLocation id, Transformation transformation, boolean isUvLocked) {
    }

    @OnlyIn(Dist.CLIENT)
    public static record BakingResult(
        BakedModel missingModel,
        Map<ModelResourceLocation, BakedModel> blockStateModels,
        ItemModel missingItemModel,
        Map<ResourceLocation, ItemModel> itemStackModels,
        Map<ResourceLocation, ClientItem.Properties> itemProperties
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    class ModelBakerImpl implements ModelBaker {
        private final ModelDebugName rootName;
        private final SpriteGetter modelTextureGetter;

        ModelBakerImpl(final ModelBakery.TextureGetter pTextureGetter, final ModelDebugName pRootName) {
            this.modelTextureGetter = pTextureGetter.bind(pRootName);
            this.rootName = pRootName;
        }

        @Override
        public SpriteGetter sprites() {
            return this.modelTextureGetter;
        }

        private UnbakedModel getModel(ResourceLocation pName) {
            UnbakedModel unbakedmodel = ModelBakery.this.unbakedPlainModels.get(pName);
            if (unbakedmodel == null) {
                ModelBakery.LOGGER.warn("Requested a model that was not discovered previously: {}", pName);
                return ModelBakery.this.missingModel;
            } else {
                return unbakedmodel;
            }
        }

        @Override
        public BakedModel bake(ResourceLocation p_252176_, ModelState p_249765_) {
            ModelBakery.BakedCacheKey modelbakery$bakedcachekey = new ModelBakery.BakedCacheKey(p_252176_, p_249765_.getRotation(), p_249765_.isUvLocked());
            BakedModel bakedmodel = ModelBakery.this.bakedCache.get(modelbakery$bakedcachekey);
            if (bakedmodel != null) {
                return bakedmodel;
            } else {
                UnbakedModel unbakedmodel = this.getModel(p_252176_);
                BakedModel bakedmodel1 = UnbakedModel.bakeWithTopModelValues(unbakedmodel, this, p_249765_);
                ModelBakery.this.bakedCache.put(modelbakery$bakedcachekey, bakedmodel1);
                return bakedmodel1;
            }
        }

        @Override
        public ModelDebugName rootName() {
            return this.rootName;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface TextureGetter {
        TextureAtlasSprite get(ModelDebugName pName, Material pMaterial);

        TextureAtlasSprite reportMissingReference(ModelDebugName pName, String pReference);

        default SpriteGetter bind(final ModelDebugName pName) {
            return new SpriteGetter() {
                @Override
                public TextureAtlasSprite get(Material p_376856_) {
                    return TextureGetter.this.get(pName, p_376856_);
                }

                @Override
                public TextureAtlasSprite reportMissingReference(String p_375947_) {
                    return TextureGetter.this.reportMissingReference(pName, p_375947_);
                }
            };
        }
    }
}
package net.minecraft.client.resources.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BlockStateModelLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter BLOCKSTATE_LISTER = FileToIdConverter.json("blockstates");
    private static final String FRAME_MAP_PROPERTY = "map";
    private static final String FRAME_MAP_PROPERTY_TRUE = "map=true";
    private static final String FRAME_MAP_PROPERTY_FALSE = "map=false";
    private static final StateDefinition<Block, BlockState> ITEM_FRAME_FAKE_DEFINITION = new StateDefinition.Builder<Block, BlockState>(Blocks.AIR)
        .add(BooleanProperty.create("map"))
        .create(Block::defaultBlockState, BlockState::new);
    private static final ResourceLocation GLOW_ITEM_FRAME_LOCATION = ResourceLocation.withDefaultNamespace("glow_item_frame");
    private static final ResourceLocation ITEM_FRAME_LOCATION = ResourceLocation.withDefaultNamespace("item_frame");
    private static final Map<ResourceLocation, StateDefinition<Block, BlockState>> STATIC_DEFINITIONS = Map.of(ITEM_FRAME_LOCATION, ITEM_FRAME_FAKE_DEFINITION, GLOW_ITEM_FRAME_LOCATION, ITEM_FRAME_FAKE_DEFINITION);
    public static final ModelResourceLocation GLOW_MAP_FRAME_LOCATION = new ModelResourceLocation(GLOW_ITEM_FRAME_LOCATION, "map=true");
    public static final ModelResourceLocation GLOW_FRAME_LOCATION = new ModelResourceLocation(GLOW_ITEM_FRAME_LOCATION, "map=false");
    public static final ModelResourceLocation MAP_FRAME_LOCATION = new ModelResourceLocation(ITEM_FRAME_LOCATION, "map=true");
    public static final ModelResourceLocation FRAME_LOCATION = new ModelResourceLocation(ITEM_FRAME_LOCATION, "map=false");

    private static Function<ResourceLocation, StateDefinition<Block, BlockState>> definitionLocationToBlockMapper() {
        Map<ResourceLocation, StateDefinition<Block, BlockState>> map = new HashMap<>(STATIC_DEFINITIONS);
        var event = net.minecraftforge.client.event.ForgeEventFactoryClient.onRegisterModeStateDefinitions();
        map.putAll(event.getStates());

        for (Block block : BuiltInRegistries.BLOCK) {
            map.put(block.builtInRegistryHolder().key().location(), block.getStateDefinition());
        }

        return map::get;
    }

    public static CompletableFuture<BlockStateModelLoader.LoadedModels> loadBlockStates(UnbakedModel pModel, ResourceManager pResourceManager, Executor pExecutor) {
        Function<ResourceLocation, StateDefinition<Block, BlockState>> function = definitionLocationToBlockMapper();
        return CompletableFuture.<Map<ResourceLocation, List<Resource>>>supplyAsync(() -> BLOCKSTATE_LISTER.listMatchingResourceStacks(pResourceManager), pExecutor).thenCompose(p_374696_ -> {
            List<CompletableFuture<BlockStateModelLoader.LoadedModels>> list = new ArrayList<>(p_374696_.size());

            for (Entry<ResourceLocation, List<Resource>> entry : p_374696_.entrySet()) {
                list.add(CompletableFuture.supplyAsync(() -> {
                    ResourceLocation resourcelocation = BLOCKSTATE_LISTER.fileToId(entry.getKey());
                    StateDefinition<Block, BlockState> statedefinition = function.apply(resourcelocation);
                    if (statedefinition == null) {
                        LOGGER.debug("Discovered unknown block state definition {}, ignoring", resourcelocation);
                        return null;
                    } else {
                        List<Resource> list1 = entry.getValue();
                        List<BlockStateModelLoader.LoadedBlockModelDefinition> list2 = new ArrayList<>(list1.size());

                        for (Resource resource : list1) {
                            try (Reader reader = resource.openAsReader()) {
                                JsonObject jsonobject = GsonHelper.parse(reader);
                                BlockModelDefinition blockmodeldefinition = BlockModelDefinition.fromJsonElement(jsonobject);
                                list2.add(new BlockStateModelLoader.LoadedBlockModelDefinition(resource.sourcePackId(), blockmodeldefinition));
                            } catch (Exception exception1) {
                                LOGGER.error("Failed to load blockstate definition {} from pack {}", resourcelocation, resource.sourcePackId(), exception1);
                            }
                        }

                        try {
                            return loadBlockStateDefinitionStack(resourcelocation, statedefinition, list2, pModel);
                        } catch (Exception exception) {
                            LOGGER.error("Failed to load blockstate definition {}", resourcelocation, exception);
                            return null;
                        }
                    }
                }, pExecutor));
            }

            return Util.sequence(list).thenApply(p_374692_ -> {
                Map<ModelResourceLocation, BlockStateModelLoader.LoadedModel> map = new HashMap<>();

                for (BlockStateModelLoader.LoadedModels blockstatemodelloader$loadedmodels : p_374692_) {
                    if (blockstatemodelloader$loadedmodels != null) {
                        map.putAll(blockstatemodelloader$loadedmodels.models());
                    }
                }

                return new BlockStateModelLoader.LoadedModels(map);
            });
        });
    }

    private static BlockStateModelLoader.LoadedModels loadBlockStateDefinitionStack(
        ResourceLocation pId,
        StateDefinition<Block, BlockState> pStateDefinition,
        List<BlockStateModelLoader.LoadedBlockModelDefinition> pModelDefinitions,
        UnbakedModel pModel
    ) {
        Map<ModelResourceLocation, BlockStateModelLoader.LoadedModel> map = new HashMap<>();

        for (BlockStateModelLoader.LoadedBlockModelDefinition blockstatemodelloader$loadedblockmodeldefinition : pModelDefinitions) {
            blockstatemodelloader$loadedblockmodeldefinition.contents
                .instantiate(pStateDefinition, pId + "/" + blockstatemodelloader$loadedblockmodeldefinition.source)
                .forEach((p_374690_, p_374691_) -> {
                    ModelResourceLocation modelresourcelocation = BlockModelShaper.stateToModelLocation(pId, p_374690_);
                    map.put(modelresourcelocation, new BlockStateModelLoader.LoadedModel(p_374690_, p_374691_));
                });
        }

        return new BlockStateModelLoader.LoadedModels(map);
    }

    @OnlyIn(Dist.CLIENT)
    static record LoadedBlockModelDefinition(String source, BlockModelDefinition contents) {
    }

    @OnlyIn(Dist.CLIENT)
    public static record LoadedModel(BlockState state, UnbakedBlockStateModel model) {
    }

    @OnlyIn(Dist.CLIENT)
    public static record LoadedModels(Map<ModelResourceLocation, BlockStateModelLoader.LoadedModel> models) {
        public Stream<ResolvableModel> forResolving() {
            return this.models.values().stream().map(BlockStateModelLoader.LoadedModel::model);
        }

        public Map<ModelResourceLocation, UnbakedBlockStateModel> plainModels() {
            return Maps.transformValues(this.models, BlockStateModelLoader.LoadedModel::model);
        }
    }
}

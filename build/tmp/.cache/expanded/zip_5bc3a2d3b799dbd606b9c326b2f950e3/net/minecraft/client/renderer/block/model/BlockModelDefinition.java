package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BlockModelDefinition {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(BlockModelDefinition.class, new BlockModelDefinition.Deserializer())
        .registerTypeAdapter(Variant.class, new Variant.Deserializer())
        .registerTypeAdapter(MultiVariant.class, new MultiVariant.Deserializer())
        .registerTypeAdapter(MultiPart.Definition.class, new MultiPart.Deserializer())
        .registerTypeAdapter(Selector.class, new Selector.Deserializer())
        .create();
    private final Map<String, MultiVariant> variants;
    @Nullable
    private final MultiPart.Definition multiPart;

    public static BlockModelDefinition fromStream(Reader pReader) {
        return GsonHelper.fromJson(GSON, pReader, BlockModelDefinition.class);
    }

    public static BlockModelDefinition fromJsonElement(JsonElement pJson) {
        return GSON.fromJson(pJson, BlockModelDefinition.class);
    }

    public BlockModelDefinition(Map<String, MultiVariant> pVariants, @Nullable MultiPart.Definition pMultiPart) {
        this.multiPart = pMultiPart;
        this.variants = pVariants;
    }

    @VisibleForTesting
    public MultiVariant getVariant(String pKey) {
        MultiVariant multivariant = this.variants.get(pKey);
        if (multivariant == null) {
            throw new BlockModelDefinition.MissingVariantException();
        } else {
            return multivariant;
        }
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof BlockModelDefinition blockmodeldefinition)
                ? false
                : this.variants.equals(blockmodeldefinition.variants) && Objects.equals(this.multiPart, blockmodeldefinition.multiPart);
        }
    }

    @Override
    public int hashCode() {
        return 31 * this.variants.hashCode() + (this.multiPart != null ? this.multiPart.hashCode() : 0);
    }

    @VisibleForTesting
    public Set<MultiVariant> getMultiVariants() {
        Set<MultiVariant> set = Sets.newHashSet(this.variants.values());
        if (this.multiPart != null) {
            set.addAll(this.multiPart.getMultiVariants());
        }

        return set;
    }

    @Nullable
    public MultiPart.Definition getMultiPart() {
        return this.multiPart;
    }

    public Map<BlockState, UnbakedBlockStateModel> instantiate(StateDefinition<Block, BlockState> pStateDefinition, String pName) {
        Map<BlockState, UnbakedBlockStateModel> map = new IdentityHashMap<>();
        List<BlockState> list = pStateDefinition.getPossibleStates();
        MultiPart multipart;
        if (this.multiPart != null) {
            multipart = this.multiPart.instantiate(pStateDefinition);
            list.forEach(p_363978_ -> map.put(p_363978_, multipart));
        } else {
            multipart = null;
        }

        this.variants
            .forEach(
                (p_364884_, p_363250_) -> {
                    try {
                        list.stream()
                            .filter(VariantSelector.predicate(pStateDefinition, p_364884_))
                            .forEach(
                                p_374633_ -> {
                                    UnbakedBlockStateModel unbakedblockstatemodel = map.put(p_374633_, p_363250_);
                                    if (unbakedblockstatemodel != null && unbakedblockstatemodel != multipart) {
                                        String s = this.variants
                                            .entrySet()
                                            .stream()
                                            .filter(p_367129_ -> p_367129_.getValue() == unbakedblockstatemodel)
                                            .findFirst()
                                            .get()
                                            .getKey();
                                        throw new RuntimeException("Overlapping definition with: " + s);
                                    }
                                }
                            );
                    } catch (Exception exception) {
                        LOGGER.warn("Exception loading blockstate definition: '{}' for variant: '{}': {}", pName, p_364884_, exception.getMessage());
                    }
                }
            );
        return map;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Deserializer implements JsonDeserializer<BlockModelDefinition> {
        public BlockModelDefinition deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
            JsonObject jsonobject = pJson.getAsJsonObject();
            Map<String, MultiVariant> map = this.getVariants(pContext, jsonobject);
            MultiPart.Definition multipart$definition = this.getMultiPart(pContext, jsonobject);
            if (map.isEmpty() && multipart$definition == null) {
                throw new JsonParseException("Neither 'variants' nor 'multipart' found");
            } else {
                return new BlockModelDefinition(map, multipart$definition);
            }
        }

        protected Map<String, MultiVariant> getVariants(JsonDeserializationContext pContext, JsonObject pJson) {
            Map<String, MultiVariant> map = Maps.newHashMap();
            if (pJson.has("variants")) {
                JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "variants");

                for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                    map.put(entry.getKey(), pContext.deserialize(entry.getValue(), MultiVariant.class));
                }
            }

            return map;
        }

        @Nullable
        protected MultiPart.Definition getMultiPart(JsonDeserializationContext pContext, JsonObject pJson) {
            if (!pJson.has("multipart")) {
                return null;
            } else {
                JsonArray jsonarray = GsonHelper.getAsJsonArray(pJson, "multipart");
                return pContext.deserialize(jsonarray, MultiPart.Definition.class);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected static class MissingVariantException extends RuntimeException {
    }
}
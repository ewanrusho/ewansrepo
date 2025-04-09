/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Forge reimplementation of vanilla's {@link ItemModelGenerator}, i.e. builtin/generated models with some tweaks:
 * - Represented as {@link IUnbakedGeometry} so it can be baked as usual instead of being special-cased
 * - Not limited to an arbitrary number of layers (5)
 * - Support for per-layer render types
 */
public class ItemLayerModel implements IUnbakedGeometry<ItemLayerModel> {
    @Nullable
    private ImmutableList<Material> textures;
    private final Int2ObjectMap<ResourceLocation> renderTypeNames;
    private final Int2ObjectMap<ResourceLocation> renderTypeFastNames;

    private ItemLayerModel(@Nullable ImmutableList<Material> textures, Int2ObjectMap<ResourceLocation> renderTypeNames) {
        this(textures, renderTypeNames, new Int2ObjectOpenHashMap<>());
    }

    private ItemLayerModel(@Nullable ImmutableList<Material> textures, Int2ObjectMap<ResourceLocation> renderTypeNames, Int2ObjectMap<ResourceLocation> renderTypeFastNames) {
        this.textures = textures;
        this.renderTypeNames = renderTypeNames;
        this.renderTypeFastNames = renderTypeFastNames;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, TextureSlots spriteGetter, ModelState modelState) {
        if (textures == null) {
            ImmutableList.Builder<Material> builder = ImmutableList.builder();
            int i = 0;
            Material material;
            while ((material = spriteGetter.getMaterial("layer" + i++)) != null) {
                builder.add(material);
            }
            textures = builder.build();
        }

        Material particleMaterial = spriteGetter.getMaterial("particle");
        var particle = baker.sprites().get(particleMaterial  == null ? textures.get(0) : particleMaterial);
        var rootTransform = context.getRootTransform();
        if (!rootTransform.isIdentity())
            modelState = UnbakedGeometryHelper.composeRootTransformIntoModelState(modelState, rootTransform);

        var normalRenderTypes = new RenderTypeGroup(RenderType.translucent(), ForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
        var builder = CompositeModel.Baked.builder(context, particle, context.getTransforms());
        for (int i = 0; i < textures.size(); i++) {
            var sprite = baker.sprites().get(textures.get(i));
            var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(i, sprite.contents());
            var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> sprite, modelState);
            var renderTypeName = renderTypeNames.get(i);
            var renderTypes = renderTypeName != null ? context.getRenderType(renderTypeName) : null;
            var renderTypeFastName = renderTypeFastNames.get(i);
            var renderTypesFast = renderTypeFastName != null ? context.getRenderType(renderTypeFastName) : null;
            builder.addQuads(renderTypes != null ? renderTypes : normalRenderTypes, renderTypesFast != null ? renderTypesFast : RenderTypeGroup.EMPTY, quads);
        }

        return builder.build();
    }

    public static final class Loader implements IGeometryLoader<ItemLayerModel> {
        public static final Loader INSTANCE = new Loader();

        @Override
        public ItemLayerModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
            var renderTypeNames = readRenderTypeNames(jsonObject, "render_types");
            var renderTypeFastNames = readRenderTypeNames(jsonObject, "render_types_fast");
            return new ItemLayerModel(null, renderTypeNames, renderTypeFastNames);
        }

        private static Int2ObjectMap<ResourceLocation> readRenderTypeNames(JsonObject jsonObject, String key) {
            var renderTypeNames = new Int2ObjectOpenHashMap<ResourceLocation>();
            if (jsonObject.has(key)) {
                var renderTypes = jsonObject.getAsJsonObject(key);
                for (var entry : renderTypes.entrySet()) {
                    var renderType = ResourceLocation.parse(entry.getKey());
                    for (var layer : entry.getValue().getAsJsonArray())
                        if (renderTypeNames.put(layer.getAsInt(), renderType) != null)
                            throw new JsonParseException("Registered duplicate " + key + " for layer " + layer);
                }
            }

            return renderTypeNames;
        }

        @Deprecated(forRemoval = true, since = "1.21.4")
        protected void readLayerData(JsonObject jsonObject, String name, Int2ObjectOpenHashMap<ResourceLocation> renderTypeNames, Int2ObjectMap<ForgeFaceData> layerData, boolean logWarning) {
            if (!jsonObject.has(name))
                return;

            var fullbrightLayers = jsonObject.getAsJsonObject(name);
            for (var entry : fullbrightLayers.entrySet()) {
                int layer = Integer.parseInt(entry.getKey());
                var data = ForgeFaceData.read(entry.getValue(), ForgeFaceData.DEFAULT);
                layerData.put(layer, data);
            }
        }
    }
}

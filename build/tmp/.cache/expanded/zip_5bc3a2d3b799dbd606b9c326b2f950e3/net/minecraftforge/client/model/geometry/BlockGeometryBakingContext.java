/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model.geometry;

import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A {@linkplain IGeometryBakingContext geometry baking context} that is bound to a {@link BlockModel}.
 * <p>
 * Users should not be instantiating this themselves.
 */
public class BlockGeometryBakingContext implements IGeometryBakingContext {
    public final BlockModel owner;
    public final VisibilityData visibilityData = new VisibilityData();
    @Nullable
    private IUnbakedGeometry<?> customGeometry;
    @Nullable
    private Transformation rootTransform;
    @Nullable
    private ResourceLocation renderTypeHint;
    @Nullable
    private ResourceLocation renderTypeFastHint;
    private boolean gui3d = true;

    @ApiStatus.Internal
    public BlockGeometryBakingContext(BlockModel owner) {
        this.owner = owner;
    }

    public boolean hasCustomGeometry() {
        return getCustomGeometry() != null;
    }

    private BlockGeometryBakingContext parentContext() {
        return owner.parent instanceof BlockModel block ? block.customData : null;
    }

    @Nullable
    public IUnbakedGeometry<?> getCustomGeometry() {
        if (customGeometry != null)
            return customGeometry;
        var pctx = parentContext();
        return pctx == null ? null : pctx.getCustomGeometry();
    }

    public void setCustomGeometry(IUnbakedGeometry<?> geometry) {
        this.customGeometry = geometry;
    }

    @Override
    public boolean isComponentVisible(String part, boolean fallback) {
        var pctx = parentContext();
        if (pctx == null || visibilityData.hasCustomVisibility(part))
            return visibilityData.isVisible(part, fallback);
        return pctx.isComponentVisible(part, fallback);
    }

    @Override
    public boolean isGui3d() {
        return gui3d;
    }

    @Override
    public boolean useBlockLight() {
        return owner.getGuiLight().lightLikeBlock();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return owner.getAmbientOcclusion();
    }

    @Override
    public ItemTransforms getTransforms() {
        return owner.getTransforms();
    }

    @Override
    public Transformation getRootTransform() {
        if (rootTransform != null)
            return rootTransform;
        var pctx = parentContext();
        return pctx == null ? Transformation.identity() : pctx.getRootTransform();
    }

    public void setRootTransform(Transformation rootTransform) {
        this.rootTransform = rootTransform;
    }

    @Nullable
    @Override
    public ResourceLocation getRenderTypeHint() {
        if (renderTypeHint != null)
            return renderTypeHint;
        var pctx = parentContext();
        return pctx == null ? null: pctx.getRenderTypeHint();
    }

    @Nullable
    @Override
    public ResourceLocation getRenderTypeFastHint() {
        if (renderTypeFastHint != null)
            return renderTypeFastHint;
        var pctx = parentContext();
        return pctx == null ? null: pctx.getRenderTypeFastHint();
    }

    public void setRenderTypeHint(ResourceLocation renderTypeHint) {
        this.renderTypeHint = renderTypeHint;
    }

    public void setRenderTypeFastHint(ResourceLocation renderTypeFastHint) {
        this.renderTypeFastHint = renderTypeFastHint;
    }

    public void setGui3d(boolean gui3d) {
        this.gui3d = gui3d;
    }

    public void copyFrom(BlockGeometryBakingContext other) {
        this.customGeometry = other.customGeometry;
        this.rootTransform = other.rootTransform;
        this.visibilityData.copyFrom(other.visibilityData);
        this.renderTypeHint = other.renderTypeHint;
        this.renderTypeFastHint = other.renderTypeFastHint;
        this.gui3d = other.gui3d;
    }

    public BakedModel bake(ModelBaker baker, TextureSlots textures, ModelState modelTransform) {
        IUnbakedGeometry<?> geometry = getCustomGeometry();
        if (geometry == null)
            throw new IllegalStateException("Can not use custom baking without custom geometry");
        return geometry.bake(this, baker, textures, modelTransform);
    }

    public static class VisibilityData {
        private final Map<String, Boolean> data = new HashMap<>();

        public boolean hasCustomVisibility(String part) {
            return data.containsKey(part);
        }

        public boolean isVisible(String part, boolean fallback) {
            return data.getOrDefault(part, fallback);
        }

        public void setVisibilityState(String partName, boolean type) {
            data.put(partName, type);
        }

        public void copyFrom(VisibilityData visibilityData) {
            data.clear();
            data.putAll(visibilityData.data);
        }
    }
}

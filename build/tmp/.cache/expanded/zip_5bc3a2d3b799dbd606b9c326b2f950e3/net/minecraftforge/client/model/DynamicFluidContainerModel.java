/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.geometry.StandaloneGeometryBakingContext;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A dynamic fluid container model, capable of re-texturing itself at runtime to match the contained fluid.
 * <p>
 * Composed of a base layer, a fluid layer (applied with a mask) and a cover layer (optionally applied with a mask).
 * The entire model may optionally be flipped if the fluid is gaseous, and the fluid layer may glow if light-emitting.
 * <p>
 * Fluid tinting requires registering a separate {@link ItemColor}. An implementation is provided in {@link Colors}.
 *
 * @see Colors
 */
public class DynamicFluidContainerModel implements IUnbakedGeometry<DynamicFluidContainerModel> {
    // Depth offsets to prevent Z-fighting
    private static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());
    private static final Transformation COVER_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.004f), new Quaternionf());

    private final Fluid fluid;
    private final boolean flipGas;
    private final boolean coverIsMask;
    private final boolean applyFluidLuminosity;

    private DynamicFluidContainerModel(Fluid fluid, boolean flipGas, boolean coverIsMask, boolean applyFluidLuminosity) {
        this.fluid = fluid;
        this.flipGas = flipGas;
        this.coverIsMask = coverIsMask;
        this.applyFluidLuminosity = applyFluidLuminosity;
    }

    public static RenderTypeGroup getLayerRenderTypes(boolean unlit) {
        return new RenderTypeGroup(RenderType.translucent(), unlit ? ForgeRenderTypes.ITEM_UNSORTED_UNLIT_TRANSLUCENT.get() : ForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
    }

    /**
     * Returns a new ModelDynBucket representing the given fluid, but with the same
     * other properties (flipGas, tint, coverIsMask).
     */
    public DynamicFluidContainerModel withFluid(Fluid newFluid) {
        return new DynamicFluidContainerModel(newFluid, flipGas, coverIsMask, applyFluidLuminosity);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, TextureSlots textures, ModelState modelState) {
        Material fluidMaskLocation = textures.getMaterial("fluid");
        Material stillMaterial = null;
        if (fluid != Fluids.EMPTY) {
            var stillTexture = IClientFluidTypeExtensions.of(fluid).getStillTexture();
            stillMaterial = new Material(TextureAtlas.LOCATION_BLOCKS, stillTexture);
        }

        var sprites = baker.sprites();
        var baseSprite = sprites.maybeMissing(textures, "base");
        var fluidSprite = stillMaterial == null ? null : sprites.get(stillMaterial);
        var coverSprite = sprites.maybeMissing(textures, "cover"); // (coverLocation != null && (!coverIsMask || baseLocation != null)) ? textures.apply(coverLocation) : null;
        var particleSprite = sprites.maybeMissing(textures, "particle");

        if (particleSprite == null) particleSprite = fluidSprite;
        if (particleSprite == null) particleSprite = baseSprite;
        if (particleSprite == null && !coverIsMask) particleSprite = coverSprite;

        // If the fluid is lighter than air, rotate 180deg to turn it upside down
        if (flipGas && fluid != Fluids.EMPTY && fluid.getFluidType().isLighterThanAir()) {
            modelState = new SimpleModelState(
                    modelState.getRotation().compose(
                            new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null)));
        }

        // We need to disable GUI 3D and block lighting for this to render properly
        var itemContext = StandaloneGeometryBakingContext.builder(context).withGui3d(false).withUseBlockLight(false).build();
        var modelBuilder = CompositeModel.Baked.builder(itemContext, particleSprite, /*new ContainedFluidOverrideHandler(baker, itemContext, this),*/ context.getTransforms());

        var normalRenderTypes = getLayerRenderTypes(false);

        if (baseSprite != null) {
            // Base texture
            var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(0, baseSprite.contents());
            var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> baseSprite, modelState);
            modelBuilder.addQuads(normalRenderTypes, quads);
        }

        if (fluidMaskLocation != null && fluidSprite != null) {
            TextureAtlasSprite templateSprite = sprites.get(fluidMaskLocation);
            if (templateSprite != null) {
                // Fluid layer
                var transformedState = new SimpleModelState(modelState.getRotation().compose(FLUID_TRANSFORM), modelState.isUvLocked());
                var unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(1, templateSprite.contents()); // Use template as mask
                var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> fluidSprite, transformedState); // Bake with fluid texture

                var emissive = applyFluidLuminosity && fluid.getFluidType().getLightLevel() > 0;
                var renderTypes = getLayerRenderTypes(emissive);
                if (emissive) QuadTransformers.settingMaxEmissivity().processInPlace(quads);

                modelBuilder.addQuads(renderTypes, quads);
            }
        }

        if (coverSprite != null) {
            var sprite = coverIsMask ? baseSprite : coverSprite;
            if (sprite != null) {
                // Cover/overlay
                var transformedState = new SimpleModelState(modelState.getRotation().compose(COVER_TRANSFORM), modelState.isUvLocked());
                var unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(2, coverSprite.contents()); // Use cover as mask
                var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> sprite, transformedState); // Bake with selected texture
                modelBuilder.addQuads(normalRenderTypes, quads);
            }
        }

        modelBuilder.setParticle(particleSprite);

        return modelBuilder.build();
    }

    public static final class Loader implements IGeometryLoader<DynamicFluidContainerModel> {
        public static final Loader INSTANCE = new Loader();

        private Loader() { }

        @Override
        public DynamicFluidContainerModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
            if (!jsonObject.has("fluid"))
                throw new RuntimeException("Bucket model requires 'fluid' value.");

            ResourceLocation fluidName = ResourceLocation.parse(jsonObject.get("fluid").getAsString());

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidName);

            boolean flip = GsonHelper.getAsBoolean(jsonObject, "flip_gas", false);
            boolean coverIsMask = GsonHelper.getAsBoolean(jsonObject, "cover_is_mask", true);
            boolean applyFluidLuminosity = GsonHelper.getAsBoolean(jsonObject, "apply_fluid_luminosity", true);

            // create new model with correct liquid
            return new DynamicFluidContainerModel(fluid, flip, coverIsMask, applyFluidLuminosity);
        }
    }
}

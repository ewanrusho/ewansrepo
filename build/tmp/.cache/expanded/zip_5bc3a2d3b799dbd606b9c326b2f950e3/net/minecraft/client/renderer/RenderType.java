package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RenderType extends RenderStateShard {
    private static final int MEGABYTE = 1048576;
    public static final int BIG_BUFFER_SIZE = 4194304;
    public static final int SMALL_BUFFER_SIZE = 786432;
    public static final int TRANSIENT_BUFFER_SIZE = 1536;
    private static final RenderType SOLID = create(
        "solid",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        4194304,
        true,
        false,
        RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setShaderState(RENDERTYPE_SOLID_SHADER).setTextureState(BLOCK_SHEET_MIPPED).createCompositeState(true)
    );
    private static final RenderType CUTOUT_MIPPED = create(
        "cutout_mipped",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        4194304,
        true,
        false,
        RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setShaderState(RENDERTYPE_CUTOUT_MIPPED_SHADER).setTextureState(BLOCK_SHEET_MIPPED).createCompositeState(true)
    );
    private static final RenderType CUTOUT = create(
        "cutout",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        786432,
        true,
        false,
        RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setShaderState(RENDERTYPE_CUTOUT_SHADER).setTextureState(BLOCK_SHEET).createCompositeState(true)
    );
    private static final RenderType TRANSLUCENT = create(
        "translucent", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, true, true, translucentState(RENDERTYPE_TRANSLUCENT_SHADER)
    );
    private static final RenderType TRANSLUCENT_MOVING_BLOCK = create(
        "translucent_moving_block", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, false, true, translucentMovingBlockState()
    );
    private static final Function<ResourceLocation, RenderType> ARMOR_CUTOUT_NO_CULL = Util.memoize(p_296247_ -> createArmorCutoutNoCull("armor_cutout_no_cull", p_296247_, false));
    private static final Function<ResourceLocation, RenderType> ARMOR_TRANSLUCENT = Util.memoize(
        p_357880_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ARMOR_TRANSLUCENT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357880_, TriState.FALSE, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(true);
            return create("armor_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_SOLID = Util.memoize(
        p_357882_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357882_, TriState.FALSE, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("entity_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_SOLID_Z_OFFSET_FORWARD = Util.memoize(
        p_357893_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357893_, TriState.FALSE, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING_FORWARD)
                .createCompositeState(true);
            return create(
                "entity_solid_z_offset_forward", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate
            );
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT = Util.memoize(
        p_357900_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357900_, TriState.FALSE, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("entity_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL = Util.memoize(
        (p_357902_, p_357903_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357902_, TriState.FALSE, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(p_357903_);
            return create("entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL_Z_OFFSET = Util.memoize(
        (p_357908_, p_357909_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357908_, TriState.FALSE, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(p_357909_);
            return create(
                "entity_cutout_no_cull_z_offset", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate
            );
        }
    );
    private static final Function<ResourceLocation, RenderType> ITEM_ENTITY_TRANSLUCENT_CULL = Util.memoize(
        p_357895_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357895_, TriState.FALSE, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(true);
            return create("item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT = Util.memoize(
        (p_357891_, p_357892_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357891_, TriState.FALSE, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(p_357892_);
            return create("entity_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE = Util.memoize(
        (p_357888_, p_357889_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357888_, TriState.FALSE, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .setOverlayState(OVERLAY)
                .createCompositeState(p_357889_);
            return create("entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_SMOOTH_CUTOUT = Util.memoize(
        p_357897_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357897_, TriState.FALSE, false))
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .createCompositeState(true);
            return create("entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> BEACON_BEAM = Util.memoize(
        (p_357906_, p_357907_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357906_, TriState.FALSE, false))
                .setTransparencyState(p_357907_ ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                .setWriteMaskState(p_357907_ ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                .createCompositeState(false);
            return create("beacon_beam", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, false, true, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_DECAL = Util.memoize(
        p_357881_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_DECAL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357881_, TriState.FALSE, false))
                .setDepthTestState(EQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(false);
            return create("entity_decal", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_NO_OUTLINE = Util.memoize(
        p_357899_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_NO_OUTLINE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357899_, TriState.FALSE, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
            return create("entity_no_outline", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_SHADOW = Util.memoize(
        p_357887_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_SHADOW_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357887_, TriState.FALSE, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false);
            return create("entity_shadow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> DRAGON_EXPLOSION_ALPHA = Util.memoize(
        p_357910_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_ALPHA_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_357910_, TriState.FALSE, false))
                .setCullState(NO_CULL)
                .createCompositeState(true);
            return create("entity_alpha", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, RenderStateShard.TransparencyStateShard, RenderType> EYES = Util.memoize(
        (p_357911_, p_357912_) -> {
            RenderStateShard.TextureStateShard renderstateshard$texturestateshard = new RenderStateShard.TextureStateShard(p_357911_, TriState.FALSE, false);
            return create(
                "eyes",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(renderstateshard$texturestateshard)
                    .setTransparencyState(p_357912_)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
            );
        }
    );
    private static final RenderType LEASH = create(
        "leash",
        DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
        VertexFormat.Mode.TRIANGLE_STRIP,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LEASH_SHADER).setTextureState(NO_TEXTURE).setCullState(NO_CULL).setLightmapState(LIGHTMAP).createCompositeState(false)
    );
    private static final RenderType WATER_MASK = create(
        "water_mask",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_WATER_MASK_SHADER).setTextureState(NO_TEXTURE).setWriteMaskState(DEPTH_WRITE).createCompositeState(false)
    );
    private static final RenderType ARMOR_ENTITY_GLINT = create(
        "armor_entity_glint",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ENTITY, TriState.DEFAULT, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(ENTITY_GLINT_TEXTURING)
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
    );
    private static final RenderType GLINT_TRANSLUCENT = create(
        "glint_translucent",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, TriState.DEFAULT, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(GLINT_TEXTURING)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(false)
    );
    private static final RenderType GLINT = create(
        "glint",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_GLINT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, TriState.DEFAULT, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(GLINT_TEXTURING)
            .createCompositeState(false)
    );
    private static final RenderType ENTITY_GLINT = create(
        "entity_glint",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ENTITY, TriState.DEFAULT, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(ENTITY_GLINT_TEXTURING)
            .createCompositeState(false)
    );
    private static final Function<ResourceLocation, RenderType> CRUMBLING = Util.memoize(
        p_357904_ -> {
            RenderStateShard.TextureStateShard renderstateshard$texturestateshard = new RenderStateShard.TextureStateShard(p_357904_, TriState.FALSE, false);
            return create(
                "crumbling",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_CRUMBLING_SHADER)
                    .setTextureState(renderstateshard$texturestateshard)
                    .setTransparencyState(CRUMBLING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false)
            );
        }
    );
    private static final Function<ResourceLocation, RenderType> TEXT = Util.memoize(
        p_357898_ -> create(
                "text",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                786432,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357898_, TriState.FALSE, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)
            )
    );
    private static final RenderType TEXT_BACKGROUND = create(
        "text_background",
        DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_TEXT_BACKGROUND_SHADER).setTextureState(NO_TEXTURE).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).createCompositeState(false)
    );
    private static final Function<ResourceLocation, RenderType> TEXT_INTENSITY = Util.memoize(
        p_357896_ -> create(
                "text_intensity",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                786432,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_INTENSITY_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357896_, TriState.FALSE, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(
        p_357884_ -> create(
                "text_polygon_offset",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357884_, TriState.FALSE, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
        p_357885_ -> create(
                "text_intensity_polygon_offset",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_INTENSITY_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357885_, TriState.FALSE, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> TEXT_SEE_THROUGH = Util.memoize(
        p_357894_ -> create(
                "text_see_through",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357894_, TriState.FALSE, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
            )
    );
    private static final RenderType TEXT_BACKGROUND_SEE_THROUGH = create(
        "text_background_see_through",
        DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER)
            .setTextureState(NO_TEXTURE)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    );
    private static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_SEE_THROUGH = Util.memoize(
        p_357886_ -> create(
                "text_intensity_see_through",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357886_, TriState.FALSE, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
            )
    );
    private static final RenderType LIGHTNING = create(
        "lightning",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER).setWriteMaskState(COLOR_DEPTH_WRITE).setTransparencyState(LIGHTNING_TRANSPARENCY).setOutputState(WEATHER_TARGET).createCompositeState(false)
    );
    private static final RenderType DRAGON_RAYS = create(
        "dragon_rays",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLES,
        1536,
        false,
        false,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER).setWriteMaskState(COLOR_WRITE).setTransparencyState(LIGHTNING_TRANSPARENCY).createCompositeState(false)
    );
    private static final RenderType DRAGON_RAYS_DEPTH = create(
        "dragon_rays_depth",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.TRIANGLES,
        1536,
        false,
        false,
        RenderType.CompositeState.builder().setShaderState(POSITION_SHADER).setWriteMaskState(DEPTH_WRITE).createCompositeState(false)
    );
    private static final RenderType TRIPWIRE = create("tripwire", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, true, true, tripwireState());
    private static final RenderType END_PORTAL = create(
        "end_portal",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_END_PORTAL_SHADER)
            .setTextureState(
                RenderStateShard.MultiTextureStateShard.builder()
                    .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                    .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                    .build()
            )
            .createCompositeState(false)
    );
    private static final RenderType END_GATEWAY = create(
        "end_gateway",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_END_GATEWAY_SHADER)
            .setTextureState(
                RenderStateShard.MultiTextureStateShard.builder()
                    .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                    .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                    .build()
            )
            .createCompositeState(false)
    );
    private static final RenderType FLAT_CLOUDS = createClouds(false, false);
    private static final RenderType CLOUDS = createClouds(false, true);
    private static final RenderType CLOUDS_DEPTH_ONLY = createClouds(true, true);
    public static final RenderType.CompositeRenderType LINES = create(
        "lines",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType SECONDARY_BLOCK_OUTLINE = create(
        "secondary_block_outline",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(7.0)))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType LINE_STRIP = create(
        "line_strip",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINE_STRIP,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
    );
    private static final Function<Double, RenderType.CompositeRenderType> DEBUG_LINE_STRIP = Util.memoize(
        p_286162_ -> create(
                "debug_line_strip",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.DEBUG_LINE_STRIP,
                1536,
                RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(p_286162_)))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
            )
    );
    private static final RenderType.CompositeRenderType DEBUG_FILLED_BOX = create(
        "debug_filled_box",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLE_STRIP,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType DEBUG_QUADS = create(
        "debug_quads",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType DEBUG_TRIANGLE_FAN = create(
        "debug_triangle_fan",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLE_FAN,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType DEBUG_STRUCTURE_QUADS = create(
        "debug_structure_quads",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(POSITION_COLOR_SHADER)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType DEBUG_SECTION_QUADS = create(
        "debug_section_quads",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(CULL).createCompositeState(false)
    );
    private static final RenderType WORLD_BORDER_NO_DEPTH_WRITE = createWorldBorder(false);
    private static final RenderType WORLD_BORDER_DEPTH_WRITE = createWorldBorder(true);
    private static final Function<ResourceLocation, RenderType> OPAQUE_PARTICLE = Util.memoize(
        p_374629_ -> create(
                "opaque_particle",
                DefaultVertexFormat.PARTICLE,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(PARTICLE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_374629_, TriState.FALSE, false))
                    .setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> TRANSLUCENT_PARTICLE = Util.memoize(
        p_374625_ -> create(
                "translucent_particle",
                DefaultVertexFormat.PARTICLE,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(PARTICLE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_374625_, TriState.FALSE, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(PARTICLES_TARGET)
                    .setLightmapState(LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> WEATHER_DEPTH_WRITE = createWeather(true);
    private static final Function<ResourceLocation, RenderType> WEATHER_NO_DEPTH_WRITE = createWeather(false);
    private static final RenderType SKY = create(
        "sky",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder().setShaderState(POSITION_SHADER).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
    );
    private static final RenderType END_SKY = create(
        "end_sky",
        DefaultVertexFormat.POSITION_TEX_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(SkyRenderer.END_SKY_LOCATION, TriState.FALSE, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    );
    private static final RenderType SUNRISE_SUNSET = create(
        "sunrise_sunset",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLE_FAN,
        1536,
        false,
        false,
        RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
    );
    private static final RenderType STARS = create(
        "stars",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder().setShaderState(POSITION_SHADER).setTransparencyState(OVERLAY_TRANSPARENCY).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
    );
    private static final Function<ResourceLocation, RenderType> CELESTIAL = Util.memoize(
        p_374626_ -> create(
                "celestial",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_374626_, TriState.FALSE, false))
                    .setTransparencyState(OVERLAY_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> BLOCK_SCREEN_EFFECT = Util.memoize(
        p_374627_ -> create(
                "block_screen_effect",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_374627_, TriState.FALSE, false))
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> FIRE_SCREEN_EFFECT = Util.memoize(
        p_374628_ -> create(
                "fire_screen_effect",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_374628_, TriState.FALSE, false))
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .createCompositeState(false)
            )
    );
    private static final RenderType.CompositeRenderType GUI = create(
        "gui",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        786432,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType GUI_OVERLAY = create(
        "gui_overlay",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_OVERLAY_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
    );
    private static final Function<ResourceLocation, RenderType> GUI_TEXTURED_OVERLAY = Util.memoize(
        p_357913_ -> create(
                "gui_textured_overlay",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357913_, TriState.DEFAULT, false))
                    .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> GUI_OPAQUE_TEXTURED_BACKGROUND = Util.memoize(
        p_357905_ -> create(
                "gui_opaque_textured_background",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                786432,
                RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357905_, TriState.FALSE, false))
                    .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
            )
    );
    private static final RenderType.CompositeRenderType GUI_NAUSEA_OVERLAY = create(
        "gui_nausea_overlay",
        DefaultVertexFormat.POSITION_TEX_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setTextureState(new RenderStateShard.TextureStateShard(Gui.NAUSEA_LOCATION, TriState.DEFAULT, false))
            .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
            .setTransparencyState(NAUSEA_OVERLAY_TRANSPARENCY)
            .setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType GUI_TEXT_HIGHLIGHT = create(
        "gui_text_highlight",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_TEXT_HIGHLIGHT_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setColorLogicState(OR_REVERSE_COLOR_LOGIC).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType GUI_GHOST_RECIPE_OVERLAY = create(
        "gui_ghost_recipe_overlay",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(GREATER_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
    );
    private static final Function<ResourceLocation, RenderType> GUI_TEXTURED = Util.memoize(
        p_357901_ -> create(
                "gui_textured",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                786432,
                RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357901_, TriState.FALSE, false))
                    .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> VIGNETTE = Util.memoize(
        p_357883_ -> create(
                "vignette",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                786432,
                RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357883_, TriState.DEFAULT, false))
                    .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
                    .setTransparencyState(VIGNETTE_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> CROSSHAIR = Util.memoize(
        p_357890_ -> create(
                "crosshair",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                786432,
                RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(p_357890_, TriState.FALSE, false))
                    .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
                    .setTransparencyState(CROSSHAIR_TRANSPARENCY)
                    .createCompositeState(false)
            )
    );
    private static final RenderType.CompositeRenderType MOJANG_LOGO = create(
        "mojang_logo",
        DefaultVertexFormat.POSITION_TEX_COLOR,
        VertexFormat.Mode.QUADS,
        786432,
        RenderType.CompositeState.builder()
            .setTextureState(new RenderStateShard.TextureStateShard(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION, TriState.DEFAULT, false))
            .setShaderState(POSITION_TEXTURE_COLOR_SHADER)
            .setTransparencyState(MOJANG_LOGO_TRANSPARENCY)
            .setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    );
    private static final ImmutableList<RenderType> CHUNK_BUFFER_LAYERS = ImmutableList.of(solid(), cutoutMipped(), cutout(), translucent(), tripwire());
    static {
        int i = 0;
        for (var layer : CHUNK_BUFFER_LAYERS)
            layer.chunkLayerId = i++;
    }

    private final VertexFormat format;
    private final VertexFormat.Mode mode;
    private final int bufferSize;
    private final boolean affectsCrumbling;
    private final boolean sortOnUpload;
    private int chunkLayerId = -1;
    /** {@return the unique ID of this {@link RenderType} for chunk rendering purposes, or {@literal -1} if this is not a chunk {@link RenderType}} */
    public final int getChunkLayerId() {
        return chunkLayerId;
    }

    public static RenderType solid() {
        return SOLID;
    }

    public static RenderType cutoutMipped() {
        return CUTOUT_MIPPED;
    }

    public static RenderType cutout() {
        return CUTOUT;
    }

    private static RenderType.CompositeState translucentState(RenderStateShard.ShaderStateShard pState) {
        return RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(pState)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(TRANSLUCENT_TARGET)
            .createCompositeState(true);
    }

    public static RenderType translucent() {
        return TRANSLUCENT;
    }

    private static RenderType.CompositeState translucentMovingBlockState() {
        return RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(true);
    }

    public static RenderType translucentMovingBlock() {
        return TRANSLUCENT_MOVING_BLOCK;
    }

    private static RenderType.CompositeRenderType createArmorCutoutNoCull(String pName, ResourceLocation pId, boolean pEqualDepthTest) {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(pId, TriState.FALSE, false))
            .setTransparencyState(NO_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setDepthTestState(pEqualDepthTest ? EQUAL_DEPTH_TEST : LEQUAL_DEPTH_TEST)
            .createCompositeState(true);
        return create(pName, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
    }

    public static RenderType armorCutoutNoCull(ResourceLocation pLocation) {
        return ARMOR_CUTOUT_NO_CULL.apply(pLocation);
    }

    public static RenderType createArmorDecalCutoutNoCull(ResourceLocation pId) {
        return createArmorCutoutNoCull("armor_decal_cutout_no_cull", pId, true);
    }

    public static RenderType armorTranslucent(ResourceLocation pId) {
        return ARMOR_TRANSLUCENT.apply(pId);
    }

    public static RenderType entitySolid(ResourceLocation pLocation) {
        return ENTITY_SOLID.apply(pLocation);
    }

    public static RenderType entitySolidZOffsetForward(ResourceLocation pLocation) {
        return ENTITY_SOLID_Z_OFFSET_FORWARD.apply(pLocation);
    }

    public static RenderType entityCutout(ResourceLocation pLocation) {
        return ENTITY_CUTOUT.apply(pLocation);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation pLocation, boolean pOutline) {
        return ENTITY_CUTOUT_NO_CULL.apply(pLocation, pOutline);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation pLocation) {
        return entityCutoutNoCull(pLocation, true);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation pLocation, boolean pOutline) {
        return ENTITY_CUTOUT_NO_CULL_Z_OFFSET.apply(pLocation, pOutline);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation pLocation) {
        return entityCutoutNoCullZOffset(pLocation, true);
    }

    public static RenderType itemEntityTranslucentCull(ResourceLocation pLocation) {
        return ITEM_ENTITY_TRANSLUCENT_CULL.apply(pLocation);
    }

    public static RenderType entityTranslucent(ResourceLocation pLocation, boolean pOutline) {
        return ENTITY_TRANSLUCENT.apply(pLocation, pOutline);
    }

    public static RenderType entityTranslucent(ResourceLocation pLocation) {
        return entityTranslucent(pLocation, true);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation pLocation, boolean pOutline) {
        return ENTITY_TRANSLUCENT_EMISSIVE.apply(pLocation, pOutline);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation pLocation) {
        return entityTranslucentEmissive(pLocation, true);
    }

    public static RenderType entitySmoothCutout(ResourceLocation pLocation) {
        return ENTITY_SMOOTH_CUTOUT.apply(pLocation);
    }

    public static RenderType beaconBeam(ResourceLocation pLocation, boolean pColorFlag) {
        return BEACON_BEAM.apply(pLocation, pColorFlag);
    }

    public static RenderType entityDecal(ResourceLocation pLocation) {
        return ENTITY_DECAL.apply(pLocation);
    }

    public static RenderType entityNoOutline(ResourceLocation pLocation) {
        return ENTITY_NO_OUTLINE.apply(pLocation);
    }

    public static RenderType entityShadow(ResourceLocation pLocation) {
        return ENTITY_SHADOW.apply(pLocation);
    }

    public static RenderType dragonExplosionAlpha(ResourceLocation pId) {
        return DRAGON_EXPLOSION_ALPHA.apply(pId);
    }

    public static RenderType eyes(ResourceLocation pLocation) {
        return EYES.apply(pLocation, TRANSLUCENT_TRANSPARENCY);
    }

    public static RenderType breezeEyes(ResourceLocation pLocation) {
        return ENTITY_TRANSLUCENT_EMISSIVE.apply(pLocation, false);
    }

    public static RenderType breezeWind(ResourceLocation pLocation, float pU, float pV) {
        return create(
            "breeze_wind",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_BREEZE_WIND_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(pLocation, TriState.FALSE, false))
                .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(pU, pV))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .createCompositeState(false)
        );
    }

    public static RenderType energySwirl(ResourceLocation pLocation, float pU, float pV) {
        return create(
            "energy_swirl",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(pLocation, TriState.FALSE, false))
                .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(pU, pV))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(false)
        );
    }

    public static RenderType leash() {
        return LEASH;
    }

    public static RenderType waterMask() {
        return WATER_MASK;
    }

    public static RenderType outline(ResourceLocation pLocation) {
        return RenderType.CompositeRenderType.OUTLINE.apply(pLocation, NO_CULL);
    }

    public static RenderType armorEntityGlint() {
        return ARMOR_ENTITY_GLINT;
    }

    public static RenderType glintTranslucent() {
        return GLINT_TRANSLUCENT;
    }

    public static RenderType glint() {
        return GLINT;
    }

    public static RenderType entityGlint() {
        return ENTITY_GLINT;
    }

    public static RenderType crumbling(ResourceLocation pLocation) {
        return CRUMBLING.apply(pLocation);
    }

    public static RenderType text(ResourceLocation pLocation) {
        return net.minecraftforge.client.ForgeRenderTypes.getText(pLocation);
    }

    public static RenderType textBackground() {
        return TEXT_BACKGROUND;
    }

    public static RenderType textIntensity(ResourceLocation pId) {
        return net.minecraftforge.client.ForgeRenderTypes.getTextIntensity(pId);
    }

    public static RenderType textPolygonOffset(ResourceLocation pId) {
        return net.minecraftforge.client.ForgeRenderTypes.getTextPolygonOffset(pId);
    }

    public static RenderType textIntensityPolygonOffset(ResourceLocation pId) {
        return net.minecraftforge.client.ForgeRenderTypes.getTextIntensityPolygonOffset(pId);
    }

    public static RenderType textSeeThrough(ResourceLocation pLocation) {
        return net.minecraftforge.client.ForgeRenderTypes.getTextSeeThrough(pLocation);
    }

    public static RenderType textBackgroundSeeThrough() {
        return TEXT_BACKGROUND_SEE_THROUGH;
    }

    public static RenderType textIntensitySeeThrough(ResourceLocation pId) {
        return net.minecraftforge.client.ForgeRenderTypes.getTextIntensitySeeThrough(pId);
    }

    public static RenderType lightning() {
        return LIGHTNING;
    }

    public static RenderType dragonRays() {
        return DRAGON_RAYS;
    }

    public static RenderType dragonRaysDepth() {
        return DRAGON_RAYS_DEPTH;
    }

    private static RenderType.CompositeState tripwireState() {
        return RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_TRIPWIRE_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(WEATHER_TARGET)
            .createCompositeState(true);
    }

    public static RenderType tripwire() {
        return TRIPWIRE;
    }

    public static RenderType endPortal() {
        return END_PORTAL;
    }

    public static RenderType endGateway() {
        return END_GATEWAY;
    }

    private static RenderType.CompositeRenderType createClouds(boolean pColor, boolean pCull) {
        return create(
            "clouds",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            786432,
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_CLOUDS_SHADER)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(pCull ? CULL : NO_CULL)
                .setWriteMaskState(pColor ? DEPTH_WRITE : COLOR_DEPTH_WRITE)
                .setOutputState(CLOUDS_TARGET)
                .createCompositeState(true)
        );
    }

    public static RenderType flatClouds() {
        return FLAT_CLOUDS;
    }

    public static RenderType clouds() {
        return CLOUDS;
    }

    public static RenderType cloudsDepthOnly() {
        return CLOUDS_DEPTH_ONLY;
    }

    public static RenderType lines() {
        return LINES;
    }

    public static RenderType secondaryBlockOutline() {
        return SECONDARY_BLOCK_OUTLINE;
    }

    public static RenderType lineStrip() {
        return LINE_STRIP;
    }

    public static RenderType debugLineStrip(double pWidth) {
        return DEBUG_LINE_STRIP.apply(pWidth);
    }

    public static RenderType debugFilledBox() {
        return DEBUG_FILLED_BOX;
    }

    public static RenderType debugQuads() {
        return DEBUG_QUADS;
    }

    public static RenderType debugTriangleFan() {
        return DEBUG_TRIANGLE_FAN;
    }

    public static RenderType debugStructureQuads() {
        return DEBUG_STRUCTURE_QUADS;
    }

    public static RenderType debugSectionQuads() {
        return DEBUG_SECTION_QUADS;
    }

    private static RenderType createWorldBorder(boolean pDepthWrite) {
        return create(
            "world_border",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(POSITION_TEX_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(WorldBorderRenderer.FORCEFIELD_LOCATION, TriState.FALSE, false))
                .setTransparencyState(OVERLAY_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOutputState(WEATHER_TARGET)
                .setWriteMaskState(pDepthWrite ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                .setLayeringState(WORLD_BORDER_LAYERING)
                .setCullState(NO_CULL)
                .createCompositeState(false)
        );
    }

    public static RenderType worldBorder(boolean pDepthWrite) {
        return pDepthWrite ? WORLD_BORDER_DEPTH_WRITE : WORLD_BORDER_NO_DEPTH_WRITE;
    }

    public static RenderType opaqueParticle(ResourceLocation pTexture) {
        return OPAQUE_PARTICLE.apply(pTexture);
    }

    public static RenderType translucentParticle(ResourceLocation pTexture) {
        return TRANSLUCENT_PARTICLE.apply(pTexture);
    }

    private static Function<ResourceLocation, RenderType> createWeather(boolean pDepthWrite) {
        return Util.memoize(
            p_374624_ -> create(
                    "weather",
                    DefaultVertexFormat.PARTICLE,
                    VertexFormat.Mode.QUADS,
                    1536,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                        .setShaderState(PARTICLE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(p_374624_, TriState.FALSE, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(WEATHER_TARGET)
                        .setLightmapState(LIGHTMAP)
                        .setWriteMaskState(pDepthWrite ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)
                )
        );
    }

    public static RenderType weather(ResourceLocation pTexture, boolean pDepthWrite) {
        return (pDepthWrite ? WEATHER_DEPTH_WRITE : WEATHER_NO_DEPTH_WRITE).apply(pTexture);
    }

    public static RenderType sky() {
        return SKY;
    }

    public static RenderType endSky() {
        return END_SKY;
    }

    public static RenderType sunriseSunset() {
        return SUNRISE_SUNSET;
    }

    public static RenderType stars() {
        return STARS;
    }

    public static RenderType celestial(ResourceLocation pTexture) {
        return CELESTIAL.apply(pTexture);
    }

    public static RenderType blockScreenEffect(ResourceLocation pTexture) {
        return BLOCK_SCREEN_EFFECT.apply(pTexture);
    }

    public static RenderType fireScreenEffect(ResourceLocation pTexture) {
        return FIRE_SCREEN_EFFECT.apply(pTexture);
    }

    public static RenderType gui() {
        return GUI;
    }

    public static RenderType guiOverlay() {
        return GUI_OVERLAY;
    }

    public static RenderType guiTexturedOverlay(ResourceLocation pLocation) {
        return GUI_TEXTURED_OVERLAY.apply(pLocation);
    }

    public static RenderType guiOpaqueTexturedBackground(ResourceLocation pLocation) {
        return GUI_OPAQUE_TEXTURED_BACKGROUND.apply(pLocation);
    }

    public static RenderType guiNauseaOverlay() {
        return GUI_NAUSEA_OVERLAY;
    }

    public static RenderType guiTextHighlight() {
        return GUI_TEXT_HIGHLIGHT;
    }

    public static RenderType guiGhostRecipeOverlay() {
        return GUI_GHOST_RECIPE_OVERLAY;
    }

    public static RenderType guiTextured(ResourceLocation pLocation) {
        return GUI_TEXTURED.apply(pLocation);
    }

    public static RenderType vignette(ResourceLocation pLocation) {
        return VIGNETTE.apply(pLocation);
    }

    public static RenderType crosshair(ResourceLocation pLocation) {
        return CROSSHAIR.apply(pLocation);
    }

    public static RenderType mojangLogo() {
        return MOJANG_LOGO;
    }

    public RenderType(
        String pName,
        VertexFormat pFormat,
        VertexFormat.Mode pMode,
        int pBufferSize,
        boolean pAffectsCrumbling,
        boolean pSortOnUpload,
        Runnable pSetupState,
        Runnable pClearState
    ) {
        super(pName, pSetupState, pClearState);
        this.format = pFormat;
        this.mode = pMode;
        this.bufferSize = pBufferSize;
        this.affectsCrumbling = pAffectsCrumbling;
        this.sortOnUpload = pSortOnUpload;
    }

    static RenderType.CompositeRenderType create(
        String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, RenderType.CompositeState pState
    ) {
        return create(pName, pFormat, pMode, pBufferSize, false, false, pState);
    }

    public static RenderType.CompositeRenderType create(
        String pName,
        VertexFormat pFormat,
        VertexFormat.Mode pMode,
        int pBufferSize,
        boolean pAffectsCrumbling,
        boolean pSortOnUpload,
        RenderType.CompositeState pState
    ) {
        return new RenderType.CompositeRenderType(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pState);
    }

    public void draw(MeshData pMeshData) {
        this.setupRenderState();
        BufferUploader.drawWithShader(pMeshData);
        this.clearRenderState();
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static List<RenderType> chunkBufferLayers() {
        return CHUNK_BUFFER_LAYERS;
    }

    public int bufferSize() {
        return this.bufferSize;
    }

    public VertexFormat format() {
        return this.format;
    }

    public VertexFormat.Mode mode() {
        return this.mode;
    }

    public Optional<RenderType> outline() {
        return Optional.empty();
    }

    public boolean isOutline() {
        return false;
    }

    public boolean affectsCrumbling() {
        return this.affectsCrumbling;
    }

    public boolean canConsolidateConsecutiveGeometry() {
        return !this.mode.connectedPrimitives;
    }

    public boolean sortOnUpload() {
        return this.sortOnUpload;
    }

    @OnlyIn(Dist.CLIENT)
    static final class CompositeRenderType extends RenderType {
        static final BiFunction<ResourceLocation, RenderStateShard.CullStateShard, RenderType> OUTLINE = Util.memoize(
            (p_357914_, p_357915_) -> RenderType.create(
                    "outline",
                    DefaultVertexFormat.POSITION_TEX_COLOR,
                    VertexFormat.Mode.QUADS,
                    1536,
                    RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_OUTLINE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(p_357914_, TriState.FALSE, false))
                        .setCullState(p_357915_)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setOutputState(OUTLINE_TARGET)
                        .createCompositeState(RenderType.OutlineProperty.IS_OUTLINE)
                )
        );
        private final RenderType.CompositeState state;
        private final Optional<RenderType> outline;
        private final boolean isOutline;

        CompositeRenderType(
            String pName,
            VertexFormat pFormat,
            VertexFormat.Mode pMode,
            int pBufferSize,
            boolean pAffectsCrumbling,
            boolean pSortOnUpload,
            RenderType.CompositeState pState
        ) {
            super(
                pName,
                pFormat,
                pMode,
                pBufferSize,
                pAffectsCrumbling,
                pSortOnUpload,
                () -> pState.states.forEach(RenderStateShard::setupRenderState),
                () -> pState.states.forEach(RenderStateShard::clearRenderState)
            );
            this.state = pState;
            this.outline = pState.outlineProperty == RenderType.OutlineProperty.AFFECTS_OUTLINE
                ? pState.textureState.cutoutTexture().map(p_173270_ -> OUTLINE.apply(p_173270_, pState.cullState))
                : Optional.empty();
            this.isOutline = pState.outlineProperty == RenderType.OutlineProperty.IS_OUTLINE;
        }

        @Override
        public Optional<RenderType> outline() {
            return this.outline;
        }

        @Override
        public boolean isOutline() {
            return this.isOutline;
        }

        protected final RenderType.CompositeState state() {
            return this.state;
        }

        @Override
        public String toString() {
            return "RenderType[" + this.name + ":" + this.state + "]";
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class CompositeState {
        final RenderStateShard.EmptyTextureStateShard textureState;
        private final RenderStateShard.ShaderStateShard shaderState;
        private final RenderStateShard.TransparencyStateShard transparencyState;
        private final RenderStateShard.DepthTestStateShard depthTestState;
        final RenderStateShard.CullStateShard cullState;
        private final RenderStateShard.LightmapStateShard lightmapState;
        private final RenderStateShard.OverlayStateShard overlayState;
        private final RenderStateShard.LayeringStateShard layeringState;
        private final RenderStateShard.OutputStateShard outputState;
        private final RenderStateShard.TexturingStateShard texturingState;
        private final RenderStateShard.WriteMaskStateShard writeMaskState;
        private final RenderStateShard.LineStateShard lineState;
        private final RenderStateShard.ColorLogicStateShard colorLogicState;
        final RenderType.OutlineProperty outlineProperty;
        final ImmutableList<RenderStateShard> states;

        CompositeState(
            RenderStateShard.EmptyTextureStateShard pTextureState,
            RenderStateShard.ShaderStateShard pShaderState,
            RenderStateShard.TransparencyStateShard pTransparencyState,
            RenderStateShard.DepthTestStateShard pDepthState,
            RenderStateShard.CullStateShard pCullState,
            RenderStateShard.LightmapStateShard pLightmapState,
            RenderStateShard.OverlayStateShard pOverlayState,
            RenderStateShard.LayeringStateShard pLayeringState,
            RenderStateShard.OutputStateShard pOutputState,
            RenderStateShard.TexturingStateShard pTexturingState,
            RenderStateShard.WriteMaskStateShard pWriteMaskState,
            RenderStateShard.LineStateShard pLineState,
            RenderStateShard.ColorLogicStateShard pColorLogicState,
            RenderType.OutlineProperty pOutlineProperty
        ) {
            this.textureState = pTextureState;
            this.shaderState = pShaderState;
            this.transparencyState = pTransparencyState;
            this.depthTestState = pDepthState;
            this.cullState = pCullState;
            this.lightmapState = pLightmapState;
            this.overlayState = pOverlayState;
            this.layeringState = pLayeringState;
            this.outputState = pOutputState;
            this.texturingState = pTexturingState;
            this.writeMaskState = pWriteMaskState;
            this.lineState = pLineState;
            this.colorLogicState = pColorLogicState;
            this.outlineProperty = pOutlineProperty;
            this.states = ImmutableList.of(
                this.textureState,
                this.shaderState,
                this.transparencyState,
                this.depthTestState,
                this.cullState,
                this.lightmapState,
                this.overlayState,
                this.layeringState,
                this.outputState,
                this.texturingState,
                this.writeMaskState,
                this.colorLogicState,
                this.lineState
            );
        }

        @Override
        public String toString() {
            return "CompositeState[" + this.states + ", outlineProperty=" + this.outlineProperty + "]";
        }

        public static RenderType.CompositeState.CompositeStateBuilder builder() {
            return new RenderType.CompositeState.CompositeStateBuilder();
        }

        @OnlyIn(Dist.CLIENT)
        public static class CompositeStateBuilder {
            private RenderStateShard.EmptyTextureStateShard textureState = RenderStateShard.NO_TEXTURE;
            private RenderStateShard.ShaderStateShard shaderState = RenderStateShard.NO_SHADER;
            private RenderStateShard.TransparencyStateShard transparencyState = RenderStateShard.NO_TRANSPARENCY;
            private RenderStateShard.DepthTestStateShard depthTestState = RenderStateShard.LEQUAL_DEPTH_TEST;
            private RenderStateShard.CullStateShard cullState = RenderStateShard.CULL;
            private RenderStateShard.LightmapStateShard lightmapState = RenderStateShard.NO_LIGHTMAP;
            private RenderStateShard.OverlayStateShard overlayState = RenderStateShard.NO_OVERLAY;
            private RenderStateShard.LayeringStateShard layeringState = RenderStateShard.NO_LAYERING;
            private RenderStateShard.OutputStateShard outputState = RenderStateShard.MAIN_TARGET;
            private RenderStateShard.TexturingStateShard texturingState = RenderStateShard.DEFAULT_TEXTURING;
            private RenderStateShard.WriteMaskStateShard writeMaskState = RenderStateShard.COLOR_DEPTH_WRITE;
            private RenderStateShard.LineStateShard lineState = RenderStateShard.DEFAULT_LINE;
            private RenderStateShard.ColorLogicStateShard colorLogicState = RenderStateShard.NO_COLOR_LOGIC;

            CompositeStateBuilder() {
            }

            public RenderType.CompositeState.CompositeStateBuilder setTextureState(RenderStateShard.EmptyTextureStateShard pTextureState) {
                this.textureState = pTextureState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setShaderState(RenderStateShard.ShaderStateShard pShaderState) {
                this.shaderState = pShaderState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setTransparencyState(RenderStateShard.TransparencyStateShard pTransparencyState) {
                this.transparencyState = pTransparencyState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setDepthTestState(RenderStateShard.DepthTestStateShard pDepthTestState) {
                this.depthTestState = pDepthTestState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setCullState(RenderStateShard.CullStateShard pCullState) {
                this.cullState = pCullState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLightmapState(RenderStateShard.LightmapStateShard pLightmapState) {
                this.lightmapState = pLightmapState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setOverlayState(RenderStateShard.OverlayStateShard pOverlayState) {
                this.overlayState = pOverlayState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLayeringState(RenderStateShard.LayeringStateShard pLayerState) {
                this.layeringState = pLayerState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setOutputState(RenderStateShard.OutputStateShard pOutputState) {
                this.outputState = pOutputState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setTexturingState(RenderStateShard.TexturingStateShard pTexturingState) {
                this.texturingState = pTexturingState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setWriteMaskState(RenderStateShard.WriteMaskStateShard pWriteMaskState) {
                this.writeMaskState = pWriteMaskState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLineState(RenderStateShard.LineStateShard pLineState) {
                this.lineState = pLineState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setColorLogicState(RenderStateShard.ColorLogicStateShard pColorLogicState) {
                this.colorLogicState = pColorLogicState;
                return this;
            }

            public RenderType.CompositeState createCompositeState(boolean pOutline) {
                return this.createCompositeState(pOutline ? RenderType.OutlineProperty.AFFECTS_OUTLINE : RenderType.OutlineProperty.NONE);
            }

            public RenderType.CompositeState createCompositeState(RenderType.OutlineProperty pOutlineState) {
                return new RenderType.CompositeState(
                    this.textureState,
                    this.shaderState,
                    this.transparencyState,
                    this.depthTestState,
                    this.cullState,
                    this.lightmapState,
                    this.overlayState,
                    this.layeringState,
                    this.outputState,
                    this.texturingState,
                    this.writeMaskState,
                    this.lineState,
                    this.colorLogicState,
                    pOutlineState
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum OutlineProperty {
        NONE("none"),
        IS_OUTLINE("is_outline"),
        AFFECTS_OUTLINE("affects_outline");

        private final String name;

        private OutlineProperty(final String pName) {
            this.name = pName;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}

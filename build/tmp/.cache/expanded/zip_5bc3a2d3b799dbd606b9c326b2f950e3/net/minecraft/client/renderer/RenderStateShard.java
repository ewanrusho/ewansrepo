package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

@OnlyIn(Dist.CLIENT)
public abstract class RenderStateShard {
    public static final double MAX_ENCHANTMENT_GLINT_SPEED_MILLIS = 8.0;
    protected final String name;
    protected Runnable setupState;
    private final Runnable clearState;
    protected static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
        "no_transparency", () -> RenderSystem.disableBlend(), () -> {
        }
    );
    protected static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("additive_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    protected static final RenderStateShard.TransparencyStateShard LIGHTNING_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("lightning_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    protected static final RenderStateShard.TransparencyStateShard GLINT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
        "glint_transparency",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE
            );
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );
    protected static final RenderStateShard.TransparencyStateShard CRUMBLING_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
        "crumbling_transparency",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );
    protected static final RenderStateShard.TransparencyStateShard OVERLAY_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
        "overlay_transparency",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );
    protected static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
        "translucent_transparency",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );
    protected static final RenderStateShard.TransparencyStateShard VIGNETTE_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("vignette_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    protected static final RenderStateShard.TransparencyStateShard CROSSHAIR_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
        "crosshair_transparency",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            );
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );
    protected static final RenderStateShard.TransparencyStateShard MOJANG_LOGO_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("mojang_logo_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    protected static final RenderStateShard.TransparencyStateShard NAUSEA_OVERLAY_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
        "nausea_overlay_transparency",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE
            );
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );
    protected static final RenderStateShard.ShaderStateShard NO_SHADER = new RenderStateShard.ShaderStateShard();
    protected static final RenderStateShard.ShaderStateShard POSITION_COLOR_LIGHTMAP_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.POSITION_COLOR_LIGHTMAP);
    protected static final RenderStateShard.ShaderStateShard POSITION_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.POSITION);
    protected static final RenderStateShard.ShaderStateShard POSITION_TEX_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.POSITION_TEX);
    protected static final RenderStateShard.ShaderStateShard POSITION_COLOR_TEX_LIGHTMAP_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.POSITION_COLOR_TEX_LIGHTMAP);
    protected static final RenderStateShard.ShaderStateShard POSITION_COLOR_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.POSITION_COLOR);
    protected static final RenderStateShard.ShaderStateShard POSITION_TEXTURE_COLOR_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.POSITION_TEX_COLOR);
    protected static final RenderStateShard.ShaderStateShard PARTICLE_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.PARTICLE);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_SOLID_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_SOLID);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_CUTOUT_MIPPED_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_CUTOUT_MIPPED);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_CUTOUT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_TRANSLUCENT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_TRANSLUCENT_MOVING_BLOCK);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ARMOR_CUTOUT_NO_CULL);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ARMOR_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ARMOR_TRANSLUCENT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SOLID_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_SOLID);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_CUTOUT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_CUTOUT_NO_CULL);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_TRANSLUCENT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_SMOOTH_CUTOUT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_BEACON_BEAM_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_BEACON_BEAM);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_DECAL_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_DECAL);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_NO_OUTLINE_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_NO_OUTLINE);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SHADOW_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_SHADOW);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_ALPHA_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_ALPHA);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_EYES_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_EYES);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENERGY_SWIRL_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENERGY_SWIRL);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_LEASH_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_LEASH);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_WATER_MASK_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_WATER_MASK);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_OUTLINE_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_OUTLINE);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ARMOR_ENTITY_GLINT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GLINT_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_GLINT_TRANSLUCENT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GLINT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_GLINT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_GLINT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_ENTITY_GLINT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_CRUMBLING_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_CRUMBLING);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_TEXT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_BACKGROUND_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_TEXT_BACKGROUND);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_INTENSITY_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_TEXT_INTENSITY);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_SEE_THROUGH_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_TEXT_SEE_THROUGH);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_LIGHTNING_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_LIGHTNING);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TRIPWIRE_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_TRIPWIRE);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_END_PORTAL_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_END_PORTAL);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_END_GATEWAY_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_END_GATEWAY);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_CLOUDS_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_CLOUDS);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_LINES_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_LINES);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_GUI);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_OVERLAY_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_GUI_OVERLAY);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_TEXT_HIGHLIGHT_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_GUI_TEXT_HIGHLIGHT);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_BREEZE_WIND_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.RENDERTYPE_BREEZE_WIND);
    protected static final RenderStateShard.TextureStateShard BLOCK_SHEET_MIPPED = new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, TriState.FALSE, true);
    protected static final RenderStateShard.TextureStateShard BLOCK_SHEET = new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, TriState.FALSE, false);
    protected static final RenderStateShard.EmptyTextureStateShard NO_TEXTURE = new RenderStateShard.EmptyTextureStateShard();
    protected static final RenderStateShard.TexturingStateShard DEFAULT_TEXTURING = new RenderStateShard.TexturingStateShard("default_texturing", () -> {
    }, () -> {
    });
    protected static final RenderStateShard.TexturingStateShard GLINT_TEXTURING = new RenderStateShard.TexturingStateShard(
        "glint_texturing", () -> setupGlintTexturing(8.0F), () -> RenderSystem.resetTextureMatrix()
    );
    protected static final RenderStateShard.TexturingStateShard ENTITY_GLINT_TEXTURING = new RenderStateShard.TexturingStateShard(
        "entity_glint_texturing", () -> setupGlintTexturing(0.16F), () -> RenderSystem.resetTextureMatrix()
    );
    protected static final RenderStateShard.LightmapStateShard LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
    protected static final RenderStateShard.LightmapStateShard NO_LIGHTMAP = new RenderStateShard.LightmapStateShard(false);
    protected static final RenderStateShard.OverlayStateShard OVERLAY = new RenderStateShard.OverlayStateShard(true);
    protected static final RenderStateShard.OverlayStateShard NO_OVERLAY = new RenderStateShard.OverlayStateShard(false);
    protected static final RenderStateShard.CullStateShard CULL = new RenderStateShard.CullStateShard(true);
    protected static final RenderStateShard.CullStateShard NO_CULL = new RenderStateShard.CullStateShard(false);
    protected static final RenderStateShard.DepthTestStateShard NO_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("always", 519);
    protected static final RenderStateShard.DepthTestStateShard EQUAL_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("==", 514);
    protected static final RenderStateShard.DepthTestStateShard LEQUAL_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("<=", 515);
    protected static final RenderStateShard.DepthTestStateShard GREATER_DEPTH_TEST = new RenderStateShard.DepthTestStateShard(">", 516);
    protected static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH_WRITE = new RenderStateShard.WriteMaskStateShard(true, true);
    protected static final RenderStateShard.WriteMaskStateShard COLOR_WRITE = new RenderStateShard.WriteMaskStateShard(true, false);
    protected static final RenderStateShard.WriteMaskStateShard DEPTH_WRITE = new RenderStateShard.WriteMaskStateShard(false, true);
    protected static final RenderStateShard.LayeringStateShard NO_LAYERING = new RenderStateShard.LayeringStateShard("no_layering", () -> {
    }, () -> {
    });
    protected static final RenderStateShard.LayeringStateShard POLYGON_OFFSET_LAYERING = new RenderStateShard.LayeringStateShard("polygon_offset_layering", () -> {
        RenderSystem.polygonOffset(-1.0F, -10.0F);
        RenderSystem.enablePolygonOffset();
    }, () -> {
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
    });
    protected static final RenderStateShard.LayeringStateShard VIEW_OFFSET_Z_LAYERING = new RenderStateShard.LayeringStateShard("view_offset_z_layering", () -> {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        RenderSystem.getProjectionType().applyLayeringTransform(matrix4fstack, 1.0F);
    }, () -> {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.popMatrix();
    });
    protected static final RenderStateShard.LayeringStateShard VIEW_OFFSET_Z_LAYERING_FORWARD = new RenderStateShard.LayeringStateShard("view_offset_z_layering_forward", () -> {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        RenderSystem.getProjectionType().applyLayeringTransform(matrix4fstack, -1.0F);
    }, () -> {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.popMatrix();
    });
    protected static final RenderStateShard.LayeringStateShard WORLD_BORDER_LAYERING = new RenderStateShard.LayeringStateShard("world_border_layering", () -> {
        RenderSystem.polygonOffset(-3.0F, -3.0F);
        RenderSystem.enablePolygonOffset();
    }, () -> {
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
    });
    protected static final RenderStateShard.OutputStateShard MAIN_TARGET = new RenderStateShard.OutputStateShard(
        "main_target", () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(false), () -> {
        }
    );
    protected static final RenderStateShard.OutputStateShard OUTLINE_TARGET = new RenderStateShard.OutputStateShard("outline_target", () -> {
        RenderTarget rendertarget = Minecraft.getInstance().levelRenderer.entityOutlineTarget();
        if (rendertarget != null) {
            rendertarget.bindWrite(false);
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }, () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(false));
    protected static final RenderStateShard.OutputStateShard TRANSLUCENT_TARGET = new RenderStateShard.OutputStateShard("translucent_target", () -> {
        RenderTarget rendertarget = Minecraft.getInstance().levelRenderer.getTranslucentTarget();
        if (rendertarget != null) {
            rendertarget.bindWrite(false);
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }, () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(false));
    protected static final RenderStateShard.OutputStateShard PARTICLES_TARGET = new RenderStateShard.OutputStateShard("particles_target", () -> {
        RenderTarget rendertarget = Minecraft.getInstance().levelRenderer.getParticlesTarget();
        if (rendertarget != null) {
            rendertarget.bindWrite(false);
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }, () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(false));
    protected static final RenderStateShard.OutputStateShard WEATHER_TARGET = new RenderStateShard.OutputStateShard("weather_target", () -> {
        RenderTarget rendertarget = Minecraft.getInstance().levelRenderer.getWeatherTarget();
        if (rendertarget != null) {
            rendertarget.bindWrite(false);
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }, () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(false));
    protected static final RenderStateShard.OutputStateShard CLOUDS_TARGET = new RenderStateShard.OutputStateShard("clouds_target", () -> {
        RenderTarget rendertarget = Minecraft.getInstance().levelRenderer.getCloudsTarget();
        if (rendertarget != null) {
            rendertarget.bindWrite(false);
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }, () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(false));
    protected static final RenderStateShard.OutputStateShard ITEM_ENTITY_TARGET = new RenderStateShard.OutputStateShard("item_entity_target", () -> {
        RenderTarget rendertarget = Minecraft.getInstance().levelRenderer.getItemEntityTarget();
        if (rendertarget != null) {
            rendertarget.bindWrite(false);
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }, () -> Minecraft.getInstance().getMainRenderTarget().bindWrite(false));
    protected static final RenderStateShard.LineStateShard DEFAULT_LINE = new RenderStateShard.LineStateShard(OptionalDouble.of(1.0));
    protected static final RenderStateShard.ColorLogicStateShard NO_COLOR_LOGIC = new RenderStateShard.ColorLogicStateShard(
        "no_color_logic", () -> RenderSystem.disableColorLogicOp(), () -> {
        }
    );
    protected static final RenderStateShard.ColorLogicStateShard OR_REVERSE_COLOR_LOGIC = new RenderStateShard.ColorLogicStateShard("or_reverse", () -> {
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
    }, () -> RenderSystem.disableColorLogicOp());

    public RenderStateShard(String pName, Runnable pSetupState, Runnable pClearState) {
        this.name = pName;
        this.setupState = pSetupState;
        this.clearState = pClearState;
    }

    public void setupRenderState() {
        this.setupState.run();
    }

    public void clearRenderState() {
        this.clearState.run();
    }

    @Override
    public String toString() {
        return this.name;
    }

    private static void setupGlintTexturing(float pScale) {
        long i = (long)((double)Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * 8.0);
        float f = (float)(i % 110000L) / 110000.0F;
        float f1 = (float)(i % 30000L) / 30000.0F;
        Matrix4f matrix4f = new Matrix4f().translation(-f, f1, 0.0F);
        matrix4f.rotateZ((float) (Math.PI / 18)).scale(pScale);
        RenderSystem.setTextureMatrix(matrix4f);
    }

    @OnlyIn(Dist.CLIENT)
    public static class BooleanStateShard extends RenderStateShard {
        private final boolean enabled;

        public BooleanStateShard(String pName, Runnable pSetupState, Runnable pClearState, boolean pEnabled) {
            super(pName, pSetupState, pClearState);
            this.enabled = pEnabled;
        }

        @Override
        public String toString() {
            return this.name + "[" + this.enabled + "]";
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected static class ColorLogicStateShard extends RenderStateShard {
        public ColorLogicStateShard(String p_286784_, Runnable p_286884_, Runnable p_286375_) {
            super(p_286784_, p_286884_, p_286375_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class CullStateShard extends RenderStateShard.BooleanStateShard {
        public CullStateShard(boolean pUseCull) {
            super("cull", () -> {
                if (!pUseCull) {
                    RenderSystem.disableCull();
                }
            }, () -> {
                if (!pUseCull) {
                    RenderSystem.enableCull();
                }
            }, pUseCull);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class DepthTestStateShard extends RenderStateShard {
        private final String functionName;

        public DepthTestStateShard(String pFunctionName, int pDepthFunc) {
            super("depth_test", () -> {
                if (pDepthFunc != 519) {
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(pDepthFunc);
                }
            }, () -> {
                if (pDepthFunc != 519) {
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthFunc(515);
                }
            });
            this.functionName = pFunctionName;
        }

        @Override
        public String toString() {
            return this.name + "[" + this.functionName + "]";
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class EmptyTextureStateShard extends RenderStateShard {
        public EmptyTextureStateShard(Runnable pSetupState, Runnable pClearState) {
            super("texture", pSetupState, pClearState);
        }

        EmptyTextureStateShard() {
            super("texture", () -> {
            }, () -> {
            });
        }

        protected Optional<ResourceLocation> cutoutTexture() {
            return Optional.empty();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LayeringStateShard extends RenderStateShard {
        public LayeringStateShard(String p_110267_, Runnable p_110268_, Runnable p_110269_) {
            super(p_110267_, p_110268_, p_110269_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LightmapStateShard extends RenderStateShard.BooleanStateShard {
        public LightmapStateShard(boolean pUseLightmap) {
            super("lightmap", () -> {
                if (pUseLightmap) {
                    Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
                }
            }, () -> {
                if (pUseLightmap) {
                    Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
                }
            }, pUseLightmap);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected static class LineStateShard extends RenderStateShard {
        private final OptionalDouble width;

        public LineStateShard(OptionalDouble pWidth) {
            super("line_width", () -> {
                if (!Objects.equals(pWidth, OptionalDouble.of(1.0))) {
                    if (pWidth.isPresent()) {
                        RenderSystem.lineWidth((float)pWidth.getAsDouble());
                    } else {
                        RenderSystem.lineWidth(Math.max(2.5F, (float)Minecraft.getInstance().getWindow().getWidth() / 1920.0F * 2.5F));
                    }
                }
            }, () -> {
                if (!Objects.equals(pWidth, OptionalDouble.of(1.0))) {
                    RenderSystem.lineWidth(1.0F);
                }
            });
            this.width = pWidth;
        }

        @Override
        public String toString() {
            return this.name + "[" + (this.width.isPresent() ? this.width.getAsDouble() : "window_scale") + "]";
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MultiTextureStateShard extends RenderStateShard.EmptyTextureStateShard {
        private final Optional<ResourceLocation> cutoutTexture;

        MultiTextureStateShard(List<RenderStateShard.MultiTextureStateShard.Entry> pEntries) {
            super(() -> {
                for (int i = 0; i < pEntries.size(); i++) {
                    RenderStateShard.MultiTextureStateShard.Entry renderstateshard$multitexturestateshard$entry = pEntries.get(i);
                    TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
                    AbstractTexture abstracttexture = texturemanager.getTexture(renderstateshard$multitexturestateshard$entry.id);
                    abstracttexture.setFilter(renderstateshard$multitexturestateshard$entry.blur, renderstateshard$multitexturestateshard$entry.mipmap);
                    RenderSystem.setShaderTexture(i, abstracttexture.getId());
                }
            }, () -> {
            });
            this.cutoutTexture = pEntries.isEmpty() ? Optional.empty() : Optional.of(pEntries.getFirst().id);
        }

        @Override
        protected Optional<ResourceLocation> cutoutTexture() {
            return this.cutoutTexture;
        }

        public static RenderStateShard.MultiTextureStateShard.Builder builder() {
            return new RenderStateShard.MultiTextureStateShard.Builder();
        }

        @OnlyIn(Dist.CLIENT)
        public static final class Builder {
            private final ImmutableList.Builder<RenderStateShard.MultiTextureStateShard.Entry> builder = new ImmutableList.Builder<>();

            public RenderStateShard.MultiTextureStateShard.Builder add(ResourceLocation pTexture, boolean pBlur, boolean pMipmap) {
                this.builder.add(new RenderStateShard.MultiTextureStateShard.Entry(pTexture, pBlur, pMipmap));
                return this;
            }

            public RenderStateShard.MultiTextureStateShard build() {
                return new RenderStateShard.MultiTextureStateShard(this.builder.build());
            }
        }

        @OnlyIn(Dist.CLIENT)
        static record Entry(ResourceLocation id, boolean blur, boolean mipmap) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class OffsetTexturingStateShard extends RenderStateShard.TexturingStateShard {
        public OffsetTexturingStateShard(float pU, float pV) {
            super(
                "offset_texturing",
                () -> RenderSystem.setTextureMatrix(new Matrix4f().translation(pU, pV, 0.0F)),
                () -> RenderSystem.resetTextureMatrix()
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class OutputStateShard extends RenderStateShard {
        public OutputStateShard(String p_110300_, Runnable p_110301_, Runnable p_110302_) {
            super(p_110300_, p_110301_, p_110302_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class OverlayStateShard extends RenderStateShard.BooleanStateShard {
        public OverlayStateShard(boolean pUseOverlay) {
            super("overlay", () -> {
                if (pUseOverlay) {
                    Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
                }
            }, () -> {
                if (pUseOverlay) {
                    Minecraft.getInstance().gameRenderer.overlayTexture().teardownOverlayColor();
                }
            }, pUseOverlay);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ShaderStateShard extends RenderStateShard {
        private final Optional<ShaderProgram> shader;

        public ShaderStateShard(ShaderProgram pShader) {
            super("shader", () -> RenderSystem.setShader(pShader), () -> {
            });
            this.shader = Optional.of(pShader);
        }

        public ShaderStateShard() {
            super("shader", RenderSystem::clearShader, () -> {
            });
            this.shader = Optional.empty();
        }

        @Override
        public String toString() {
            return this.name + "[" + this.shader + "]";
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class TextureStateShard extends RenderStateShard.EmptyTextureStateShard {
        private final Optional<ResourceLocation> texture;
        protected TriState blur;
        protected boolean mipmap;

        public TextureStateShard(ResourceLocation pTexture, TriState pBlur, boolean pMipmap) {
            super(() -> {
                TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
                AbstractTexture abstracttexture = texturemanager.getTexture(pTexture);
                abstracttexture.setFilter(pBlur, pMipmap);
                RenderSystem.setShaderTexture(0, abstracttexture.getId());
            }, () -> {
            });
            this.texture = Optional.of(pTexture);
            this.blur = pBlur;
            this.mipmap = pMipmap;
        }

        @Override
        public String toString() {
            return this.name + "[" + this.texture + "(blur=" + this.blur + ", mipmap=" + this.mipmap + ")]";
        }

        @Override
        protected Optional<ResourceLocation> cutoutTexture() {
            return this.texture;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class TexturingStateShard extends RenderStateShard {
        public TexturingStateShard(String p_110349_, Runnable p_110350_, Runnable p_110351_) {
            super(p_110349_, p_110350_, p_110351_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class TransparencyStateShard extends RenderStateShard {
        public TransparencyStateShard(String p_110353_, Runnable p_110354_, Runnable p_110355_) {
            super(p_110353_, p_110354_, p_110355_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WriteMaskStateShard extends RenderStateShard {
        private final boolean writeColor;
        private final boolean writeDepth;

        public WriteMaskStateShard(boolean pWriteColor, boolean pWriteDepth) {
            super("write_mask_state", () -> {
                if (!pWriteDepth) {
                    RenderSystem.depthMask(pWriteDepth);
                }

                if (!pWriteColor) {
                    RenderSystem.colorMask(pWriteColor, pWriteColor, pWriteColor, pWriteColor);
                }
            }, () -> {
                if (!pWriteDepth) {
                    RenderSystem.depthMask(true);
                }

                if (!pWriteColor) {
                    RenderSystem.colorMask(true, true, true, true);
                }
            });
            this.writeColor = pWriteColor;
            this.writeDepth = pWriteDepth;
        }

        @Override
        public String toString() {
            return this.name + "[writeColor=" + this.writeColor + ", writeDepth=" + this.writeDepth + "]";
        }
    }
}
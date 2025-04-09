package net.minecraft.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class LightTexture implements AutoCloseable {
    public static final int FULL_BRIGHT = 15728880;
    public static final int FULL_SKY = 15728640;
    public static final int FULL_BLOCK = 240;
    private static final int TEXTURE_SIZE = 16;
    private final TextureTarget target;
    private boolean updateLightTexture;
    private float blockLightRedFlicker;
    private final GameRenderer renderer;
    private final Minecraft minecraft;

    public LightTexture(GameRenderer pRenderer, Minecraft pMinecraft) {
        this.renderer = pRenderer;
        this.minecraft = pMinecraft;
        this.target = new TextureTarget(16, 16, false);
        this.target.setFilterMode(9729);
        this.target.setClearColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.target.clear();
    }

    @Override
    public void close() {
        this.target.destroyBuffers();
    }

    public void tick() {
        this.blockLightRedFlicker = this.blockLightRedFlicker + (float)((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1);
        this.blockLightRedFlicker *= 0.9F;
        this.updateLightTexture = true;
    }

    public void turnOffLightLayer() {
        RenderSystem.setShaderTexture(2, 0);
    }

    public void turnOnLightLayer() {
        RenderSystem.setShaderTexture(2, this.target.getColorTextureId());
    }

    private float getDarknessGamma(float pPartialTick) {
        MobEffectInstance mobeffectinstance = this.minecraft.player.getEffect(MobEffects.DARKNESS);
        return mobeffectinstance != null ? mobeffectinstance.getBlendFactor(this.minecraft.player, pPartialTick) : 0.0F;
    }

    private float calculateDarknessScale(LivingEntity pEntity, float pGamma, float pPartialTick) {
        float f = 0.45F * pGamma;
        return Math.max(0.0F, Mth.cos(((float)pEntity.tickCount - pPartialTick) * (float) Math.PI * 0.025F) * f);
    }

    public void updateLightTexture(float pPartialTicks) {
        if (this.updateLightTexture) {
            this.updateLightTexture = false;
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("lightTex");
            ClientLevel clientlevel = this.minecraft.level;
            if (clientlevel != null) {
                float f = clientlevel.getSkyDarken(1.0F);
                float f1;
                if (clientlevel.getSkyFlashTime() > 0) {
                    f1 = 1.0F;
                } else {
                    f1 = f * 0.95F + 0.05F;
                }

                float f2 = this.minecraft.options.darknessEffectScale().get().floatValue();
                float f3 = this.getDarknessGamma(pPartialTicks) * f2;
                float f4 = this.calculateDarknessScale(this.minecraft.player, f3, pPartialTicks) * f2;
                float f6 = this.minecraft.player.getWaterVision();
                float f5;
                if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                    f5 = GameRenderer.getNightVisionScale(this.minecraft.player, pPartialTicks);
                } else if (f6 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                    f5 = f6;
                } else {
                    f5 = 0.0F;
                }

                Vector3f vector3f = new Vector3f(f, f, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
                float f7 = this.blockLightRedFlicker + 1.5F;
                float f8 = clientlevel.dimensionType().ambientLight();
                boolean flag = clientlevel.effects().forceBrightLightmap();
                float f9 = this.minecraft.options.gamma().get().floatValue();
                CompiledShaderProgram compiledshaderprogram = Objects.requireNonNull(
                    RenderSystem.setShader(CoreShaders.LIGHTMAP), "Lightmap shader not loaded"
                );
                compiledshaderprogram.safeGetUniform("AmbientLightFactor").set(f8);
                compiledshaderprogram.safeGetUniform("SkyFactor").set(f1);
                compiledshaderprogram.safeGetUniform("BlockFactor").set(f7);
                compiledshaderprogram.safeGetUniform("UseBrightLightmap").set(flag ? 1 : 0);
                compiledshaderprogram.safeGetUniform("SkyLightColor").set(vector3f);
                compiledshaderprogram.safeGetUniform("NightVisionFactor").set(f5);
                compiledshaderprogram.safeGetUniform("DarknessScale").set(f4);
                compiledshaderprogram.safeGetUniform("DarkenWorldFactor").set(this.renderer.getDarkenWorldAmount(pPartialTicks));
                compiledshaderprogram.safeGetUniform("BrightnessFactor").set(Math.max(0.0F, f9 - f3));
                this.target.bindWrite(true);
                BufferBuilder bufferbuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
                bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
                bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
                bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
                bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);
                BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                this.target.unbindWrite();
                profilerfiller.pop();
            }
        }
    }

    public static float getBrightness(DimensionType pDimensionType, int pLightLevel) {
        return getBrightness(pDimensionType.ambientLight(), pLightLevel);
    }

    public static float getBrightness(float pAmbientLight, int pLightLevel) {
        float f = (float)pLightLevel / 15.0F;
        float f1 = f / (4.0F - 3.0F * f);
        return Mth.lerp(pAmbientLight, f1, 1.0F);
    }

    public static int pack(int pBlockLight, int pSkyLight) {
        return pBlockLight << 4 | pSkyLight << 20;
    }

    public static int block(int pPackedLight) {
        return (pPackedLight & 0xFFFF) >> 4; // Forge: Fix fullbright quads showing dark artifacts. Reported as MC-169806
    }

    public static int sky(int pPackedLight) {
        return pPackedLight >>> 20 & 15;
    }

    public static int lightCoordsWithEmission(int pPackedLight, int pEmission) {
        if (pEmission == 0) {
            return pPackedLight;
        } else {
            int i = Math.max(sky(pPackedLight), pEmission);
            int j = Math.max(block(pPackedLight), pEmission);
            return pack(j, i);
        }
    }
}

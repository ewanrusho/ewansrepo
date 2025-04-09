package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.CloudStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CloudRenderer extends SimplePreparableReloadListener<Optional<CloudRenderer.TextureData>> implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final float HEIGHT_IN_BLOCKS = 4.0F;
    private static final float BLOCKS_PER_SECOND = 0.6F;
    private static final long EMPTY_CELL = 0L;
    private static final int COLOR_OFFSET = 4;
    private static final int NORTH_OFFSET = 3;
    private static final int EAST_OFFSET = 2;
    private static final int SOUTH_OFFSET = 1;
    private static final int WEST_OFFSET = 0;
    private boolean needsRebuild = true;
    private int prevCellX = Integer.MIN_VALUE;
    private int prevCellZ = Integer.MIN_VALUE;
    private CloudRenderer.RelativeCameraPos prevRelativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
    @Nullable
    private CloudStatus prevType;
    @Nullable
    private CloudRenderer.TextureData texture;
    private final VertexBuffer vertexBuffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
    private boolean vertexBufferEmpty;

    protected Optional<CloudRenderer.TextureData> prepare(ResourceManager p_361257_, ProfilerFiller p_362196_) {
        try {
            Optional optional;
            try (
                InputStream inputstream = p_361257_.open(TEXTURE_LOCATION);
                NativeImage nativeimage = NativeImage.read(inputstream);
            ) {
                int i = nativeimage.getWidth();
                int j = nativeimage.getHeight();
                long[] along = new long[i * j];

                for (int k = 0; k < j; k++) {
                    for (int l = 0; l < i; l++) {
                        int i1 = nativeimage.getPixel(l, k);
                        if (isCellEmpty(i1)) {
                            along[l + k * i] = 0L;
                        } else {
                            boolean flag = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k - 1, j)));
                            boolean flag1 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l + 1, j), k));
                            boolean flag2 = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k + 1, j)));
                            boolean flag3 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l - 1, j), k));
                            along[l + k * i] = packCellData(i1, flag, flag1, flag2, flag3);
                        }
                    }
                }

                optional = Optional.of(new CloudRenderer.TextureData(along, i, j));
            }

            return optional;
        } catch (IOException ioexception) {
            LOGGER.error("Failed to load cloud texture", (Throwable)ioexception);
            return Optional.empty();
        }
    }

    protected void apply(Optional<CloudRenderer.TextureData> p_370042_, ResourceManager p_368869_, ProfilerFiller p_367795_) {
        this.texture = p_370042_.orElse(null);
        this.needsRebuild = true;
    }

    private static boolean isCellEmpty(int pColor) {
        return ARGB.alpha(pColor) < 10;
    }

    private static long packCellData(int pColor, boolean pNorthEmpty, boolean pEastEmpty, boolean pSouthEmpty, boolean pWestEmpty) {
        return (long)pColor << 4
            | (long)((pNorthEmpty ? 1 : 0) << 3)
            | (long)((pEastEmpty ? 1 : 0) << 2)
            | (long)((pSouthEmpty ? 1 : 0) << 1)
            | (long)((pWestEmpty ? 1 : 0) << 0);
    }

    private static int getColor(long pCellData) {
        return (int)(pCellData >> 4 & 4294967295L);
    }

    private static boolean isNorthEmpty(long pCellData) {
        return (pCellData >> 3 & 1L) != 0L;
    }

    private static boolean isEastEmpty(long pCellData) {
        return (pCellData >> 2 & 1L) != 0L;
    }

    private static boolean isSouthEmpty(long pCellData) {
        return (pCellData >> 1 & 1L) != 0L;
    }

    private static boolean isWestEmpty(long pCellData) {
        return (pCellData >> 0 & 1L) != 0L;
    }

    public void render(int pHeight, CloudStatus pCloudStatus, float pColor, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix, Vec3 pCameraPosiiton, float pTicks) {
        if (this.texture != null) {
            float f = (float)((double)pColor - pCameraPosiiton.y);
            float f1 = f + 4.0F;
            CloudRenderer.RelativeCameraPos cloudrenderer$relativecamerapos;
            if (f1 < 0.0F) {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
            } else if (f > 0.0F) {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
            } else {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
            }

            double d0 = pCameraPosiiton.x + (double)(pTicks * 0.030000001F);
            double d1 = pCameraPosiiton.z + 3.96F;
            double d2 = (double)this.texture.width * 12.0;
            double d3 = (double)this.texture.height * 12.0;
            d0 -= (double)Mth.floor(d0 / d2) * d2;
            d1 -= (double)Mth.floor(d1 / d3) * d3;
            int i = Mth.floor(d0 / 12.0);
            int j = Mth.floor(d1 / 12.0);
            float f2 = (float)(d0 - (double)((float)i * 12.0F));
            float f3 = (float)(d1 - (double)((float)j * 12.0F));
            RenderType rendertype = pCloudStatus == CloudStatus.FANCY ? RenderType.clouds() : RenderType.flatClouds();
            this.vertexBuffer.bind();
            if (this.needsRebuild
                || i != this.prevCellX
                || j != this.prevCellZ
                || cloudrenderer$relativecamerapos != this.prevRelativeCameraPos
                || pCloudStatus != this.prevType) {
                this.needsRebuild = false;
                this.prevCellX = i;
                this.prevCellZ = j;
                this.prevRelativeCameraPos = cloudrenderer$relativecamerapos;
                this.prevType = pCloudStatus;
                MeshData meshdata = this.buildMesh(Tesselator.getInstance(), i, j, pCloudStatus, cloudrenderer$relativecamerapos, rendertype);
                if (meshdata != null) {
                    this.vertexBuffer.upload(meshdata);
                    this.vertexBufferEmpty = false;
                } else {
                    this.vertexBufferEmpty = true;
                }
            }

            if (!this.vertexBufferEmpty) {
                RenderSystem.setShaderColor(ARGB.redFloat(pHeight), ARGB.greenFloat(pHeight), ARGB.blueFloat(pHeight), 1.0F);
                if (pCloudStatus == CloudStatus.FANCY) {
                    this.drawWithRenderType(RenderType.cloudsDepthOnly(), pFrustumMatrix, pProjectionMatrix, f2, f, f3);
                }

                this.drawWithRenderType(rendertype, pFrustumMatrix, pProjectionMatrix, f2, f, f3);
                VertexBuffer.unbind();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private void drawWithRenderType(RenderType pRenderType, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix, float pX, float pY, float pZ) {
        pRenderType.setupRenderState();
        CompiledShaderProgram compiledshaderprogram = RenderSystem.getShader();
        if (compiledshaderprogram != null && compiledshaderprogram.MODEL_OFFSET != null) {
            compiledshaderprogram.MODEL_OFFSET.set(-pX, pY, -pZ);
        }

        this.vertexBuffer.drawWithShader(pFrustumMatrix, pProjectionMatrix, compiledshaderprogram);
        pRenderType.clearRenderState();
    }

    @Nullable
    private MeshData buildMesh(
        Tesselator pTesselator, int pCellX, int pCellZ, CloudStatus pCloudStatus, CloudRenderer.RelativeCameraPos pRelativeCameraPos, RenderType pRenderType
    ) {
        float f = 0.8F;
        int i = ARGB.colorFromFloat(0.8F, 1.0F, 1.0F, 1.0F);
        int j = ARGB.colorFromFloat(0.8F, 0.9F, 0.9F, 0.9F);
        int k = ARGB.colorFromFloat(0.8F, 0.7F, 0.7F, 0.7F);
        int l = ARGB.colorFromFloat(0.8F, 0.8F, 0.8F, 0.8F);
        BufferBuilder bufferbuilder = pTesselator.begin(pRenderType.mode(), pRenderType.format());
        this.buildMesh(pRelativeCameraPos, bufferbuilder, pCellX, pCellZ, k, i, j, l, pCloudStatus == CloudStatus.FANCY);
        return bufferbuilder.build();
    }

    private void buildMesh(
        CloudRenderer.RelativeCameraPos pRelativeCameraPos,
        BufferBuilder pBufferBuilder,
        int pCellX,
        int pCellZ,
        int pBottomColor,
        int pTopColor,
        int pSideColor,
        int pFrontColor,
        boolean pFancyClouds
    ) {
        if (this.texture != null) {
            int i = 32;
            long[] along = this.texture.cells;
            int j = this.texture.width;
            int k = this.texture.height;

            for (int l = -32; l <= 32; l++) {
                for (int i1 = -32; i1 <= 32; i1++) {
                    int j1 = Math.floorMod(pCellX + i1, j);
                    int k1 = Math.floorMod(pCellZ + l, k);
                    long l1 = along[j1 + k1 * j];
                    if (l1 != 0L) {
                        int i2 = getColor(l1);
                        if (pFancyClouds) {
                            this.buildExtrudedCell(
                                pRelativeCameraPos,
                                pBufferBuilder,
                                ARGB.multiply(pBottomColor, i2),
                                ARGB.multiply(pTopColor, i2),
                                ARGB.multiply(pSideColor, i2),
                                ARGB.multiply(pFrontColor, i2),
                                i1,
                                l,
                                l1
                            );
                        } else {
                            this.buildFlatCell(pBufferBuilder, ARGB.multiply(pTopColor, i2), i1, l);
                        }
                    }
                }
            }
        }
    }

    private void buildFlatCell(BufferBuilder pBufferBuilder, int pColor, int pX, int pY) {
        float f = (float)pX * 12.0F;
        float f1 = f + 12.0F;
        float f2 = (float)pY * 12.0F;
        float f3 = f2 + 12.0F;
        pBufferBuilder.addVertex(f, 0.0F, f2).setColor(pColor);
        pBufferBuilder.addVertex(f, 0.0F, f3).setColor(pColor);
        pBufferBuilder.addVertex(f1, 0.0F, f3).setColor(pColor);
        pBufferBuilder.addVertex(f1, 0.0F, f2).setColor(pColor);
    }

    private void buildExtrudedCell(
        CloudRenderer.RelativeCameraPos pRelativeCameraPos,
        BufferBuilder pBufferBuilder,
        int pBottomColor,
        int pTopColor,
        int pSideColor,
        int pFrontColor,
        int pX,
        int pY,
        long pCellData
    ) {
        float f = (float)pX * 12.0F;
        float f1 = f + 12.0F;
        float f2 = 0.0F;
        float f3 = 4.0F;
        float f4 = (float)pY * 12.0F;
        float f5 = f4 + 12.0F;
        if (pRelativeCameraPos != CloudRenderer.RelativeCameraPos.BELOW_CLOUDS) {
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pTopColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pTopColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pTopColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pTopColor);
        }

        if (pRelativeCameraPos != CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS) {
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pBottomColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pBottomColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pBottomColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pBottomColor);
        }

        if (isNorthEmpty(pCellData) && pY > 0) {
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pFrontColor);
        }

        if (isSouthEmpty(pCellData) && pY < 0) {
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pFrontColor);
        }

        if (isWestEmpty(pCellData) && pX > 0) {
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pSideColor);
        }

        if (isEastEmpty(pCellData) && pX < 0) {
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pSideColor);
        }

        boolean flag = Math.abs(pX) <= 1 && Math.abs(pY) <= 1;
        if (flag) {
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pTopColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pTopColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pTopColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pTopColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pBottomColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pBottomColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pBottomColor);
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pBottomColor);
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pSideColor);
        }
    }

    public void markForRebuild() {
        this.needsRebuild = true;
    }

    @Override
    public void close() {
        this.vertexBuffer.close();
    }

    @OnlyIn(Dist.CLIENT)
    static enum RelativeCameraPos {
        ABOVE_CLOUDS,
        INSIDE_CLOUDS,
        BELOW_CLOUDS;
    }

    @OnlyIn(Dist.CLIENT)
    public static record TextureData(long[] cells, int width, int height) {
    }
}
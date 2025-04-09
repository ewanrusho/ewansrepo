package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class VertexBuffer implements AutoCloseable {
    private final BufferUsage usage;
    private final GpuBuffer vertexBuffer;
    @Nullable
    private GpuBuffer indexBuffer = null;
    private int arrayObjectId;
    @Nullable
    private VertexFormat format;
    @Nullable
    private RenderSystem.AutoStorageIndexBuffer sequentialIndices;
    private VertexFormat.IndexType indexType;
    private int indexCount;
    private VertexFormat.Mode mode;

    public VertexBuffer(BufferUsage pUsage) {
        this.usage = pUsage;
        RenderSystem.assertOnRenderThread();
        this.vertexBuffer = new GpuBuffer(BufferType.VERTICES, pUsage, 0);
        this.arrayObjectId = GlStateManager._glGenVertexArrays();
    }

    public static VertexBuffer uploadStatic(VertexFormat.Mode pMode, VertexFormat pFormat, Consumer<VertexConsumer> pBuilder) {
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(pMode, pFormat);
        pBuilder.accept(bufferbuilder);
        VertexBuffer vertexbuffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
        vertexbuffer.bind();
        vertexbuffer.upload(bufferbuilder.buildOrThrow());
        unbind();
        return vertexbuffer;
    }

    public void upload(MeshData pMeshData) {
        MeshData meshdata = pMeshData;

        label40: {
            try {
                if (this.isInvalid()) {
                    break label40;
                }

                RenderSystem.assertOnRenderThread();
                MeshData.DrawState meshdata$drawstate = pMeshData.drawState();
                this.format = this.uploadVertexBuffer(meshdata$drawstate, pMeshData.vertexBuffer());
                this.sequentialIndices = this.uploadIndexBuffer(meshdata$drawstate, pMeshData.indexBuffer());
                this.indexCount = meshdata$drawstate.indexCount();
                this.indexType = meshdata$drawstate.indexType();
                this.mode = meshdata$drawstate.mode();
            } catch (Throwable throwable1) {
                if (pMeshData != null) {
                    try {
                        meshdata.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }
                }

                throw throwable1;
            }

            if (pMeshData != null) {
                pMeshData.close();
            }

            return;
        }

        if (pMeshData != null) {
            pMeshData.close();
        }
    }

    public void uploadIndexBuffer(ByteBufferBuilder.Result pResult) {
        ByteBufferBuilder.Result bytebufferbuilder$result = pResult;

        label46: {
            try {
                if (this.isInvalid()) {
                    break label46;
                }

                RenderSystem.assertOnRenderThread();
                if (this.indexBuffer != null) {
                    this.indexBuffer.close();
                }

                this.indexBuffer = new GpuBuffer(BufferType.INDICES, this.usage, pResult.byteBuffer());
                this.sequentialIndices = null;
            } catch (Throwable throwable1) {
                if (pResult != null) {
                    try {
                        bytebufferbuilder$result.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }
                }

                throw throwable1;
            }

            if (pResult != null) {
                pResult.close();
            }

            return;
        }

        if (pResult != null) {
            pResult.close();
        }
    }

    private VertexFormat uploadVertexBuffer(MeshData.DrawState pDrawState, @Nullable ByteBuffer pBuffer) {
        boolean flag = false;
        if (!pDrawState.format().equals(this.format)) {
            if (this.format != null) {
                this.format.clearBufferState();
            }

            this.vertexBuffer.bind();
            pDrawState.format().setupBufferState();
            flag = true;
        }

        if (pBuffer != null) {
            if (!flag) {
                this.vertexBuffer.bind();
            }

            this.vertexBuffer.resize(pBuffer.remaining());
            this.vertexBuffer.write(pBuffer, 0);
        }

        return pDrawState.format();
    }

    @Nullable
    private RenderSystem.AutoStorageIndexBuffer uploadIndexBuffer(MeshData.DrawState pDrawState, @Nullable ByteBuffer pBuffer) {
        if (pBuffer != null) {
            if (this.indexBuffer != null) {
                this.indexBuffer.close();
            }

            this.indexBuffer = new GpuBuffer(BufferType.INDICES, this.usage, pBuffer);
            return null;
        } else {
            RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(pDrawState.mode());
            if (rendersystem$autostorageindexbuffer != this.sequentialIndices || !rendersystem$autostorageindexbuffer.hasStorage(pDrawState.indexCount())) {
                rendersystem$autostorageindexbuffer.bind(pDrawState.indexCount());
            }

            return rendersystem$autostorageindexbuffer;
        }
    }

    public void bind() {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(this.arrayObjectId);
    }

    public static void unbind() {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(0);
    }

    public void draw() {
        RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.getIndexType().asGLType);
    }

    private VertexFormat.IndexType getIndexType() {
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = this.sequentialIndices;
        return rendersystem$autostorageindexbuffer != null ? rendersystem$autostorageindexbuffer.type() : this.indexType;
    }

    public void drawWithShader(Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix, @Nullable CompiledShaderProgram pShader) {
        if (pShader != null) {
            RenderSystem.assertOnRenderThread();
            pShader.setDefaultUniforms(this.mode, pFrustumMatrix, pProjectionMatrix, Minecraft.getInstance().getWindow());
            pShader.apply();
            this.draw();
            pShader.clear();
        }
    }

    public void drawWithRenderType(RenderType pRenderType) {
        pRenderType.setupRenderState();
        this.bind();
        this.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        unbind();
        pRenderType.clearRenderState();
    }

    @Override
    public void close() {
        this.vertexBuffer.close();
        if (this.indexBuffer != null) {
            this.indexBuffer.close();
            this.indexBuffer = null;
        }

        if (this.arrayObjectId >= 0) {
            RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
            this.arrayObjectId = -1;
        }
    }

    public VertexFormat getFormat() {
        return this.format;
    }

    public boolean isInvalid() {
        return this.arrayObjectId == -1;
    }
}
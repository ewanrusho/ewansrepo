package com.mojang.blaze3d.systems;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class RenderSystem {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final ConcurrentLinkedQueue<RenderCall> recordingQueue = Queues.newConcurrentLinkedQueue();
    private static final Tesselator RENDER_THREAD_TESSELATOR = new Tesselator(1536);
    private static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
    @Nullable
    private static Thread renderThread;
    private static int MAX_SUPPORTED_TEXTURE_SIZE = -1;
    private static boolean isInInit;
    private static double lastDrawTime = Double.MIN_VALUE;
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequential = new RenderSystem.AutoStorageIndexBuffer(1, 1, IntConsumer::accept);
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialQuad = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_157398_, p_157399_) -> {
        p_157398_.accept(p_157399_ + 0);
        p_157398_.accept(p_157399_ + 1);
        p_157398_.accept(p_157399_ + 2);
        p_157398_.accept(p_157399_ + 2);
        p_157398_.accept(p_157399_ + 3);
        p_157398_.accept(p_157399_ + 0);
    });
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialLines = new RenderSystem.AutoStorageIndexBuffer(4, 6, (p_157401_, p_157402_) -> {
        p_157401_.accept(p_157402_ + 0);
        p_157401_.accept(p_157402_ + 1);
        p_157401_.accept(p_157402_ + 2);
        p_157401_.accept(p_157402_ + 3);
        p_157401_.accept(p_157402_ + 2);
        p_157401_.accept(p_157402_ + 1);
    });
    private static Matrix4f projectionMatrix = new Matrix4f();
    private static Matrix4f savedProjectionMatrix = new Matrix4f();
    private static ProjectionType projectionType = ProjectionType.PERSPECTIVE;
    private static ProjectionType savedProjectionType = ProjectionType.PERSPECTIVE;
    private static final Matrix4fStack modelViewStack = new Matrix4fStack(16);
    private static Matrix4f textureMatrix = new Matrix4f();
    private static final int[] shaderTextures = new int[12];
    private static final float[] shaderColor = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    private static float shaderGlintAlpha = 1.0F;
    private static FogParameters shaderFog = FogParameters.NO_FOG;
    private static final Vector3f[] shaderLightDirections = new Vector3f[2];
    private static float shaderGameTime;
    private static float shaderLineWidth = 1.0F;
    private static String apiDescription = "Unknown";
    @Nullable
    private static CompiledShaderProgram shader;
    private static final AtomicLong pollEventsWaitStart = new AtomicLong();
    private static final AtomicBoolean pollingEvents = new AtomicBoolean(false);

    public static void initRenderThread() {
        if (renderThread != null) {
            throw new IllegalStateException("Could not initialize render thread");
        } else {
            renderThread = Thread.currentThread();
        }
    }

    public static boolean isOnRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    public static boolean isOnRenderThreadOrInit() {
        return isInInit || isOnRenderThread();
    }

    public static void assertOnRenderThreadOrInit() {
        if (!isInInit && !isOnRenderThread()) {
            throw constructThreadException();
        }
    }

    public static void assertOnRenderThread() {
        if (!isOnRenderThread()) {
            throw constructThreadException();
        }
    }

    private static IllegalStateException constructThreadException() {
        return new IllegalStateException("Rendersystem called from wrong thread");
    }

    public static void recordRenderCall(RenderCall pRenderCall) {
        recordingQueue.add(pRenderCall);
    }

    private static void pollEvents() {
        pollEventsWaitStart.set(Util.getMillis());
        pollingEvents.set(true);
        GLFW.glfwPollEvents();
        pollingEvents.set(false);
    }

    public static boolean isFrozenAtPollEvents() {
        return pollingEvents.get() && Util.getMillis() - pollEventsWaitStart.get() > 200L;
    }

    public static void flipFrame(long pWindow, @Nullable TracyFrameCapture pTracyFrameCapture) {
        pollEvents();
        replayQueue();
        Tesselator.getInstance().clear();
        GLFW.glfwSwapBuffers(pWindow);
        if (pTracyFrameCapture != null) {
            pTracyFrameCapture.endFrame();
        }

        pollEvents();
    }

    public static void replayQueue() {
        while (!recordingQueue.isEmpty()) {
            RenderCall rendercall = recordingQueue.poll();
            rendercall.execute();
        }
    }

    public static void limitDisplayFPS(int pFrameRateLimit) {
        double d0 = lastDrawTime + 1.0 / (double)pFrameRateLimit;

        double d1;
        for (d1 = GLFW.glfwGetTime(); d1 < d0; d1 = GLFW.glfwGetTime()) {
            GLFW.glfwWaitEventsTimeout(d0 - d1);
        }

        lastDrawTime = d1;
    }

    public static void disableDepthTest() {
        assertOnRenderThread();
        GlStateManager._disableDepthTest();
    }

    public static void enableDepthTest() {
        GlStateManager._enableDepthTest();
    }

    public static void enableScissor(int pX, int pY, int pWidth, int pHeight) {
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(pX, pY, pWidth, pHeight);
    }

    public static void disableScissor() {
        GlStateManager._disableScissorTest();
    }

    public static void depthFunc(int pDepthFunc) {
        assertOnRenderThread();
        GlStateManager._depthFunc(pDepthFunc);
    }

    public static void depthMask(boolean pFlag) {
        assertOnRenderThread();
        GlStateManager._depthMask(pFlag);
    }

    public static boolean isBlendEnabled() {
        assertOnRenderThread();
        return GlStateManager._isBlendEnabled();
    }

    public static void enableBlend() {
        assertOnRenderThread();
        GlStateManager._enableBlend();
    }

    public static void disableBlend() {
        assertOnRenderThread();
        GlStateManager._disableBlend();
    }

    public static void blendFunc(GlStateManager.SourceFactor pSourceFactor, GlStateManager.DestFactor pDestFactor) {
        assertOnRenderThread();
        GlStateManager._blendFunc(pSourceFactor.value, pDestFactor.value);
    }

    public static void blendFunc(int pSourceFactor, int pDestFactor) {
        assertOnRenderThread();
        GlStateManager._blendFunc(pSourceFactor, pDestFactor);
    }

    public static void blendFuncSeparate(
        GlStateManager.SourceFactor pSourceFactor, GlStateManager.DestFactor pDestFactor, GlStateManager.SourceFactor pSourceFactorAlpha, GlStateManager.DestFactor pDestFactorAlpha
    ) {
        assertOnRenderThread();
        GlStateManager._blendFuncSeparate(pSourceFactor.value, pDestFactor.value, pSourceFactorAlpha.value, pDestFactorAlpha.value);
    }

    public static void blendFuncSeparate(int pSourceFactor, int pDestFactor, int pSourceFactorAlpha, int pDestFactorAlpha) {
        assertOnRenderThread();
        GlStateManager._blendFuncSeparate(pSourceFactor, pDestFactor, pSourceFactorAlpha, pDestFactorAlpha);
    }

    public static void blendEquation(int pMode) {
        assertOnRenderThread();
        GlStateManager._blendEquation(pMode);
    }

    public static void enableCull() {
        assertOnRenderThread();
        GlStateManager._enableCull();
    }

    public static void disableCull() {
        assertOnRenderThread();
        GlStateManager._disableCull();
    }

    public static void polygonMode(int pFace, int pMode) {
        assertOnRenderThread();
        GlStateManager._polygonMode(pFace, pMode);
    }

    public static void enablePolygonOffset() {
        assertOnRenderThread();
        GlStateManager._enablePolygonOffset();
    }

    public static void disablePolygonOffset() {
        assertOnRenderThread();
        GlStateManager._disablePolygonOffset();
    }

    public static void polygonOffset(float pFactor, float pUnits) {
        assertOnRenderThread();
        GlStateManager._polygonOffset(pFactor, pUnits);
    }

    public static void enableColorLogicOp() {
        assertOnRenderThread();
        GlStateManager._enableColorLogicOp();
    }

    public static void disableColorLogicOp() {
        assertOnRenderThread();
        GlStateManager._disableColorLogicOp();
    }

    public static void logicOp(GlStateManager.LogicOp pOp) {
        assertOnRenderThread();
        GlStateManager._logicOp(pOp.value);
    }

    public static void activeTexture(int pTexture) {
        assertOnRenderThread();
        GlStateManager._activeTexture(pTexture);
    }

    public static void texParameter(int pTarget, int pParameterName, int pParameter) {
        GlStateManager._texParameter(pTarget, pParameterName, pParameter);
    }

    public static void deleteTexture(int pTexture) {
        GlStateManager._deleteTexture(pTexture);
    }

    public static void bindTextureForSetup(int pTexture) {
        bindTexture(pTexture);
    }

    public static void bindTexture(int pTexture) {
        GlStateManager._bindTexture(pTexture);
    }

    public static void viewport(int pX, int pY, int pWidth, int pHeight) {
        GlStateManager._viewport(pX, pY, pWidth, pHeight);
    }

    public static void colorMask(boolean pRed, boolean pGreen, boolean pBlue, boolean pAlpha) {
        assertOnRenderThread();
        GlStateManager._colorMask(pRed, pGreen, pBlue, pAlpha);
    }

    public static void stencilFunc(int pFunc, int pRef, int pMask) {
        assertOnRenderThread();
        GlStateManager._stencilFunc(pFunc, pRef, pMask);
    }

    public static void stencilMask(int pMask) {
        assertOnRenderThread();
        GlStateManager._stencilMask(pMask);
    }

    public static void stencilOp(int pSFail, int pDpFail, int pDpPass) {
        assertOnRenderThread();
        GlStateManager._stencilOp(pSFail, pDpFail, pDpPass);
    }

    public static void clearDepth(double pDepth) {
        GlStateManager._clearDepth(pDepth);
    }

    public static void clearColor(float pRed, float pGreen, float pBlue, float pAlpha) {
        GlStateManager._clearColor(pRed, pGreen, pBlue, pAlpha);
    }

    public static void clearStencil(int pIndex) {
        assertOnRenderThread();
        GlStateManager._clearStencil(pIndex);
    }

    public static void clear(int pMask) {
        GlStateManager._clear(pMask);
    }

    public static void setShaderFog(FogParameters pShaderFog) {
        assertOnRenderThread();
        shaderFog = pShaderFog;
    }

    public static FogParameters getShaderFog() {
        assertOnRenderThread();
        return shaderFog;
    }

    public static void setShaderGlintAlpha(double pShaderGlintAlpha) {
        setShaderGlintAlpha((float)pShaderGlintAlpha);
    }

    public static void setShaderGlintAlpha(float pShaderGlintAlpha) {
        assertOnRenderThread();
        shaderGlintAlpha = pShaderGlintAlpha;
    }

    public static float getShaderGlintAlpha() {
        assertOnRenderThread();
        return shaderGlintAlpha;
    }

    public static void setShaderLights(Vector3f pLightingVector0, Vector3f pLightingVector1) {
        assertOnRenderThread();
        shaderLightDirections[0] = pLightingVector0;
        shaderLightDirections[1] = pLightingVector1;
    }

    public static void setupShaderLights(CompiledShaderProgram pShader) {
        assertOnRenderThread();
        if (pShader.LIGHT0_DIRECTION != null) {
            pShader.LIGHT0_DIRECTION.set(shaderLightDirections[0]);
        }

        if (pShader.LIGHT1_DIRECTION != null) {
            pShader.LIGHT1_DIRECTION.set(shaderLightDirections[1]);
        }
    }

    public static void setShaderColor(float pRed, float pGreen, float pBlue, float pAlpha) {
        assertOnRenderThread();
        shaderColor[0] = pRed;
        shaderColor[1] = pGreen;
        shaderColor[2] = pBlue;
        shaderColor[3] = pAlpha;
    }

    public static float[] getShaderColor() {
        assertOnRenderThread();
        return shaderColor;
    }

    public static void drawElements(int pMode, int pCount, int pType) {
        assertOnRenderThread();
        GlStateManager._drawElements(pMode, pCount, pType, 0L);
    }

    public static void lineWidth(float pShaderLineWidth) {
        assertOnRenderThread();
        shaderLineWidth = pShaderLineWidth;
    }

    public static float getShaderLineWidth() {
        assertOnRenderThread();
        return shaderLineWidth;
    }

    public static void pixelStore(int pParameterName, int pParameter) {
        GlStateManager._pixelStore(pParameterName, pParameter);
    }

    public static void readPixels(int pX, int pY, int pWidth, int pHeight, int pFormat, int pType, ByteBuffer pPixels) {
        assertOnRenderThread();
        GlStateManager._readPixels(pX, pY, pWidth, pHeight, pFormat, pType, pPixels);
    }

    public static void getString(int pName, Consumer<String> pConsumer) {
        assertOnRenderThread();
        pConsumer.accept(GlStateManager._getString(pName));
    }

    public static String getBackendDescription() {
        return String.format(Locale.ROOT, "LWJGL version %s", GLX._getLWJGLVersion());
    }

    public static String getApiDescription() {
        return apiDescription;
    }

    public static TimeSource.NanoTimeSource initBackendSystem() {
        return GLX._initGlfw()::getAsLong;
    }

    public static void initRenderer(int pDebugVerbosity, boolean pSynchronous) {
        GLX._init(pDebugVerbosity, pSynchronous);
        apiDescription = GLX.getOpenGLVersionString();
    }

    public static void setErrorCallback(GLFWErrorCallbackI pCallback) {
        GLX._setGlfwErrorCallback(pCallback);
    }

    public static void renderCrosshair(int pLineLength) {
        assertOnRenderThread();
        GLX._renderCrosshair(pLineLength, true, true, true);
    }

    public static String getCapsString() {
        assertOnRenderThread();
        return "Using framebuffer using OpenGL 3.2";
    }

    public static void setupDefaultState(int pX, int pY, int pWidth, int pHeight) {
        GlStateManager._clearDepth(1.0);
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(515);
        projectionMatrix.identity();
        savedProjectionMatrix.identity();
        modelViewStack.clear();
        textureMatrix.identity();
        GlStateManager._viewport(pX, pY, pWidth, pHeight);
    }

    public static int maxSupportedTextureSize() {
        if (MAX_SUPPORTED_TEXTURE_SIZE == -1) {
            assertOnRenderThreadOrInit();
            int i = GlStateManager._getInteger(3379);

            for (int j = Math.max(32768, i); j >= 1024; j >>= 1) {
                GlStateManager._texImage2D(32868, 0, 6408, j, j, 0, 6408, 5121, null);
                int k = GlStateManager._getTexLevelParameter(32868, 0, 4096);
                if (k != 0) {
                    MAX_SUPPORTED_TEXTURE_SIZE = j;
                    return j;
                }
            }

            MAX_SUPPORTED_TEXTURE_SIZE = Math.max(i, 1024);
            LOGGER.info("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = {}", MAX_SUPPORTED_TEXTURE_SIZE);
        }

        return MAX_SUPPORTED_TEXTURE_SIZE;
    }

    public static void glBindBuffer(int pTarget, int pBuffer) {
        GlStateManager._glBindBuffer(pTarget, pBuffer);
    }

    public static void glBindVertexArray(int pArray) {
        GlStateManager._glBindVertexArray(pArray);
    }

    public static void glBufferData(int pTarget, ByteBuffer pData, int pUsage) {
        assertOnRenderThreadOrInit();
        GlStateManager._glBufferData(pTarget, pData, pUsage);
    }

    public static void glDeleteBuffers(int pBuffer) {
        assertOnRenderThread();
        GlStateManager._glDeleteBuffers(pBuffer);
    }

    public static void glDeleteVertexArrays(int pArray) {
        assertOnRenderThread();
        GlStateManager._glDeleteVertexArrays(pArray);
    }

    public static void glUniform1i(int pLocation, int pValue) {
        assertOnRenderThread();
        GlStateManager._glUniform1i(pLocation, pValue);
    }

    public static void glUniform1(int pLocation, IntBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniform1(pLocation, pValue);
    }

    public static void glUniform2(int pLocation, IntBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniform2(pLocation, pValue);
    }

    public static void glUniform3(int pLocation, IntBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniform3(pLocation, pValue);
    }

    public static void glUniform4(int pLocation, IntBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniform4(pLocation, pValue);
    }

    public static void glUniform1(int pLocation, FloatBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniform1(pLocation, pValue);
    }

    public static void glUniform2(int pLocation, FloatBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniform2(pLocation, pValue);
    }

    public static void glUniform3(int pLocation, FloatBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniform3(pLocation, pValue);
    }

    public static void glUniform4(int pLocation, FloatBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniform4(pLocation, pValue);
    }

    public static void glUniformMatrix2(int pLocation, boolean pTranspose, FloatBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniformMatrix2(pLocation, pTranspose, pValue);
    }

    public static void glUniformMatrix3(int pLocation, boolean pTranspose, FloatBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniformMatrix3(pLocation, pTranspose, pValue);
    }

    public static void glUniformMatrix4(int pLocation, boolean pTranspose, FloatBuffer pValue) {
        assertOnRenderThread();
        GlStateManager._glUniformMatrix4(pLocation, pTranspose, pValue);
    }

    public static void setupOverlayColor(int pTextureId, int pColor) {
        assertOnRenderThread();
        setShaderTexture(1, pTextureId);
    }

    public static void teardownOverlayColor() {
        assertOnRenderThread();
        setShaderTexture(1, 0);
    }

    public static void setupLevelDiffuseLighting(Vector3f pLightingVector0, Vector3f pLightingVector1) {
        assertOnRenderThread();
        setShaderLights(pLightingVector0, pLightingVector1);
    }

    public static void setupGuiFlatDiffuseLighting(Vector3f pLightingVector1, Vector3f pLightingVector2) {
        assertOnRenderThread();
        GlStateManager.setupGuiFlatDiffuseLighting(pLightingVector1, pLightingVector2);
    }

    public static void setupGui3DDiffuseLighting(Vector3f pLightingVector1, Vector3f pLightingVector2) {
        assertOnRenderThread();
        GlStateManager.setupGui3DDiffuseLighting(pLightingVector1, pLightingVector2);
    }

    public static void beginInitialization() {
        isInInit = true;
    }

    public static void finishInitialization() {
        isInInit = false;
        if (!recordingQueue.isEmpty()) {
            replayQueue();
        }

        if (!recordingQueue.isEmpty()) {
            throw new IllegalStateException("Recorded to render queue during initialization");
        }
    }

    public static Tesselator renderThreadTesselator() {
        assertOnRenderThread();
        return RENDER_THREAD_TESSELATOR;
    }

    public static void defaultBlendFunc() {
        blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
    }

    @Nullable
    public static CompiledShaderProgram setShader(ShaderProgram pShader) {
        assertOnRenderThread();
        CompiledShaderProgram compiledshaderprogram = Minecraft.getInstance().getShaderManager().getProgram(pShader);
        shader = compiledshaderprogram;
        return compiledshaderprogram;
    }

    public static void setShader(CompiledShaderProgram pShader) {
        assertOnRenderThread();
        shader = pShader;
    }

    public static void clearShader() {
        assertOnRenderThread();
        shader = null;
    }

    @Nullable
    public static CompiledShaderProgram getShader() {
        assertOnRenderThread();
        return shader;
    }

    public static void setShaderTexture(int pShaderTexture, ResourceLocation pTextureId) {
        assertOnRenderThread();
        if (pShaderTexture >= 0 && pShaderTexture < shaderTextures.length) {
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstracttexture = texturemanager.getTexture(pTextureId);
            shaderTextures[pShaderTexture] = abstracttexture.getId();
        }
    }

    public static void setShaderTexture(int pShaderTexture, int pTextureId) {
        assertOnRenderThread();
        if (pShaderTexture >= 0 && pShaderTexture < shaderTextures.length) {
            shaderTextures[pShaderTexture] = pTextureId;
        }
    }

    public static int getShaderTexture(int pShaderTexture) {
        assertOnRenderThread();
        return pShaderTexture >= 0 && pShaderTexture < shaderTextures.length ? shaderTextures[pShaderTexture] : 0;
    }

    public static void setProjectionMatrix(Matrix4f pProjectionMatrix, ProjectionType pProjectionType) {
        assertOnRenderThread();
        projectionMatrix = new Matrix4f(pProjectionMatrix);
        projectionType = pProjectionType;
    }

    public static void setTextureMatrix(Matrix4f pTextureMatrix) {
        assertOnRenderThread();
        textureMatrix = new Matrix4f(pTextureMatrix);
    }

    public static void resetTextureMatrix() {
        assertOnRenderThread();
        textureMatrix.identity();
    }

    public static void backupProjectionMatrix() {
        assertOnRenderThread();
        savedProjectionMatrix = projectionMatrix;
        savedProjectionType = projectionType;
    }

    public static void restoreProjectionMatrix() {
        assertOnRenderThread();
        projectionMatrix = savedProjectionMatrix;
        projectionType = savedProjectionType;
    }

    public static Matrix4f getProjectionMatrix() {
        assertOnRenderThread();
        return projectionMatrix;
    }

    public static Matrix4f getModelViewMatrix() {
        assertOnRenderThread();
        return modelViewStack;
    }

    public static Matrix4fStack getModelViewStack() {
        assertOnRenderThread();
        return modelViewStack;
    }

    public static Matrix4f getTextureMatrix() {
        assertOnRenderThread();
        return textureMatrix;
    }

    public static RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(VertexFormat.Mode pFormatMode) {
        assertOnRenderThread();

        return switch (pFormatMode) {
            case QUADS -> sharedSequentialQuad;
            case LINES -> sharedSequentialLines;
            default -> sharedSequential;
        };
    }

    public static void setShaderGameTime(long pTickTime, float pPartialTicks) {
        assertOnRenderThread();
        shaderGameTime = ((float)(pTickTime % 24000L) + pPartialTicks) / 24000.0F;
    }

    public static float getShaderGameTime() {
        assertOnRenderThread();
        return shaderGameTime;
    }

    public static ProjectionType getProjectionType() {
        assertOnRenderThread();
        return projectionType;
    }

    @OnlyIn(Dist.CLIENT)
    public static final class AutoStorageIndexBuffer {
        private final int vertexStride;
        private final int indexStride;
        private final RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator;
        @Nullable
        private GpuBuffer buffer;
        private VertexFormat.IndexType type = VertexFormat.IndexType.SHORT;
        private int indexCount;

        AutoStorageIndexBuffer(int pVertexStride, int pIndexStride, RenderSystem.AutoStorageIndexBuffer.IndexGenerator pGenerator) {
            this.vertexStride = pVertexStride;
            this.indexStride = pIndexStride;
            this.generator = pGenerator;
        }

        public boolean hasStorage(int pIndex) {
            return pIndex <= this.indexCount;
        }

        public void bind(int pIndex) {
            if (this.buffer == null) {
                this.buffer = new GpuBuffer(BufferType.INDICES, BufferUsage.DYNAMIC_WRITE, 0);
            }

            this.buffer.bind();
            this.ensureStorage(pIndex);
        }

        private void ensureStorage(int pNeededIndexCount) {
            if (!this.hasStorage(pNeededIndexCount)) {
                pNeededIndexCount = Mth.roundToward(pNeededIndexCount * 2, this.indexStride);
                RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", this.indexCount, pNeededIndexCount);
                int i = pNeededIndexCount / this.indexStride;
                int j = i * this.vertexStride;
                VertexFormat.IndexType vertexformat$indextype = VertexFormat.IndexType.least(j);
                int k = Mth.roundToward(pNeededIndexCount * vertexformat$indextype.bytes, 4);
                ByteBuffer bytebuffer = MemoryUtil.memAlloc(k);

                try {
                    this.type = vertexformat$indextype;
                    it.unimi.dsi.fastutil.ints.IntConsumer intconsumer = this.intConsumer(bytebuffer);

                    for (int l = 0; l < pNeededIndexCount; l += this.indexStride) {
                        this.generator.accept(intconsumer, l * this.vertexStride / this.indexStride);
                    }

                    bytebuffer.flip();
                    this.buffer.resize(k);
                    this.buffer.write(bytebuffer, 0);
                } finally {
                    MemoryUtil.memFree(bytebuffer);
                }

                this.indexCount = pNeededIndexCount;
            }
        }

        private it.unimi.dsi.fastutil.ints.IntConsumer intConsumer(ByteBuffer pBuffer) {
            switch (this.type) {
                case SHORT:
                    return p_157482_ -> pBuffer.putShort((short)p_157482_);
                case INT:
                default:
                    return pBuffer::putInt;
            }
        }

        public VertexFormat.IndexType type() {
            return this.type;
        }

        @OnlyIn(Dist.CLIENT)
        interface IndexGenerator {
            void accept(it.unimi.dsi.fastutil.ints.IntConsumer pConsumer, int pIndex);
        }
    }
}

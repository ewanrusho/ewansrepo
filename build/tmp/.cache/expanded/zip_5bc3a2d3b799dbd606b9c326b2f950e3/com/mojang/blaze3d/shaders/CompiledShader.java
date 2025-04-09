package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class CompiledShader implements AutoCloseable {
    private static final int NOT_ALLOCATED = -1;
    private final ResourceLocation id;
    private int shaderId;

    private CompiledShader(int pShaderId, ResourceLocation pId) {
        this.id = pId;
        this.shaderId = pShaderId;
    }

    public static CompiledShader compile(ResourceLocation pShaderId, CompiledShader.Type pType, String pSource) throws ShaderManager.CompilationException {
        RenderSystem.assertOnRenderThread();
        int i = GlStateManager.glCreateShader(pType.glType());
        GlStateManager.glShaderSource(i, pSource);
        GlStateManager.glCompileShader(i);
        if (GlStateManager.glGetShaderi(i, 35713) == 0) {
            String s = StringUtils.trim(GlStateManager.glGetShaderInfoLog(i, 32768));
            throw new ShaderManager.CompilationException("Couldn't compile " + pType.getName() + " shader (" + pShaderId + ") : " + s);
        } else {
            return new CompiledShader(i, pShaderId);
        }
    }

    @Override
    public void close() {
        if (this.shaderId == -1) {
            throw new IllegalStateException("Already closed");
        } else {
            RenderSystem.assertOnRenderThread();
            GlStateManager.glDeleteShader(this.shaderId);
            this.shaderId = -1;
        }
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public int getShaderId() {
        return this.shaderId;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        VERTEX("vertex", ".vsh", 35633),
        FRAGMENT("fragment", ".fsh", 35632);

        private static final CompiledShader.Type[] TYPES = values();
        private final String name;
        private final String extension;
        private final int glType;

        private Type(final String pName, final String pExtension, final int pGlType) {
            this.name = pName;
            this.extension = pExtension;
            this.glType = pGlType;
        }

        @Nullable
        public static CompiledShader.Type byLocation(ResourceLocation pLocation) {
            for (CompiledShader.Type compiledshader$type : TYPES) {
                if (pLocation.getPath().endsWith(compiledshader$type.extension)) {
                    return compiledshader$type;
                }
            }

            return null;
        }

        public String getName() {
            return this.name;
        }

        public int glType() {
            return this.glType;
        }

        public FileToIdConverter idConverter() {
            return new FileToIdConverter("shaders", this.extension);
        }
    }
}
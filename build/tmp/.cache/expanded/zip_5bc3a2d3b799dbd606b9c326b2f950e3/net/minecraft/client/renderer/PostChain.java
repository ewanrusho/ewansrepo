package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PostChain {
    public static final ResourceLocation MAIN_TARGET_ID = ResourceLocation.withDefaultNamespace("main");
    private final List<PostPass> passes;
    private final Map<ResourceLocation, PostChainConfig.InternalTarget> internalTargets;
    private final Set<ResourceLocation> externalTargets;

    private PostChain(List<PostPass> pPasses, Map<ResourceLocation, PostChainConfig.InternalTarget> pInternalTargets, Set<ResourceLocation> pExternalTargets) {
        this.passes = pPasses;
        this.internalTargets = pInternalTargets;
        this.externalTargets = pExternalTargets;
    }

    public static PostChain load(PostChainConfig pConfig, TextureManager pTextureManager, ShaderManager pShaderManager, Set<ResourceLocation> pExternalTargets) throws ShaderManager.CompilationException {
        Stream<ResourceLocation> stream = pConfig.passes()
            .stream()
            .flatMap(p_357873_ -> p_357873_.inputs().stream())
            .flatMap(p_357872_ -> p_357872_.referencedTargets().stream());
        Set<ResourceLocation> set = stream.filter(p_357871_ -> !pConfig.internalTargets().containsKey(p_357871_)).collect(Collectors.toSet());
        Set<ResourceLocation> set1 = Sets.difference(set, pExternalTargets);
        if (!set1.isEmpty()) {
            throw new ShaderManager.CompilationException("Referenced external targets are not available in this context: " + set1);
        } else {
            Builder<PostPass> builder = ImmutableList.builder();

            for (PostChainConfig.Pass postchainconfig$pass : pConfig.passes()) {
                builder.add(createPass(pTextureManager, pShaderManager, postchainconfig$pass));
            }

            return new PostChain(builder.build(), pConfig.internalTargets(), set);
        }
    }

    private static PostPass createPass(TextureManager pTextureManager, ShaderManager pShaderManager, PostChainConfig.Pass pPass) throws ShaderManager.CompilationException {
        CompiledShaderProgram compiledshaderprogram = pShaderManager.getProgramForLoading(pPass.program());

        for (PostChainConfig.Uniform postchainconfig$uniform : pPass.uniforms()) {
            String s = postchainconfig$uniform.name();
            if (compiledshaderprogram.getUniform(s) == null) {
                throw new ShaderManager.CompilationException("Uniform '" + s + "' does not exist for " + pPass.programId());
            }
        }

        String s2 = pPass.programId().toString();
        PostPass postpass = new PostPass(s2, compiledshaderprogram, pPass.outputTarget(), pPass.uniforms());

        for (PostChainConfig.Input postchainconfig$input : pPass.inputs()) {
            switch (postchainconfig$input) {
                case PostChainConfig.TextureInput texutre:
                    try {
                    String s3 = texutre.samplerName();
                    ResourceLocation resourcelocation = texutre.location();
                    int i = texutre.width();
                    int j = texutre.height();
                    boolean flag = texutre.bilinear();
                    AbstractTexture abstracttexture = pTextureManager.getTexture(resourcelocation.withPath(p_357869_ -> "textures/effect/" + p_357869_ + ".png"));
                    abstracttexture.setFilter(flag, false);
                    postpass.addInput(new PostPass.TextureInput(s3, abstracttexture, i, j));
                    continue;
                    } catch (Throwable t) {
                        throw new MatchException(t.toString(), t);
                    }
                case PostChainConfig.TargetInput target:
                    try {
                    String s1 = target.samplerName();
                    ResourceLocation resourcelocation1 = target.targetId();
                    boolean flag1 = target.useDepthBuffer();
                    boolean flag2 = target.bilinear();
                    postpass.addInput(new PostPass.TargetInput(s1, resourcelocation1, flag1, flag2));
                    continue;
                    } catch (Throwable t) {
                        throw new MatchException(t.toString(), t);
                    }
                default:
                    throw new MatchException(null, null);
            }
        }

        return postpass;
    }

    public void addToFrame(FrameGraphBuilder pFrameGraphBuilder, int pWidth, int pHeight, PostChain.TargetBundle pTargetBundle) {
        Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, (float)pWidth, 0.0F, (float)pHeight, 0.1F, 1000.0F);
        Map<ResourceLocation, ResourceHandle<RenderTarget>> map = new HashMap<>(this.internalTargets.size() + this.externalTargets.size());

        for (ResourceLocation resourcelocation : this.externalTargets) {
            map.put(resourcelocation, pTargetBundle.getOrThrow(resourcelocation));
        }

        for (Entry<ResourceLocation, PostChainConfig.InternalTarget> entry : this.internalTargets.entrySet()) {
            ResourceLocation resourcelocation1 = entry.getKey();
            Objects.requireNonNull(entry.getValue());
            RenderTargetDescriptor rendertargetdescriptor = switch (entry.getValue()) {
                case PostChainConfig.FixedSizedTarget target -> new RenderTargetDescriptor(target.width(), target.height(), true);
                case PostChainConfig.FullScreenTarget target -> new RenderTargetDescriptor(pWidth, pHeight, true);
                default -> throw new MatchException(null, null);
            };
            map.put(resourcelocation1, pFrameGraphBuilder.createInternal(resourcelocation1.toString(), rendertargetdescriptor));
        }

        for (PostPass postpass : this.passes) {
            postpass.addToFrame(pFrameGraphBuilder, map, matrix4f);
        }

        for (ResourceLocation resourcelocation2 : this.externalTargets) {
            pTargetBundle.replace(resourcelocation2, map.get(resourcelocation2));
        }
    }

    @Deprecated
    public void process(RenderTarget pTarget, GraphicsResourceAllocator pGraphicsResourceAllocator) {
        FrameGraphBuilder framegraphbuilder = new FrameGraphBuilder();
        PostChain.TargetBundle postchain$targetbundle = PostChain.TargetBundle.of(MAIN_TARGET_ID, framegraphbuilder.importExternal("main", pTarget));
        this.addToFrame(framegraphbuilder, pTarget.width, pTarget.height, postchain$targetbundle);
        framegraphbuilder.execute(pGraphicsResourceAllocator);
    }

    public void setUniform(String pName, float pBackgroundBlurriness) {
        for (PostPass postpass : this.passes) {
            postpass.getShader().safeGetUniform(pName).set(pBackgroundBlurriness);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface TargetBundle {
        static PostChain.TargetBundle of(final ResourceLocation pId, final ResourceHandle<RenderTarget> pHandle) {
            return new PostChain.TargetBundle() {
                private ResourceHandle<RenderTarget> handle = pHandle;

                @Override
                public void replace(ResourceLocation p_368607_, ResourceHandle<RenderTarget> p_369595_) {
                    if (p_368607_.equals(pId)) {
                        this.handle = p_369595_;
                    } else {
                        throw new IllegalArgumentException("No target with id " + p_368607_);
                    }
                }

                @Nullable
                @Override
                public ResourceHandle<RenderTarget> get(ResourceLocation p_364302_) {
                    return p_364302_.equals(pId) ? this.handle : null;
                }
            };
        }

        void replace(ResourceLocation pId, ResourceHandle<RenderTarget> pHandle);

        @Nullable
        ResourceHandle<RenderTarget> get(ResourceLocation pId);

        default ResourceHandle<RenderTarget> getOrThrow(ResourceLocation pId) {
            ResourceHandle<RenderTarget> resourcehandle = this.get(pId);
            if (resourcehandle == null) {
                throw new IllegalArgumentException("Missing target with id " + pId);
            } else {
                return resourcehandle;
            }
        }
    }
}
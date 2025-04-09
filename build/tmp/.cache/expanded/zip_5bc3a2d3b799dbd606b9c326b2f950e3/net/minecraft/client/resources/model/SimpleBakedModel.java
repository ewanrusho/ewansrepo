package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleBakedModel implements BakedModel {
    public static final String PARTICLE_TEXTURE_REFERENCE = "particle";
    private final List<BakedQuad> unculledFaces;
    private final Map<Direction, List<BakedQuad>> culledFaces;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final boolean usesBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final ItemTransforms transforms;
    /** Forge: Block render types to be used with {@linkplain net.minecraft.client.GraphicsStatus#FANCY fancy graphics} */
    protected final net.minecraftforge.client.ChunkRenderTypeSet blockRenderTypes;
    /** Forge: Block render types to be used with {@linkplain net.minecraft.client.GraphicsStatus#FAST fast graphics} */
    protected final net.minecraftforge.client.ChunkRenderTypeSet blockRenderTypesFast;
    /** Forge: If this model's {@linkplain #blockRenderTypes block render types} are rendering cutout, to account for older leaves model JSONs */
    protected final boolean isRenderingCutout;

    /** @deprecated Forge: Use {@linkplain #SimpleBakedModel(List, Map, boolean, boolean, boolean, TextureAtlasSprite, ItemTransforms, net.minecraftforge.client.RenderTypeGroup, net.minecraftforge.client.RenderTypeGroup) variant with RenderTypeGroup} **/
    @Deprecated
    public SimpleBakedModel(List<BakedQuad> pUnculledFaces, Map<Direction, List<BakedQuad>> pCulledFaces, boolean pHasAmbientOcclusion, boolean pUseBlockLight, boolean pIsGui3d, TextureAtlasSprite pParticleIcon, ItemTransforms pTransforms) {
        this(pUnculledFaces, pCulledFaces, pHasAmbientOcclusion, pUseBlockLight, pIsGui3d, pParticleIcon, pTransforms, net.minecraftforge.client.RenderTypeGroup.EMPTY);
    }

    /** @deprecated Forge: Use {@linkplain #SimpleBakedModel(List, Map, boolean, boolean, boolean, TextureAtlasSprite, ItemTransforms, net.minecraftforge.client.RenderTypeGroup, net.minecraftforge.client.RenderTypeGroup) variant with RenderTypeGroup for fast graphics} **/
    @Deprecated(forRemoval = true, since = "1.21.4")
    public SimpleBakedModel(List<BakedQuad> pUnculledFaces, Map<Direction, List<BakedQuad>> pCulledFaces, boolean pHasAmbientOcclusion, boolean pUseBlockLight, boolean pIsGui3d, TextureAtlasSprite pParticleIcon, ItemTransforms pTransforms, net.minecraftforge.client.RenderTypeGroup renderTypes) {
        this(pUnculledFaces, pCulledFaces, pHasAmbientOcclusion, pUseBlockLight, pIsGui3d, pParticleIcon, pTransforms, renderTypes, net.minecraftforge.client.RenderTypeGroup.EMPTY);
    }

    /** Constructor with {@link net.minecraftforge.client.RenderTypeGroup RenderTypeGroup} for fancy and fast graphics. Preferred over {@link net.minecraft.client.renderer.ItemBlockRenderTypes#setRenderLayer(net.minecraft.world.level.block.Block, net.minecraft.client.renderer.RenderType) ItemBlockRenderTypes.setRenderLayer(Block, RenderType)}. */
    public SimpleBakedModel(
        List<BakedQuad> pUnculledFaces,
        Map<Direction, List<BakedQuad>> pCulledFaces,
        boolean pHasAmbientOcclusion,
        boolean pUseBlockLight,
        boolean pIsGui3d,
        TextureAtlasSprite pParticleIcon,
        ItemTransforms pTransforms,
        net.minecraftforge.client.RenderTypeGroup renderTypes,
        net.minecraftforge.client.RenderTypeGroup renderTypesFast
    ) {
        this.unculledFaces = pUnculledFaces;
        this.culledFaces = pCulledFaces;
        this.hasAmbientOcclusion = pHasAmbientOcclusion;
        this.isGui3d = pIsGui3d;
        this.usesBlockLight = pUseBlockLight;
        this.particleIcon = pParticleIcon;
        this.transforms = pTransforms;

        boolean hasRenderTypes = renderTypes != null && !renderTypes.isEmpty();
        boolean hasRenderTypesFast = renderTypesFast != null && !renderTypesFast.isEmpty();
        this.blockRenderTypes = hasRenderTypes ? net.minecraftforge.client.ChunkRenderTypeSet.of(renderTypes.block()) : null;
        this.blockRenderTypesFast = hasRenderTypesFast ? net.minecraftforge.client.ChunkRenderTypeSet.of(renderTypesFast.block()) : null;
        this.isRenderingCutout = hasRenderTypes && (renderTypes.block() == net.minecraft.client.renderer.RenderType.cutout() || renderTypes.block() == net.minecraft.client.renderer.RenderType.cutoutMipped());
    }

    public static BakedModel bakeElements(List<BlockElement> pElements, TextureSlots pTextureSlots, SpriteGetter pSpriteGetter, ModelState pModelState, boolean pHasAmbientOcclusion, boolean pUseBlockLight, boolean pIsGui3d, ItemTransforms pTransforms) {
        return bakeElements(pElements, pTextureSlots, pSpriteGetter, pModelState, pHasAmbientOcclusion, pUseBlockLight, pIsGui3d, pTransforms, null);
    }

    public static BakedModel bakeElements(List<BlockElement> pElements, TextureSlots pTextureSlots, SpriteGetter pSpriteGetter, ModelState pModelState, boolean pHasAmbientOcclusion, boolean pUseBlockLight, boolean pIsGui3d, ItemTransforms pTransforms, @Nullable net.minecraftforge.client.RenderTypeGroup renderType) {
        return bakeElements(pElements, pTextureSlots, pSpriteGetter, pModelState, pHasAmbientOcclusion, pUseBlockLight, pIsGui3d, pTransforms, renderType, null);
    }

    public static BakedModel bakeElements(
        List<BlockElement> pElements,
        TextureSlots pTextureSlots,
        SpriteGetter pSpriteGetter,
        ModelState pModelState,
        boolean pHasAmbientOcclusion,
        boolean pUseBlockLight,
        boolean pIsGui3d,
        ItemTransforms pTransforms,
        @Nullable net.minecraftforge.client.RenderTypeGroup renderType,
        @Nullable net.minecraftforge.client.RenderTypeGroup renderTypeFast
    ) {
        TextureAtlasSprite textureatlassprite = findSprite(pSpriteGetter, pTextureSlots, "particle");
        SimpleBakedModel.Builder simplebakedmodel$builder = new SimpleBakedModel.Builder(pHasAmbientOcclusion, pUseBlockLight, pIsGui3d, pTransforms)
            .particle(textureatlassprite);

        for (BlockElement blockelement : pElements) {
            for (Direction direction : blockelement.faces.keySet()) {
                BlockElementFace blockelementface = blockelement.faces.get(direction);
                TextureAtlasSprite textureatlassprite1 = findSprite(pSpriteGetter, pTextureSlots, blockelementface.texture());
                if (blockelementface.cullForDirection() == null) {
                    simplebakedmodel$builder.addUnculledFace(bakeFace(blockelement, blockelementface, textureatlassprite1, direction, pModelState));
                } else {
                    simplebakedmodel$builder.addCulledFace(
                        Direction.rotate(pModelState.getRotation().getMatrix(), blockelementface.cullForDirection()),
                        bakeFace(blockelement, blockelementface, textureatlassprite1, direction, pModelState)
                    );
                }
            }
        }

        if (renderType != null && !renderType.isEmpty()) {
            if (renderTypeFast != null && !renderTypeFast.isEmpty())
                simplebakedmodel$builder.renderTypes(renderType, renderTypeFast);
            else
                simplebakedmodel$builder.renderTypes(renderType);
        }

        return simplebakedmodel$builder.build();
    }

    public static BakedQuad bakeFace(
        BlockElement pElement, BlockElementFace pFace, TextureAtlasSprite pSprite, Direction pFacing, ModelState pTransform
    ) {
        return FaceBakery.bakeQuad(
            pElement.from, pElement.to, pFace, pSprite, pFacing, pTransform, pElement.rotation, pElement.shade, pElement.lightEmission
        );
    }

    private static TextureAtlasSprite findSprite(SpriteGetter pSpriteGetter, TextureSlots pTextureSlots, String pMaterial) {
        Material material = pTextureSlots.getMaterial(pMaterial);
        return material != null ? pSpriteGetter.get(material) : pSpriteGetter.reportMissingReference(pMaterial);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState p_235054_, @Nullable Direction p_235055_, RandomSource p_235056_) {
        return p_235055_ == null ? this.unculledFaces : this.culledFaces.get(p_235055_);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return this.isGui3d;
    }

    @Override
    public boolean usesBlockLight() {
        return this.usesBlockLight;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.particleIcon;
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.transforms;
    }

    private static final net.minecraftforge.client.ChunkRenderTypeSet SOLID_BLOCK = net.minecraftforge.client.ChunkRenderTypeSet.of(net.minecraft.client.renderer.RenderType.solid());

    @Override
    public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, net.minecraftforge.client.model.data.ModelData data) {
        if (!net.minecraft.client.renderer.ItemBlockRenderTypes.isFancy()) {
            if (blockRenderTypesFast != null)
                return blockRenderTypesFast;
            if (isRenderingCutout && state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock)
                return SOLID_BLOCK;
        }
        return blockRenderTypes != null ? blockRenderTypes : BakedModel.super.getRenderTypes(state, rand, data);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final ImmutableList.Builder<BakedQuad> unculledFaces = ImmutableList.builder();
        private final EnumMap<Direction, ImmutableList.Builder<BakedQuad>> culledFaces = Maps.newEnumMap(Direction.class);
        private final boolean hasAmbientOcclusion;
        @Nullable
        private TextureAtlasSprite particleIcon;
        private final boolean usesBlockLight;
        private final boolean isGui3d;
        private final ItemTransforms transforms;

        public Builder(boolean pHasAmbientOcclusion, boolean pUseBlockLight, boolean pIsGui3d, ItemTransforms pTransforms) {
            this.hasAmbientOcclusion = pHasAmbientOcclusion;
            this.usesBlockLight = pUseBlockLight;
            this.isGui3d = pIsGui3d;
            this.transforms = pTransforms;

            for (Direction direction : Direction.values()) {
                this.culledFaces.put(direction, ImmutableList.builder());
            }
        }

        public SimpleBakedModel.Builder addCulledFace(Direction pFacing, BakedQuad pQuad) {
            this.culledFaces.get(pFacing).add(pQuad);
            return this;
        }

        public SimpleBakedModel.Builder addUnculledFace(BakedQuad pQuad) {
            this.unculledFaces.add(pQuad);
            return this;
        }

        public SimpleBakedModel.Builder particle(TextureAtlasSprite pParticleIcon) {
            this.particleIcon = pParticleIcon;
            return this;
        }

        public SimpleBakedModel.Builder item() {
            return this;
        }

        private net.minecraftforge.client.RenderTypeGroup renderTypes = net.minecraftforge.client.RenderTypeGroup.EMPTY;
        private net.minecraftforge.client.RenderTypeGroup renderTypesFast = net.minecraftforge.client.RenderTypeGroup.EMPTY;

        /**
         * Sets the render type to be used for this model, and will be used for any graphics setting.
         * <p>
         * If you need to set a specific render type for
         * {@linkplain net.minecraft.client.GraphicsStatus#FAST fast graphics}, consider using
         * {@link #renderTypes(net.minecraftforge.client.RenderTypeGroup, net.minecraftforge.client.RenderTypeGroup)
         * renderTypes(RenderTypeGroup, RenderTypeGroup)} instead which allows choosing render types for both fancy and
         * fast graphics.
         *
         * @apiNote If this model is for {@linkplain net.minecraft.world.level.block.LeavesBlock leaves} and if the
         * given render type is either {@linkplain net.minecraft.client.renderer.RenderType#cutout() cutout} or
         * {@linkplain net.minecraft.client.renderer.RenderType#cutoutMipped() cutout mipped}, it will be overridden
         * with {@linkplain net.minecraft.client.renderer.RenderType#solid() solid} when fast graphics is enabled.
         * @see #renderTypes(net.minecraftforge.client.RenderTypeGroup, net.minecraftforge.client.RenderTypeGroup)
         * renderTypes(RenderTypeGroup, RenderTypeGroup)
         */
        public SimpleBakedModel.Builder renderTypes(net.minecraftforge.client.RenderTypeGroup renderTypes) {
            return this.renderTypes(renderTypes, net.minecraftforge.client.RenderTypeGroup.EMPTY);
        }

        /**
         * Sets the render types to be used for this model: one for
         * {@linkplain net.minecraft.client.GraphicsStatus#FANCY fancy graphics} and one for
         * {@linkplain net.minecraft.client.GraphicsStatus#FAST fast graphics}.
         *
         * @see #renderTypes(net.minecraftforge.client.RenderTypeGroup)
         */
        public SimpleBakedModel.Builder renderTypes(net.minecraftforge.client.RenderTypeGroup renderTypes, net.minecraftforge.client.RenderTypeGroup renderTypesFast) {
            this.renderTypes = renderTypes;
            this.renderTypesFast = renderTypesFast;
            return this;
        }

        public BakedModel build() {
            if (this.particleIcon == null) {
                throw new RuntimeException("Missing particle!");
            } else {
                Map<Direction, List<BakedQuad>> map = Maps.transformValues(this.culledFaces, ImmutableList.Builder::build);
                return new SimpleBakedModel(
                    // Forge: Account for render types in model JSONs
                    this.unculledFaces.build(), new EnumMap<>(map), this.hasAmbientOcclusion, this.usesBlockLight, this.isGui3d, this.particleIcon, this.transforms, this.renderTypes, this.renderTypesFast
                );
            }
        }
    }
}

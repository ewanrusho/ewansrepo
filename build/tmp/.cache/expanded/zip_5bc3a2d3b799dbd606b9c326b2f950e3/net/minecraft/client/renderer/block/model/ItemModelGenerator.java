package net.minecraft.client.renderer.block.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ItemModelGenerator implements UnbakedModel {
    public static final ResourceLocation GENERATED_ITEM_MODEL_ID = ResourceLocation.withDefaultNamespace("builtin/generated");
    public static final List<String> LAYERS = List.of("layer0", "layer1", "layer2", "layer3", "layer4");
    private static final float MIN_Z = 7.5F;
    private static final float MAX_Z = 8.5F;
    private static final TextureSlots.Data TEXTURE_SLOTS = new TextureSlots.Data.Builder().addReference("particle", "layer0").build();

    @Override
    public TextureSlots.Data getTextureSlots() {
        return TEXTURE_SLOTS;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver p_376467_) {
    }

    @Nullable
    @Override
    public UnbakedModel.GuiLight getGuiLight() {
        return UnbakedModel.GuiLight.FRONT;
    }

    @Override
    public BakedModel bake(
        TextureSlots p_378742_, ModelBaker p_376373_, ModelState p_377118_, boolean p_375903_, boolean p_378244_, ItemTransforms p_377372_
    ) {
        return this.bake(p_378742_, p_376373_.sprites(), p_377118_, p_375903_, p_378244_, p_377372_);
    }

    private BakedModel bake(
        TextureSlots pTextureSlots, SpriteGetter pSpriteGetter, ModelState pModelState, boolean pHasAmbientOcclusion, boolean pUseBlockLight, ItemTransforms pTransforms
    ) {
        TextureSlots.Data.Builder textureslots$data$builder = new TextureSlots.Data.Builder();
        List<BlockElement> list = new ArrayList<>();

        for (int i = 0; i < LAYERS.size(); i++) {
            String s = LAYERS.get(i);
            Material material = pTextureSlots.getMaterial(s);
            if (material == null) {
                break;
            }

            textureslots$data$builder.addTexture(s, material);
            SpriteContents spritecontents = pSpriteGetter.get(material).contents();
            list.addAll(this.processFrames(i, s, spritecontents));
        }

        return SimpleBakedModel.bakeElements(list, pTextureSlots, pSpriteGetter, pModelState, pHasAmbientOcclusion, pUseBlockLight, false, pTransforms);
    }

    public List<BlockElement> processFrames(int pTintIndex, String pTexture, SpriteContents pSprite) {
        Map<Direction, BlockElementFace> map = Map.of(
            Direction.SOUTH,
            new BlockElementFace(null, pTintIndex, pTexture, new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0)),
            Direction.NORTH,
            new BlockElementFace(null, pTintIndex, pTexture, new BlockFaceUV(new float[]{16.0F, 0.0F, 0.0F, 16.0F}, 0))
        );
        List<BlockElement> list = new ArrayList<>();
        list.add(new BlockElement(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), map));
        list.addAll(this.createSideElements(pSprite, pTexture, pTintIndex));
        return list;
    }

    private List<BlockElement> createSideElements(SpriteContents pSprite, String pTexture, int pTintIndex) {
        float f = (float)pSprite.width();
        float f1 = (float)pSprite.height();
        List<BlockElement> list = new ArrayList<>();

        for (ItemModelGenerator.Span itemmodelgenerator$span : this.getSpans(pSprite)) {
            float f2 = 0.0F;
            float f3 = 0.0F;
            float f4 = 0.0F;
            float f5 = 0.0F;
            float f6 = 0.0F;
            float f7 = 0.0F;
            float f8 = 0.0F;
            float f9 = 0.0F;
            float f10 = 16.0F / f;
            float f11 = 16.0F / f1;
            float f12 = (float)itemmodelgenerator$span.getMin();
            float f13 = (float)itemmodelgenerator$span.getMax();
            float f14 = (float)itemmodelgenerator$span.getAnchor();
            ItemModelGenerator.SpanFacing itemmodelgenerator$spanfacing = itemmodelgenerator$span.getFacing();
            switch (itemmodelgenerator$spanfacing) {
                case UP:
                    f6 = f12;
                    f2 = f12;
                    f4 = f7 = f13 + 1.0F;
                    f8 = f14;
                    f3 = f14;
                    f5 = f14;
                    f9 = f14 + 1.0F;
                    break;
                case DOWN:
                    f8 = f14;
                    f9 = f14 + 1.0F;
                    f6 = f12;
                    f2 = f12;
                    f4 = f7 = f13 + 1.0F;
                    f3 = f14 + 1.0F;
                    f5 = f14 + 1.0F;
                    break;
                case LEFT:
                    f6 = f14;
                    f2 = f14;
                    f4 = f14;
                    f7 = f14 + 1.0F;
                    f9 = f12;
                    f3 = f12;
                    f5 = f8 = f13 + 1.0F;
                    break;
                case RIGHT:
                    f6 = f14;
                    f7 = f14 + 1.0F;
                    f2 = f14 + 1.0F;
                    f4 = f14 + 1.0F;
                    f9 = f12;
                    f3 = f12;
                    f5 = f8 = f13 + 1.0F;
            }

            f2 *= f10;
            f4 *= f10;
            f3 *= f11;
            f5 *= f11;
            f3 = 16.0F - f3;
            f5 = 16.0F - f5;
            f6 *= f10;
            f7 *= f10;
            f8 *= f11;
            f9 *= f11;
            Map<Direction, BlockElementFace> map = Map.of(
                itemmodelgenerator$spanfacing.getDirection(), new BlockElementFace(null, pTintIndex, pTexture, new BlockFaceUV(new float[]{f6, f8, f7, f9}, 0))
            );
            switch (itemmodelgenerator$spanfacing) {
                case UP:
                    list.add(new BlockElement(new Vector3f(f2, f3, 7.5F), new Vector3f(f4, f3, 8.5F), map));
                    break;
                case DOWN:
                    list.add(new BlockElement(new Vector3f(f2, f5, 7.5F), new Vector3f(f4, f5, 8.5F), map));
                    break;
                case LEFT:
                    list.add(new BlockElement(new Vector3f(f2, f3, 7.5F), new Vector3f(f2, f5, 8.5F), map));
                    break;
                case RIGHT:
                    list.add(new BlockElement(new Vector3f(f4, f3, 7.5F), new Vector3f(f4, f5, 8.5F), map));
            }
        }

        return list;
    }

    private List<ItemModelGenerator.Span> getSpans(SpriteContents pSprite) {
        int i = pSprite.width();
        int j = pSprite.height();
        List<ItemModelGenerator.Span> list = new ArrayList<>();
        pSprite.getUniqueFrames().forEach(p_173444_ -> {
            for (int k = 0; k < j; k++) {
                for (int l = 0; l < i; l++) {
                    boolean flag = !this.isTransparent(pSprite, p_173444_, l, k, i, j);
                    this.checkTransition(ItemModelGenerator.SpanFacing.UP, list, pSprite, p_173444_, l, k, i, j, flag);
                    this.checkTransition(ItemModelGenerator.SpanFacing.DOWN, list, pSprite, p_173444_, l, k, i, j, flag);
                    this.checkTransition(ItemModelGenerator.SpanFacing.LEFT, list, pSprite, p_173444_, l, k, i, j, flag);
                    this.checkTransition(ItemModelGenerator.SpanFacing.RIGHT, list, pSprite, p_173444_, l, k, i, j, flag);
                }
            }
        });
        return list;
    }

    private void checkTransition(
        ItemModelGenerator.SpanFacing pSpanFacing,
        List<ItemModelGenerator.Span> pListSpans,
        SpriteContents pContents,
        int pFrameIndex,
        int pPixelX,
        int pPixelY,
        int pSpriteWidth,
        int pSpriteHeight,
        boolean pTransparent
    ) {
        boolean flag = this.isTransparent(pContents, pFrameIndex, pPixelX + pSpanFacing.getXOffset(), pPixelY + pSpanFacing.getYOffset(), pSpriteWidth, pSpriteHeight)
            && pTransparent;
        if (flag) {
            this.createOrExpandSpan(pListSpans, pSpanFacing, pPixelX, pPixelY);
        }
    }

    private void createOrExpandSpan(List<ItemModelGenerator.Span> pListSpans, ItemModelGenerator.SpanFacing pSpanFacing, int pPixelX, int pPixelY) {
        ItemModelGenerator.Span itemmodelgenerator$span = null;

        for (ItemModelGenerator.Span itemmodelgenerator$span1 : pListSpans) {
            if (itemmodelgenerator$span1.getFacing() == pSpanFacing) {
                int i = pSpanFacing.isHorizontal() ? pPixelY : pPixelX;
                if (itemmodelgenerator$span1.getAnchor() == i) {
                    itemmodelgenerator$span = itemmodelgenerator$span1;
                    break;
                }
            }
        }

        int j = pSpanFacing.isHorizontal() ? pPixelY : pPixelX;
        int k = pSpanFacing.isHorizontal() ? pPixelX : pPixelY;
        if (itemmodelgenerator$span == null) {
            pListSpans.add(new ItemModelGenerator.Span(pSpanFacing, k, j));
        } else {
            itemmodelgenerator$span.expand(k);
        }
    }

    private boolean isTransparent(SpriteContents pSprite, int pFrameIndex, int pPixelX, int pPixelY, int pSpriteWidth, int pSpriteHeight) {
        return pPixelX >= 0 && pPixelY >= 0 && pPixelX < pSpriteWidth && pPixelY < pSpriteHeight ? pSprite.isTransparent(pFrameIndex, pPixelX, pPixelY) : true;
    }

    @OnlyIn(Dist.CLIENT)
    static class Span {
        private final ItemModelGenerator.SpanFacing facing;
        private int min;
        private int max;
        private final int anchor;

        public Span(ItemModelGenerator.SpanFacing pFacing, int pMinMax, int pAnchor) {
            this.facing = pFacing;
            this.min = pMinMax;
            this.max = pMinMax;
            this.anchor = pAnchor;
        }

        public void expand(int pPos) {
            if (pPos < this.min) {
                this.min = pPos;
            } else if (pPos > this.max) {
                this.max = pPos;
            }
        }

        public ItemModelGenerator.SpanFacing getFacing() {
            return this.facing;
        }

        public int getMin() {
            return this.min;
        }

        public int getMax() {
            return this.max;
        }

        public int getAnchor() {
            return this.anchor;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum SpanFacing {
        UP(Direction.UP, 0, -1),
        DOWN(Direction.DOWN, 0, 1),
        LEFT(Direction.EAST, -1, 0),
        RIGHT(Direction.WEST, 1, 0);

        private final Direction direction;
        private final int xOffset;
        private final int yOffset;

        private SpanFacing(final Direction pDirection, final int pXOffset, final int pYOffset) {
            this.direction = pDirection;
            this.xOffset = pXOffset;
            this.yOffset = pYOffset;
        }

        public Direction getDirection() {
            return this.direction;
        }

        public int getXOffset() {
            return this.xOffset;
        }

        public int getYOffset() {
            return this.yOffset;
        }

        boolean isHorizontal() {
            return this == DOWN || this == UP;
        }
    }
}
package net.minecraft.client.resources.model;

import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface UnbakedModel extends ResolvableModel {
    boolean DEFAULT_AMBIENT_OCCLUSION = true;
    UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.SIDE;

    default BakedModel bake(TextureSlots textures, ModelBaker baker, ModelState state) {
        return bake(textures, baker, state, this.getAmbientOcclusion(), this.getGuiLight().lightLikeBlock(), this.getTransforms());
    }

    BakedModel bake(TextureSlots pTextureSlots, ModelBaker pBaker, ModelState pModelState, boolean pHasAmbientOcclusion, boolean pUseBlockLight, ItemTransforms pTransforms);

    @Nullable
    default Boolean getAmbientOcclusion() {
        return null;
    }

    @Nullable
    default UnbakedModel.GuiLight getGuiLight() {
        return null;
    }

    @Nullable
    default ItemTransforms getTransforms() {
        return null;
    }

    default TextureSlots.Data getTextureSlots() {
        return TextureSlots.Data.EMPTY;
    }

    @Nullable
    default UnbakedModel getParent() {
        return null;
    }

    static BakedModel bakeWithTopModelValues(UnbakedModel pModel, ModelBaker pBaker, ModelState pModelState) {
        TextureSlots textureslots = getTopTextureSlots(pModel, pBaker.rootName());
        boolean flag = getTopAmbientOcclusion(pModel);
        boolean flag1 = getTopGuiLight(pModel).lightLikeBlock();
        ItemTransforms itemtransforms = getTopTransforms(pModel);
        return pModel.bake(textureslots, pBaker, pModelState, flag, flag1, itemtransforms);
    }

    static TextureSlots getTopTextureSlots(UnbakedModel pModel, ModelDebugName pName) {
        TextureSlots.Resolver textureslots$resolver = new TextureSlots.Resolver();

        while (pModel != null) {
            textureslots$resolver.addLast(pModel.getTextureSlots());
            pModel = pModel.getParent();
        }

        return textureslots$resolver.resolve(pName);
    }

    static boolean getTopAmbientOcclusion(UnbakedModel pModel) {
        while (pModel != null) {
            Boolean obool = pModel.getAmbientOcclusion();
            if (obool != null) {
                return obool;
            }

            pModel = pModel.getParent();
        }

        return true;
    }

    static UnbakedModel.GuiLight getTopGuiLight(UnbakedModel pModel) {
        while (pModel != null) {
            UnbakedModel.GuiLight unbakedmodel$guilight = pModel.getGuiLight();
            if (unbakedmodel$guilight != null) {
                return unbakedmodel$guilight;
            }

            pModel = pModel.getParent();
        }

        return DEFAULT_GUI_LIGHT;
    }

    static ItemTransform getTopTransform(UnbakedModel pModel, ItemDisplayContext pDisplayContext) {
        while (pModel != null) {
            ItemTransforms itemtransforms = pModel.getTransforms();
            if (itemtransforms != null) {
                ItemTransform itemtransform = itemtransforms.getTransform(pDisplayContext);
                if (itemtransform != ItemTransform.NO_TRANSFORM) {
                    return itemtransform;
                }
            }

            pModel = pModel.getParent();
        }

        return ItemTransform.NO_TRANSFORM;
    }

    static ItemTransforms getTopTransforms(UnbakedModel pUnbakedModel) {
        ItemTransform itemtransform = getTopTransform(pUnbakedModel, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        ItemTransform itemtransform1 = getTopTransform(pUnbakedModel, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        ItemTransform itemtransform2 = getTopTransform(pUnbakedModel, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        ItemTransform itemtransform3 = getTopTransform(pUnbakedModel, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        ItemTransform itemtransform4 = getTopTransform(pUnbakedModel, ItemDisplayContext.HEAD);
        ItemTransform itemtransform5 = getTopTransform(pUnbakedModel, ItemDisplayContext.GUI);
        ItemTransform itemtransform6 = getTopTransform(pUnbakedModel, ItemDisplayContext.GROUND);
        ItemTransform itemtransform7 = getTopTransform(pUnbakedModel, ItemDisplayContext.FIXED);
        return new ItemTransforms(itemtransform, itemtransform1, itemtransform2, itemtransform3, itemtransform4, itemtransform5, itemtransform6, itemtransform7);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum GuiLight {
        FRONT("front"),
        SIDE("side");

        private final String name;

        private GuiLight(final String pName) {
            this.name = pName;
        }

        public static UnbakedModel.GuiLight getByName(String pName) {
            for (UnbakedModel.GuiLight unbakedmodel$guilight : values()) {
                if (unbakedmodel$guilight.name.equals(pName)) {
                    return unbakedmodel$guilight;
                }
            }

            throw new IllegalArgumentException("Invalid gui light: " + pName);
        }

        public boolean lightLikeBlock() {
            return this == SIDE;
        }

        public String getSerializedName() {
            return name;
        }
    }
}

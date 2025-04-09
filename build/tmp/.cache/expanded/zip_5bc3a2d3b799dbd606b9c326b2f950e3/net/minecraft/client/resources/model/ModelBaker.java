package net.minecraft.client.resources.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.VisibleForDebug;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ModelBaker {
    BakedModel bake(ResourceLocation pLocation, ModelState pTransform);

    SpriteGetter sprites();

    @VisibleForDebug
    ModelDebugName rootName();

    /** Forge: Return the render type to use when baking this model, its a dirty hack to pass down this value to parents */
    @org.jetbrains.annotations.Nullable
    default net.minecraftforge.client.RenderTypeGroup renderType() {
        return null;
    }

    /** Forge: Return the fast graphics render type to use when baking this model, its a dirty hack to pass down this value to parents */
    @org.jetbrains.annotations.Nullable
    default net.minecraftforge.client.RenderTypeGroup renderTypeFast() {
        return null;
    }
}

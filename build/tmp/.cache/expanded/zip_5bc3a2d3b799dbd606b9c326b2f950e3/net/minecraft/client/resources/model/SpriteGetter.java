package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteGetter {
    TextureAtlasSprite get(Material pMaterial);

    TextureAtlasSprite reportMissingReference(String pReference);

    default TextureAtlasSprite maybeMissing(net.minecraft.client.renderer.block.model.TextureSlots textures, String name) {
        var material = textures.getMaterial(name);
        return material != null ? get(material) : reportMissingReference(name);
    }
}

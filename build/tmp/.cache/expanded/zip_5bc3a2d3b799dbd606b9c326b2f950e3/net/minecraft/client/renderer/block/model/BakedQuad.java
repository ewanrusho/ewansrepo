package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BakedQuad {
    protected final int[] vertices;
    protected final int tintIndex;
    protected final Direction direction;
    protected final TextureAtlasSprite sprite;
    private final boolean shade;
    private final int lightEmission;
    private final boolean hasAmbientOcclusion;

    public BakedQuad(int[] pVerticies, int pTintIndex, Direction pDirection, TextureAtlasSprite pSprite, boolean pShade, int pLightEmission) {
        this(pVerticies, pTintIndex, pDirection, pSprite, pShade, pLightEmission, true);
    }

    public BakedQuad(int[] pVerticies, int pTintIndex, Direction pDirection, TextureAtlasSprite pSprite, boolean pShade, int pLightEmission, boolean hasAmbientOcclusion) {
        this.vertices = pVerticies;
        this.tintIndex = pTintIndex;
        this.direction = pDirection;
        this.sprite = pSprite;
        this.shade = pShade;
        this.lightEmission = pLightEmission;
        this.hasAmbientOcclusion = hasAmbientOcclusion;
    }

    public TextureAtlasSprite getSprite() {
        return this.sprite;
    }

    public int[] getVertices() {
        return this.vertices;
    }

    public boolean isTinted() {
        return this.tintIndex != -1;
    }

    public int getTintIndex() {
        return this.tintIndex;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public boolean isShade() {
        return this.shade;
    }

    public int getLightEmission() {
        return this.lightEmission;
    }

    public boolean hasAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }
}

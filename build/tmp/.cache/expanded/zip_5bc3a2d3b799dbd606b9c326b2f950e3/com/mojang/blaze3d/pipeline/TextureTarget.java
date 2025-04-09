package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureTarget extends RenderTarget {
    public TextureTarget(int pWidth, int pHeight, boolean pUseDepth) {
        super(pUseDepth);
        RenderSystem.assertOnRenderThreadOrInit();
        this.resize(pWidth, pHeight);
    }
}
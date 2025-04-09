/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.loading;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.StartupMessageManager;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.loading.progress.ProgressMeter;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This is an implementation of the LoadingOverlay that calls back into the early window rendering, as part of the
 * game loading cycle. We completely replace the {@link #render(GuiGraphics, int, int, float)} call from the parent
 * with one of our own, that allows us to blend our early loading screen into the main window, in the same manner as
 * the Mojang screen. It also allows us to see and tick appropriately as the later stages of the loading system run.
 *
 * It is somewhat a copy of the superclass render method.
 */
public class ForgeLoadingOverlay extends LoadingOverlay {
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final DisplayWindow displayWindow;
    private final ProgressMeter progress;

    public ForgeLoadingOverlay(final Minecraft mc, final ReloadInstance reloader, final Consumer<Optional<Throwable>> errorConsumer, DisplayWindow displayWindow) {
        super(mc, reloader, errorConsumer, false);
        this.minecraft = mc;
        this.reload = reloader;
        this.displayWindow = displayWindow;
        displayWindow.addMojangTexture(mc.getTextureManager().getTexture(MOJANG_STUDIOS_LOGO_LOCATION).getId());
        this.progress = StartupMessageManager.prependProgressBar("Minecraft Progress", 100);
    }

    public static Supplier<LoadingOverlay> newInstance(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> handler, DisplayWindow window) {
        return ()->new ForgeLoadingOverlay(mc.get(), ri.get(), handler, window);
    }

    @Override
    protected boolean renderContents(GuiGraphics gui, float fade) {
        progress.setAbsolute(Mth.clamp((int)(this.reload.getActualProgress() * 100f), 0, 100));

        int alpha = (int)(fade * 255);
        this.displayWindow.render(alpha);

        int width = gui.guiWidth();
        int height = gui.guiHeight();

        var fbWidth = this.minecraft.getWindow().getWidth();
        var fbHeight = this.minecraft.getWindow().getHeight();
        GL30C.glViewport(0, 0, fbWidth, fbHeight);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fade);
        Matrix4f pos = gui.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, displayWindow.getFramebufferTextureId());
        GL30C.glTexParameterIi(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_NEAREST);
        GL30C.glTexParameterIi(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_NEAREST);

        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(pos, 0,     0,      0f).setUv(0, 0).setColor(1f, 1f, 1f, fade);
        buf.addVertex(pos, 0,     height, 0f).setUv(0, 1).setColor(1f, 1f, 1f, fade);
        buf.addVertex(pos, width, height, 0f).setUv(1, 1).setColor(1f, 1f, 1f, fade);
        buf.addVertex(pos, width, 0,      0f).setUv(1, 0).setColor(1f, 1f, 1f, fade);
        BufferUploader.drawWithShader(buf.buildOrThrow());

        // I dont know what exactly this does, but without it the screen flickers black.
        // So as a hack we just render the mojang logo as a 0x0 cube
        // TODO: Remove this when early screen is re-written to not be a texture based renderer
        var logo = gui.getBufferSource().getBuffer(RenderType.mojangLogo());
        logo.addVertex(pos, 0, 0, 0f).setUv(0, 0).setColor(1f, 1f, 1f, fade);
        logo.addVertex(pos, 0, 0, 0f).setUv(0, 1).setColor(1f, 1f, 1f, fade);
        logo.addVertex(pos, 0, 0, 0f).setUv(1, 1).setColor(1f, 1f, 1f, fade);
        logo.addVertex(pos, 0, 0, 0f).setUv(1, 0).setColor(1f, 1f, 1f, fade);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1f);

        return false;
    }
}

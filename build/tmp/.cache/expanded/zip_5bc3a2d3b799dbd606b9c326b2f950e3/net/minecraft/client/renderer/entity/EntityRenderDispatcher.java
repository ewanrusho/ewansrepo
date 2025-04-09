package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class EntityRenderDispatcher implements ResourceManagerReloadListener {
    private static final RenderType SHADOW_RENDER_TYPE = RenderType.entityShadow(ResourceLocation.withDefaultNamespace("textures/misc/shadow.png"));
    private static final float MAX_SHADOW_RADIUS = 32.0F;
    private static final float SHADOW_POWER_FALLOFF_Y = 0.5F;
    public Map<EntityType<?>, EntityRenderer<?, ?>> renderers = ImmutableMap.of();
    private Map<PlayerSkin.Model, EntityRenderer<? extends Player, ?>> playerRenderers = Map.of();
    public final TextureManager textureManager;
    private Level level;
    public Camera camera;
    private Quaternionf cameraOrientation;
    public Entity crosshairPickEntity;
    private final ItemModelResolver itemModelResolver;
    private final MapRenderer mapRenderer;
    private final BlockRenderDispatcher blockRenderDispatcher;
    private final ItemInHandRenderer itemInHandRenderer;
    private final Font font;
    public final Options options;
    private final Supplier<EntityModelSet> entityModels;
    private final EquipmentAssetManager equipmentAssets;
    private boolean shouldRenderShadow = true;
    private boolean renderHitBoxes;

    public <E extends Entity> int getPackedLightCoords(E pEntity, float pPartialTicks) {
        return this.getRenderer(pEntity).getPackedLightCoords(pEntity, pPartialTicks);
    }

    public EntityRenderDispatcher(
        Minecraft pMinecraft,
        TextureManager pTextureManager,
        ItemModelResolver pItemModelResolver,
        ItemRenderer pItemRenderer,
        MapRenderer pMapRenderer,
        BlockRenderDispatcher pBlockRenderDispatcher,
        Font pFont,
        Options pOptions,
        Supplier<EntityModelSet> pEntityModels,
        EquipmentAssetManager pEquipmentModels
    ) {
        this.textureManager = pTextureManager;
        this.itemModelResolver = pItemModelResolver;
        this.mapRenderer = pMapRenderer;
        this.itemInHandRenderer = new ItemInHandRenderer(pMinecraft, this, pItemRenderer, pItemModelResolver);
        this.blockRenderDispatcher = pBlockRenderDispatcher;
        this.font = pFont;
        this.options = pOptions;
        this.entityModels = pEntityModels;
        this.equipmentAssets = pEquipmentModels;
    }

    public <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T pEntity) {
        if (pEntity instanceof AbstractClientPlayer abstractclientplayer) {
            PlayerSkin.Model playerskin$model = abstractclientplayer.getSkin().model();
            EntityRenderer<? extends Player, ?> entityrenderer = this.playerRenderers.get(playerskin$model);
            return (EntityRenderer<? super T, ?>)(entityrenderer != null ? entityrenderer : this.playerRenderers.get(PlayerSkin.Model.WIDE));
        } else {
            return (EntityRenderer<? super T, ?>)this.renderers.get(pEntity.getType());
        }
    }

    public void prepare(Level pLevel, Camera pActiveRenderInfo, Entity pEntity) {
        this.level = pLevel;
        this.camera = pActiveRenderInfo;
        this.cameraOrientation = pActiveRenderInfo.rotation();
        this.crosshairPickEntity = pEntity;
    }

    public void overrideCameraOrientation(Quaternionf pCameraOrientation) {
        this.cameraOrientation = pCameraOrientation;
    }

    public void setRenderShadow(boolean pRenderShadow) {
        this.shouldRenderShadow = pRenderShadow;
    }

    public void setRenderHitBoxes(boolean pDebugBoundingBox) {
        this.renderHitBoxes = pDebugBoundingBox;
    }

    public boolean shouldRenderHitBoxes() {
        return this.renderHitBoxes;
    }

    public <E extends Entity> boolean shouldRender(E pEntity, Frustum pFrustum, double pCamX, double pCamY, double pCamZ) {
        EntityRenderer<? super E, ?> entityrenderer = this.getRenderer(pEntity);
        return entityrenderer.shouldRender(pEntity, pFrustum, pCamX, pCamY, pCamZ);
    }

    public <E extends Entity> void render(
        E pEntity, double pXOffset, double pYOffset, double pZOffset, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight
    ) {
        EntityRenderer<? super E, ?> entityrenderer = this.getRenderer(pEntity);
        this.render(pEntity, pXOffset, pYOffset, pZOffset, pPartialTick, pPoseStack, pBufferSource, pPackedLight, entityrenderer);
    }

    private <E extends Entity, S extends EntityRenderState> void render(
        E pEntity,
        double pXOffset,
        double pYOffset,
        double pZOffset,
        float pPartialTick,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        EntityRenderer<? super E, S> pRenderer
    ) {
        try {
            S s = pRenderer.createRenderState(pEntity, pPartialTick);
            Vec3 vec3 = pRenderer.getRenderOffset(s);
            double d3 = pXOffset + vec3.x();
            double d0 = pYOffset + vec3.y();
            double d1 = pZOffset + vec3.z();
            pPoseStack.pushPose();
            pPoseStack.translate(d3, d0, d1);
            pRenderer.render(s, pPoseStack, pBufferSource, pPackedLight);
            if (s.displayFireAnimation) {
                this.renderFlame(pPoseStack, pBufferSource, s, Mth.rotationAroundAxis(Mth.Y_AXIS, this.cameraOrientation, new Quaternionf()));
            }

            if (pEntity instanceof Player) {
                pPoseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            }

            if (this.options.entityShadows().get() && this.shouldRenderShadow && !s.isInvisible) {
                float f = pRenderer.getShadowRadius(s);
                if (f > 0.0F) {
                    double d2 = s.distanceToCameraSq;
                    float f1 = (float)((1.0 - d2 / 256.0) * (double)pRenderer.getShadowStrength(s));
                    if (f1 > 0.0F) {
                        renderShadow(pPoseStack, pBufferSource, s, f1, pPartialTick, this.level, Math.min(f, 32.0F));
                    }
                }
            }

            if (!(pEntity instanceof Player)) {
                pPoseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            }

            if (this.renderHitBoxes && !s.isInvisible && !Minecraft.getInstance().showOnlyReducedInfo()) {
                renderHitbox(pPoseStack, pBufferSource.getBuffer(RenderType.lines()), pEntity, pPartialTick, 1.0F, 1.0F, 1.0F);
            }

            pPoseStack.popPose();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering entity in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being rendered");
            pEntity.fillCrashReportCategory(crashreportcategory);
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
            crashreportcategory1.setDetail("Assigned renderer", pRenderer);
            crashreportcategory1.setDetail("Location", CrashReportCategory.formatLocation(this.level, pXOffset, pYOffset, pZOffset));
            crashreportcategory1.setDetail("Delta", pPartialTick);
            throw new ReportedException(crashreport);
        }
    }

    private static void renderServerSideHitbox(PoseStack pPoseStack, Entity pEntity, MultiBufferSource pBufferSource) {
        Entity entity = getServerSideEntity(pEntity);
        if (entity == null) {
            DebugRenderer.renderFloatingText(pPoseStack, pBufferSource, "Missing", pEntity.getX(), pEntity.getBoundingBox().maxY + 1.5, pEntity.getZ(), -65536);
        } else {
            pPoseStack.pushPose();
            pPoseStack.translate(entity.getX() - pEntity.getX(), entity.getY() - pEntity.getY(), entity.getZ() - pEntity.getZ());
            renderHitbox(pPoseStack, pBufferSource.getBuffer(RenderType.lines()), entity, 1.0F, 0.0F, 1.0F, 0.0F);
            ShapeRenderer.renderVector(pPoseStack, pBufferSource.getBuffer(RenderType.lines()), new Vector3f(), entity.getDeltaMovement(), -256);
            pPoseStack.popPose();
        }
    }

    @Nullable
    private static Entity getServerSideEntity(Entity pEntity) {
        IntegratedServer integratedserver = Minecraft.getInstance().getSingleplayerServer();
        if (integratedserver != null) {
            ServerLevel serverlevel = integratedserver.getLevel(pEntity.level().dimension());
            if (serverlevel != null) {
                return serverlevel.getEntity(pEntity.getId());
            }
        }

        return null;
    }

    private static void renderHitbox(
        PoseStack pPoseStack, VertexConsumer pBuffer, Entity pEntity, float pRed, float pGreen, float pBlue, float pAlpha
    ) {
        AABB aabb = pEntity.getBoundingBox().move(-pEntity.getX(), -pEntity.getY(), -pEntity.getZ());
        ShapeRenderer.renderLineBox(pPoseStack, pBuffer, aabb, pGreen, pBlue, pAlpha, 1.0F);
        if (pEntity.isMultipartEntity()) {
            double d0 = -Mth.lerp((double)pRed, pEntity.xOld, pEntity.getX());
            double d1 = -Mth.lerp((double)pRed, pEntity.yOld, pEntity.getY());
            double d2 = -Mth.lerp((double)pRed, pEntity.zOld, pEntity.getZ());

            for (var enderdragonpart : pEntity.getParts()) {
                pPoseStack.pushPose();
                double d3 = d0 + Mth.lerp((double)pRed, enderdragonpart.xOld, enderdragonpart.getX());
                double d4 = d1 + Mth.lerp((double)pRed, enderdragonpart.yOld, enderdragonpart.getY());
                double d5 = d2 + Mth.lerp((double)pRed, enderdragonpart.zOld, enderdragonpart.getZ());
                pPoseStack.translate(d3, d4, d5);
                ShapeRenderer.renderLineBox(
                    pPoseStack,
                    pBuffer,
                    enderdragonpart.getBoundingBox().move(-enderdragonpart.getX(), -enderdragonpart.getY(), -enderdragonpart.getZ()),
                    0.25F,
                    1.0F,
                    0.0F,
                    1.0F
                );
                pPoseStack.popPose();
            }
        }

        if (pEntity instanceof LivingEntity) {
            float f1 = 0.01F;
            ShapeRenderer.renderLineBox(
                pPoseStack,
                pBuffer,
                aabb.minX,
                (double)(pEntity.getEyeHeight() - 0.01F),
                aabb.minZ,
                aabb.maxX,
                (double)(pEntity.getEyeHeight() + 0.01F),
                aabb.maxZ,
                1.0F,
                0.0F,
                0.0F,
                1.0F
            );
        }

        Entity entity = pEntity.getVehicle();
        if (entity != null) {
            float f = Math.min(entity.getBbWidth(), pEntity.getBbWidth()) / 2.0F;
            float f2 = 0.0625F;
            Vec3 vec3 = entity.getPassengerRidingPosition(pEntity).subtract(pEntity.position());
            ShapeRenderer.renderLineBox(
                pPoseStack,
                pBuffer,
                vec3.x - (double)f,
                vec3.y,
                vec3.z - (double)f,
                vec3.x + (double)f,
                vec3.y + 0.0625,
                vec3.z + (double)f,
                1.0F,
                1.0F,
                0.0F,
                1.0F
            );
        }

        ShapeRenderer.renderVector(pPoseStack, pBuffer, new Vector3f(0.0F, pEntity.getEyeHeight(), 0.0F), pEntity.getViewVector(pRed).scale(2.0), -16776961);
    }

    private void renderFlame(PoseStack pPoseStack, MultiBufferSource pBufferSource, EntityRenderState pRenderState, Quaternionf pQuaternion) {
        TextureAtlasSprite textureatlassprite = ModelBakery.FIRE_0.sprite();
        TextureAtlasSprite textureatlassprite1 = ModelBakery.FIRE_1.sprite();
        pPoseStack.pushPose();
        float f = pRenderState.boundingBoxWidth * 1.4F;
        pPoseStack.scale(f, f, f);
        float f1 = 0.5F;
        float f2 = 0.0F;
        float f3 = pRenderState.boundingBoxHeight / f;
        float f4 = 0.0F;
        pPoseStack.mulPose(pQuaternion);
        pPoseStack.translate(0.0F, 0.0F, 0.3F - (float)((int)f3) * 0.02F);
        float f5 = 0.0F;
        int i = 0;
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(Sheets.cutoutBlockSheet());

        for (PoseStack.Pose posestack$pose = pPoseStack.last(); f3 > 0.0F; i++) {
            TextureAtlasSprite textureatlassprite2 = i % 2 == 0 ? textureatlassprite : textureatlassprite1;
            float f6 = textureatlassprite2.getU0();
            float f7 = textureatlassprite2.getV0();
            float f8 = textureatlassprite2.getU1();
            float f9 = textureatlassprite2.getV1();
            if (i / 2 % 2 == 0) {
                float f10 = f8;
                f8 = f6;
                f6 = f10;
            }

            fireVertex(posestack$pose, vertexconsumer, -f1 - 0.0F, 0.0F - f4, f5, f8, f9);
            fireVertex(posestack$pose, vertexconsumer, f1 - 0.0F, 0.0F - f4, f5, f6, f9);
            fireVertex(posestack$pose, vertexconsumer, f1 - 0.0F, 1.4F - f4, f5, f6, f7);
            fireVertex(posestack$pose, vertexconsumer, -f1 - 0.0F, 1.4F - f4, f5, f8, f7);
            f3 -= 0.45F;
            f4 -= 0.45F;
            f1 *= 0.9F;
            f5 -= 0.03F;
        }

        pPoseStack.popPose();
    }

    private static void fireVertex(
        PoseStack.Pose pMatrixEntry, VertexConsumer pBuffer, float pX, float pY, float pZ, float pTexU, float pTexV
    ) {
        pBuffer.addVertex(pMatrixEntry, pX, pY, pZ)
            .setColor(-1)
            .setUv(pTexU, pTexV)
            .setUv1(0, 10)
            .setLight(240)
            .setNormal(pMatrixEntry, 0.0F, 1.0F, 0.0F);
    }

    private static void renderShadow(
        PoseStack pPoseStack, MultiBufferSource pBufferSource, EntityRenderState pRenderState, float pShadowStrength, float pPartialTick, LevelReader pLevel, float pSize
    ) {
        float f = Math.min(pShadowStrength / 0.5F, pSize);
        int i = Mth.floor(pRenderState.x - (double)pSize);
        int j = Mth.floor(pRenderState.x + (double)pSize);
        int k = Mth.floor(pRenderState.y - (double)f);
        int l = Mth.floor(pRenderState.y);
        int i1 = Mth.floor(pRenderState.z - (double)pSize);
        int j1 = Mth.floor(pRenderState.z + (double)pSize);
        PoseStack.Pose posestack$pose = pPoseStack.last();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(SHADOW_RENDER_TYPE);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int k1 = i1; k1 <= j1; k1++) {
            for (int l1 = i; l1 <= j; l1++) {
                blockpos$mutableblockpos.set(l1, 0, k1);
                ChunkAccess chunkaccess = pLevel.getChunk(blockpos$mutableblockpos);

                for (int i2 = k; i2 <= l; i2++) {
                    blockpos$mutableblockpos.setY(i2);
                    float f1 = pShadowStrength - (float)(pRenderState.y - (double)blockpos$mutableblockpos.getY()) * 0.5F;
                    renderBlockShadow(
                        posestack$pose,
                        vertexconsumer,
                        chunkaccess,
                        pLevel,
                        blockpos$mutableblockpos,
                        pRenderState.x,
                        pRenderState.y,
                        pRenderState.z,
                        pSize,
                        f1
                    );
                }
            }
        }
    }

    private static void renderBlockShadow(
        PoseStack.Pose pPose,
        VertexConsumer pBuffer,
        ChunkAccess pChunk,
        LevelReader pLevel,
        BlockPos pPos,
        double pX,
        double pY,
        double pZ,
        float pSize,
        float pWeight
    ) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pChunk.getBlockState(blockpos);
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE && pLevel.getMaxLocalRawBrightness(pPos) > 3) {
            if (blockstate.isCollisionShapeFullBlock(pChunk, blockpos)) {
                VoxelShape voxelshape = blockstate.getShape(pChunk, blockpos);
                if (!voxelshape.isEmpty()) {
                    float f = LightTexture.getBrightness(pLevel.dimensionType(), pLevel.getMaxLocalRawBrightness(pPos));
                    float f1 = pWeight * 0.5F * f;
                    if (f1 >= 0.0F) {
                        if (f1 > 1.0F) {
                            f1 = 1.0F;
                        }

                        int i = ARGB.color(Mth.floor(f1 * 255.0F), 255, 255, 255);
                        AABB aabb = voxelshape.bounds();
                        double d0 = (double)pPos.getX() + aabb.minX;
                        double d1 = (double)pPos.getX() + aabb.maxX;
                        double d2 = (double)pPos.getY() + aabb.minY;
                        double d3 = (double)pPos.getZ() + aabb.minZ;
                        double d4 = (double)pPos.getZ() + aabb.maxZ;
                        float f2 = (float)(d0 - pX);
                        float f3 = (float)(d1 - pX);
                        float f4 = (float)(d2 - pY);
                        float f5 = (float)(d3 - pZ);
                        float f6 = (float)(d4 - pZ);
                        float f7 = -f2 / 2.0F / pSize + 0.5F;
                        float f8 = -f3 / 2.0F / pSize + 0.5F;
                        float f9 = -f5 / 2.0F / pSize + 0.5F;
                        float f10 = -f6 / 2.0F / pSize + 0.5F;
                        shadowVertex(pPose, pBuffer, i, f2, f4, f5, f7, f9);
                        shadowVertex(pPose, pBuffer, i, f2, f4, f6, f7, f10);
                        shadowVertex(pPose, pBuffer, i, f3, f4, f6, f8, f10);
                        shadowVertex(pPose, pBuffer, i, f3, f4, f5, f8, f9);
                    }
                }
            }
        }
    }

    private static void shadowVertex(
        PoseStack.Pose pPose, VertexConsumer pConsumer, int pColor, float pOffsetX, float pOffsetY, float pOffsetZ, float pU, float pV
    ) {
        Vector3f vector3f = pPose.pose().transformPosition(pOffsetX, pOffsetY, pOffsetZ, new Vector3f());
        pConsumer.addVertex(vector3f.x(), vector3f.y(), vector3f.z(), pColor, pU, pV, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
    }

    public void setLevel(@Nullable Level pLevel) {
        this.level = pLevel;
        if (pLevel == null) {
            this.camera = null;
        }
    }

    public double distanceToSqr(Entity pEntity) {
        return this.camera.getPosition().distanceToSqr(pEntity.position());
    }

    public double distanceToSqr(double pX, double pY, double pZ) {
        return this.camera.getPosition().distanceToSqr(pX, pY, pZ);
    }

    public Quaternionf cameraOrientation() {
        return this.cameraOrientation;
    }

    public ItemInHandRenderer getItemInHandRenderer() {
        return this.itemInHandRenderer;
    }

    public Map<PlayerSkin.Model, EntityRenderer<? extends Player, ?>> getSkinMap() {
        return java.util.Collections.unmodifiableMap(playerRenderers);
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_174004_) {
        EntityRendererProvider.Context entityrendererprovider$context = new EntityRendererProvider.Context(
            this, this.itemModelResolver, this.mapRenderer, this.blockRenderDispatcher, p_174004_, this.entityModels.get(), this.equipmentAssets, this.font
        );
        this.renderers = EntityRenderers.createEntityRenderers(entityrendererprovider$context);
        this.playerRenderers = EntityRenderers.createPlayerRenderers(entityrendererprovider$context);
        net.minecraftforge.client.event.ForgeEventFactoryClient.onGatherLayers(renderers, playerRenderers, entityrendererprovider$context);
    }
}

package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class MapItemSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAP_SIZE = 128;
    private static final int HALF_MAP_SIZE = 64;
    public static final int MAX_SCALE = 4;
    public static final int TRACKED_DECORATION_LIMIT = 256;
    private static final String FRAME_PREFIX = "frame-";
    public final int centerX;
    public final int centerZ;
    public final ResourceKey<Level> dimension;
    private final boolean trackingPosition;
    private final boolean unlimitedTracking;
    public final byte scale;
    public byte[] colors = new byte[16384];
    public final boolean locked;
    private final List<MapItemSavedData.HoldingPlayer> carriedBy = Lists.newArrayList();
    private final Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers = Maps.newHashMap();
    private final Map<String, MapBanner> bannerMarkers = Maps.newHashMap();
    final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
    private final Map<String, MapFrame> frameMarkers = Maps.newHashMap();
    private int trackedDecorationCount;

    public static SavedData.Factory<MapItemSavedData> factory() {
        return new SavedData.Factory<>(() -> {
            throw new IllegalStateException("Should never create an empty map saved data");
        }, MapItemSavedData::load, DataFixTypes.SAVED_DATA_MAP_DATA);
    }

    private MapItemSavedData(
        int pX, int pZ, byte pScale, boolean pTrackingPosition, boolean pUnlimitedTracking, boolean pLocked, ResourceKey<Level> pDimension
    ) {
        this.scale = pScale;
        this.centerX = pX;
        this.centerZ = pZ;
        this.dimension = pDimension;
        this.trackingPosition = pTrackingPosition;
        this.unlimitedTracking = pUnlimitedTracking;
        this.locked = pLocked;
    }

    public static MapItemSavedData createFresh(
        double pX, double pZ, byte pScale, boolean pTrackingPosition, boolean pUnlimitedTracking, ResourceKey<Level> pDimension
    ) {
        int i = 128 * (1 << pScale);
        int j = Mth.floor((pX + 64.0) / (double)i);
        int k = Mth.floor((pZ + 64.0) / (double)i);
        int l = j * i + i / 2 - 64;
        int i1 = k * i + i / 2 - 64;
        return new MapItemSavedData(l, i1, pScale, pTrackingPosition, pUnlimitedTracking, false, pDimension);
    }

    public static MapItemSavedData createForClient(byte pScale, boolean pLocked, ResourceKey<Level> pDimension) {
        return new MapItemSavedData(0, 0, pScale, false, false, pLocked, pDimension);
    }

    public static MapItemSavedData load(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        ResourceKey<Level> resourcekey = DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, pTag.get("dimension")))
            .resultOrPartial(LOGGER::error)
            .orElseThrow(() -> new IllegalArgumentException("Invalid map dimension: " + pTag.get("dimension")));
        int i = pTag.getInt("xCenter");
        int j = pTag.getInt("zCenter");
        byte b0 = (byte)Mth.clamp(pTag.getByte("scale"), 0, 4);
        boolean flag = !pTag.contains("trackingPosition", 1) || pTag.getBoolean("trackingPosition");
        boolean flag1 = pTag.getBoolean("unlimitedTracking");
        boolean flag2 = pTag.getBoolean("locked");
        MapItemSavedData mapitemsaveddata = new MapItemSavedData(i, j, b0, flag, flag1, flag2, resourcekey);
        byte[] abyte = pTag.getByteArray("colors");
        if (abyte.length == 16384) {
            mapitemsaveddata.colors = abyte;
        }

        RegistryOps<Tag> registryops = pLevelRegistry.createSerializationContext(NbtOps.INSTANCE);

        for (MapBanner mapbanner : MapBanner.LIST_CODEC
            .parse(registryops, pTag.get("banners"))
            .resultOrPartial(p_327533_ -> LOGGER.warn("Failed to parse map banner: '{}'", p_327533_))
            .orElse(List.of())) {
            mapitemsaveddata.bannerMarkers.put(mapbanner.getId(), mapbanner);
            mapitemsaveddata.addDecoration(
                mapbanner.getDecoration(),
                null,
                mapbanner.getId(),
                (double)mapbanner.pos().getX(),
                (double)mapbanner.pos().getZ(),
                180.0,
                mapbanner.name().orElse(null)
            );
        }

        ListTag listtag = pTag.getList("frames", 10);

        for (int k = 0; k < listtag.size(); k++) {
            MapFrame mapframe = MapFrame.load(listtag.getCompound(k));
            if (mapframe != null) {
                mapitemsaveddata.frameMarkers.put(mapframe.getId(), mapframe);
                mapitemsaveddata.addDecoration(
                    MapDecorationTypes.FRAME,
                    null,
                    getFrameKey(mapframe.getEntityId()),
                    (double)mapframe.getPos().getX(),
                    (double)mapframe.getPos().getZ(),
                    (double)mapframe.getRotation(),
                    null
                );
            }
        }

        return mapitemsaveddata;
    }

    @Override
    public CompoundTag save(CompoundTag p_77956_, HolderLookup.Provider p_327825_) {
        ResourceLocation.CODEC
            .encodeStart(NbtOps.INSTANCE, this.dimension.location())
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_77954_ -> p_77956_.put("dimension", p_77954_));
        p_77956_.putInt("xCenter", this.centerX);
        p_77956_.putInt("zCenter", this.centerZ);
        p_77956_.putByte("scale", this.scale);
        p_77956_.putByteArray("colors", this.colors);
        p_77956_.putBoolean("trackingPosition", this.trackingPosition);
        p_77956_.putBoolean("unlimitedTracking", this.unlimitedTracking);
        p_77956_.putBoolean("locked", this.locked);
        RegistryOps<Tag> registryops = p_327825_.createSerializationContext(NbtOps.INSTANCE);
        p_77956_.put("banners", MapBanner.LIST_CODEC.encodeStart(registryops, List.copyOf(this.bannerMarkers.values())).getOrThrow());
        ListTag listtag = new ListTag();

        for (MapFrame mapframe : this.frameMarkers.values()) {
            listtag.add(mapframe.save());
        }

        p_77956_.put("frames", listtag);
        return p_77956_;
    }

    public MapItemSavedData locked() {
        MapItemSavedData mapitemsaveddata = new MapItemSavedData(
            this.centerX, this.centerZ, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension
        );
        mapitemsaveddata.bannerMarkers.putAll(this.bannerMarkers);
        mapitemsaveddata.decorations.putAll(this.decorations);
        mapitemsaveddata.trackedDecorationCount = this.trackedDecorationCount;
        System.arraycopy(this.colors, 0, mapitemsaveddata.colors, 0, this.colors.length);
        return mapitemsaveddata;
    }

    public MapItemSavedData scaled() {
        return createFresh(
            (double)this.centerX, (double)this.centerZ, (byte)Mth.clamp(this.scale + 1, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension
        );
    }

    private static Predicate<ItemStack> mapMatcher(ItemStack pStack) {
        MapId mapid = pStack.get(DataComponents.MAP_ID);
        return p_327526_ -> p_327526_ == pStack
                ? true
                : p_327526_.is(pStack.getItem()) && Objects.equals(mapid, p_327526_.get(DataComponents.MAP_ID));
    }

    public void tickCarriedBy(Player pPlayer, ItemStack pMapStack) {
        if (!this.carriedByPlayers.containsKey(pPlayer)) {
            MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = new MapItemSavedData.HoldingPlayer(pPlayer);
            this.carriedByPlayers.put(pPlayer, mapitemsaveddata$holdingplayer);
            this.carriedBy.add(mapitemsaveddata$holdingplayer);
        }

        Predicate<ItemStack> predicate = mapMatcher(pMapStack);
        if (!pPlayer.getInventory().contains(predicate)) {
            this.removeDecoration(pPlayer.getName().getString());
        }

        for (int i = 0; i < this.carriedBy.size(); i++) {
            MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer1 = this.carriedBy.get(i);
            Player player = mapitemsaveddata$holdingplayer1.player;
            String s = player.getName().getString();
            if (!player.isRemoved() && (player.getInventory().contains(predicate) || pMapStack.isFramed())) {
                if (!pMapStack.isFramed() && player.level().dimension() == this.dimension && this.trackingPosition) {
                    this.addDecoration(MapDecorationTypes.PLAYER, player.level(), s, player.getX(), player.getZ(), (double)player.getYRot(), null);
                }
            } else {
                this.carriedByPlayers.remove(player);
                this.carriedBy.remove(mapitemsaveddata$holdingplayer1);
                this.removeDecoration(s);
            }

            if (!player.equals(pPlayer) && hasMapInvisibilityItemEquipped(player)) {
                this.removeDecoration(s);
            }
        }

        if (pMapStack.isFramed() && this.trackingPosition) {
            ItemFrame itemframe = pMapStack.getFrame();
            BlockPos blockpos = itemframe.getPos();
            MapFrame mapframe = this.frameMarkers.get(MapFrame.frameId(blockpos));
            if (mapframe != null && itemframe.getId() != mapframe.getEntityId() && this.frameMarkers.containsKey(mapframe.getId())) {
                this.removeDecoration(getFrameKey(mapframe.getEntityId()));
            }

            MapFrame mapframe1 = new MapFrame(blockpos, itemframe.getDirection().get2DDataValue() * 90, itemframe.getId());
            this.addDecoration(
                MapDecorationTypes.FRAME,
                pPlayer.level(),
                getFrameKey(itemframe.getId()),
                (double)blockpos.getX(),
                (double)blockpos.getZ(),
                (double)(itemframe.getDirection().get2DDataValue() * 90),
                null
            );
            this.frameMarkers.put(mapframe1.getId(), mapframe1);
        }

        MapDecorations mapdecorations = pMapStack.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
        if (!this.decorations.keySet().containsAll(mapdecorations.decorations().keySet())) {
            mapdecorations.decorations()
                .forEach(
                    (p_375362_, p_375363_) -> {
                        if (!this.decorations.containsKey(p_375362_)) {
                            this.addDecoration(
                                p_375363_.type(),
                                pPlayer.level(),
                                p_375362_,
                                p_375363_.x(),
                                p_375363_.z(),
                                (double)p_375363_.rotation(),
                                null
                            );
                        }
                    }
                );
        }
    }

    private static boolean hasMapInvisibilityItemEquipped(Player pPlayer) {
        for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
            if (equipmentslot != EquipmentSlot.MAINHAND
                && equipmentslot != EquipmentSlot.OFFHAND
                && pPlayer.getItemBySlot(equipmentslot).is(ItemTags.MAP_INVISIBILITY_EQUIPMENT)) {
                return true;
            }
        }

        return false;
    }

    private void removeDecoration(String pIdentifier) {
        MapDecoration mapdecoration = this.decorations.remove(pIdentifier);
        if (mapdecoration != null && mapdecoration.type().value().trackCount()) {
            this.trackedDecorationCount--;
        }

        this.setDecorationsDirty();
    }

    public static void addTargetDecoration(ItemStack pStack, BlockPos pPos, String pType, Holder<MapDecorationType> pMapDecorationType) {
        MapDecorations.Entry mapdecorations$entry = new MapDecorations.Entry(pMapDecorationType, (double)pPos.getX(), (double)pPos.getZ(), 180.0F);
        pStack.update(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY, p_327532_ -> p_327532_.withDecoration(pType, mapdecorations$entry));
        if (pMapDecorationType.value().hasMapColor()) {
            pStack.set(DataComponents.MAP_COLOR, new MapItemColor(pMapDecorationType.value().mapColor()));
        }
    }

    private void addDecoration(
        Holder<MapDecorationType> pDecorationType,
        @Nullable LevelAccessor pLevel,
        String pId,
        double pX,
        double pZ,
        double pYRot,
        @Nullable Component pDisplayName
    ) {
        int i = 1 << this.scale;
        float f = (float)(pX - (double)this.centerX) / (float)i;
        float f1 = (float)(pZ - (double)this.centerZ) / (float)i;
        MapItemSavedData.MapDecorationLocation mapitemsaveddata$mapdecorationlocation = this.calculateDecorationLocationAndType(pDecorationType, pLevel, pYRot, f, f1);
        if (mapitemsaveddata$mapdecorationlocation == null) {
            this.removeDecoration(pId);
        } else {
            MapDecoration mapdecoration = new MapDecoration(
                mapitemsaveddata$mapdecorationlocation.type(),
                mapitemsaveddata$mapdecorationlocation.x(),
                mapitemsaveddata$mapdecorationlocation.y(),
                mapitemsaveddata$mapdecorationlocation.rot(),
                Optional.ofNullable(pDisplayName)
            );
            MapDecoration mapdecoration1 = this.decorations.put(pId, mapdecoration);
            if (!mapdecoration.equals(mapdecoration1)) {
                if (mapdecoration1 != null && mapdecoration1.type().value().trackCount()) {
                    this.trackedDecorationCount--;
                }

                if (mapitemsaveddata$mapdecorationlocation.type().value().trackCount()) {
                    this.trackedDecorationCount++;
                }

                this.setDecorationsDirty();
            }
        }
    }

    @Nullable
    private MapItemSavedData.MapDecorationLocation calculateDecorationLocationAndType(
        Holder<MapDecorationType> pDecorationType, @Nullable LevelAccessor pLevel, double pYRot, float pX, float pZ
    ) {
        byte b0 = clampMapCoordinate(pX);
        byte b1 = clampMapCoordinate(pZ);
        if (pDecorationType.is(MapDecorationTypes.PLAYER)) {
            Pair<Holder<MapDecorationType>, Byte> pair = this.playerDecorationTypeAndRotation(pDecorationType, pLevel, pYRot, pX, pZ);
            return pair == null ? null : new MapItemSavedData.MapDecorationLocation(pair.getFirst(), b0, b1, pair.getSecond());
        } else {
            return !isInsideMap(pX, pZ) && !this.unlimitedTracking
                ? null
                : new MapItemSavedData.MapDecorationLocation(pDecorationType, b0, b1, this.calculateRotation(pLevel, pYRot));
        }
    }

    @Nullable
    private Pair<Holder<MapDecorationType>, Byte> playerDecorationTypeAndRotation(
        Holder<MapDecorationType> pDecorationType, @Nullable LevelAccessor pLevel, double pYRot, float pX, float pZ
    ) {
        if (isInsideMap(pX, pZ)) {
            return Pair.of(pDecorationType, this.calculateRotation(pLevel, pYRot));
        } else {
            Holder<MapDecorationType> holder = this.decorationTypeForPlayerOutsideMap(pX, pZ);
            return holder == null ? null : Pair.of(holder, (byte)0);
        }
    }

    private byte calculateRotation(@Nullable LevelAccessor pLevel, double pYRot) {
        if (this.dimension == Level.NETHER && pLevel != null) {
            int i = (int)(pLevel.getLevelData().getDayTime() / 10L);
            return (byte)(i * i * 34187121 + i * 121 >> 15 & 15);
        } else {
            double d0 = pYRot < 0.0 ? pYRot - 8.0 : pYRot + 8.0;
            return (byte)((int)(d0 * 16.0 / 360.0));
        }
    }

    private static boolean isInsideMap(float pX, float pZ) {
        int i = 63;
        return pX >= -63.0F && pZ >= -63.0F && pX <= 63.0F && pZ <= 63.0F;
    }

    @Nullable
    private Holder<MapDecorationType> decorationTypeForPlayerOutsideMap(float pX, float pZ) {
        int i = 320;
        boolean flag = Math.abs(pX) < 320.0F && Math.abs(pZ) < 320.0F;
        if (flag) {
            return MapDecorationTypes.PLAYER_OFF_MAP;
        } else {
            return this.unlimitedTracking ? MapDecorationTypes.PLAYER_OFF_LIMITS : null;
        }
    }

    private static byte clampMapCoordinate(float pCoord) {
        int i = 63;
        if (pCoord <= -63.0F) {
            return -128;
        } else {
            return pCoord >= 63.0F ? 127 : (byte)((int)((double)(pCoord * 2.0F) + 0.5));
        }
    }

    @Nullable
    public Packet<?> getUpdatePacket(MapId pMapId, Player pPlayer) {
        MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = this.carriedByPlayers.get(pPlayer);
        return mapitemsaveddata$holdingplayer == null ? null : mapitemsaveddata$holdingplayer.nextUpdatePacket(pMapId);
    }

    private void setColorsDirty(int pX, int pZ) {
        this.setDirty();

        for (MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer : this.carriedBy) {
            mapitemsaveddata$holdingplayer.markColorsDirty(pX, pZ);
        }
    }

    private void setDecorationsDirty() {
        this.setDirty();
        this.carriedBy.forEach(MapItemSavedData.HoldingPlayer::markDecorationsDirty);
    }

    public MapItemSavedData.HoldingPlayer getHoldingPlayer(Player pPlayer) {
        MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = this.carriedByPlayers.get(pPlayer);
        if (mapitemsaveddata$holdingplayer == null) {
            mapitemsaveddata$holdingplayer = new MapItemSavedData.HoldingPlayer(pPlayer);
            this.carriedByPlayers.put(pPlayer, mapitemsaveddata$holdingplayer);
            this.carriedBy.add(mapitemsaveddata$holdingplayer);
        }

        return mapitemsaveddata$holdingplayer;
    }

    public boolean toggleBanner(LevelAccessor pAccessor, BlockPos pPos) {
        double d0 = (double)pPos.getX() + 0.5;
        double d1 = (double)pPos.getZ() + 0.5;
        int i = 1 << this.scale;
        double d2 = (d0 - (double)this.centerX) / (double)i;
        double d3 = (d1 - (double)this.centerZ) / (double)i;
        int j = 63;
        if (d2 >= -63.0 && d3 >= -63.0 && d2 <= 63.0 && d3 <= 63.0) {
            MapBanner mapbanner = MapBanner.fromWorld(pAccessor, pPos);
            if (mapbanner == null) {
                return false;
            }

            if (this.bannerMarkers.remove(mapbanner.getId(), mapbanner)) {
                this.removeDecoration(mapbanner.getId());
                return true;
            }

            if (!this.isTrackedCountOverLimit(256)) {
                this.bannerMarkers.put(mapbanner.getId(), mapbanner);
                this.addDecoration(mapbanner.getDecoration(), pAccessor, mapbanner.getId(), d0, d1, 180.0, mapbanner.name().orElse(null));
                return true;
            }
        }

        return false;
    }

    public void checkBanners(BlockGetter pReader, int pX, int pZ) {
        Iterator<MapBanner> iterator = this.bannerMarkers.values().iterator();

        while (iterator.hasNext()) {
            MapBanner mapbanner = iterator.next();
            if (mapbanner.pos().getX() == pX && mapbanner.pos().getZ() == pZ) {
                MapBanner mapbanner1 = MapBanner.fromWorld(pReader, mapbanner.pos());
                if (!mapbanner.equals(mapbanner1)) {
                    iterator.remove();
                    this.removeDecoration(mapbanner.getId());
                }
            }
        }
    }

    public Collection<MapBanner> getBanners() {
        return this.bannerMarkers.values();
    }

    public void removedFromFrame(BlockPos pPos, int pEntityId) {
        this.removeDecoration(getFrameKey(pEntityId));
        this.frameMarkers.remove(MapFrame.frameId(pPos));
        this.setDirty();
    }

    public boolean updateColor(int pX, int pZ, byte pColor) {
        byte b0 = this.colors[pX + pZ * 128];
        if (b0 != pColor) {
            this.setColor(pX, pZ, pColor);
            return true;
        } else {
            return false;
        }
    }

    public void setColor(int pX, int pZ, byte pColor) {
        this.colors[pX + pZ * 128] = pColor;
        this.setColorsDirty(pX, pZ);
    }

    public boolean isExplorationMap() {
        for (MapDecoration mapdecoration : this.decorations.values()) {
            if (mapdecoration.type().value().explorationMapElement()) {
                return true;
            }
        }

        return false;
    }

    public void addClientSideDecorations(List<MapDecoration> pDecorations) {
        this.decorations.clear();
        this.trackedDecorationCount = 0;

        for (int i = 0; i < pDecorations.size(); i++) {
            MapDecoration mapdecoration = pDecorations.get(i);
            this.decorations.put("icon-" + i, mapdecoration);
            if (mapdecoration.type().value().trackCount()) {
                this.trackedDecorationCount++;
            }
        }
    }

    public Iterable<MapDecoration> getDecorations() {
        return this.decorations.values();
    }

    public boolean isTrackedCountOverLimit(int pTrackedCount) {
        return this.trackedDecorationCount >= pTrackedCount;
    }

    private static String getFrameKey(int pEntityId) {
        return "frame-" + pEntityId;
    }

    public class HoldingPlayer {
        public final Player player;
        private boolean dirtyData = true;
        private int minDirtyX;
        private int minDirtyY;
        private int maxDirtyX = 127;
        private int maxDirtyY = 127;
        private boolean dirtyDecorations = true;
        private int tick;
        public int step;

        HoldingPlayer(final Player pPlayer) {
            this.player = pPlayer;
        }

        private MapItemSavedData.MapPatch createPatch() {
            int i = this.minDirtyX;
            int j = this.minDirtyY;
            int k = this.maxDirtyX + 1 - this.minDirtyX;
            int l = this.maxDirtyY + 1 - this.minDirtyY;
            byte[] abyte = new byte[k * l];

            for (int i1 = 0; i1 < k; i1++) {
                for (int j1 = 0; j1 < l; j1++) {
                    abyte[i1 + j1 * k] = MapItemSavedData.this.colors[i + i1 + (j + j1) * 128];
                }
            }

            return new MapItemSavedData.MapPatch(i, j, k, l, abyte);
        }

        @Nullable
        Packet<?> nextUpdatePacket(MapId pMapId) {
            MapItemSavedData.MapPatch mapitemsaveddata$mappatch;
            if (this.dirtyData) {
                this.dirtyData = false;
                mapitemsaveddata$mappatch = this.createPatch();
            } else {
                mapitemsaveddata$mappatch = null;
            }

            Collection<MapDecoration> collection;
            if (this.dirtyDecorations && this.tick++ % 5 == 0) {
                this.dirtyDecorations = false;
                collection = MapItemSavedData.this.decorations.values();
            } else {
                collection = null;
            }

            return collection == null && mapitemsaveddata$mappatch == null
                ? null
                : new ClientboundMapItemDataPacket(
                    pMapId, MapItemSavedData.this.scale, MapItemSavedData.this.locked, collection, mapitemsaveddata$mappatch
                );
        }

        void markColorsDirty(int pX, int pZ) {
            if (this.dirtyData) {
                this.minDirtyX = Math.min(this.minDirtyX, pX);
                this.minDirtyY = Math.min(this.minDirtyY, pZ);
                this.maxDirtyX = Math.max(this.maxDirtyX, pX);
                this.maxDirtyY = Math.max(this.maxDirtyY, pZ);
            } else {
                this.dirtyData = true;
                this.minDirtyX = pX;
                this.minDirtyY = pZ;
                this.maxDirtyX = pX;
                this.maxDirtyY = pZ;
            }
        }

        private void markDecorationsDirty() {
            this.dirtyDecorations = true;
        }
    }

    static record MapDecorationLocation(Holder<MapDecorationType> type, byte x, byte y, byte rot) {
    }

    public static record MapPatch(int startX, int startY, int width, int height, byte[] mapColors) {
        public static final StreamCodec<ByteBuf, Optional<MapItemSavedData.MapPatch>> STREAM_CODEC = StreamCodec.of(
            MapItemSavedData.MapPatch::write, MapItemSavedData.MapPatch::read
        );

        private static void write(ByteBuf pBuffer, Optional<MapItemSavedData.MapPatch> pMapPatch) {
            if (pMapPatch.isPresent()) {
                MapItemSavedData.MapPatch mapitemsaveddata$mappatch = pMapPatch.get();
                pBuffer.writeByte(mapitemsaveddata$mappatch.width);
                pBuffer.writeByte(mapitemsaveddata$mappatch.height);
                pBuffer.writeByte(mapitemsaveddata$mappatch.startX);
                pBuffer.writeByte(mapitemsaveddata$mappatch.startY);
                FriendlyByteBuf.writeByteArray(pBuffer, mapitemsaveddata$mappatch.mapColors);
            } else {
                pBuffer.writeByte(0);
            }
        }

        private static Optional<MapItemSavedData.MapPatch> read(ByteBuf pBuffer) {
            int i = pBuffer.readUnsignedByte();
            if (i > 0) {
                int j = pBuffer.readUnsignedByte();
                int k = pBuffer.readUnsignedByte();
                int l = pBuffer.readUnsignedByte();
                byte[] abyte = FriendlyByteBuf.readByteArray(pBuffer);
                return Optional.of(new MapItemSavedData.MapPatch(k, l, i, j, abyte));
            } else {
                return Optional.empty();
            }
        }

        public void applyToMap(MapItemSavedData pSavedData) {
            for (int i = 0; i < this.width; i++) {
                for (int j = 0; j < this.height; j++) {
                    pSavedData.setColor(this.startX + i, this.startY + j, this.mapColors[i + j * this.width]);
                }
            }
        }
    }
}
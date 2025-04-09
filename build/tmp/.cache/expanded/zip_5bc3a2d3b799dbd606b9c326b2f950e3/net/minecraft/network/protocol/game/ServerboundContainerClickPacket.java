package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class ServerboundContainerClickPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundContainerClickPacket> STREAM_CODEC = Packet.codec(
        ServerboundContainerClickPacket::write, ServerboundContainerClickPacket::new
    );
    private static final int MAX_SLOT_COUNT = 128;
    private static final StreamCodec<RegistryFriendlyByteBuf, Int2ObjectMap<ItemStack>> SLOTS_STREAM_CODEC = ByteBufCodecs.map(
        Int2ObjectOpenHashMap::new, ByteBufCodecs.SHORT.map(Short::intValue, Integer::shortValue), ItemStack.OPTIONAL_STREAM_CODEC, 128
    );
    private final int containerId;
    private final int stateId;
    private final int slotNum;
    private final int buttonNum;
    private final ClickType clickType;
    private final ItemStack carriedItem;
    private final Int2ObjectMap<ItemStack> changedSlots;

    public ServerboundContainerClickPacket(
        int pContainerId, int pStateId, int pSlotNum, int pButtonNum, ClickType pClickType, ItemStack pCarriedItem, Int2ObjectMap<ItemStack> pChangedSlots
    ) {
        this.containerId = pContainerId;
        this.stateId = pStateId;
        this.slotNum = pSlotNum;
        this.buttonNum = pButtonNum;
        this.clickType = pClickType;
        this.carriedItem = pCarriedItem;
        this.changedSlots = Int2ObjectMaps.unmodifiable(pChangedSlots);
    }

    private ServerboundContainerClickPacket(RegistryFriendlyByteBuf pBuffer) {
        this.containerId = pBuffer.readContainerId();
        this.stateId = pBuffer.readVarInt();
        this.slotNum = pBuffer.readShort();
        this.buttonNum = pBuffer.readByte();
        this.clickType = pBuffer.readEnum(ClickType.class);
        this.changedSlots = Int2ObjectMaps.unmodifiable(SLOTS_STREAM_CODEC.decode(pBuffer));
        this.carriedItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(pBuffer);
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeContainerId(this.containerId);
        pBuffer.writeVarInt(this.stateId);
        pBuffer.writeShort(this.slotNum);
        pBuffer.writeByte(this.buttonNum);
        pBuffer.writeEnum(this.clickType);
        SLOTS_STREAM_CODEC.encode(pBuffer, this.changedSlots);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(pBuffer, this.carriedItem);
    }

    @Override
    public PacketType<ServerboundContainerClickPacket> type() {
        return GamePacketTypes.SERVERBOUND_CONTAINER_CLICK;
    }

    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleContainerClick(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getSlotNum() {
        return this.slotNum;
    }

    public int getButtonNum() {
        return this.buttonNum;
    }

    public ItemStack getCarriedItem() {
        return this.carriedItem;
    }

    public Int2ObjectMap<ItemStack> getChangedSlots() {
        return this.changedSlots;
    }

    public ClickType getClickType() {
        return this.clickType;
    }

    public int getStateId() {
        return this.stateId;
    }
}
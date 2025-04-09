package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.ItemStack;

public class ClientboundContainerSetContentPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundContainerSetContentPacket> STREAM_CODEC = Packet.codec(
        ClientboundContainerSetContentPacket::write, ClientboundContainerSetContentPacket::new
    );
    private final int containerId;
    private final int stateId;
    private final List<ItemStack> items;
    private final ItemStack carriedItem;

    public ClientboundContainerSetContentPacket(int pContainerId, int pStateId, NonNullList<ItemStack> pItems, ItemStack pCarriedItem) {
        this.containerId = pContainerId;
        this.stateId = pStateId;
        this.items = NonNullList.withSize(pItems.size(), ItemStack.EMPTY);

        for (int i = 0; i < pItems.size(); i++) {
            this.items.set(i, pItems.get(i).copy());
        }

        this.carriedItem = pCarriedItem.copy();
    }

    private ClientboundContainerSetContentPacket(RegistryFriendlyByteBuf pBuffer) {
        this.containerId = pBuffer.readContainerId();
        this.stateId = pBuffer.readVarInt();
        this.items = ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(pBuffer);
        this.carriedItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(pBuffer);
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeContainerId(this.containerId);
        pBuffer.writeVarInt(this.stateId);
        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(pBuffer, this.items);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(pBuffer, this.carriedItem);
    }

    @Override
    public PacketType<ClientboundContainerSetContentPacket> type() {
        return GamePacketTypes.CLIENTBOUND_CONTAINER_SET_CONTENT;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleContainerContent(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public List<ItemStack> getItems() {
        return this.items;
    }

    public ItemStack getCarriedItem() {
        return this.carriedItem;
    }

    public int getStateId() {
        return this.stateId;
    }
}
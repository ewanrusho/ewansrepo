package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddExperienceOrbPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundAddExperienceOrbPacket> STREAM_CODEC = Packet.codec(
        ClientboundAddExperienceOrbPacket::write, ClientboundAddExperienceOrbPacket::new
    );
    private final int id;
    private final double x;
    private final double y;
    private final double z;
    private final int value;

    public ClientboundAddExperienceOrbPacket(ExperienceOrb pOrb, ServerEntity pEntity) {
        this.id = pOrb.getId();
        Vec3 vec3 = pEntity.getPositionBase();
        this.x = vec3.x();
        this.y = vec3.y();
        this.z = vec3.z();
        this.value = pOrb.getValue();
    }

    private ClientboundAddExperienceOrbPacket(FriendlyByteBuf pBuffer) {
        this.id = pBuffer.readVarInt();
        this.x = pBuffer.readDouble();
        this.y = pBuffer.readDouble();
        this.z = pBuffer.readDouble();
        this.value = pBuffer.readShort();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.id);
        pBuffer.writeDouble(this.x);
        pBuffer.writeDouble(this.y);
        pBuffer.writeDouble(this.z);
        pBuffer.writeShort(this.value);
    }

    @Override
    public PacketType<ClientboundAddExperienceOrbPacket> type() {
        return GamePacketTypes.CLIENTBOUND_ADD_EXPERIENCE_ORB;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleAddExperienceOrb(this);
    }

    public int getId() {
        return this.id;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public int getValue() {
        return this.value;
    }
}
package gg.aquatic.aquaticseries.nms.v1_18_2;

import net.minecraft.network.protocol.Packet;

public class ProtectedPacket {

    private final Packet<?> packet;

    public ProtectedPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}

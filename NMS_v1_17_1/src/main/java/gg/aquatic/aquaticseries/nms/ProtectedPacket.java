package gg.aquatic.aquaticseries.nms;

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

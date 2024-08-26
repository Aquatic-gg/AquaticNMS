package gg.aquatic.aquaticseries.nms.v1_17_1.listener;

import gg.aquatic.aquaticseries.lib.nms.listener.AbstractPacketListener;
import gg.aquatic.aquaticseries.lib.nms.listener.PacketEvent;
import gg.aquatic.aquaticseries.lib.nms.listener.PacketListenerAdapter;
import gg.aquatic.aquaticseries.lib.nms.packet.WrappedClientboundContainerSetContentPacket;
import gg.aquatic.aquaticseries.lib.nms.packet.WrappedClientboundContainerSetSlotPacket;
import gg.aquatic.aquaticseries.lib.nms.packet.WrappedClientboundOpenScreenPacket;
import gg.aquatic.aquaticseries.nms.v1_17_1.NMS_1_17_1;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketListenerAdapterImpl implements PacketListenerAdapter {
    private UUID uuid = UUID.randomUUID();
    private NMS_1_17_1 nms;
    public PacketListenerAdapterImpl(NMS_1_17_1 nms) {
        this.nms = nms;
    }
    public NMS_1_17_1 getNms() {
        return nms;
    }
    private List<AbstractPacketListener> listeners = new ArrayList<>();

    @Override
    public void inject(Player player) {
        var craftPlayer = (CraftPlayer) player;
        var packetListener = craftPlayer.getHandle();

        var connection = packetListener.connection.connection;
        var pipeline = connection.channel.pipeline();
        var listener = new AquaticPacketListener(player, this);
        for (String ignored : pipeline.toMap().keySet()) {
            pipeline.addBefore("packet_handler", "AquaticPacketListener" + uuid.toString(), listener);
            break;
        }
    }

    @Override
    public void eject(Player player) {
        var craftPlayer = (CraftPlayer) player;
        var packetListener = craftPlayer.getHandle();
        var connection = packetListener.connection.connection;
        var channel = connection.channel;
        try {
            if (channel.pipeline().names().contains("AquaticPacketListener" + uuid.toString())) {
                channel.pipeline().remove("AquaticPacketListener" + uuid.toString());
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void registerListener(AbstractPacketListener abstractPacketListener) {
        listeners.add(abstractPacketListener);
    }

    public void onPacketEvent(PacketEvent<?> event) {
        var packet = event.getPacket();
        if (packet instanceof WrappedClientboundContainerSetContentPacket) {
            for (AbstractPacketListener listener : listeners) {
                listener.onClientboundContainerSetContentPacket((PacketEvent<WrappedClientboundContainerSetContentPacket>) event);
            }
        } else if (packet instanceof WrappedClientboundOpenScreenPacket) {
            for (AbstractPacketListener listener : listeners) {
                listener.onClientboundOpenScreenPacket((PacketEvent<WrappedClientboundOpenScreenPacket>) event);
            }
        } else if (packet instanceof WrappedClientboundContainerSetSlotPacket) {
            for (AbstractPacketListener listener : listeners) {
                listener.onClientboundContainerSetSlotPacket((PacketEvent<WrappedClientboundContainerSetSlotPacket>) event);
            }
        }
    }
}

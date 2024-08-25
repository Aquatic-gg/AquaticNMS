package gg.aquatic.aquaticseries.nms.v1_19_4.listener;

import gg.aquatic.aquaticseries.lib.nms.listener.AbstractPacketListener;
import gg.aquatic.aquaticseries.lib.nms.listener.PacketEvent;
import gg.aquatic.aquaticseries.lib.nms.listener.PacketListenerAdapter;
import gg.aquatic.aquaticseries.lib.nms.packet.WrappedClientboundContainerSetContentPacket;
import gg.aquatic.aquaticseries.lib.nms.packet.WrappedClientboundContainerSetSlotPacket;
import gg.aquatic.aquaticseries.lib.nms.packet.WrappedClientboundOpenScreenPacket;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketListenerAdapterImpl implements PacketListenerAdapter {
    private UUID uuid = UUID.randomUUID();
    private List<AbstractPacketListener> listeners = new ArrayList<>();

    @Override
    public void inject(Player player) {
        var craftPlayer = (CraftPlayer) player;
        var packetListener = craftPlayer.getHandle();

        var connection = getConnection(packetListener.connection);
        var pipeline = connection.channel.pipeline();
        var listener = new AquaticPacketListener(player,this);
        for (String ignored : pipeline.toMap().keySet()) {
            pipeline.addBefore("packet_handler", "AquaticPacketListener" + uuid.toString(), listener);
            break;
        }
    }

    @Override
    public void eject(Player player) {
        var craftPlayer = (CraftPlayer) player;
        var packetListener = craftPlayer.getHandle();
        var connection = getConnection(packetListener.connection);
        var channel = connection.channel;
        try {
            if (channel.pipeline().names().contains("AquaticPacketListener" + uuid.toString())) {
                channel.pipeline().remove("AquaticPacketListener" + uuid.toString());
            }
        } catch (Exception ignored) {

        }
    }

    private Field connectionField;

    private Connection getConnection(final ServerGamePacketListenerImpl playerConnection) {
        try {
            if (connectionField == null) {
                for (Field declaredField : ServerGamePacketListenerImpl.class.getDeclaredFields()) {
                    if (declaredField.getType().equals(Connection.class)) {
                        connectionField = declaredField;
                        connectionField.setAccessible(true);
                        break;
                    }
                }
            }
            if (connectionField == null) {
                throw new Exception("Could not find connection field in ServerGamePacketListenerImpl");
            }

            return (Connection) connectionField.get(playerConnection);
        } catch (final Exception e) {
            throw new RuntimeException(e);
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

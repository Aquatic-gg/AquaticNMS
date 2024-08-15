package gg.aquatic.aquaticseries.nms.v1_21.listener;

import gg.aquatic.aquaticseries.lib.nms.PacketListenerAdapter;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.UUID;

public class PacketListenerAdapterImpl implements PacketListenerAdapter {
    private UUID uuid = UUID.randomUUID();

    @Override
    public void inject(Player player) {
        var craftPlayer = (CraftPlayer) player;
        var packetListener = craftPlayer.getHandle();

        var connection = getConnection(packetListener.connection);
        var pipeline = connection.channel.pipeline();
        var listener = new AquaticPacketListener(player);
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
                for (Field declaredField : ServerCommonPacketListenerImpl.class.getDeclaredFields()) {
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
}

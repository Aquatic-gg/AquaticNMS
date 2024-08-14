package gg.aquatic.aquaticseries.nms.v1_20_4.menu;

import gg.aquatic.aquaticseries.lib.nms.InventoryAdapter;
import gg.aquatic.aquaticseries.nms.v1_20_4.listener.MenuPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.UUID;

public class InventoryAdapterImpl implements InventoryAdapter {

    private UUID uuid = UUID.randomUUID();

    @Override
    public void injectPlayer(Player player) {
        var craftPlayer = (CraftPlayer) player;
        var packetListener = craftPlayer.getHandle();

        var connection = getConnection(packetListener.connection);
        var pipeline = connection.channel.pipeline();
        var listener = new MenuPacketListener(player);
        for (String ignored : pipeline.toMap().keySet()) {
            pipeline.addBefore("packet_handler", "InventoryLibListener" + uuid.toString(), listener);
            break;
        }
    }

    @Override
    public void ejectPlayer(Player player) {
        var craftPlayer = (CraftPlayer) player;
        var packetListener = craftPlayer.getHandle();
        var connection = getConnection(packetListener.connection);
        var channel = connection.channel;
        try {
            if (channel.pipeline().names().contains("InventoryLibListener" + uuid.toString())) {
                channel.pipeline().remove("InventoryLibListener" + uuid.toString());
            }
        } catch (Exception ignored) {

        }
    }

    private Field connectionField;

    private Connection getConnection(final ServerGamePacketListenerImpl playerConnection) {
        try {
            if (connectionField == null) {
                connectionField = ServerGamePacketListenerImpl.class.getDeclaredField("h");
                connectionField.setAccessible(true);
            }
            return (Connection) connectionField.get(playerConnection);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}

package gg.aquatic.aquaticseries.nms.v1_17_1.menu;

import gg.aquatic.aquaticseries.lib.nms.InventoryAdapter;
import gg.aquatic.aquaticseries.nms.v1_17_1.listener.MenuPacketListener;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class InventoryAdapterImpl implements InventoryAdapter {

    private UUID uuid = UUID.randomUUID();

    @Override
    public void injectPlayer(Player player) {
        var craftPlayer = (org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer) player;
        var packetListener = craftPlayer.getHandle().connection;
        var connection = packetListener.connection;
        var pipeline = connection.channel.pipeline();
        var listener = new MenuPacketListener(player);
        for (String ignored : pipeline.toMap().keySet()) {
            pipeline.addBefore("packet_handler", "InventoryLibListener" + uuid.toString(), listener);
            break;
        }
    }

    @Override
    public void ejectPlayer(Player player) {
        var connection = ((CraftPlayer) player).getHandle().connection.connection;
        var channel = connection.channel;
        try {
            if (channel.pipeline().names().contains("InventoryLibListener" + uuid.toString())) {
                channel.pipeline().remove("InventoryLibListener" + uuid.toString());
            }
        } catch (Exception ignored) {

        }
    }
}

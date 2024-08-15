package gg.aquatic.aquaticseries.nms.v1_17_1.listener;

import gg.aquatic.aquaticseries.lib.nms.PacketListenerAdapter;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PacketListenerAdapterImpl implements PacketListenerAdapter {
    private UUID uuid = UUID.randomUUID();

    @Override
    public void inject(Player player) {
        var craftPlayer = (CraftPlayer) player;
        var packetListener = craftPlayer.getHandle();

        var connection = packetListener.connection.connection;
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
        var connection = packetListener.connection.connection;
        var channel = connection.channel;
        try {
            if (channel.pipeline().names().contains("AquaticPacketListener" + uuid.toString())) {
                channel.pipeline().remove("AquaticPacketListener" + uuid.toString());
            }
        } catch (Exception ignored) {

        }
    }
}

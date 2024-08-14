package gg.aquatic.aquaticseries.nms.listener;

import gg.aquatic.aquaticseries.lib.inventory.lib.InventoryHandler;
import gg.aquatic.aquaticseries.nms.ProtectedPacket;
import gg.aquatic.aquaticseries.paper.adapt.PaperString;
import gg.aquatic.aquaticseries.spigot.adapt.SpigotString;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;

public class MenuPacketListener extends ChannelDuplexHandler {

    private final Player player;

    public MenuPacketListener(Player player) {
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
        if (packet instanceof ProtectedPacket protectedPacket) {
            super.write(ctx, protectedPacket.getPacket(), promise);
            return;
        }

        if (!(packet instanceof Packet<?>)) {
            super.write(ctx, packet, promise);
            return;
        }

        if (packet instanceof ClientboundOpenScreenPacket pkt) {
            try {
                var upcomingTitle = InventoryHandler.INSTANCE.getUpcommingTitles().remove(player.getUniqueId());
                if (upcomingTitle == null) {
                    super.write(ctx, packet, promise);
                    return;
                }

                ClientboundOpenScreenPacket newPacket = null;
                if (upcomingTitle instanceof PaperString paperString) {
                    newPacket = new ClientboundOpenScreenPacket(pkt.getContainerId(),
                            pkt.getType(),
                            CraftChatMessage.fromJSONOrString(GsonComponentSerializer.gson().serialize(paperString))
                    );
                } else if (upcomingTitle instanceof SpigotString spigotString) {
                    newPacket = new ClientboundOpenScreenPacket(
                            pkt.getContainerId(),
                            pkt.getType(),
                            CraftChatMessage.fromJSONOrString(spigotString.getFormatted())
                    );
                }

                if (newPacket != null) {
                    super.write(ctx, newPacket, promise);
                    return;
                }

                super.write(ctx, packet, promise);
                return;


            } catch (Exception ex) {
                super.write(ctx, packet, promise);
                return;
            }
        }
    }
}

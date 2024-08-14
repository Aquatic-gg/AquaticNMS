package gg.aquatic.aquaticseries.nms.v1_19_4.listener;

import gg.aquatic.aquaticseries.lib.inventory.lib.InventoryHandler;
import gg.aquatic.aquaticseries.lib.inventory.lib.inventory.CustomInventory;
import gg.aquatic.aquaticseries.nms.ProtectedPacket;
import gg.aquatic.aquaticseries.paper.adapt.PaperString;
import gg.aquatic.aquaticseries.spigot.adapt.SpigotString;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftChatMessage;
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
                            CraftChatMessage.fromJSONOrString(paperString.toJson())
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

        if (packet instanceof ClientboundContainerSetContentPacket pkt) {
            try {
                var openedInventory = player.getOpenInventory().getTopInventory();
                if (!(openedInventory instanceof CustomInventory customInventory)) {
                    super.write(ctx, packet, promise);
                    return;
                }
                var size = player.getOpenInventory().getTopInventory().getSize();
                var customInvContent = customInventory.getContent();
                NonNullList<ItemStack> items = NonNullList.create();
                var i = 0;
                for (ItemStack item : pkt.getItems()) {
                    if (i > size) {
                        var contentItem = customInvContent.get(i);
                        if (contentItem == null) {
                            items.add(item);
                        } else {
                            items.add(CraftItemStack.asNMSCopy(contentItem));
                        }
                    } else {
                        items.add(item);
                    }
                    i++;
                }
                var newPacket = new ClientboundContainerSetContentPacket(
                        pkt.getContainerId(),
                        pkt.getStateId(),
                        items,
                        pkt.getCarriedItem()
                );
                super.write(ctx, newPacket, promise);
                return;

            } catch (Exception ex) {
                super.write(ctx, packet, promise);
                return;
            }
        }

        if (packet instanceof ClientboundContainerSetSlotPacket pkt) {
            try {
                var openedInventory = player.getOpenInventory().getTopInventory();
                if (!(openedInventory instanceof CustomInventory customInventory)) {
                    super.write(ctx, packet, promise);
                    return;
                }
                var size = player.getOpenInventory().getTopInventory().getSize();
                if (pkt.getSlot() > size) {
                    return;
                }
            } catch (Exception ignored) {

            }
        }
        super.write(ctx, packet, promise);
    }
}

package gg.aquatic.aquaticseries.nms.v1_21.listener;

import gg.aquatic.aquaticseries.lib.AbstractAquaticSeriesLib;
import gg.aquatic.aquaticseries.lib.NMSEntityInteractEvent;
import gg.aquatic.aquaticseries.lib.PlayerChunkLoadEvent;
import gg.aquatic.aquaticseries.lib.nms.listener.PacketEvent;
import gg.aquatic.aquaticseries.lib.nms.packet.WrappedClientboundContainerSetContentPacket;
import gg.aquatic.aquaticseries.lib.nms.packet.WrappedClientboundContainerSetSlotPacket;
import gg.aquatic.aquaticseries.lib.nms.packet.WrappedClientboundOpenScreenPacket;
import gg.aquatic.aquaticseries.lib.util.EventExtKt;
import gg.aquatic.aquaticseries.nms.v1_21.ProtectedPacket;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;

public class AquaticPacketListener extends ChannelDuplexHandler {

    private final Player player;
    private final PacketListenerAdapterImpl packetListenerAdapter;

    public AquaticPacketListener(Player player, PacketListenerAdapterImpl packetListenerAdapter) {
        this.player = player;
        this.packetListenerAdapter = packetListenerAdapter;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object pkt, ChannelPromise promise) throws Exception {
        if (pkt instanceof ProtectedPacket protectedPacket) {
            super.write(ctx, protectedPacket.getPacket(), promise);
            return;
        }

        if (!(pkt instanceof Packet<?>)) {
            super.write(ctx, pkt, promise);
            return;
        }

        try {
            if (pkt instanceof ClientboundContainerSetContentPacket packet) {
                var wrapped = new WrappedClientboundContainerSetContentPacket(
                        packet.getContainerId(),
                        packet.getStateId(),
                        packet.getItems().stream().map(i -> {
                            if (i == null) {
                                return null;
                            } else {
                                return CraftItemStack.asBukkitCopy(i);
                            }
                        }).toList(),
                        CraftItemStack.asBukkitCopy(packet.getCarriedItem())
                );
                var event = new PacketEvent<>(player, wrapped);
                packetListenerAdapter.onPacketEvent(event);
                if (event.getCancelled()) {
                    return;
                }
                if (event.getPacket().getModified()) {
                    NonNullList<ItemStack> items = NonNullList.create();
                    items.addAll(
                            wrapped.getItems().stream().map(i -> {
                                if (i == null) {
                                    return null;
                                } else {
                                    return CraftItemStack.asNMSCopy(i);
                                }
                            }).toList());
                    var newPacket = new ClientboundContainerSetContentPacket(
                            wrapped.getContainerId(),
                            wrapped.getStateId(),
                            items,
                            CraftItemStack.asNMSCopy(wrapped.getCarriedItem())
                    );
                    super.write(ctx, newPacket, promise);
                    return;
                }
                super.write(ctx, pkt, promise);
                return;
            } else if (pkt instanceof ClientboundContainerSetSlotPacket packet) {
                org.bukkit.inventory.ItemStack item;
                if (packet.getItem() == null) {
                    item = null;
                } else {
                    item = CraftItemStack.asBukkitCopy(packet.getItem());
                }
                var wrapped = new WrappedClientboundContainerSetSlotPacket(
                        packet.getContainerId(),
                        packet.getStateId(),
                        packet.getSlot(),
                        item
                );
                var event = new PacketEvent<>(player, wrapped);
                packetListenerAdapter.onPacketEvent(event);
                if (event.getCancelled()) {
                    return;
                }
                if (event.getPacket().getModified()) {
                    var newPacket = new ClientboundContainerSetSlotPacket(
                            wrapped.getContainerId(),
                            wrapped.getStateId(),
                            wrapped.getSlot(),
                            CraftItemStack.asNMSCopy(wrapped.getItemStack())
                    );
                    super.write(ctx, newPacket, promise);
                    return;
                }
                super.write(ctx, pkt, promise);
                return;
            } else if (pkt instanceof ClientboundOpenScreenPacket packet) {
                var wrapped = new WrappedClientboundOpenScreenPacket(
                        packet.getContainerId(),
                        BuiltInRegistries.MENU.getId(packet.getType()),
                        Component.Serializer.toJson(packet.getTitle(), ((CraftPlayer) player).getHandle().registryAccess())
                );
                var event = new PacketEvent<>(player, wrapped);
                packetListenerAdapter.onPacketEvent(event);
                if (event.getCancelled()) {
                    return;
                }
                if (event.getPacket().getModified()) {
                    var newPacket = new ClientboundOpenScreenPacket(
                            wrapped.getContainerId(),
                            BuiltInRegistries.MENU.byId(wrapped.getType()),
                            CraftChatMessage.fromJSONOrString(wrapped.getStringOrJsonTitle())
                    );
                    super.write(ctx, newPacket, promise);
                    return;
                }
                super.write(ctx, pkt, promise);
                return;
            }
            if (pkt instanceof ClientboundLevelChunkWithLightPacket packet) {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        var chunk = player.getLocation().getWorld().getChunkAt(packet.getX(), packet.getZ());
                        var event = new PlayerChunkLoadEvent(player, chunk);
                        EventExtKt.call(event);
                    }
                }.runTask(AbstractAquaticSeriesLib.Companion.getINSTANCE().getPlugin());
            }
        }catch (Exception ignored) {
            super.write(ctx, pkt, promise);
            return;
        }

        super.write(ctx, pkt, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof ServerboundInteractPacket packet) {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        packet.dispatch(new ServerboundInteractPacket.Handler() {
                            @Override
                            public void onInteraction(InteractionHand interactionHand) {
                                Action action = Action.RIGHT_CLICK_AIR;
                                for (Field declaredField : packet.getClass().getDeclaredFields()) {
                                    if (declaredField.getType() == int.class) {
                                        declaredField.setAccessible(true);
                                        try {
                                            var id = (int) declaredField.get(packet);
                                            if (packetListenerAdapter.getNms().getEntity(id) == null) return;
                                            var event = new NMSEntityInteractEvent(player, id, action);
                                            EventExtKt.call(event);
                                        } catch (IllegalAccessException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onInteraction(InteractionHand interactionHand, Vec3 vec3) {

                            }

                            @Override
                            public void onAttack() {
                                Action action = Action.LEFT_CLICK_AIR;
                                for (Field declaredField : packet.getClass().getDeclaredFields()) {
                                    if (declaredField.getType() == int.class) {
                                        declaredField.setAccessible(true);
                                        try {
                                            var id = (int) declaredField.get(packet);
                                            if (packetListenerAdapter.getNms().getEntity(id) == null) return;
                                            var event = new NMSEntityInteractEvent(player, id, action);
                                            EventExtKt.call(event);
                                        } catch (IllegalAccessException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            }
                        });

                    }
                }.runTask(AbstractAquaticSeriesLib.Companion.getINSTANCE().getPlugin());
            }
        } catch (Exception ignored) {
            super.channelRead(ctx,msg);
            return;
        }

        super.channelRead(ctx, msg);
    }
}

package gg.aquatic.aquaticseries.nms.v1_19_4.listener;

import gg.aquatic.aquaticseries.lib.AbstractAquaticSeriesLib;
import gg.aquatic.aquaticseries.lib.NMSEntityInteractEvent;
import gg.aquatic.aquaticseries.lib.PlayerChunkLoadEvent;
import gg.aquatic.aquaticseries.lib.util.EventExtKt;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;

public class AquaticPacketListener extends ChannelDuplexHandler {

    private final Player player;

    public AquaticPacketListener(Player player) {
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundLevelChunkWithLightPacket packet) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    var chunk = player.getLocation().getWorld().getChunkAt(packet.getX(), packet.getZ());
                    var event = new PlayerChunkLoadEvent(player, chunk);
                    EventExtKt.call(event);
                }
            }.runTask(AbstractAquaticSeriesLib.Companion.getINSTANCE().getPlugin());
        }

        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServerboundInteractPacket packet) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    packet.dispatch(new ServerboundInteractPacket.Handler() {
                        @Override
                        public void onInteraction(InteractionHand interactionHand) {
                            Action action = Action.RIGHT_CLICK_AIR;
                            var event = new NMSEntityInteractEvent(player,packet.getTarget(((CraftWorld)player.getLocation().getWorld()).getHandle()).getId(),action);
                            EventExtKt.call(event);
                        }

                        @Override
                        public void onInteraction(InteractionHand interactionHand, Vec3 vec3) {

                        }

                        @Override
                        public void onAttack() {
                            Action action = Action.LEFT_CLICK_AIR;
                            var event = new NMSEntityInteractEvent(player,packet.getTarget(((CraftWorld)player.getLocation().getWorld()).getHandle()).getId(),action);
                            EventExtKt.call(event);
                        }
                    });

                }
            }.runTask(AbstractAquaticSeriesLib.Companion.getINSTANCE().getPlugin());
        }

        super.channelRead(ctx, msg);
    }
}

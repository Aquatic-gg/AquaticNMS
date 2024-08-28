package gg.aquatic.aquaticseries.nms.v1_18_2;

import com.mojang.datafixers.util.Pair;
import gg.aquatic.aquaticseries.lib.StringExtKt;
import gg.aquatic.aquaticseries.lib.adapt.AquaticString;
import gg.aquatic.aquaticseries.lib.audience.AquaticAudience;
import gg.aquatic.aquaticseries.lib.inventory.lib.event.InventoryTitleUpdateEvent;
import gg.aquatic.aquaticseries.lib.inventory.lib.inventory.CustomInventory;
import gg.aquatic.aquaticseries.lib.nms.NMSAdapter;
import gg.aquatic.aquaticseries.lib.nms.listener.PacketListenerAdapter;
import gg.aquatic.aquaticseries.lib.util.EventExtKt;
import gg.aquatic.aquaticseries.nms.v1_18_2.listener.PacketListenerAdapterImpl;
import gg.aquatic.aquaticseries.paper.adapt.PaperString;
import gg.aquatic.aquaticseries.spigot.adapt.SpigotString;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftVector;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public final class NMS_1_18_2 implements NMSAdapter {

    private final Map<Integer, Entity> entities = new HashMap<>();

    @Override
    public int spawnEntity(Location location, String s, AquaticAudience abstractAudience, Consumer<org.bukkit.entity.Entity> consumer) {
        final var entityOpt = EntityType.byString(s.toLowerCase());
        if (entityOpt.isEmpty()) {
            return -1;
        }

        final var worldServer = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
        final var entity = entityOpt.get().create(
                worldServer,
                null,
                null,
                null,
                new BlockPos(CraftVector.toNMS(location.toVector())),
                MobSpawnType.COMMAND,
                true,
                false
        );

        entity.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        if (consumer != null) {
            consumer.accept(entity.getBukkitEntity());
        }

        final var packetData = new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData(), true);
        sendPacket(abstractAudience,entity.getAddEntityPacket(), true);
        sendPacket(abstractAudience, packetData, true);

        if (entity instanceof LivingEntity livingEntity) {
            List<Pair<EquipmentSlot, ItemStack>> list = new ArrayList<>();
            for (EquipmentSlot value : EquipmentSlot.values()) {
                list.add(Pair.of(value, livingEntity.getItemBySlot(value)));
            }
            final var packet = new ClientboundSetEquipmentPacket(entity.getId(),list);
            sendPacket(abstractAudience,packet, true);
        }

        entities.put(entity.getId(), entity);
        return entity.getId();
    }

    @Override
    public org.bukkit.entity.Entity getEntity(int i) {
        var entity = entities.get(i);
        if (entity == null) return null;
        return entity.getBukkitEntity();
    }

    @Override
    public void despawnEntity(List<Integer> list, AquaticAudience abstractAudience) {
        final var packet = new ClientboundRemoveEntitiesPacket(new IntArrayList(list));
        sendPacket(abstractAudience, packet, true);

    }

    @Override
    public void updateEntity(int i, Consumer<org.bukkit.entity.Entity> consumer, AquaticAudience abstractAudience) {
        net.minecraft.world.entity.Entity entity = entities.get(i);

        if (consumer != null) {
            consumer.accept(entity.getBukkitEntity());
        }

        final var packetMetadata = new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData(), true);
        sendPacket(new ArrayList<>(Bukkit.getOnlinePlayers()), packetMetadata, true);

        if (entity instanceof LivingEntity livingEntity) {
            final List<Pair<EquipmentSlot, ItemStack>> equipmentMap = new ArrayList<>();
            for (EquipmentSlot value : EquipmentSlot.values()) {
                equipmentMap.add(Pair.of(value,livingEntity.getItemBySlot(value)));
            }
            final var packet = new ClientboundSetEquipmentPacket(entity.getId(),equipmentMap);
            sendPacket(new ArrayList<>(Bukkit.getOnlinePlayers()),packet, true);
        }
    }

    @Override
    public void updateEntityVelocity(int i, Vector vector, AquaticAudience abstractAudience) {
        net.minecraft.world.entity.Entity entity = entities.get(i);
        entity.getBukkitEntity().setVelocity(vector);
        final var packet = new ClientboundSetEntityMotionPacket(i,new Vec3(vector.getX(),vector.getY(),vector.getZ()));
        sendPacket(abstractAudience,packet, true);
    }

    @Override
    public void teleportEntity(int i, Location location, AquaticAudience abstractAudience) {
        if (!entities.containsKey(i)) {
            return;
        }
        net.minecraft.world.entity.Entity entity = entities.get(i);

        entity.getBukkitEntity().teleport(location);
        final var packet = new ClientboundTeleportEntityPacket(entity);

        sendPacket(abstractAudience,packet, true);
    }

    @Override
    public void moveEntity(int i, Location location, AquaticAudience abstractAudience) {
        if (!entities.containsKey(i)) {
            return;
        }
        net.minecraft.world.entity.Entity entity = entities.get(i);
        Location prevLoc = entity.getBukkitEntity().getLocation();

        entity.getBukkitEntity().teleport(location);
        final var packet = new ClientboundMoveEntityPacket.PosRot(
                i,
                (short)((location.getX() * 32 - prevLoc.getX() * 32) * 128),
                (short)((location.getY() * 32 - prevLoc.getY() * 32) * 128),
                (short)((location.getZ() * 32 - prevLoc.getZ() * 32) * 128),
                (byte) ((int) (location.getYaw() * 256.0F / 360.0F)),
                (byte) ((int) (location.getPitch() * 256.0F / 360.0F)),
                true
        );

        sendPacket(abstractAudience,packet, true);
        sendPacket(abstractAudience,
                new ClientboundRotateHeadPacket(entities.get(i),(byte) ((int) (location.getYaw() * 256.0F / 360.0F))), true
        );
    }

    @Override
    public void setSpectatorTarget(int i, AquaticAudience abstractAudience) {
        net.minecraft.world.entity.Entity entity = entities.get(i);
        if (entity == null) {
            for (UUID uuid : abstractAudience.getUuids()) {
                Player player = Bukkit.getPlayer(uuid);
                entity = ((CraftPlayer) Objects.requireNonNull(player)).getHandle();

                final var packet = new ClientboundSetCameraPacket(entity);
                sendPacket(List.of(player),packet, true);
            }
            return;
        }

        final var packet = new ClientboundSetCameraPacket(entity);
        sendPacket(abstractAudience,packet, true);

    }

    @Override
    public void setGamemode(GameMode gameMode, Player player) {
        final var packet = new ClientboundGameEventPacket(new ClientboundGameEventPacket.Type(3),gameMode.getValue());
        sendPacket(Arrays.asList(player),packet, true);
    }

    @Override
    public void setPlayerInfoGamemode(GameMode gameMode, Player player) {
        final var playerHandle = ((CraftPlayer)player).getHandle();

        ClientboundPlayerInfoPacket.Action action2 = ClientboundPlayerInfoPacket.Action.valueOf("UPDATE_GAME_MODE");
        final var packet = new ClientboundPlayerInfoPacket(action2,playerHandle);

        try {
            final Field packetsField;
            packetsField = packet.getClass().getDeclaredField("b");
            packetsField.setAccessible(true);

            List<ClientboundPlayerInfoPacket.PlayerUpdate> list = new ArrayList<>();
            list.add(new ClientboundPlayerInfoPacket.PlayerUpdate(
                    playerHandle.getGameProfile(),
                    playerHandle.latency,
                    GameType.valueOf(gameMode.toString().toUpperCase()),
                    playerHandle.listName)
            );

            packetsField.set(packet,list);
            sendPacket(Arrays.asList(player), packet, true);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendPacket(List<Player> players, Packet packet, boolean isProtected) {
        if (isProtected) {
            var protectedPacket = new ProtectedPacket(packet);
            players.forEach(player -> {
                var craftPlayer = (CraftPlayer) player;
                var packetListener = craftPlayer.getHandle().connection;
                var connection = packetListener.connection;
                var pipeline = connection.channel.pipeline();
                pipeline.writeAndFlush(protectedPacket);
            });
            return;
        }
        players.forEach(player -> {
            ((CraftPlayer) player).getHandle().connection.send(packet);
        });
    }

    private void sendPacket(AquaticAudience audience, Packet packet, boolean isProtected) {
        sendPacket(audience.getUuids().stream().map(Bukkit::getPlayer
        ).filter(Objects::nonNull).toList(), packet, isProtected);
    }

    @Override
    public void setContainerItem(Player player, org.bukkit.inventory.ItemStack itemStack, int i) {
        var serverPlayer = ((CraftPlayer) player).getHandle();
        var container = serverPlayer.containerMenu;
        var containerId = container.containerId;

        var packet = new ClientboundContainerSetSlotPacket(containerId, container.getStateId(), i, CraftItemStack.asNMSCopy(itemStack));
        sendPacket(List.of(player), packet, true);
    }

    @Override
    public void setInventoryContent(AquaticAudience abstractAudience, InventoryType inventoryType, Collection<? extends org.bukkit.inventory.ItemStack> collection, org.bukkit.inventory.ItemStack itemStack) {

    }

    public void sendTitleUpdate(Player player, AquaticString aquaticString) {
        var serverPlayer = ((CraftPlayer) player).getHandle();
        if (serverPlayer.containerMenu == null) {
            return;
        }
        var container = serverPlayer.containerMenu;
        var containerId = container.containerId;
        var title = serverPlayer.containerMenu.getTitle();

        if (player.getOpenInventory().getTopInventory().getHolder() instanceof CustomInventory customInventory) {
            EventExtKt.call(
                    new InventoryTitleUpdateEvent(
                            customInventory,
                            StringExtKt.toAquatic(net.minecraft.network.chat.Component.Serializer.toJson(title)),
                            aquaticString
                    )
            );
        }

        Component serializedTitle = null;
        if (aquaticString instanceof PaperString paperString) {
            serializedTitle = net.minecraft.network.chat.Component.Serializer.fromJson(
                    paperString.toJson()
            );
        } else if (aquaticString instanceof SpigotString spigotString) {
            serializedTitle = CraftChatMessage.fromJSONOrString(spigotString.getFormatted());
        }

        if (serializedTitle == null) {
            return;
        }

        var packet = new ClientboundOpenScreenPacket(
                containerId,
                serverPlayer.containerMenu.getType(),
                serializedTitle
        );
        sendPacket(List.of(player), packet, true);

    }
    private PacketListenerAdapterImpl packetListenerAdapter = new PacketListenerAdapterImpl(this);

    @Override
    public PacketListenerAdapter packetListenerAdapter() {
        return packetListenerAdapter;
    }

    @Override
    public void resendEntitySpawnPacket(Player player, int i) {
        var entity = entities.get(i);
        if (entity == null) {
            return;
        }
        var packet = entity.getAddEntityPacket();
        sendPacket(List.of(player), packet, true);

        var data = entity.getEntityData();
        var dataPacket = new ClientboundSetEntityDataPacket(i, data, true);
        sendPacket(List.of(player), dataPacket, true);
    }
}


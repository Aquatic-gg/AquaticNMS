package gg.aquatic.aquaticseries.nms;

import com.mojang.datafixers.util.Pair;
import gg.aquatic.aquaticseries.lib.nms.NMSAdapter;
import gg.aquatic.aquaticseries.lib.util.AbstractAudience;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public class NMS_1_20_4 implements NMSAdapter {

    private final Map<Integer, Entity> entities = new HashMap<>();

    @Override
    public int spawnEntity(Location location, String s, AbstractAudience abstractAudience, Consumer<org.bukkit.entity.Entity> consumer) {
        final var entityOpt = EntityType.byString(s.toLowerCase());
        if (entityOpt.isEmpty()) {
            return -1;
        }

        final var worldServer = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
        final var entity = entityOpt.get().create(
                worldServer,
                null,
                null,
                new BlockPos((int) location.toVector().getX(), (int) location.toVector().getY(), (int) location.toVector().getZ()),
                MobSpawnType.COMMAND,
                true,
                false
        );

        entity.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        if (consumer != null) {
            consumer.accept(entity.getBukkitEntity());
        }

        final var packetData = new ClientboundSetEntityDataPacket(entity.getId(),entity.getEntityData().getNonDefaultValues());
        sendPacket(abstractAudience,entity.getAddEntityPacket());
        sendPacket(abstractAudience, packetData);

        if (entity instanceof LivingEntity livingEntity) {
            List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
            for (EquipmentSlot value : EquipmentSlot.values()) {
                list.add(Pair.of(value, livingEntity.getItemBySlot(value)));
            }
            final var packet = new ClientboundSetEquipmentPacket(entity.getId(),list);
            sendPacket(abstractAudience,packet);
        }

        entities.put(entity.getId(), entity);
        return entity.getId();
    }

    @Override
    public org.bukkit.entity.Entity getEntity(int i) {
        return entities.get(i).getBukkitEntity();
    }

    @Override
    public void despawnEntity(List<Integer> list, AbstractAudience abstractAudience) {
        final var packet = new ClientboundRemoveEntitiesPacket(new IntArrayList(list));
        sendPacket(abstractAudience, packet);

    }

    @Override
    public void updateEntity(int i, Consumer<org.bukkit.entity.Entity> consumer, AbstractAudience abstractAudience) {
        net.minecraft.world.entity.Entity entity = entities.get(i);

        if (consumer != null) {
            consumer.accept(entity.getBukkitEntity());
        }

        final var packetMetadata = new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData().getNonDefaultValues());
        sendPacket(new ArrayList<>(Bukkit.getOnlinePlayers()), packetMetadata);

        if (entity instanceof LivingEntity livingEntity) {
            final List<Pair<EquipmentSlot, ItemStack>> equipmentMap = new ArrayList<>();
            for (EquipmentSlot value : EquipmentSlot.values()) {
                equipmentMap.add(Pair.of(value,livingEntity.getItemBySlot(value)));
            }
            final var packet = new ClientboundSetEquipmentPacket(entity.getId(),equipmentMap);
            sendPacket(new ArrayList<>(Bukkit.getOnlinePlayers()),packet);
        }
    }

    @Override
    public void updateEntityVelocity(int i, Vector vector, AbstractAudience abstractAudience) {
        net.minecraft.world.entity.Entity entity = entities.get(i);
        entity.getBukkitEntity().setVelocity(vector);
        final var packet = new ClientboundSetEntityMotionPacket(i,new Vec3(vector.getX(),vector.getY(),vector.getZ()));
        sendPacket(abstractAudience,packet);
    }


    @Override
    public void teleportEntity(int i, Location location, AbstractAudience abstractAudience) {
        if (!entities.containsKey(i)) {
            return;
        }
        net.minecraft.world.entity.Entity entity = entities.get(i);

        entity.getBukkitEntity().teleport(location);
        final var packet = new ClientboundTeleportEntityPacket(entity);

        sendPacket(abstractAudience,packet);
    }

    @Override
    public void moveEntity(int i, Location location, AbstractAudience abstractAudience) {
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

        sendPacket(abstractAudience,packet);
        sendPacket(abstractAudience,
                new ClientboundRotateHeadPacket(entities.get(i),(byte) ((int) (location.getYaw() * 256.0F / 360.0F)))
        );
    }

    @Override
    public void setSpectatorTarget(int i, int i1, AbstractAudience abstractAudience) {
        net.minecraft.world.entity.Entity entity = entities.get(i);
        if (entity == null) {
            for (UUID uuid : abstractAudience.getCurrentlyViewing()) {
                Player player = Bukkit.getPlayer(uuid);
                entity = ((CraftPlayer) Objects.requireNonNull(player)).getHandle();

                final var packet = new ClientboundSetCameraPacket(entity);
                sendPacket(List.of(player),packet);
            }
            return;
        }

        final var packet = new ClientboundSetCameraPacket(entity);
        sendPacket(abstractAudience,packet);

    }

    @Override
    public void setGamemode(GameMode gameMode, Player player) {
        final var packet = new ClientboundGameEventPacket(new ClientboundGameEventPacket.Type(3),gameMode.getValue());
        sendPacket(Arrays.asList(player),packet);
    }

    @Override
    public void setPlayerInfoGamemode(GameMode gameMode, Player player) {
        final var playerHandle = ((CraftPlayer)player).getHandle();

        ClientboundPlayerInfoUpdatePacket.Action action2 = ClientboundPlayerInfoUpdatePacket.Action.valueOf("UPDATE_GAME_MODE");
        final var packet = new ClientboundPlayerInfoUpdatePacket(action2,playerHandle);

        try {
            final Field packetsField;
            packetsField = packet.getClass().getDeclaredField("b");
            packetsField.setAccessible(true);

            List<ClientboundPlayerInfoUpdatePacket.Entry> list = new ArrayList<>();
            list.add(new ClientboundPlayerInfoUpdatePacket.Entry(
                            playerHandle.getUUID(),
                            playerHandle.getGameProfile(),
                            true,
                            player.getPing(),
                            GameType.valueOf(gameMode.toString().toUpperCase()),
                            playerHandle.listName,
                            null
                    )
            );

            packetsField.set(packet,list);
            sendPacket(Arrays.asList(player), packet);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendPacket(List<Player> players, Packet packet) {
        players.forEach(player -> {
            ((CraftPlayer)player).getHandle().connection.send(packet);
        });
    }

    private void sendPacket(AbstractAudience audience, Packet packet) {
        sendPacket(audience.getCurrentlyViewing().stream().map(Bukkit::getPlayer
        ).toList(), packet);
    }

    @Override
    public void setContainerItem(Player player, org.bukkit.inventory.ItemStack itemStack, int i) {
        var serverPlayer = ((CraftPlayer) player).getHandle();
        var container = serverPlayer.containerMenu;
        var containerId = container.containerId;

        var packet = new ClientboundContainerSetSlotPacket(containerId, container.getStateId(), i, CraftItemStack.asNMSCopy(itemStack));
        sendPacket(List.of(player), packet);
    }

    @Override
    public void setInventoryContent(AbstractAudience abstractAudience, InventoryType inventoryType, Collection<? extends org.bukkit.inventory.ItemStack> collection, org.bukkit.inventory.ItemStack itemStack) {

    }
}

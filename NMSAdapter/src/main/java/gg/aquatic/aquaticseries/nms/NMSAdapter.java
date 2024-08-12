package gg.aquatic.aquaticseries.nms;

import gg.aquatic.aquaticseries.lib.util.AbstractAudience;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface NMSAdapter {

    int spawnEntity(Location location, String type, AbstractAudience audience, Consumer<Entity> factory);
    Entity getEntity(int id);
    void despawnEntity(List<Integer> ids, AbstractAudience audience);
    void updateEntity(int id, Consumer<Entity> factory, AbstractAudience audience);
    void updateEntityVelocity(int id, Vector velocity, AbstractAudience audience);
    void teleportEntity(int id, Location location, AbstractAudience audience);
    void moveEntity(int id, Location location, AbstractAudience audience);
    void setSpectatorTarget(int id, int target, AbstractAudience audience);
    void setGamemode(GameMode gamemode, Player player);
    void setPlayerInfoGamemode(GameMode gamemode, Player player);
    void setContainerItem(Player player, ItemStack itemStack, int slot);
    void setInventoryContent(AbstractAudience audience, InventoryType inventoryType, Collection<ItemStack> content, ItemStack activeItem);

    enum InventoryType {
        PLAYER,
        TOP
    }
}

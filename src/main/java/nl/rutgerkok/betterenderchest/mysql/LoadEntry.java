package nl.rutgerkok.betterenderchest.mysql;

import nl.rutgerkok.betterenderchest.BetterEnderInventoryHolder;
import nl.rutgerkok.betterenderchest.WorldGroup;
import nl.rutgerkok.betterenderchest.io.Consumer;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

public class LoadEntry {
    private final Consumer<Inventory> callback;
    private final String inventoryName;
    private final WorldGroup worldGroup;

    public LoadEntry(String inventoryName, WorldGroup worldGroup, Consumer<Inventory> callback) {
        Validate.notNull(inventoryName, "inventoryName cannot be null");
        Validate.notNull(worldGroup, "worldGroup cannot be null");
        Validate.notNull(callback, "callback cannot be null");
        this.inventoryName = inventoryName;
        this.worldGroup = worldGroup;
        this.callback = callback;
    }

    /**
     * Calls the callback on the main thread. This method can be called from any
     * thread.
     * 
     * @param plugin
     *            The plugin, needed for Bukkit's scheduler.
     * @param inventoryData
     *            The raw bytes of the inventory that was just loaded.
     */
    public void callback(final BetterEnderSQLCache cache, final byte[] inventoryData) {
        if (Bukkit.isPrimaryThread()) {
            // On main thread for whatever reason, no need to schedule task
            callbackOnMainThread(cache, inventoryData);
        } else {
            // Schedule task to run on the main thread.
            Bukkit.getScheduler().runTask(cache.plugin.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    callbackOnMainThread(cache, inventoryData);
                }
            });
        }
    }

    private void callbackOnMainThread(BetterEnderSQLCache cache, byte[] inventoryData) {
        Inventory inventory;
        boolean isNew = false;

        // Load inventory
        if (inventoryData == null) {
            inventory = cache.plugin.getEmptyInventoryProvider().loadEmptyInventory(inventoryName);
            isNew = true;
        } else {
            inventory = cache.plugin.getNMSHandlers().getSelectedRegistration().loadNBTInventory(inventoryData, inventoryName, "Inventory");
        }

        // Update whether inventory is new
        ((BetterEnderInventoryHolder) inventory.getHolder()).setChestIsNew(isNew);

        // Add to loaded inventories
        cache.setInventory(inventoryName, worldGroup, inventory);

        // Call callback
        callback.consume(inventory);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LoadEntry)) {
            return false;
        }
        LoadEntry other = (LoadEntry) obj;
        if (!callback.equals(other.callback)) {
            return false;
        }
        if (!inventoryName.equals(other.inventoryName)) {
            return false;
        }
        if (!worldGroup.equals(other.worldGroup)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the name of the inventory that should be loaded.
     * 
     * @return The name of the inventory.
     */
    public String getInventoryName() {
        return inventoryName;
    }

    /**
     * Gets the world group of the inventory that should be loaded.
     * 
     * @return
     */
    public WorldGroup getWorldGroup() {
        return worldGroup;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + callback.hashCode();
        result = prime * result + inventoryName.hashCode();
        result = prime * result + worldGroup.hashCode();
        return result;
    }
}

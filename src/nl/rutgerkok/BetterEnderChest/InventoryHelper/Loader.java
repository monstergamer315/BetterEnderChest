package nl.rutgerkok.BetterEnderChest.InventoryHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ListIterator;

import net.minecraftwiki.wiki.NBTClass.Tag;
import nl.rutgerkok.BetterEnderChest.BetterEnderChest;
import nl.rutgerkok.BetterEnderChest.BetterEnderHolder;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Loader {

    /**
     * Creates an empty inventory with the given name. The number of rows is
     * automatically calculated by the plugin.
     * 
     * @param inventoryName
     * @param plugin
     * @return
     */
    public static Inventory loadEmptyInventory(String inventoryName, BetterEnderChest plugin) {
        return loadEmptyInventory(inventoryName, LoadHelper.getInventoryRows(inventoryName, plugin));
    }

    /**
     * Creates an empty inventory with the given name. Allows you to override
     * the default number of rows.
     * 
     * @param inventoryName
     * @param inventoryRows
     * @return
     */
    public static Inventory loadEmptyInventory(String inventoryName, int inventoryRows) {

        // Owner name
        // Find out if it's case-correct
        boolean caseCorrect = false;

        if (inventoryName.equals(BetterEnderChest.publicChestName)) {
            // It's the public chest, so it IS case-correct
            caseCorrect = true;
        } else {
            // Check if the player is online
            Player player = Bukkit.getPlayerExact(inventoryName);
            if (player != null) {
                // Player is online, so we have the correct name
                inventoryName = player.getName();
                caseCorrect = true;
            }
        }

        // Return the inventory
        return Bukkit.createInventory(new BetterEnderHolder(inventoryName, caseCorrect), inventoryRows * 9, LoadHelper.getInventoryTitle(inventoryName)); // Smiley
    }

    /**
     * Loads an inventory from a file.
     * 
     * @param inventoryName
     *            The name of the inventory (Notch,
     *            BetterEnderChest.publicChestName, etc.)
     * @param file
     *            The file to load from
     * @param inventoryTagName
     *            The tag name of the Ender Inventory tag in that file
     * @param plugin
     *            Needed to calculate the number of rows
     * @return
     * @throws IOException
     */
    public static Inventory loadInventoryFromFile(String inventoryName, File file, String inventoryTagName, BetterEnderChest plugin) throws IOException {
        if (!file.exists()) {
            // Return nothing if the file doesn't exist
            return null;
        }
        // Main tag, represents the file
        Tag mainNBT = Tag.readFrom(new FileInputStream(file));

        // Inventory tag, inside the file
        Tag inventoryNBT = mainNBT.findTagByName(inventoryTagName);

        // Inventory rows
        int inventoryRows = 0; // Start small
        if (mainNBT.findTagByName("Rows") != null) {
            // Load the number of rows
            inventoryRows = ((Byte) mainNBT.findTagByName("Rows").getValue()).intValue();
            // TODO: remove debug code
            plugin.logThis("Rows loaded: " + inventoryRows);
        } else {
            // Guess the number of rows
            inventoryRows = LoadHelper.getInventoryRows(inventoryName, inventoryNBT, plugin);
            // TODO: remove debug code
            plugin.logThis("Rows guessed: " + inventoryRows);
        }

        // Whether the player name is case-correct (to be loaded from file)
        boolean caseCorrect = false;

        // Try to get correct-case player name from file
        if (mainNBT.findTagByName("OwnerName") != null && mainNBT.findTagByName("NameCaseCorrect") != null) {

            // Get whether the saved name is case-correct
            caseCorrect = (((Byte) mainNBT.findTagByName("NameCaseCorrect").getValue()).byteValue() == 1);

            if (caseCorrect) {
                // If yes, load the saved name
                inventoryName = (String) mainNBT.findTagByName("OwnerName").getValue();
            }
        }

        // No case-correct save name found, let's look on some other ways
        if (!caseCorrect) {
            if (inventoryName.equals(BetterEnderChest.publicChestName)) {
                // It's the public chest, so it IS case-correct
                caseCorrect = true;
            } else {
                // Check if the player is online
                Player player = Bukkit.getPlayerExact(inventoryName);
                if (player != null) {
                    // Player is online, so we have the correct name
                    inventoryName = player.getName();
                    caseCorrect = true;
                }
            }
        }

        // Create the inventory
        Inventory inventory = loadEmptyInventory(inventoryName, inventoryRows);
        ((BetterEnderHolder) inventory.getHolder()).setOwnerName(inventoryName, caseCorrect);

        // Parse the stacks
        Tag[] stacksNBT = (Tag[]) inventoryNBT.getValue();
        ItemStack stack;
        int slot;

        for (Tag stackNBT : stacksNBT) { // parse the NBT-stack
            stack = ItemStackHelper.getStackFromNBT(stackNBT);
            slot = ItemStackHelper.getSlotFromNBT(stackNBT);

            // Add item to inventory
            if (slot < inventoryRows * 9)
                inventory.setItem(slot, stack);
        }

        // Done
        return inventory;
    }

    /**
     * Imports inventory from Bukkit.
     * 
     * @param inventoryName
     * @param inventoryRows
     * @return The inventory, null if there isn't an inventory
     * @throws IOException
     */
    public static Inventory loadInventoryFromCraftBukkit(final String inventoryName, BetterEnderChest plugin) throws IOException {
        Player player = Bukkit.getPlayerExact(inventoryName);
        Inventory betterEnderInventory;
        if (player == null) {

            // Offline, load from file
            File playerStorage = new File(Bukkit.getWorlds().get(0).getWorldFolder().getAbsolutePath() + "/players");
            String[] files = playerStorage.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String fileName) {
                    return fileName.equalsIgnoreCase(inventoryName + ".dat");
                }
            });

            // Check if the file exists
            if (files.length == 0) {
                // File not found, return null
                return null;
            }

            // Load it from the file (mainworld/players/playername.dat)
            betterEnderInventory = loadInventoryFromFile(inventoryName, new File(playerStorage.getAbsolutePath() + "/" + files[0]), "EnderItems", plugin);
        } else {
            // Online, load now
            Inventory vanillaInventory = player.getEnderChest();
            int inventoryRows = LoadHelper.getInventoryRows(inventoryName, vanillaInventory, plugin);
            betterEnderInventory = loadEmptyInventory(inventoryName, inventoryRows);

            // Copy all items
            ListIterator<ItemStack> copyIterator = vanillaInventory.iterator();
            while (copyIterator.hasNext()) {
                int slot = copyIterator.nextIndex();
                ItemStack stack = copyIterator.next();
                if (slot < betterEnderInventory.getSize()) {
                    betterEnderInventory.setItem(slot, stack);
                }
            }
        }

        // Check if the inventory is empty
        boolean empty = true;
        ListIterator<ItemStack> iterator = betterEnderInventory.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() != null) {
                empty = false;
            }
        }
        if (empty) {
            // Empty inventory, return null
            return null;
        } else {
            // Return the inventory
            return betterEnderInventory;
        }
    }
}

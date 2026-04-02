package space.tinkerlab.quantumCheckpoints.checkpoint;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a snapshot of a player's state at a specific point in time.
 * Captures inventory, armor, off-hand, XP, health, and hunger levels.
 */
public class PlayerState {

    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final ItemStack offHandItem;
    private final int totalExperience;
    private final int experienceLevel;
    private final float experienceProgress;
    private final double health;
    private final int foodLevel;
    private final float saturation;

    /**
     * Creates a new PlayerState by capturing the current state of a player.
     *
     * @param player the player whose state to capture
     */
    public PlayerState(Player player) {
        this.inventoryContents = cloneItemArray(player.getInventory().getContents());
        this.armorContents = cloneItemArray(player.getInventory().getArmorContents());
        this.offHandItem = player.getInventory().getItemInOffHand().clone();

        this.totalExperience = player.getTotalExperience();
        this.experienceLevel = player.getLevel();
        this.experienceProgress = player.getExp();

        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
    }

    /**
     * Creates a PlayerState from stored values.
     */
    private PlayerState(ItemStack[] inventoryContents, ItemStack[] armorContents,
                        ItemStack offHandItem, int totalExperience, int experienceLevel,
                        float experienceProgress, double health, int foodLevel, float saturation) {
        this.inventoryContents = inventoryContents;
        this.armorContents = armorContents;
        this.offHandItem = offHandItem;
        this.totalExperience = totalExperience;
        this.experienceLevel = experienceLevel;
        this.experienceProgress = experienceProgress;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
    }

    /**
     * Restores this state to the specified player.
     *
     * @param player the player to restore the state to
     */
    public void restore(Player player) {
        player.getInventory().setContents(cloneItemArray(inventoryContents));
        player.getInventory().setArmorContents(cloneItemArray(armorContents));
        player.getInventory().setItemInOffHand(offHandItem != null ? offHandItem.clone() : null);

        player.setTotalExperience(0);
        player.setLevel(experienceLevel);
        player.setExp(experienceProgress);

        player.setHealth(Math.min(health, player.getMaxHealth()));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
    }

    /**
     * Clones an array of ItemStacks to prevent reference issues.
     */
    private ItemStack[] cloneItemArray(ItemStack[] original) {
        if (original == null) return null;
        ItemStack[] cloned = new ItemStack[original.length];
        for (int i = 0; i < original.length; i++) {
            cloned[i] = original[i] != null ? original[i].clone() : null;
        }
        return cloned;
    }

    /**
     * Serializes this state into a ConfigurationSection.
     * ItemStack arrays are stored as lists, which Bukkit's YAML
     * serializer handles natively (preserving all NBT data).
     *
     * @param section the section to write into
     */
    public void serialize(ConfigurationSection section) {
        section.set("inventoryContents", Arrays.asList(inventoryContents));
        section.set("armorContents", Arrays.asList(armorContents));
        section.set("offHandItem", offHandItem);
        section.set("totalExperience", totalExperience);
        section.set("experienceLevel", experienceLevel);
        section.set("experienceProgress", (double) experienceProgress);
        section.set("health", health);
        section.set("foodLevel", foodLevel);
        section.set("saturation", (double) saturation);
    }

    /**
     * Deserializes a PlayerState from a ConfigurationSection.
     *
     * @param section the section to read from
     * @return the deserialized PlayerState
     */
    @SuppressWarnings("unchecked")
    public static PlayerState deserialize(ConfigurationSection section) {
        // Bukkit deserializes ItemStack lists as List<ItemStack> (with nulls for empty slots)
        List<?> rawInventory = section.getList("inventoryContents", new ArrayList<>());
        ItemStack[] inventory = rawInventory.toArray(new ItemStack[0]);

        List<?> rawArmor = section.getList("armorContents", new ArrayList<>());
        ItemStack[] armor = rawArmor.toArray(new ItemStack[0]);

        ItemStack offHand = section.getItemStack("offHandItem");

        int totalExp = section.getInt("totalExperience");
        int expLevel = section.getInt("experienceLevel");
        float expProgress = (float) section.getDouble("experienceProgress");
        double health = section.getDouble("health");
        int food = section.getInt("foodLevel");
        float saturation = (float) section.getDouble("saturation");

        return new PlayerState(inventory, armor, offHand, totalExp, expLevel,
                expProgress, health, food, saturation);
    }

    public ItemStack[] getInventoryContents() {
        return inventoryContents;
    }

    public ItemStack[] getArmorContents() {
        return armorContents;
    }

    public ItemStack getOffHandItem() {
        return offHandItem;
    }

    public int getTotalExperience() {
        return totalExperience;
    }

    public double getHealth() {
        return health;
    }

    public int getFoodLevel() {
        return foodLevel;
    }
}
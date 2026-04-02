package space.tinkerlab.quantumCheckpoints.checkpoint;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a snapshot of a player's state at a specific point in time.
 * Captures inventory, armor, off-hand, XP, health, and hunger levels.
 */
public class PlayerState implements Serializable {

    private static final long serialVersionUID = 1L;

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
        // Clone all inventory items to prevent reference issues
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
     * Creates a PlayerState from serialized data.
     * Used when loading from storage.
     */
    public PlayerState(ItemStack[] inventoryContents, ItemStack[] armorContents,
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
     *
     * @param original the original array
     * @return a cloned array with cloned items
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
     * Converts this state to a Map for YAML serialization.
     *
     * @return a map representation of this state
     */
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("inventoryContents", inventoryContents);
        map.put("armorContents", armorContents);
        map.put("offHandItem", offHandItem);
        map.put("totalExperience", totalExperience);
        map.put("experienceLevel", experienceLevel);
        map.put("experienceProgress", experienceProgress);
        map.put("health", health);
        map.put("foodLevel", foodLevel);
        map.put("saturation", saturation);
        return map;
    }

    /**
     * Creates a PlayerState from a serialized Map.
     *
     * @param map the serialized data
     * @return the deserialized PlayerState
     */
    @SuppressWarnings("unchecked")
    public static PlayerState deserialize(Map<String, Object> map) {
        ItemStack[] inventory = ((java.util.List<ItemStack>) map.get("inventoryContents"))
                .toArray(new ItemStack[0]);
        ItemStack[] armor = ((java.util.List<ItemStack>) map.get("armorContents"))
                .toArray(new ItemStack[0]);
        ItemStack offHand = (ItemStack) map.get("offHandItem");
        int totalExp = (int) map.get("totalExperience");
        int expLevel = (int) map.get("experienceLevel");
        float expProgress = ((Number) map.get("experienceProgress")).floatValue();
        double health = ((Number) map.get("health")).doubleValue();
        int food = (int) map.get("foodLevel");
        float saturation = ((Number) map.get("saturation")).floatValue();

        return new PlayerState(inventory, armor, offHand, totalExp, expLevel,
                expProgress, health, food, saturation);
    }

    // Getters for accessing state data
    public ItemStack[] getInventoryContents() {
        return inventoryContents;
    }

    public ItemStack[] getArmorContents() {
        return armorContents;
    }

    public ItemStack offHandItem() {
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
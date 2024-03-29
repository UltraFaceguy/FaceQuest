package com.questworld.quest;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.questworld.api.MissionType;
import com.questworld.api.QuestWorld;
import com.questworld.api.contract.IMissionState;
import com.questworld.manager.ProgressTracker;
import com.questworld.util.EntityTools;
import com.questworld.util.Log;
import com.questworld.util.Text;

class Mission extends UniqueObject implements IMissionState {

	private WeakReference<Quest> quest;
	private int amount = 1;
	private int customInt = 0;
	private String customString = "";
	private boolean deathReset = false;
	private String description = "Hey there! Do this Quest.";
	private List<String> dialogue = new ArrayList<>();
	private String displayName = "";
	private EntityType entity = EntityType.PLAYER;
	private ItemStack item = new ItemStack(Material.STONE);
	private int index = -1;
	private Location location = Bukkit.getWorlds().get(0).getSpawnLocation();
	private boolean spawnerSupport = true;
	private boolean partySupport = false;
	private boolean actionBarUpdates = false;
	private String waypointerId = "";
	private int timeframe = 0;
	private MissionType type = QuestWorld.getMissionType("SUBMIT");

	@Deprecated
	private String missingWorldName = location.getWorld().getName();
	private static HashSet<String> missingWorlds = new HashSet<>();

	public Mission(int menuIndex, Quest quest) {
		this.index = menuIndex;
		this.quest = new WeakReference<>(quest);
	}

	public Mission(Map<String, Object> data) {
		loadMap(data);
		ProgressTracker.loadDialogue(this);
	}

	protected Mission(Mission source) {
		copy(source);
	}

	// Repair any quests that would have been broken by updates, namely location
	// quests
	public void validate() {
		type.validate(this.getState());
	}

	//// IMission
	@Override
	public int getAmount() {
		return amount;
	}

	@Override
	public int getCustomInt() {
		return customInt;
	}

	@Override
	public String getCustomString() {
		return customString;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<String> getDialogue() {
		return Collections.unmodifiableList(dialogue);
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public EntityType getEntity() {
		return entity;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public Location getLocation() {
		return location.clone();
	}

	@Override
	public ItemStack getItem() {
		return item.clone();
	}

	@Override
	public Quest getQuest() {
		return quest.get();
	}

	@Override
	public boolean getSpawnerSupport() {
		return spawnerSupport;
	}

	@Override
	public int getTimeframe() {
		return timeframe;
	}

	@Override
	public boolean getActionBarUpdates() {
		return actionBarUpdates;
	}

	@Override
	public String getWaypointerId() {
		return waypointerId;
	}

	@Override
	public MissionType getType() {
		return type;
	}

	@Override
	public boolean getDeathReset() {
		return deathReset && type.supportsDeathReset();
	}

	@Override
	public ItemStack getDisplayItem() {
		return type.userDisplayItem(this).clone();
	}

	@Override
	public MissionState getState() {
		return new MissionState(this);
	}

	@Override
	public String getText() {
		if (getDisplayName().length() > 0)
			return getDisplayName();

		return type.userDescription(this);
	}

	public HashMap<String, Object> serialize() {
		HashMap<String, Object> result = new HashMap<>(20);

		result.put("uniqueId", getUniqueId().toString());
		result.put("quest", getQuest());
		result.put("type", type.toString());
		result.put("item", item);
		result.put("amount", amount);
		result.put("entity", EntityTools.serialNameOf(entity));
		result.put("location", locationHelper(location));
		result.put("index", index);
		result.put("custom_string", Text.escapeColor(customString));
		result.put("display-name", Text.escapeColor(displayName));
		result.put("timeframe", timeframe);
		result.put("reset-on-death", deathReset);
		result.put("lore", Text.escapeColor(description));
		result.put("custom_int", customInt);
		result.put("exclude-spawners", !spawnerSupport);
		result.put("allow-parties", partySupport);
		result.put("action-bar-updates", actionBarUpdates);
		result.put("waypointer-id", waypointerId);

		result.put("questId", getQuest().getUniqueId().toString());

		return result;
	}

	@Override
	public void setAmount(int amount) {
		this.amount = amount;
	}

	@Override
	public void setCustomInt(int val) {
		customInt = val;
	}

	@Override
	public void setCustomString(String customString) {
		this.customString = customString;
	}

	@Override
	public void setDeathReset(boolean deathReset) {
		this.deathReset = deathReset;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setDialogue(List<String> dialogue) {
		this.dialogue.clear();
		this.dialogue.addAll(dialogue);
	}

	@Override
	public void setDisplayName(String name) {
		displayName = name;
	}

	@Override
	public void setEntity(EntityType entity) {
		this.entity = entity;
	}

	@Override
	public void setItem(ItemStack item) {
		this.item = item.clone();
		this.item.setAmount(1);
	}

	@Override
	public void setLocation(Location loc) {
		this.location = loc.clone();
		this.missingWorldName = loc.getWorld().getName();
	}

	@Override
	public void setSpawnerSupport(boolean acceptsSpawners) {
		spawnerSupport = acceptsSpawners;
	}

	@Override
	public void setType(MissionType type) {
		this.type = type;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public void setTimeframe(int timeframe) {
		this.timeframe = timeframe;
	}

	@Override
	public void setPartySupport(boolean partySupport) {
		this.partySupport = partySupport;
	}

	@Override
	public void setActionBarUpdates(boolean actionBarUpdates) {
		this.actionBarUpdates = actionBarUpdates;
	}

	@Override
	public void setWaypointerId(String waypointerId) {
		this.waypointerId = waypointerId;
	}

	@Override
	public boolean apply() {
		return true;
	}

	@Override
	public boolean discard() {
		return false;
	}

	@Override
	public Mission getSource() {
		return this;
	}

	@Override
	public boolean hasChange(Member field) {
		return true;
	}

	@Override
	protected void updateLastModified() {
		// TODO Remove if/when quests get their own files
		getQuest().updateLastModified();
	}

	protected void copy(Mission source) {
		quest = source.quest;
		amount = source.amount;
		customInt = source.customInt;
		customString = source.customString;
		deathReset = source.deathReset;
		description = source.description;
		setDialogue(source.dialogue);
		displayName = source.displayName;
		entity = source.entity;
		item = source.item.clone();
		index = source.index;
		location = source.location;
		spawnerSupport = source.spawnerSupport;
		timeframe = source.timeframe;
		type = source.type;
		missingWorldName = source.missingWorldName;
		actionBarUpdates = source.actionBarUpdates;
		partySupport = source.partySupport;
		waypointerId = source.waypointerId;
	}

	protected void copyTo(Mission dest) {
		dest.copy(this);
	}

	protected void loadDefaults() {
		copy(new Mission(index, getQuest()));
	}

	private int fromMaybeString(Object o) {
		if (o instanceof Integer)
			return (Integer) o;
		if (o instanceof String)
			return Integer.parseInt((String) o);

		throw new IllegalArgumentException("Expected Integer or String, got " + o.getClass().getSimpleName());
	}

	@SuppressWarnings("unchecked")
	private void loadMap(Map<String, Object> data) {
		setUniqueId((String) data.get("uniqueId"));

		quest = new WeakReference<>((Quest) data.get("quest"));
		type = QuestWorld.getMissionType((String) data.getOrDefault("type", type.toString()));
		ItemStack i2 = (ItemStack) data.getOrDefault("item", item);

		if (i2.getType() != Material.AIR)
			item = i2;

		amount = (Integer) data.getOrDefault("amount", amount);
		
		entity = EntityTools.deserializeType((String)data.get("entity"));
		location = locationHelper((Map<String, Object>) data.get("location"));
		if (location.getWorld() == null && !missingWorlds.contains(missingWorldName)) {
			Log.warning("Mission location exists in missing world \"" + missingWorldName + "\". Was it deleted?");
			missingWorlds.add(missingWorldName);
		}
		index = (Integer) data.getOrDefault("index", index);
		// Chain to handle old name
		customString = (String) data.getOrDefault("name", customString);
		customString = Text.colorize((String) data.getOrDefault("custom_string", customString));
		displayName = Text.colorize((String) data.getOrDefault("display-name", displayName));

		timeframe = fromMaybeString(data.getOrDefault("timeframe", timeframe));

		deathReset = (Boolean) data.getOrDefault("reset-on-death", deathReset);
		description = Text.colorize((String) data.getOrDefault("lore", description));
		// Chain to handle old name
		customInt = (Integer) data.getOrDefault("citizen", customInt);
		customInt = (Integer) data.getOrDefault("custom_int", customInt);
		spawnerSupport = !(Boolean) data.getOrDefault("exclude-spawners", !spawnerSupport);
		partySupport = (Boolean) data.getOrDefault("allow-parties", partySupport);
		actionBarUpdates = (Boolean) data.getOrDefault("action-bar-updates", actionBarUpdates);
		waypointerId = (String) data.getOrDefault("waypointer-id", waypointerId);
	}

	private Location locationHelper(Map<String, Object> data) {
		missingWorldName = (String) data.getOrDefault("world", missingWorldName);
		return new Location(Bukkit.getWorld(missingWorldName), (Double) data.getOrDefault("x", 0.0),
				(Double) data.getOrDefault("y", 64.0), (Double) data.getOrDefault("z", 0.0),
				((Double) data.getOrDefault("yaw", 0.0)).floatValue(),
				((Double) data.getOrDefault("pitch", 0.0)).floatValue());
	}

	private HashMap<String, Object> locationHelper(Location location) {
		HashMap<String, Object> result = new HashMap<>();
		String worldName = missingWorldName;
		if (location.getWorld() != null)
			worldName = location.getWorld().getName();

		result.put("world", worldName);
		result.put("x", location.getX());
		result.put("y", location.getY());
		result.put("z", location.getZ());
		result.put("yaw", (double) location.getYaw());
		result.put("pitch", (double) location.getPitch());

		return result;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
}

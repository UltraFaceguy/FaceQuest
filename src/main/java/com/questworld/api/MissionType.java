package com.questworld.api;

import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.questworld.api.annotation.Control;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IMissionState;
import com.questworld.api.menu.Menu;
import com.questworld.api.menu.MenuData;
import com.questworld.api.menu.MissionButton;
import com.questworld.util.Log;
import com.questworld.util.Text;

public abstract class MissionType {

	private String name;
	private final boolean supportsTimeframes;
	private final Map<Integer, MenuData> menuData;

	private final ItemStack selectorItem;

	public MissionType(String name, boolean supportsTimeframes, int modelData) {
		Log.fine("MissionType - Creating: " + name);
		this.name = name;
		this.supportsTimeframes = supportsTimeframes;
		selectorItem = new ItemStack(Material.PAPER);
		ItemStackExtensionsKt.setCustomModelData(selectorItem, modelData);
		menuData = new HashMap<>();
	}

	public final String userDescription(IMission instance) {
		return userInstanceDescription(instance) + formatTimeframe(instance) + formatDeathReset(instance);
	}

	protected abstract String userInstanceDescription(IMission instance);

	public abstract ItemStack userDisplayItem(IMission instance);

	private final String formatTimeframe(IMission instance) {
		if (instance.getTimeframe() == 0 || !supportsTimeframes)
			return "";
		long duration = instance.getTimeframe();

		return " " + QuestWorld.translate(Translation.TIMEFRAME_LABEL, Text.timeFromNum(duration));
	}

	private final String formatDeathReset(IMission instance) {
		if (!instance.getDeathReset() || !supportsDeathReset())
			return "";

		return " " + QuestWorld.translate(Translation.DEATH_LABEL);
	}

	public final ItemStack getSelectorItem() {
		return selectorItem;
	}

	public final String getName() {
		return name;
	}

	public final boolean supportsTimeframes() {
		return supportsTimeframes;
	}

	@Override
	@Control
	public String toString() {
		return getName();
	}

	public final boolean supportsDeathReset() {
		return this instanceof Decaying;
	}

	protected final void setName(String newName) {
		name = newName;
	}

	@Control
	public void validate(IMissionState instance) {
	}

	public final MenuData getButton(int index) {
		return menuData.get(index);
	}

	public final MenuData putButton(int index, MenuData data) {
		return menuData.put(index, data);
	}

	public final MenuData removeButton(int index) {
		return menuData.remove(index);
	}

	public final void buildMenu(IMission mission, Menu menu) {
		IMissionState changes = mission.getState();
		putButton(3, MissionButton.actionBarUpdates(changes));
		putButton(4, MissionButton.waypointer(changes));
		if (supportsDeathReset())
			putButton(5, MissionButton.deathReset(changes));
		if (supportsTimeframes())
			putButton(6, MissionButton.timeframe(changes));
		putButton(7, MissionButton.missionName(changes));
		putButton(8, MissionButton.dialogue(mission));

		validate(changes);
		layoutMenu(changes);

		for (Map.Entry<Integer, MenuData> entry : menuData.entrySet())
			menu.put(entry.getKey(), entry.getValue().getItem(), entry.getValue().getHandler());
	}

	@Control
	protected void layoutMenu(IMissionState changes) {
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof MissionType && name.equals(((MissionType) o).name);
	}
}

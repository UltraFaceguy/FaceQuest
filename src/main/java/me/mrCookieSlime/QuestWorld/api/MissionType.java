package me.mrCookieSlime.QuestWorld.api;

import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.QuestWorld.QuestWorld;
import me.mrCookieSlime.QuestWorld.api.interfaces.IMission;
import me.mrCookieSlime.QuestWorld.api.interfaces.IMissionWrite;
import me.mrCookieSlime.QuestWorld.api.menu.MenuData;
import me.mrCookieSlime.QuestWorld.api.menu.MissionButton;
import me.mrCookieSlime.QuestWorld.utils.Log;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public abstract class MissionType {
	String name;
	MaterialData selectorItem;
	boolean supportsTimeframes, supportsDeathReset;
	private Map<Integer, MenuData> menuData;
	
	public MissionType(String name, boolean supportsTimeframes, boolean supportsDeathReset, MaterialData item) {
		Log.fine("MissionType - Creating: " + name);
		this.name = name;
		this.selectorItem = item;
		this.supportsTimeframes = supportsTimeframes;
		this.supportsDeathReset = supportsDeathReset;
		menuData = new HashMap<>();
	}
	
	public final String defaultDisplayName(IMission instance) {
		return displayString(instance) + formatTimeframe(instance) + formatDeathReset(instance);
	}
	
	protected abstract String displayString(IMission instance);
	
	public abstract ItemStack displayItem(IMission instance);
	
	private String formatTimeframe(IMission quest) {
		if(!quest.hasTimeframe() || !supportsTimeframes)
			return "";
		long duration = quest.getTimeframe();
		
		return " &7within " + (duration / 60) + "h " + (duration % 60) + "m";
	}
	
	private String formatDeathReset(IMission quest) {
		if(!quest.resetsonDeath() || !supportsDeathReset)
			return "";
		
		return " &7without dying";
	}

	public MaterialData getSelectorItem() {
		return selectorItem;
	}

	public static MissionType valueOf(String id) {
		MissionType result =  QuestWorld.getInstance().getMissionTypes().get(id);
		
		if(result == null) {
			throw new NullPointerException("Tried to fetch mission type:" + id + " that doesn't exist!");
		}
		
		return result;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean supportsTimeframes() {
		return this.supportsTimeframes;
	}
	
	@Override
	public String toString() {
		return getName();
	}

	public boolean supportsDeathReset() {
		return this.supportsDeathReset;
	}

	protected void setName(String newName) {
		name = newName;
	}
	
	public boolean attemptUpgrade(IMissionWrite instance) {
		return false;
	}
	
	protected void setSelectorMaterial(MaterialData material) {
		selectorItem = material;
	}
	
	public String progressString(float percent, int current, int total) {
		return Math.round(percent * 100) + "% (" + current + "/" + total + ")";
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
	 
	public final void buildMenu(MissionChange changes, ChestMenu menu) {
		layoutMenu(changes);
		for(Map.Entry<Integer, MenuData> entry : menuData.entrySet())
			menu.addItem(entry.getKey(), entry.getValue().getItem(), entry.getValue().getHandler());
	}
	
	protected void layoutMenu(MissionChange changes) {
		if(supportsDeathReset()) putButton(5, MissionButton.deathReset(changes));
		if(supportsTimeframes()) putButton(6, MissionButton.timeframe(changes));
		putButton(7, MissionButton.missionName(changes));
		putButton(8, MissionButton.dialogue(changes));
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}

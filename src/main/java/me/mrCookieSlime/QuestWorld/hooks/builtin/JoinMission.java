package me.mrCookieSlime.QuestWorld.hooks.builtin;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import me.mrCookieSlime.QuestWorld.QuestWorld;
import me.mrCookieSlime.QuestWorld.api.MissionChange;
import me.mrCookieSlime.QuestWorld.api.MissionType;
import me.mrCookieSlime.QuestWorld.api.interfaces.IMission;
import me.mrCookieSlime.QuestWorld.api.menu.MissionButton;

public class JoinMission extends MissionType implements Listener {
	public JoinMission() {
		super("JOIN", true, false, new MaterialData(Material.GOLD_NUGGET));
	}
	
	@Override
	public ItemStack displayItem(IMission instance) {
		return new ItemStack(Material.WATCH);
	}
	
	@Override
	protected String displayString(IMission instance) {
		return "&7Join " + instance.getAmount() + " times";
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		QuestWorld.getInstance().getManager(e.getPlayer()).forEachTaskOf(this, mission -> true);
	}
	
	@Override
	protected void layoutMenu(MissionChange changes) {
		super.layoutMenu(changes);
		putButton(17, MissionButton.amount(changes));
	}
}

package com.questworld.listener;

import com.questworld.GuideBook;
import com.questworld.QuestingImpl;
import com.questworld.api.Decaying;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IPlayerStatus;
import com.questworld.api.event.CancellableEvent;
import com.questworld.api.event.GenericPlayerLeaveEvent;
import com.questworld.api.menu.QuestBook;
import com.questworld.manager.ProgressTracker;
import com.questworld.util.AutoListener;
import com.questworld.util.TransientPermissionUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener extends AutoListener {

	private final QuestingImpl api;

	public PlayerListener(QuestingImpl api) {
		this.api = api;
		register(api.getPlugin());
	}

	@EventHandler
	public void onQuestBook(PlayerInteractEvent event) {
		Action a = event.getAction();
		if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)
			if (GuideBook.isGuide(event.getItem()))
				QuestBook.openLastMenu(event.getPlayer());
	}

	@EventHandler
	public void onDie(PlayerDeathEvent event) {
		Player p = event.getEntity();
		IPlayerStatus playerStatus = api.getPlayerStatus(p);

		for (IMission mission : api.getViewer().getDecayingMissions())
			if (playerStatus.hasDeathEvent(mission))
				((Decaying) mission.getType()).onDeath(event, api.getMissionEntry(mission, p));
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();

		if (api.getPlugin().getConfig().getBoolean("book.on-first-join") && !ProgressTracker.exists(p.getUniqueId()))
			p.getInventory().addItem(GuideBook.instance().item());

		TransientPermissionUtil.updateTransientPerms(p);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onKick(PlayerKickEvent event) {
		CancellableEvent.send(new GenericPlayerLeaveEvent(event.getPlayer()));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		CancellableEvent.send(new GenericPlayerLeaveEvent(event.getPlayer()));
	}

	@EventHandler
	public void onLeave(GenericPlayerLeaveEvent event) {
		Player player = event.getPlayer();
		api.unloadPlayerStatus(player);
	}

	// Since we can't (yet) randomly update recipes at runtime, replace result with
	// latest lore
	@EventHandler
	public void preCraft(PrepareItemCraftEvent e) {
		boolean hasTable = false;
		for (ItemStack is : e.getInventory().getMatrix())
			if (is != null) {
				if (is.getType() == Material.CRAFTING_TABLE && !hasTable)
					hasTable = true;
				else
					return;
			}

		if (hasTable)
			e.getInventory().setResult(GuideBook.instance().item());
	}
}

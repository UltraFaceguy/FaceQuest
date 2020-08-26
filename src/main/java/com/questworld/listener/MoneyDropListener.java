package com.questworld.listener;

import com.questworld.QuestWorldPlugin;
import com.questworld.util.AutoListener;
import com.tealcube.minecraft.bukkit.bullion.PlayerDeathDropEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

public class MoneyDropListener extends AutoListener {

	public MoneyDropListener(Plugin plugin) {
		register(plugin);
	}

	@EventHandler
	public void onDeathMoneyLoss(PlayerDeathDropEvent e) {
		if (e.isCancelled()) {
			return;
		}
		int qp = QuestWorldPlugin.getAPI().getPlayerStatus(e.getVictim()).getQuestPoints();
		e.setAmountProtected(e.getAmountProtected() + qp * 5);
	}
}
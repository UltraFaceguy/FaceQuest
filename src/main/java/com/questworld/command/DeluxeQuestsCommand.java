package com.questworld.command;

import com.questworld.QuestWorldPlugin;
import com.questworld.api.menu.DeluxeQuestBook;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeluxeQuestsCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			QuestWorldPlugin.get().getQuestMenu().open((Player) sender);
		}
		return true;
	}
}

package com.questworld.command;

import com.questworld.QuestingImpl;
import com.questworld.api.contract.MissionEntry;
import com.questworld.extension.builtin.ExternalCommandMission;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QuestProgressCommand implements CommandExecutor {

  private final QuestingImpl api;
  private final ExternalCommandMission externalCommandMission;

  public QuestProgressCommand(QuestingImpl api) {
    this.api = api;
    externalCommandMission = api.getMissionType("EXTERNAL_COMMAND");
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
      @NotNull String label, String[] args) {
		if (args.length < 2 || args.length > 3) {
			MessageUtils.sendMessage(sender, "/q-external <player> <missionId> [amount]");
			return false;
		}
    Player player = Bukkit.getPlayer(args[0]);
    String id = args[1];
    int amount = 1;
    if (args.length > 2) {
      try {
        amount = Integer.parseInt(args[2]);
      } catch (Exception e) {
        Bukkit.getLogger().warning("Tried to increment " + label + " by " + args[2]);
      }
    }
    amount = Math.max(1, amount);

    for (MissionEntry r : api.getMissionEntries(externalCommandMission, player)) {
      if (r.getMission().getCustomString() == null) {
        continue;
      }
      if (r.getMission().getCustomString().equals(id)) {
        r.addProgress(amount);
      }
    }
    return true;
  }
}

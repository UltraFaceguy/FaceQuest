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

public class QuestProgressCommand implements CommandExecutor {

  private final QuestingImpl api;
  private ExternalCommandMission externalCommandMission;

  public QuestProgressCommand(QuestingImpl api) {
    this.api = api;
    externalCommandMission = api.getMissionType("EXTERNAL_COMMAND");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length < 2 || args.length > 3) {
			MessageUtils.sendMessage(sender, "/q-external <player> <missionId> [amount]");
			return false;
		}
    System.out.println(args[0]);
    System.out.println(args[1]);
    Player player = Bukkit.getPlayer(args[0]);
    String id = args[1];
    int amount = args.length == 3 ? Integer.parseInt(args[2]) : 1;

    System.out.println(id);

    for (MissionEntry r : api.getMissionEntries(externalCommandMission, player)) {
      if (r.getMission().getCustomString() == null) {
        System.out.println(r.getMission().getDisplayName() + " ccccc");
        continue;
      }
      System.out.println(r.getMission().getCustomString());
      if (r.getMission().getCustomString().equals(id)) {
        r.addProgress(amount);
        System.out.println("ccccc");
      }
    }
    return true;
  }
}

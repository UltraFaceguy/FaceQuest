package com.questworld.extension.builtin;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import com.questworld.api.MissionType;
import com.questworld.api.Ticking;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IMissionState;
import com.questworld.api.contract.MissionEntry;
import com.questworld.api.event.QuestCompleteEvent;
import com.questworld.api.menu.MenuData;
import com.questworld.api.menu.MissionButton;
import com.questworld.util.ItemBuilder;
import com.questworld.util.Text;

public class PlayMission extends MissionType implements Listener, Ticking {

  private static final int TOTAL = 0;
  private HashMap<UUID, HashMap<UUID, Long>> timeMap = new HashMap<>();

  public PlayMission() {
    super("PLAY_TIME", false, 1005);
  }

  @Override
  public ItemStack userDisplayItem(IMission instance) {
    return getSelectorItem().clone();
  }

  @Override
  protected String userInstanceDescription(IMission instance) {
    return "&7Play for " + Text.timeFromNum(instance.getAmount());
  }

  @Override
  public void onManual(Player player, MissionEntry result) {
    if (result.getMission().getCustomInt() == TOTAL) {
      result.setProgress(player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60);
    } else {
			HashMap<UUID, Long> times = timeMap
					.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

			long currentTime = System.currentTimeMillis();

      times.putIfAbsent(result.getMission().getUniqueId(), currentTime);
      long startTime = times.get(result.getMission().getUniqueId());

      long elapsedMinutes = Math.max((currentTime - startTime) / 1000 / 60, 0);
      result.setProgress((int) elapsedMinutes);
    }
  }

  @Override
  protected void layoutMenu(IMissionState changes) {
    putButton(17, new MenuData(
        new ItemBuilder(Material.CLOCK).wrapText(
            "&7Time: &b" + Text.timeFromNum(changes.getAmount()),
            "",
            "&rLeft click: &e+1m",
            "&rRight click: &e-1m",
            "&rShift left click: &e+1h",
            "&rShift right click: &e-1h").get(),
        event -> {
          int amount = MissionButton.clickNumber(changes.getAmount(), 60, event);
					if (amount < 1) {
						amount = 1;
					}
          changes.setAmount(amount);
          MissionButton.apply(event, changes);
        }

    ));

    putButton(16, MissionButton.simpleButton(changes,
        new ItemBuilder(Material.GOLDEN_APPLE)
            .display("&7Counting method")
            .selector(changes.getCustomInt(), "Total", "Session").get(),
        event -> {
          changes.setCustomInt(1 - changes.getCustomInt());
        }
    ));
  }

  // Reset all on join
  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    timeMap.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onQuestComplete(QuestCompleteEvent event) {
    HashMap<UUID, Long> times = timeMap.get(event.getPlayer().getUniqueId());

    if (times != null) {
      for (IMission m : event.getQuest().getMissions()) {
        if (m.getType() == this) {
          times.remove(m.getUniqueId());
        }
      }
    }
  }
}

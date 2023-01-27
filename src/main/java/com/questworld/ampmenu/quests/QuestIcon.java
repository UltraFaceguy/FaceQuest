/**
 * The MIT License Copyright (c) 2015 Teal Cube Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.questworld.ampmenu.quests;

import com.questworld.QuestWorldPlugin;
import com.questworld.api.QuestStatus;
import com.questworld.api.QuestWorld;
import com.questworld.api.Translation;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IPlayerStatus;
import com.questworld.api.contract.IQuest;
import com.questworld.api.menu.RewardsPrompt;
import com.questworld.util.ItemBuilder;
import com.questworld.util.Text;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import land.face.waypointer.WaypointerPlugin;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class QuestIcon extends MenuItem {

  private final QuestListMenu menu;
  private IQuest currentQuest;
  private final int index;

  QuestIcon(QuestListMenu menu, int index) {
    super("", new ItemStack(Material.TOTEM_OF_UNDYING));
    this.menu = menu;
    this.index = index;
  }

  @Override
  public ItemStack getFinalIcon(Player player) {
    IQuest quest = menu.getQuest(index);
    currentQuest = quest;
    if (quest == null) {
      return new ItemStack(Material.AIR);
    }
    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(player);

    QuestStatus questStatus = playerStatus.getStatus(quest);
    int maxMissions = quest.getMissions().size();
    float completedMissions = 0;
    boolean hasCompletedAnyMission = false;
    String waypointerId = "";
    for (IMission mission : quest.getOrderedMissions()) {
      if (playerStatus.hasCompletedTask(mission)) {
        completedMissions++;
        hasCompletedAnyMission = true;
        continue;
      }
      completedMissions += (float) playerStatus.getProgress(mission) / mission.getAmount();
      if (StringUtils.isBlank(mission.getWaypointerId())) {
        continue;
      }
      waypointerId = mission.getWaypointerId();
      break;
    }

    String statusString;
    if (questStatus == QuestStatus.REWARD_CLAIMABLE) {
      statusString = QuestWorld.translate(player, Translation.quests_state_reward_claimable);
    } else if (questStatus == QuestStatus.ON_COOLDOWN) {
      statusString = QuestWorld.translate(player, Translation.quests_state_cooldown);
    } else if (playerStatus.hasFinished(quest)) {
      statusString = QuestWorld.translate(player, Translation.quests_state_completed);
    } else {
      statusString = Text.progressBar(completedMissions / maxMissions);
    }
    String progressNum = Text.progressString((int) completedMissions, maxMissions);

    ItemBuilder builder = new ItemBuilder(quest.getItem());
    if (StringUtils.isBlank(waypointerId)) {
      return builder.wrapText(
          quest.getName(),
          "",
          statusString,
          progressNum
      ).get();
    } else {
      return builder.wrapText(
          quest.getName(),
          "",
          statusString,
          progressNum,
          "",
          "&b> Right-click to set waypoint! <"
      ).get();
    }
  }

  @Override
  public void onItemClick(ItemClickEvent event) {
    super.onItemClick(event);
    event.setWillClose(false);
    event.setWillUpdate(false);
    if (currentQuest == null) {
      return;
    }
    if (event.getClickType() == ClickType.LEFT || event.getClickType() == ClickType.SHIFT_LEFT) {
      IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(event.getPlayer());
      QuestStatus questStatus = playerStatus.getStatus(currentQuest);
      if (questStatus == QuestStatus.REWARD_CLAIMABLE) {
        new RewardsPrompt(currentQuest, event.getPlayer());
      } else {
        QuestWorldPlugin.get()
            .openMissionList(event.getPlayer(), menu.getDeluxeCategory(), currentQuest);
      }
    } else if (event.getClickType() == ClickType.RIGHT || event.getClickType() == ClickType.SHIFT_RIGHT) {
      IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(event.getPlayer());
      for (IMission mission : currentQuest.getOrderedMissions()) {
        if (playerStatus.hasCompletedTask(mission)) {
          continue;
        }
        if (StringUtils.isBlank(mission.getWaypointerId())) {
          continue;
        }
        WaypointerPlugin.getInstance().getWaypointManager()
            .setWaypoint(event.getPlayer(), mission.getWaypointerId());
        return;
      }
    }
  }
}

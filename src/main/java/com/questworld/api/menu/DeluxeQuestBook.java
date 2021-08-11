package com.questworld.api.menu;

import com.questworld.api.Manual;
import com.questworld.api.QuestStatus;
import com.questworld.api.QuestWorld;
import com.questworld.api.Translation;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IPlayerStatus;
import com.questworld.api.contract.IPlayerStatus.DeluxeCategory;
import com.questworld.api.contract.IQuest;
import com.questworld.util.ItemBuilder;
import com.questworld.util.Text;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Map;
import land.face.waypointer.WaypointerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class DeluxeQuestBook {

  public static void openMainMenu(Player p) {
    QuestWorld.getSounds().QUEST_CLICK.playTo(p);

    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(p);
    playerStatus.update();

    Map<DeluxeCategory, List<IQuest>> quests = playerStatus.getQuests();

    new DeluxeCategoryPicker(quests, p, QuestWorld.translate(p, Translation.gui_title));
  }

  public static void openCategory(Player p, DeluxeCategory deluxeCategory, List<IQuest> quests,
      final boolean back) {

    if (quests == null) {
      quests = QuestWorld.getPlayerStatus(p).getQuests().get(deluxeCategory);
    }

    QuestWorld.getSounds().QUEST_CLICK.playTo(p);
    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(p);
    playerStatus.update();

    String title;
    switch (deluxeCategory) {
      case OPEN_QUESTS:
        title = "New Quests";
        break;
      case UNFINISHED_QUESTS:
        title = "Current Quests";
        break;
      case DAILY_QUESTS:
        title = "New Quests";
        break;
      case UNCLAIMED_REWARDS:
        title = "Claim Rewards!";
        break;
      case COMPLETED_QUESTS:
        title = "ayyy u dun it";
        break;
      default:
        title = "???";
    }

    Menu menu = new Menu(1, title);
    int cellsPerPage = 36;
    PagedMapping view = new PagedMapping(cellsPerPage, 18);

    if (back) {
      int page = PagedMapping.popPage(p);
      PagedMapping.putPage(p, quests.size() / cellsPerPage);
      PagedMapping.putPage(p, page);

      view.setBackButton(" " + QuestWorld.translate(p, Translation.gui_title),
          event -> openMainMenu((Player) event.getWhoClicked()));
    }

    int index = 0;
    for (final IQuest quest : quests) {

      if (!quest.isEnabled()) {
        continue;
      }

      QuestStatus questStatus = playerStatus.getStatus(quest);

      int maxMissions = quest.getMissions().size();
      float completedMissions = 0;
      String waypointerId = "";
      for (IMission mission : quest.getOrderedMissions()) {
        if (playerStatus.hasCompletedTask(mission)) {
          completedMissions++;
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
        statusString = QuestWorld.translate(p, Translation.quests_state_reward_claimable);
      } else if (questStatus == QuestStatus.ON_COOLDOWN) {
        statusString = QuestWorld.translate(p, Translation.quests_state_cooldown);
      } else if (playerStatus.hasFinished(quest)) {
        statusString = QuestWorld.translate(p, Translation.quests_state_completed);
      } else {
        int percent = (int) (100 * completedMissions / maxMissions);
        statusString = playerStatus.progressString(quest, completedMissions / maxMissions) + " "
            + ChatColor.WHITE + percent + "%";
      }

      if (StringUtils.isBlank(waypointerId)) {
        view.addButton(index,
            new ItemBuilder(quest.getItem()).wrapText(quest.getName(), "", statusString).get(),
            event -> openQuest((Player) event.getWhoClicked(), quest, deluxeCategory, true), true);
      } else {
        String finalWaypointerId = waypointerId;
        view.addButton(index,
            new ItemBuilder(quest.getItem()).wrapText(quest.getName(), "", statusString, "",
                "&b✖ Right-click to set a waypoint!").get(),
            event -> {
              if (event.getClick() == ClickType.RIGHT) {
                setWaypoint((Player) event.getWhoClicked(), finalWaypointerId);
              } else {
                openQuest((Player) event.getWhoClicked(), quest, deluxeCategory, true);
              }
            }, true);
      }
      index++;
    }
    view.build(menu, p);
    menu.openFor(p);
  }

  public static void setWaypoint(final Player p, final String waypointerId) {
    WaypointerPlugin.getInstance().getWaypointManager().setWaypoint(p, waypointerId);
  }

  public static void openQuest(final Player p, final IQuest quest, final DeluxeCategory category, final boolean back) {
    QuestWorld.getSounds().QUEST_CLICK.playTo(p);
    IPlayerStatus manager = QuestWorld.getPlayerStatus(p);
    manager.update();

    if (manager.getStatus(quest) == QuestStatus.REWARD_CLAIMABLE) {
      new RewardsPrompt(quest, p);
      return;
    }

    String title = ChatColor.stripColor(quest.getName());
    if (title.length() > 18) {
      title = title.substring(0, 15) + "...";
    }

    Menu menu = new Menu(3, title);
    PagedMapping view = new PagedMapping(27, 9);

    if (back) {
      int page = PagedMapping.popPage(p);
      PagedMapping.putPage(p, quest.getID() / 45);
      PagedMapping.putPage(p, page);

      view.setBackButton("Return to categories",
          event -> openCategory((Player) event.getWhoClicked(), category, null, true));
    }

    if (quest.getCooldown() >= 0) {
      String cooldown = quest.getFormattedCooldown();
      if (manager.getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
        // Poor mans "Math.ceil" for integers
        long remaining =
            (manager.getCooldownEnd(quest) - System.currentTimeMillis() + 59999) / 60 / 1000;
        cooldown = Text.timeFromNum(remaining) + " remaining";
      }
      view.addFrameButton(8, new ItemBuilder(Material.CLOCK)
              .wrapText(QuestWorld.translate(p, Translation.quests_display_cooldown), "",
                  "&b" + cooldown).get(),
          null, false);
    }

    view.addFrameButton(7, quest.generateRewardInfo(), null, false);

    ItemStack glassLocked = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
        .wrapText("&7&kSOMEWEIRDMISSION", "", QuestWorld.translate(p, Translation.task_locked))
        .get();

    int index = 0;
    for (final IMission mission : quest.getOrderedMissions()) {
      ItemStack item = glassLocked;

      if (manager.hasUnlockedTask(mission)) {
        ItemBuilder entryItem = new ItemBuilder(mission.getDisplayItem());
        int current = manager.getProgress(mission);
        int total = mission.getAmount();

        String progress = Text.progressBar(current, total, mission.getType().progressString(current, total));

        if (StringUtils.isBlank(mission.getWaypointerId())) {
          if (mission.getType() instanceof Manual) {
            entryItem.wrapText(mission.getText(), "", progress, "",
                ((Manual) mission.getType()).getLabel());
          } else {
            entryItem.wrapText(mission.getText(), "", progress);
          }
        } else {
          if (mission.getType() instanceof Manual) {
            entryItem.wrapText(mission.getText(), "", progress, "",
                "&b✖ Right-click to set a waypoint!", ((Manual) mission.getType()).getLabel());
          } else {
            entryItem.wrapText(mission.getText(), "", progress, "",
                "&b✖ Right-click to set a waypoint!");
          }
        }

        item = entryItem.get();
      }

      view.addButton(index, item, event -> {
        if (event.getClick() == ClickType.RIGHT && StringUtils
            .isNotBlank(mission.getWaypointerId())) {
          setWaypoint((Player) event.getWhoClicked(), mission.getWaypointerId());
          return;
        }
        if (!manager.hasUnlockedTask(mission)) {
          return;
        }
        if (manager.getStatus(quest).equals(QuestStatus.AVAILABLE)
            && quest.getWorldEnabled(p.getWorld().getName())) {
          if (manager.hasCompletedTask(mission)) {
            return;
          }

          if (mission.getType() instanceof Manual) {
            ((Manual) mission.getType()).onManual(p, QuestWorld.getMissionEntry(mission, p));
            openQuest(p, quest, category, back);
          }
        }
      }, true);
      index++;
    }

    view.build(menu, p);
    menu.openFor(p);
  }
}

package com.questworld.api.menu;

import static com.questworld.util.PlayerTools.getStrifeExpFromPercentage;

import com.questworld.Constants;
import com.questworld.api.Manual;
import com.questworld.api.MissionType;
import com.questworld.api.QuestStatus;
import com.questworld.api.QuestWorld;
import com.questworld.api.SinglePrompt;
import com.questworld.api.Translation;
import com.questworld.api.contract.DataObject;
import com.questworld.api.contract.ICategory;
import com.questworld.api.contract.ICategoryState;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IMissionState;
import com.questworld.api.contract.IPlayerStatus;
import com.questworld.api.contract.IQuest;
import com.questworld.api.contract.IQuestState;
import com.questworld.util.ItemBuilder;
import com.questworld.util.ItemBuilder.Proto;
import com.questworld.util.PlayerTools;
import com.questworld.util.Text;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class QuestBook {

  public static DataObject getLastViewed(Player p) {
    List<MetadataValue> metadata = p.getMetadata(Constants.MD_LAST_MENU);

    if (metadata.isEmpty()) {
      return null;
    }

    Object value = metadata.get(0).value();

    // Every ticket involving /reload JUST STOP DOING IT GUYS GOOD LORD THIS ISN'T RELOAD SAFE
    if (!(value instanceof DataObject)) {
      p.removeMetadata(Constants.MD_LAST_MENU, QuestWorld.getPlugin());
      return null;
    }

    return (DataObject) value;
  }

  public static boolean testCategory(Player p, ICategory category) {
    IQuest parent = category.getParent();
    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(p);

    return !category.isHidden() && category.isWorldEnabled(p.getWorld().getName())
        && PlayerTools.checkPermission(p, category.getPermission())
        && (parent == null || playerStatus.hasFinished(parent));
  }

  public static boolean testQuest(Player p, IQuest quest) {
    QuestStatus status = QuestWorld.getPlayerStatus(p).getStatus(quest);

    return quest.isEnabled() && (status == QuestStatus.AVAILABLE
        || status == QuestStatus.REWARD_CLAIMABLE
        || status == QuestStatus.ON_COOLDOWN || status == QuestStatus.FINISHED);
  }

  public static void clearLastViewed(Player p) {
    setLastViewed(p, null, true, true);
  }

  private static void setLastViewed(Player p, DataObject object) {
    p.setMetadata(Constants.MD_LAST_MENU, new FixedMetadataValue(QuestWorld.getPlugin(), object));
  }

  private static void setLastViewed(Player p, DataObject object, boolean catBack) {
    setLastViewed(p, object);

    if (!catBack) {
      p.setMetadata(Constants.MD_NO_CAT_BACK,
          new FixedMetadataValue(QuestWorld.getPlugin(), catBack));
    } else {
      p.removeMetadata(Constants.MD_NO_CAT_BACK, QuestWorld.getPlugin());
    }
  }

  private static void setLastViewed(Player p, DataObject object, boolean catBack,
      boolean questBack) {
    setLastViewed(p, object, catBack);

    if (!questBack) {
      p.setMetadata(Constants.MD_NO_QUEST_BACK,
          new FixedMetadataValue(QuestWorld.getPlugin(), questBack));
    } else {
      p.removeMetadata(Constants.MD_NO_QUEST_BACK, QuestWorld.getPlugin());
    }
  }

  private static boolean getCategoryBack(Player p) {
    return !p.hasMetadata(Constants.MD_NO_CAT_BACK);
  }

  private static boolean getQuestBack(Player p) {
    return !p.hasMetadata(Constants.MD_NO_QUEST_BACK);
  }

  public static void openMainMenu(Player p) {
    QuestWorld.getSounds().QUEST_CLICK.playTo(p);
    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(p);
    playerStatus.update();

    Menu menu = new Menu(1, QuestWorld.translate(p, Translation.gui_title));

    PagedMapping view = new PagedMapping(45, 9);

    for (ICategory category : QuestWorld.getFacade().getCategories()) {
      IQuest parent = category.getParent();

      if (!category.isHidden()) {
        if (!category.isWorldEnabled(p.getWorld().getName())) {
          view.addButton(category.getID(),
              new ItemBuilder(Material.BARRIER)
                  .wrapText(category.getName(), "",
                      QuestWorld.translate(p, Translation.LOCKED_WORLD, p.getWorld().getName()))
                  .get(),
              null, false);
        } else if (!PlayerTools.checkPermission(p, category.getPermission())) {
          String[] parts = category.getPermission().split(" ", 2);
          view.addButton(category.getID(),
              new ItemBuilder(Material.BARRIER).wrapText(category.getName(), "",
                  QuestWorld
                      .translate(p, Translation.LOCKED_NO_PERM, parts[0], parts[parts.length - 1]))
                  .get(),
              null, false);
        } else if (parent != null && !playerStatus.hasFinished(parent)) {
          view.addButton(category.getID(),
              new ItemBuilder(Material.BARRIER).wrapText(category.getName(), "",
                  QuestWorld
                      .translate(p, Translation.LOCKED_PARENT, category.getParent().getName()))
                  .get(),
              null, false);
        } else {

          int questCount = playerStatus.countQuests(category, null);
          int finishedCount = playerStatus.getProgress(category);
          view.addButton(category.getID(),
              new ItemBuilder(category.getItem()).wrapText((category.getName() + "\n"
                  + QuestWorld.translate(p, Translation.CATEGORY_DESC, String.valueOf(questCount),
                  String.valueOf(finishedCount),
                  String.valueOf(playerStatus.countQuests(category, QuestStatus.AVAILABLE)),
                  String.valueOf(playerStatus.countQuests(category, QuestStatus.ON_COOLDOWN)),
                  String.valueOf(playerStatus.countQuests(category, QuestStatus.REWARD_CLAIMABLE)),
                  Text.progressBar(finishedCount, questCount, StringUtils.EMPTY))).split("\n"))
                  .get(),
              event -> {
                Player p2 = (Player) event.getWhoClicked();
                PagedMapping.putPage(p2, 0);
                openCategory(p2, category, true);
              }, true);
        }
      }
    }
    view.build(menu, p, true);
    menu.openFor(p);
  }

  public static void openLastMenu(Player p) {
    DataObject last = getLastViewed(p);

    boolean catBack = getCategoryBack(p);
    boolean questBack = getQuestBack(p);

    if (last instanceof IQuest) {
      QuestBook.openQuest(p, (IQuest) last, catBack, questBack);
    } else if (last instanceof ICategory) {
      QuestBook.openCategory(p, (ICategory) last, catBack);
    } else {
      QuestBook.openMainMenu(p);
    }
  }

  public static void openCategory(Player p, ICategory category, final boolean back) {
    QuestWorld.getSounds().QUEST_CLICK.playTo(p);
    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(p);
    playerStatus.update();

    setLastViewed(p, category, back);

    Menu menu = new Menu(1, category.getName());
    ItemBuilder glassPane = new ItemBuilder(Material.RED_STAINED_GLASS_PANE);
    PagedMapping view = new PagedMapping(45, 9);

    if (back) {
      int page = PagedMapping.popPage(p);
      PagedMapping.putPage(p, category.getID() / 45);
      PagedMapping.putPage(p, page);

      view.setBackButton(" " + QuestWorld.translate(p, Translation.gui_title), event -> openMainMenu((Player) event.getWhoClicked()));
    }

    for (final IQuest quest : category.getQuests()) {
      if (!quest.isEnabled()) {
        continue;
      }

      IQuest parent = quest.getParent();

      QuestStatus questStatus = playerStatus.getStatus(quest);

      Translation translation;
      String[] keys;

      switch (playerStatus.getStatus(quest)) {
        case LOCKED_WORLD -> {
          translation = Translation.LOCKED_WORLD;
          keys = new String[]{p.getWorld().getName()};
        }
        case LOCKED_NO_PERM -> {
          String[] parts = quest.getPermission().split(" ", 2);
          translation = Translation.LOCKED_NO_PERM;
          keys = new String[]{parts[0], parts[parts.length - 1]};
        }
        case LOCKED_PARENT -> {
          translation = Translation.LOCKED_PARENT;
          keys = new String[]{parent.getName()};
        }
        case LOCKED_LEVEL -> {
          translation = Translation.LOCKED_LEVEL;
          keys = new String[]{Integer.toString(quest.getLevelRequirement())};
        }
        default -> {
          translation = null;
          keys = null;
        }
      }

      if (translation == null) {
        String extra = null;

        if (questStatus == QuestStatus.REWARD_CLAIMABLE) {
          extra = QuestWorld.translate(p, Translation.quests_state_reward_claimable);
        } else if (questStatus == QuestStatus.ON_COOLDOWN) {
          extra = QuestWorld.translate(p, Translation.quests_state_cooldown);
        } else if (playerStatus.hasFinished(quest)) {
          extra = QuestWorld.translate(p, Translation.quests_state_completed);
        }

        view.addButton(quest.getID(),
            new ItemBuilder(quest.getItem())
                .wrapText(quest.getName(), "", playerStatus.progressString(quest, 1), "",
                    "&7" + playerStatus.getProgress(quest) + "/" + quest.getMissions().size()
                        + QuestWorld.translate(p, Translation.quests_tasks_completed),
                    (extra == null) ? null : "", extra)
                .get(),
            event -> {
              openQuest((Player) event.getWhoClicked(), quest, back, true);
            }, true);
      } else {
        view.addButton(quest.getID(),
            glassPane.wrapText(quest.getName(), "",
                QuestWorld.translate(p, translation, keys)).getNew(),
            null, false);
      }
    }
    view.build(menu, p, false);
    menu.openFor(p);
  }

  public static void openQuest(final Player p, final IQuest quest, final boolean categoryBack,
      final boolean back) {
    QuestWorld.getSounds().QUEST_CLICK.playTo(p);
    IPlayerStatus manager = QuestWorld.getPlayerStatus(p);
    manager.update();

    setLastViewed(p, quest, categoryBack, back);

    Menu menu = new Menu(3, quest.getName());
    PagedMapping view = new PagedMapping(27, 9);

    if (back) {
      int page = PagedMapping.popPage(p);
      PagedMapping.putPage(p, quest.getID() / 45);
      PagedMapping.putPage(p, page);

      view.setBackButton(" " + quest.getCategory().getName(), event -> openCategory((Player) event.getWhoClicked(), quest.getCategory(), categoryBack));
    }

    // Detect all
    view.addFrameButton(3, new ItemBuilder(Material.CHEST).display("&7Check all Tasks").get(),
        event -> {
          for (IMission mission : quest.getOrderedMissions()) {
            if (!manager.hasUnlockedTask(mission)) {
              continue;
            }
            if (manager.getStatus(quest).equals(QuestStatus.AVAILABLE)
                && quest.getWorldEnabled(p.getWorld().getName())) {
              if (manager.hasCompletedTask(mission)) {
                continue;
              }

              if (mission.getType() instanceof Manual) {
                ((Manual) mission.getType()).onManual(p, QuestWorld.getMissionEntry(mission, p));
              }
            }
          }

          openQuest(p, quest, categoryBack, back);
        }, true);

    if (quest.getCooldown() >= 0) {
      String cooldown = quest.getFormattedCooldown();
      if (manager.getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
        // Poor mans "Math.ceil" for integers
        long remaining =
            (manager.getCooldownEnd(quest) - System.currentTimeMillis() + 59999) / 60 / 1000;
        cooldown = Text.timeFromNum(remaining) + " remaining";
      }
      view.addFrameButton(8, new ItemBuilder(Material.PAPER).modelData(994)
              .wrapText(QuestWorld.translate(p, Translation.quests_display_cooldown), "",
                  "&b" + cooldown).get(),
          null, false);
    }

    int rewardIndex = 5;
    if (quest.getMoney() > 0 && QuestWorld.getEconomy().isPresent()) {
      view.addFrameButton(rewardIndex, new ItemBuilder(Material.GOLD_INGOT)
          .wrapText(QuestWorld.translate(p, Translation.quests_display_monetary), "",
              "&6$" + quest.getMoney())
          .get(), null, false);
      rewardIndex++;
    }

    ItemStack glassLocked = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
        .wrapText("&7&kSOMEWEIRDMISSION", "", QuestWorld.translate(p, Translation.task_locked))
        .get();

    ItemStack glassClaimable = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
        .wrapText(QuestWorld.translate(p, Translation.quests_state_reward_claim)).get();

    ItemStack glassCooldown = new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
        .wrapText(QuestWorld.translate(p, Translation.quests_state_cooldown)).get();

    ItemStack glassInactive = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
        .wrapText(QuestWorld.translate(p, Translation.quests_display_rewards)).get();

    int index = 0;
    for (final IMission mission : quest.getOrderedMissions()) {
      ItemStack item = glassLocked;

      if (manager.hasUnlockedTask(mission)) {
        ItemBuilder entryItem = new ItemBuilder(mission.getDisplayItem());
        int current = manager.getProgress(mission);
        int total = mission.getAmount();
        String progress = Text.progressBar(current, total, mission.getType().progressString(current, total));
        progress += " " + ChatColor.WHITE + current + "/" + total;

        if (mission.getType() instanceof Manual) {
          entryItem.wrapText(mission.getText(), "", progress, "", ((Manual) mission.getType()).getLabel());
        } else {
          entryItem.wrapText(mission.getText(), "", progress);
        }

        item = entryItem.get();
      }

      view.addButton(index, item, event -> {
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
            openQuest(p, quest, categoryBack, back);
          }
        }
      }, true);
      index++;
    }

    int offset = 9 + 9 * Math.min(((quest.getMissions().size() + 8) / 9), 3);

    for (int i = 0; i < 9; i++) {
      if (manager.getStatus(quest).equals(QuestStatus.REWARD_CLAIMABLE)) {
        menu.put(i + offset, glassClaimable, event -> {
          quest.completeFor(p);
          // TODO QuestWorld.getSounds().muteNext();
          PagedMapping.putPage(p, view.getCurrentPage());
          openQuest(p, quest, categoryBack, back);
        });
      } else if (manager.getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
        menu.put(i + offset, glassCooldown, null);
      } else {
        menu.put(i + offset, glassInactive, null);
      }
    }

    int slot = offset + 9;
    for (ItemStack reward : quest.getRewards()) {
      menu.put(slot, reward, null);
      slot++;
    }

    view.build(menu, p, false);
    menu.openFor(p);
  }

  /*
   *
   * Quest Editor
   *
   */
  public static void openCategoryList(Player p) {
    QuestWorld.getSounds().EDITOR_CLICK.playTo(p);

    final Menu menu = new Menu(6, "&3Categories");

    ItemBuilder defaultItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
        .display("&7> Create category");

    PagedMapping view = new PagedMapping(45);
    view.reserve(1);

    for (ICategory category : QuestWorld.getFacade().getCategories()) {
      String[] lore = {category.getName(), "", "&rLeft click: &eOpen quest list",
          "&rShift left click: &eOpen category editor", "&rRight click: &eRemove category"};
      int quests = category.getQuests().size();
      if (quests > 0) {
        String[] lines = new String[lore.length + Math.min(quests, 6)];
        lines[0] = category.getName();

        int j = 1;
        for (IQuest q : category.getQuests()) {
          if (j > 5) {
            lines[j++] = "&7&oand " + (quests - 5) + " more...";
            break;
          }
          lines[j++] = "&7- " + q.getName();
        }

        System.arraycopy(lore, 1, lines, j, 4);
        /*
        for (int k = 0; k < 4; ++k) {
          lines[k + j] = lore[k + 1];
        }
        */

        lore = lines;
      }

      view.addButton(category.getID(), new ItemBuilder(category.getItem()).wrapText(lore).get(),
          Buttons.onCategory(category), true);

      view.reserve(1);
    }

    for (int i = 0; i < view.getCapacity(); ++i) {
      if (!view.hasButton(i)) {
        view.addButton(i, defaultItem.get(), Buttons.newCategory(i), true);
      }
    }

    view.build(menu, p, true);
    menu.openFor(p);
  }

  static void openQuestList(Player p, final ICategory category) {
    QuestWorld.getSounds().EDITOR_CLICK.playTo(p);

    final Menu menu = new LinkedMenu(6, "&3Quests", category, true);

    ItemBuilder defaultItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
        .display("&7> Create quest");

    PagedMapping view = new PagedMapping(45);
    view.reserve(1);
    view.setBackButton(" &3Categories", event -> openCategoryList((Player) event.getWhoClicked()));

    view.addFrameButton(4, new ItemBuilder(Material.WRITABLE_BOOK).display("&3Category editor").get(), event -> openCategoryEditor(p, category), true);

    for (IQuest quest : category.getQuests()) {
      String[] lore = {quest.getName(), "", "&rLeft click: &eOpen quest editor",
          "&rRight click: &eRemove quest"};

      int missions = quest.getMissions().size();
      if (missions > 0) {
        String[] lines = new String[lore.length + Math.min(missions, 6)];
        lines[0] = quest.getName();

        int j = 1;
        for (IMission m : quest.getOrderedMissions()) {
          if (j > 5) {
            lines[j++] = "&7&oand " + (missions - 5) + " more...";
            break;
          }
          lines[j++] = "&7- " + m.getText();

        }

        System.arraycopy(lore, 1, lines, j, 3);
        lore = lines;
      }

      view.addButton(quest.getID(), new ItemBuilder(quest.getItem()).wrapText(lore).get(),
          Buttons.onQuest(quest),
          true);

      view.reserve(1);
    }

    for (int i = 0; i < view.getCapacity(); ++i) {
      if (!view.hasButton(i)) {
        view.addButton(i, defaultItem.getNew(), Buttons.newQuest(category, i), true);
      }
    }

    view.build(menu, p, true);
    menu.openFor(p);
  }

  public static void openCategoryEditor(Player p, final ICategory category) {
    QuestWorld.getSounds().EDITOR_CLICK.playTo(p);

    final Menu menu = new LinkedMenu(2, "&3Category editor", category, true);
    ICategoryState changes = category.getState();

    menu.put(0, Proto.MAP_BACK.get().wrapLore(" &3Categories").get(), event -> {
      openCategoryList((Player) event.getWhoClicked());
    });

    menu.put(4, new ItemBuilder(Material.WRITABLE_BOOK).display("&3Quest list").get(), event -> {
      openQuestList(p, category);
    });

    menu.put(9, new ItemBuilder(category.getItem())
        .wrapText(category.getName(), "", "&e> Click to set the display item").get(), event -> {
      Player p2 = (Player) event.getWhoClicked();
      ItemStack hand = p2.getInventory().getItemInMainHand();
      if (hand == null || hand.getType() == Material.AIR) {
        changes.setItem(new ItemStack(Material.WRITABLE_BOOK));
      } else {
        changes.setItem(hand);
      }
      changes.apply();
      openCategoryEditor(p2, category);
    });

    menu.put(10, new ItemBuilder(Material.NAME_TAG)
        .wrapText(category.getName(), "", "&e> Click to set category name").get(), event -> {
      Player p2 = (Player) event.getWhoClicked();
      p2.closeInventory();
      PlayerTools.promptInput(p2, new SinglePrompt(
          PlayerTools.makeTranslation(true, Translation.CATEGORY_NAME_EDIT, category.getName()),
          (c, s) -> {
            String oldName = category.getName();
            s = Text.deserializeNewline(Text.colorize(s));
            changes.setName(s);
            if (changes.apply()) {
              PlayerTools.sendTranslation(p2, true, Translation.CATEGORY_NAME_SET, s, oldName);
            }

            QuestBook.openCategoryEditor(p2, category);
            return true;
          }));
    });

    IQuest parent = category.getParent();
    menu.put(11,
        new ItemBuilder(Material.WRITABLE_BOOK)
            .wrapText("&7Requirement: &r&o" + (parent != null ? parent.getName() : "-none-"), "",
                "&rLeft click: &eOpen requirement selector", "&rRight click: &eRemove requirement")
            .get(),
        event -> {
          Player p2 = (Player) event.getWhoClicked();
          if (event.isRightClick()) {
            changes.setParent(null);
            if (changes.apply()) {

            }
            openCategoryEditor(p2, category);
          } else {
            PagedMapping.putPage(p2, 0);
            QBDialogue.openRequirementCategories(p2, category);
          }
        });

    menu.put(12,
        new ItemBuilder(Material.NAME_TAG).wrapText(
            "&7Permission: &r"
                + (category.getPermission().equals("") ? "&o-none-" : category.getPermission()),
            "", "&e> Click to change the required permission").get(),
        event -> {
          Player p2 = (Player) event.getWhoClicked();
          p2.closeInventory();
          PlayerTools.promptInput(p2, new SinglePrompt(PlayerTools.makeTranslation(true,
              Translation.CATEGORY_PERM_EDIT, category.getName(), category.getPermission()),
              (c, s) -> {
                String permission = s.equalsIgnoreCase("none") ? "" : s;
                String oldPerm = category.getPermission();
                changes.setPermission(permission);
                if (changes.apply()) {
                  PlayerTools.sendTranslation(p2, true, Translation.CATEGORY_PERM_SET,
                      category.getName(), s, oldPerm);
                }

                QuestBook.openCategoryEditor(p2, category);
                return true;
              }));
        });

    menu.put(13,
        new ItemBuilder(Material.GOLDEN_CARROT)
            .wrapText(
                "&7Show in quest book: " + (!category.isHidden() ? "&2&l\u2714" : "&4&l\u2718"), "",
                "&e> Toggle category visibility")
            .get(),
        event -> {
          changes.setHidden(!category.isHidden());
          if (changes.apply()) {
          }
          openCategoryEditor((Player) event.getWhoClicked(), category);
        });

    menu.put(14, new ItemBuilder(Material.GRASS)
        .wrapText("&7World blacklist", "", "&e> Click open world selector")
        .get(), event -> openWorldEditor((Player) event.getWhoClicked(), category));

    menu.put(17, new ItemBuilder(Material.RED_WOOL)
            .wrapText("&4Reset progress", "",
                "&e> Click to clear all player progress for this category").get(),
        event -> {
          // TODO: Destructive action warning
          QuestWorld.getFacade().clearAllUserData(category);

          QuestWorld.getSounds().DESTRUCTIVE_CLICK.playTo((Player) event.getWhoClicked());
        });

    menu.openFor(p);
  }

  public static void openMissionList(Player p, final IQuest quest) {
    QuestWorld.getSounds().EDITOR_CLICK.playTo(p);

    final Menu menu = new LinkedMenu(6, "&3Missions", quest, true);

    ItemStack defaultItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
        .display("&7> Create mission").get();

    PagedMapping view = new PagedMapping(45);
    view.reserve(1);
    view.setBackButton(" &3Quests",
        event -> openQuestList((Player) event.getWhoClicked(), quest.getCategory()));

    view.addFrameButton(4,
        new ItemBuilder(Material.WRITABLE_BOOK).display("&3Quest editor").get(),
        event -> openQuestEditor(p, quest), true);

    IQuestState changes = quest.getState();

    // TODO: Mission move
    for (IMission mission : quest.getMissions()) {
      // TODO: Hack to maybe deal with out of order quests
      int missionIndex = mission.getIndex();
      view.addButton(missionIndex,
          new ItemBuilder(mission.getType().getSelectorItem()).flagAll()
              .wrapText(mission.getText(), "", "&rLeft click: &eOpen mission editor",
                  "&rRight click: &eRemove mission"/*
                   * , "&rShift right click: &eMove mission"
                   */)
              .get(),
          event -> {
            Player p2 = (Player) event.getWhoClicked();
            if (!event.isRightClick()) {
              openQuestMissionEditor(p2, mission);
            }
            // else if(event.isShiftClick())
            // openMissionMove(p, quest, mission);
            else {
              QBDialogue.openDeletionConfirmation(p2, mission);
            }
          }, true);

      view.reserve(1);
    }

    for (int i = 0; i < view.getCapacity(); ++i) {
      if (!view.hasButton(i)) {
        int index = i;

        view.addButton(i, defaultItem, event -> {
          changes.addMission(index);

          changes.apply();
          openMissionList((Player) event.getWhoClicked(), quest);
        }, true);
      }
    }

    view.build(menu, p, true);
    menu.openFor(p);
  }

  public static void openQuestEditor(Player p, final IQuest quest) {
    QuestWorld.getSounds().EDITOR_CLICK.playTo(p);

    final Menu menu = new LinkedMenu(6, "&3Quest editor", quest, true);
    IQuestState changes = quest.getState();

    menu.put(0, ItemBuilder.Proto.MAP_BACK.get().wrapLore(" &3Quests").get(),
        event -> openQuestList((Player) event.getWhoClicked(), quest.getCategory()));

    menu.put(4, new ItemBuilder(Material.WRITABLE_BOOK).display("&3Mission list").get(),
        event -> openMissionList(p, quest));

    menu.put(9, new ItemBuilder(quest.getItem())
        .wrapText(quest.getName(), "", "&e> Click to set the display item")
        .get(), event -> {
      Player p2 = (Player) event.getWhoClicked();
      ItemStack mainItem = p2.getInventory().getItemInMainHand();
      if (mainItem == null || mainItem.getType() == Material.AIR) {
        changes.setItem(new ItemStack(Material.WRITABLE_BOOK));
      } else {
        changes.setItem(mainItem);
      }
      changes.apply();
      openQuestEditor(p2, quest);
    });

    menu.put(10,
        new ItemBuilder(Material.NAME_TAG)
            .wrapText(quest.getName(), "", "&e> Click to set quest name").get(),
        event -> {
          Player p2 = (Player) event.getWhoClicked();
          p2.closeInventory();
          PlayerTools.promptInput(p2, new SinglePrompt(
              PlayerTools.makeTranslation(true, Translation.QUEST_NAME_EDIT, quest.getName()),
              (c, s) -> {
                String oldName = quest.getName();
                s = Text.deserializeNewline(Text.colorize(s));
                changes.setName(s);
                if (changes.apply()) {
                  PlayerTools.sendTranslation(p2, true, Translation.QUEST_NAME_SET, s, oldName);
                }

                openQuestEditor(p2, quest);
                return true;
              }));
        });

    menu.put(11,
        new ItemBuilder(Material.CHEST)
            .wrapText("&7Item reward", "",
                "&e> Click to set the reward to the items in your hotbar").get(),
        event -> {
          Player p2 = (Player) event.getWhoClicked();
          changes.setItemRewards(p2);
          changes.apply();

          openQuestEditor(p2, quest);
        });

    menu.put(12,
        new ItemBuilder(Material.PAPER).wrapText(quest.getName(), "",
            "&e> Click to set reward desc (use | for new line)").get(),
        event -> {
          Player p2 = (Player) event.getWhoClicked();
          p2.closeInventory();
          PlayerTools.promptInput(p2, new SinglePrompt(
              "Type to set new reward desc (Use | for newline) Current: " + quest.getRewardsLore(),
              (c, s) -> {
                s = Text.deserializeNewline(Text.colorize(s));
                changes.setRewardsLore(s);
                if (changes.apply()) {
                  MessageUtils.sendMessage(p2, "Current: " + changes.getRewardsLore());
                  MessageUtils.sendMessage(p2, "&aNew description set!");
                }
                openQuestEditor(p2, quest);
                return true;
              }));
        });

    menu.put(13,
        new ItemBuilder(Material.CLOCK)
            .wrapText("&7Cooldown: &b" + quest.getFormattedCooldown(), "", "&rLeft click: &e+1m",
                "&rRight click: &e-1m", "&rShift left click: &e+1h", "&rShift right click: &e-1h")
            .get(),
        event -> {
          // Work with raw cooldowns so -1 is actually -1
          long cooldown = quest.getRawCooldown();
          long delta = IQuest.COOLDOWN_SCALE;
          if (event.isShiftClick()) {
            delta *= 60;
          }
          if (event.isRightClick()) {
            delta = -delta;
          }

          // Force a step at 0, so you can't jump from 59 -> -1 or -1 -> 59
          if (cooldown + delta < 0) {
            if (cooldown <= 0) {
              cooldown = -1;
            } else {
              cooldown = 0;
            }
          } else if (cooldown == -1) {
            cooldown = 0;
          } else {
            cooldown += delta;
          }

          changes.setRawCooldown(cooldown);
          changes.apply();

          QuestBook.openQuestEditor((Player) event.getWhoClicked(), quest);
        });

    if (QuestWorld.getEconomy().isPresent()) {
      menu.put(14,
          new ItemBuilder(Material.GOLD_INGOT)
              .wrapText("&7Monetary reward: &6$" + quest.getMoney(), "", "&rLeft click: &e+1",
                  "&rRight click: &e-1", "&rShift left click: &e+100",
                  "&rShift right click: &e-100")
              .get(),
          event -> {
            int money = MissionButton.clickNumber(quest.getMoney(), 100, event);
            if (money < 0) {
              money = 0;
            }
            changes.setMoney(money);
            changes.apply();
            openQuestEditor((Player) event.getWhoClicked(), quest);
          });
    }

    String modifiedLvlString;
    int modifiedLevelRequirement = quest.getModifiedLevelRequirement();
    if (modifiedLevelRequirement == quest.getLevelRequirement()) {
      modifiedLvlString = Integer.toString(quest.getLevelRequirement());
    } else {
      modifiedLvlString = modifiedLevelRequirement + " &7[FROM PARENT]";
    }

    menu.put(15,
        new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .wrapText("&eLevel Requirement: &b" + modifiedLvlString, "",
                "&rLeft click: &e+1",
                "&rRight click: &e-1",
                "&rShift left click: &e+10",
                "&rShift right click: &e-10").get(),
        event -> {
          int req = MissionButton.clickNumber(quest.getLevelRequirement(), 10, event);
          req = Math.max(0, req);
          changes.setLevelRequirement(req);
          changes.apply();
          openQuestEditor((Player) event.getWhoClicked(), quest);
        });

    IQuest parent = quest.getParent();
    menu.put(16,
        new ItemBuilder(Material.WRITABLE_BOOK)
            .wrapText("&7Requirement: &r&o" + (parent != null ? parent.getName() : "-none-"), "",
                "&rLeft click: &eOpen requirement selector", "&rRight click: &eRemove requirement")
            .get(),
        event -> {
          Player p2 = (Player) event.getWhoClicked();
          if (event.isRightClick()) {
            changes.setParent(null);
            changes.apply();
            openQuestEditor(p2, quest);
          } else {
            PagedMapping.putPage(p2, 0);
            QBDialogue.openRequirementCategories(p2, quest);
          }
        });

    menu.put(17, new ItemBuilder(Material.COMMAND_BLOCK)
        .wrapText("&7Command rewards", "", "&e> Click to open command editor").get(), event -> {
      Player p2 = (Player) event.getWhoClicked();
      p2.closeInventory();
      QBDialogue.openCommandEditor(p2, quest);
    });

    menu.put(18,
        new ItemBuilder(Material.NAME_TAG).wrapText(
            "&7Permission: &r" + (quest.getPermission().equals("") ? "&o-none-"
                : quest.getPermission()),
            "", "&e> Click to change the required permission").get(),
        event -> {
          Player p2 = (Player) event.getWhoClicked();
          p2.closeInventory();
          PlayerTools.promptInput(p2, new SinglePrompt(PlayerTools.makeTranslation(true,
              Translation.QUEST_PERM_EDIT, quest.getName(), quest.getPermission()), (c, s) -> {
            String permission = s.equalsIgnoreCase("none") ? "" : s;
            String oldPerm = quest.getPermission();
            changes.setPermission(permission);
            if (changes.apply()) {
              PlayerTools.sendTranslation(p2, true, Translation.QUEST_PERM_SET, quest.getName(),
                  s, oldPerm);
            }

            openQuestEditor(p2, quest);
            return true;
          }));
        });

    String modifiedWeightString;
    int modifiedWeight = quest.getModifiedWeight();
    if (modifiedWeight == quest.getWeight()) {
      modifiedWeightString = Integer.toString(quest.getWeight());
    } else {
      modifiedWeightString = modifiedWeight + " &7[FROM PARENT]";
    }

    menu.put(19,
        new ItemBuilder(Material.STONE)
            .wrapText("&7Weight: &b" + modifiedWeightString, "",
                "&rLeft click: &e+1",
                "&rRight click: &e-1",
                "&rShift left click: &e+10",
                "&rShift right click: &e-10").get(),
        event -> {
          int weight = MissionButton.clickNumber(quest.getWeight(), 10, event);
          changes.setWeight(weight);
          changes.apply();
          openQuestEditor((Player) event.getWhoClicked(), quest);
        });

    menu.put(20,
        new ItemBuilder(Material.COMMAND_BLOCK)
            .wrapText("&7Ordered completion: " + (quest.getOrdered() ? "&2&l\u2714" : "&4&l\u2718"),
                "",
                "&e> Toggle whether tasks must be completed in order")
            .get(),
        event -> {
          changes.setOrdered(!quest.getOrdered());
          changes.apply();
          openQuestEditor((Player) event.getWhoClicked(), quest);
        });

    menu.put(21,
        new ItemBuilder(Material.CHEST)
            .wrapText(
                "&7Auto-claim rewards: " + (quest.getAutoClaimed() ? "&2&l\u2714" : "&4&l\u2718"),
                "",
                "&e> Toggle whether this quest's rewards will be"
                    + " automatically given or have to be claimed manually")
            .get(),
        event -> {
          changes.setAutoClaim(!changes.getAutoClaimed());
          changes.apply();
          openQuestEditor((Player) event.getWhoClicked(), quest);
        });

    menu.put(22, new ItemBuilder(Material.GRASS).wrapText("&7World blacklist", "", "&e> Click to open world selector").get(), event -> openWorldSelector((Player) event.getWhoClicked(), quest));

    menu.put(23, new ItemBuilder(Material.MAP)
        .wrapText("&aHide Until Started: " + (!quest.isHiddenUntilStarted() ? "&2&l\u2714" : "&4&l\u2718"),
            "&7Should quest be hidden until a mission is done?",
            "&e> Click To Toggle")
        .get(), event -> {
          changes.setHiddenUntilStarted(!changes.isHiddenUntilStarted());
          changes.apply();
          openQuestEditor((Player) event.getWhoClicked(), quest);
        });

    String xpDisplay;
    if (quest.getXP() >= 0) {
      xpDisplay = Integer.toString((int) quest.getXP());
    } else {
      xpDisplay = quest.getXP() + " &7[" + getStrifeExpFromPercentage(quest.getModifiedLevelRequirement(), quest.getXP()) + "]";
    }

    menu.put(24,
        new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .wrapText("&eExp Reward: &a" + xpDisplay, "",
                "Positive numbers give that amount",
                " 500 -> 500xp given",
                "Negative numbers give ratio of level requirement",
                "&f-0.2 -> 20% of needed to reach lvl req"
            ).get(),
        event -> {
          Player p2 = (Player) event.getWhoClicked();
          p2.closeInventory();
          PlayerTools.promptInput(p2, new SinglePrompt("&7Enter exp amount (number):", (c, s) -> {
            double amount;
            try {
              amount = Double.parseDouble(s);
            } catch (Exception e) {
              MessageUtils.sendMessage(p2, "&eHey dummy I said enter a number try again");
              openQuestEditor(p2, quest);
              return true;
            }
            changes.setXP(amount);
            if (changes.apply()) {
              MessageUtils.sendMessage(p2, "&aSet xp to: " + changes.getXP());
            }
            openQuestEditor(p2, quest);
            return true;
          }));
        });

    menu.put(25, new ItemBuilder(Material.NETHER_STAR)
            .wrapText("&7QuestPoints: &b" + quest.getQuestPoints(), "",
                "&rLeft click: &e+1",
                "&rRight click: &e-1",
                "&rShift left click: &e+10",
                "&rShift right click: &e-10").get(),
        event -> {
          int points = MissionButton.clickNumber(quest.getQuestPoints(), 10, event);
          changes.setQuestPoints(points);
          changes.apply();
          openQuestEditor((Player) event.getWhoClicked(), quest);
        });

    menu.put(26, new ItemBuilder(Material.EMERALD).wrapText(
        "&7Enabled: " + Text.booleanBadge(quest.isEnabled()),
        "&e> Toggles hiding and disabling the quest"
    ).get(), event -> {
      changes.setEnabled(!quest.isEnabled());
      changes.apply();
      openQuestEditor((Player) event.getWhoClicked(), quest);
    });

    menu.put(33, new ItemBuilder(Material.CHEST).wrapText(
        "&b[Generate Permissions]",
        "&e> Click to send transient perms to chat, to do whatever"
        ).get(), event -> {
          MessageUtils.sendMessage(event.getWhoClicked(), "QS." + quest.getUniqueId() + ".perm");
          MessageUtils.sendMessage(event.getWhoClicked(), "QC." + quest.getUniqueId() + ".perm");
          QuestWorld.getSounds().EDITOR_CLICK.playTo((Player) event.getWhoClicked());
    });

    menu.put(35, new ItemBuilder(Material.BARRIER)
            .wrapText("&4Reset progress", "", "&e> Click to clear all player progress for this quest")
            .get(),
        event -> {
          QuestWorld.getFacade().clearAllUserData(quest);
          QuestWorld.getSounds().DESTRUCTIVE_CLICK.playTo((Player) event.getWhoClicked());
        });

    int index = 36;
    for (ItemStack reward : quest.getRewards()) {
      menu.put(index, reward, null);
      index++;
    }

    menu.openFor(p);
  }

  /*
   * public static void openMissionMove(Player p, IQuest quest, IMission from) {
   * QuestWorld.getSounds().EDITOR_CLICK.playTo(p); Menu menu = new Menu(2,
   * "&3Mission order");
   *
   * menu.put(0,
   * ItemBuilder.Proto.MAP_BACK.get().wrapLore(" &3QMission order").get(), event
   * -> { openQuestEditor((Player) event.getWhoClicked(), quest); });
   *
   * for(int i = 0; i < 9; ++i) { int index = i + 54; menu.put(i + 9, new
   * ItemBuilder(Material.STAINED_GLASS_PANE).color(DyeColor.RED).display(
   * "&7Empty").lore( "", "&e> Move here").get(), event -> { IMissionState state =
   * from.getState(); state.setIndex(index); state.apply(); openQuestEditor(p,
   * quest); }); }
   *
   * for (IMission to : quest.getMissions()) { int index = to.getIndex();
   * menu.put(index + 9, new
   * ItemBuilder(to.getType().getSelectorItem()).flagAll().wrapText( to.getText(),
   * "", "&e> Swap missions").get(), event -> { IMissionState toState =
   * to.getState(); IMissionState fromState = from.getState();
   *
   * toState.setIndex(from.getIndex()); fromState.setIndex(index);
   * toState.apply(); fromState.apply(); openQuestEditor(p, quest); } ); }
   *
   * menu.openFor(p); }
   */

  public static void openWorldSelector(Player p, final IQuest quest) {
    QuestWorld.getSounds().EDITOR_CLICK.playTo(p);

    final Menu menu = new LinkedMenu(2, "&3World selector", quest, true);

    menu.put(0, ItemBuilder.Proto.MAP_BACK.get().wrapLore(" &3Quest editor").get(), event -> {
      openQuestEditor((Player) event.getWhoClicked(), quest);
    });

    int index = 9;
    for (final World world : Bukkit.getWorlds()) {
      menu.put(index,
          new ItemBuilder(Material.GRASS).display("&7" + Text.niceName(world.getName()) + ": "
              + (quest.getWorldEnabled(world.getName()) ? "&2&l\u2714" : "&4&l\u2718")).get(),
          event -> {
            IQuestState changes = quest.getState();
            changes.toggleWorld(world.getName());
            if (changes.apply()) {
            }

            openWorldSelector((Player) event.getWhoClicked(), quest);
          });
      index++;
    }

    menu.openFor(p);
  }

  public static void openWorldEditor(Player p, final ICategory category) {
    QuestWorld.getSounds().EDITOR_CLICK.playTo(p);

    final Menu menu = new LinkedMenu(2, "&3World selector", category, true);

    menu.put(0, ItemBuilder.Proto.MAP_BACK.get().wrapLore(" &3Category editor").get(), event -> {
      openCategoryEditor((Player) event.getWhoClicked(), category);
    });

    int index = 9;
    for (final World world : Bukkit.getWorlds()) {
      menu.put(index,
          new ItemBuilder(Material.GRASS).display("&7" + Text.niceName(world.getName()) + ": "
              + (category.isWorldEnabled(world.getName()) ? "&2&l\u2714" : "&4&l\u2718")).get(),
          event -> {
            ICategoryState changes = category.getState();
            changes.toggleWorld(world.getName());
            if (changes.apply()) {
            }

            openWorldEditor((Player) event.getWhoClicked(), category);
          });
      index++;
    }

    menu.openFor(p);
  }

  public static void openQuestMissionEditor(Player p, final IMission mission) {
    QuestWorld.getSounds().EDITOR_CLICK.playTo(p);

    Menu menu = new LinkedMenu(2, "&3Mission editor", mission, true);

    menu.put(0, ItemBuilder.Proto.MAP_BACK.get().wrapLore(" &3Missions").get(), e -> {
      openMissionList(p, mission.getQuest());
    });

    // Mission types now handle their own menu data!
    mission.getType().buildMenu(mission.getState(), menu);

    ItemStack missionSelector = new ItemBuilder(mission.getType().getSelectorItem()).flagAll()
        .wrapText(
            "&7" + Text.niceName(mission.getType().toString()), "",
            "&e> Click to change the mission type").get();

    menu.put(9, missionSelector, e -> {
      openMissionSelector(p, mission);
    });

    menu.openFor(p);
  }

  public static void openMissionSelector(Player p, IMission mission) {
    QuestWorld.getSounds().EDITOR_CLICK.playTo(p);

    IMissionState changes = mission.getState();
    final Menu menu = new LinkedMenu(3, "&3Mission selector", mission, true);

    PagedMapping.putPage(p, 0);

    PagedMapping view = new PagedMapping(45, 9);
    view.setBackButton(" &3Mission editor", event -> {
      openQuestMissionEditor((Player) event.getWhoClicked(), mission);
    });

    ArrayList<MissionType> types = new ArrayList<>(QuestWorld.getMissionTypes().values());
    Collections.sort(types, (m1, m2) -> m1.toString().compareToIgnoreCase(m2.toString()));

    int i = 0;
    for (MissionType type : types) {
      String name = Text.niceName(type.getName());
      view.addButton(i,
          new ItemBuilder(type.getSelectorItem()).display("&7" + name).flagAll().get(), event -> {
            changes.setType(type);
            MissionButton.apply(event, changes);
          }, false);
      ++i;
    }
    view.build(menu, p, true);

    menu.openFor(p);
  }
}

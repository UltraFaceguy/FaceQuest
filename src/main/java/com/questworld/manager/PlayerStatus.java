package com.questworld.manager;

import com.questworld.Directories;
import com.questworld.QuestingImpl;
import com.questworld.api.MissionType;
import com.questworld.api.QuestStatus;
import com.questworld.api.QuestWorld;
import com.questworld.api.Ticking;
import com.questworld.api.Translation;
import com.questworld.api.annotation.Nullable;
import com.questworld.api.contract.ICategory;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IPlayerStatus;
import com.questworld.api.contract.IQuest;
import com.questworld.api.event.MissionCompletedEvent;
import com.questworld.api.menu.LinkedMenu;
import com.questworld.api.menu.RewardsPrompt;
import com.questworld.quest.WeightComparator;
import com.questworld.util.PlayerTools;
import com.questworld.util.Text;
import com.questworld.util.TransientPermissionUtil;
import com.tealcube.minecraft.bukkit.facecore.utilities.AdvancedActionBarUtil;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.TextUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.TitleUtils;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import land.face.SnazzyPartiesPlugin;
import land.face.data.Party;
import land.face.waypointer.WaypointerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public class PlayerStatus implements IPlayerStatus {

  private WeightComparator weightComparator = new WeightComparator();
  private int questPoints = 0;
  private long questPointTimestamp = 0;
  private boolean inDialogue = false;
  private final UUID playerUUID;
  private final ProgressTracker tracker;

  public PlayerStatus(UUID uuid) {
    this.playerUUID = uuid;
    tracker = new ProgressTracker(uuid);
  }

  private static PlayerStatus of(OfflinePlayer player) {
    return (PlayerStatus) QuestWorld.getAPI().getPlayerStatus(player);
  }

  private static PlayerStatus of(UUID uuid) {
    return (PlayerStatus) QuestWorld.getAPI().getPlayerStatus(uuid);
  }

  @Override
  public boolean isInDialogue() {
    return inDialogue;
  }

  @Override
  public int getQuestPoints() {
    if (System.currentTimeMillis() > questPointTimestamp) {
      updateQuestPoints();
    }
    return questPoints;
  }

  @Override
  public void updateQuestPoints() {
    questPointTimestamp = System.currentTimeMillis() + 300000;
    int points = 0;
    for (ICategory category : QuestWorld.getFacade().getCategories()) {
      for (IQuest quest : category.getQuests()) {
        if (hasFinished(quest) && getStatus(quest) != QuestStatus.REWARD_CLAIMABLE) {
          points += quest.getQuestPoints();
        }
      }
    }
    questPoints = points;
  }

  @Override
  public int countQuests(@Nullable ICategory root, @Nullable QuestStatus status) {
    if (root != null) {
      return questsInCategory(root, status);
    }

    int result = 0;
    for (ICategory category : QuestWorld.getFacade().getCategories()) {
      result += questsInCategory(category, status);
    }

    return result;
  }

  @Override
  public Map<DeluxeCategory, List<IQuest>> getQuests() {

    Map<DeluxeCategory, List<IQuest>> questMap = new HashMap<>();

    for (DeluxeCategory d : DeluxeCategory.values()) {
      questMap.put(d, new ArrayList<>());
    }

    for (ICategory category : QuestWorld.getFacade().getCategories()) {
      for (IQuest quest : category.getQuests()) {
        if (!quest.isEnabled()) {
          continue;
        }
        QuestStatus status = getStatus(quest);
        if (status == QuestStatus.LOCKED_WORLD || status == QuestStatus.LOCKED_NO_PERM
            || status == QuestStatus.LOCKED_PARENT || status == QuestStatus.LOCKED_LEVEL) {
          continue;
        }
        if (status == QuestStatus.REWARD_CLAIMABLE) {
          questMap.get(DeluxeCategory.UNCLAIMED_REWARDS).add(quest);
          continue;
        }
        if (quest.getCooldown() > 0) {
          questMap.get(DeluxeCategory.DAILY_QUESTS).add(quest);
          continue;
        }
        if (status == QuestStatus.FINISHED) {
          questMap.get(DeluxeCategory.COMPLETED_QUESTS).add(quest);
          continue;
        }
        if (status == QuestStatus.AVAILABLE) {
          if (getProgress(quest) == 0) {
            if (quest.getMissions().size() == 0) {
              continue;
            }
            if (getProgress(quest.getOrderedMissions().get(0)) == 0) {
              questMap.get(DeluxeCategory.OPEN_QUESTS).add(quest);
            } else {
              questMap.get(DeluxeCategory.UNFINISHED_QUESTS).add(quest);
            }
          } else {
            questMap.get(DeluxeCategory.UNFINISHED_QUESTS).add(quest);
          }
        }
      }
    }
    for (DeluxeCategory category : questMap.keySet()) {
      questMap.get(category).sort(weightComparator);
    }
    return questMap;
  }

  private int questsInCategory(ICategory category, @Nullable QuestStatus status) {
    if (status == null) {
      return category.getQuests().size();
    }

    int result = 0;
    for (IQuest quest : category.getQuests()) {
      if (quest.isEnabled() && getStatus(quest) == status) {
        ++result;
      }
    }

    return result;
  }

  private static Player asOnline(UUID playerUUID) {
    OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
    if (player.isOnline()) {
      return (Player) player;
    }

    throw new IllegalArgumentException(
        "Player " + player.getName() + " (" + player.getUniqueId() + ") is offline");
  }

  private static Optional<Player> ifOnline(UUID playerUUID) {
    return Optional.ofNullable(Bukkit.getPlayer(playerUUID));
  }

  public List<IMission> getActiveMissions(MissionType type) {
    List<IMission> result = new ArrayList<>();

    for (IMission task : QuestWorld.getViewer().getMissionsOf(type)) {
      if (isMissionActive(task)) {
        result.add(task);
      }
    }

    return result;
  }

  public void unload() {
    tracker.onSave();
  }

  public long getCooldownEnd(IQuest quest) {
    return tracker.getQuestRefresh(quest);
  }

  public boolean isWithinTimeframe(IMission task) {
    long date = tracker.getMissionEnd(task);
    if (date == 0) {
      return true;
    }
    return date > System.currentTimeMillis();
  }

  public boolean updateTimeframe(IMission task, int amount) {
    if (task.getTimeframe() == 0) {
      return true;
    }

    if (!isWithinTimeframe(task)) {
      tracker.setMissionEnd(task, null);
      tracker.setMissionProgress(task, 0);
      ifOnline(playerUUID)
          .ifPresent(
              player -> PlayerTools.sendTranslation(player, false, Translation.NOTIFY_TIME_FAIL,
                  task.getQuest().getName(), task.getText(),
                  getProgress(task) + "/" + task.getAmount()));
      return false;
    } else if (getProgress(task) == 0 && amount > 0) {
      tracker.setMissionEnd(task, System.currentTimeMillis() + task.getTimeframe() * 60L * 1000L);

      ifOnline(playerUUID).ifPresent(player -> PlayerTools.sendTranslation(player, false,
          Translation.NOTIFY_TIME_START, task.getText(), Text.timeFromNum(task.getTimeframe())));
    }
    return true;
  }

  @Override
  public boolean isMissionActive(IMission mission) {
    boolean partial =
        mission.getQuest().isEnabled() && getStatus(mission.getQuest()) == QuestStatus.AVAILABLE &&
            !hasCompletedTask(mission) && hasUnlockedTask(mission);

    if (partial) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();

        if (holder instanceof LinkedMenu) {
          LinkedMenu menu = (LinkedMenu) holder;

          // Force missions pseudo-inactive
          if (menu.isEditor() && (menu.isLinked(mission.getQuest()) || menu.isLinked(mission))) {
            return false;
          }
        }
      }

      return true;
    }

    return false;
  }

  @Override
  public OfflinePlayer getPlayer() {
    return Bukkit.getOfflinePlayer(playerUUID);
  }

  @Override
  public void update() {
    update(false);
  }

  public void update(boolean quest_check) {

    Player p = asOnline(playerUUID);

    if (inDialogue) {
      return;
    }

    if (quest_check) {
      for (IMission mission : QuestWorld.getViewer().getTickingMissions()) {
        if (isMissionActive(mission)) {
          ((Ticking) mission.getType()).onTick(p, new MissionSet.Result(mission, this));
        }
      }
    }

    for (ICategory category : QuestWorld.getFacade().getCategories()) {
      for (IQuest quest : category.getQuests()) {
        if (quest.isEnabled() && getStatus(quest).equals(QuestStatus.AVAILABLE)) {
          boolean finished = quest.getMissions().size() != 0;
          for (IMission task : quest.getMissions()) {
            updateTimeframe(task, 0);
            if (!hasCompletedTask(task)) {
              finished = false;
            }
          }

          if (finished) {
            TitleUtils.sendTitle(p,
                TextUtils.color("&bQUEST COMPLETE!"),
                TextUtils.color("&bCompleted: &f" + ChatColor.stripColor(quest.getName())));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            tracker.setQuestFinished(quest, true);
            if (!quest.getAutoClaimed()) {
              tracker.setQuestStatus(quest, QuestStatus.REWARD_CLAIMABLE);
              new RewardsPrompt(quest, p);
            } else {
              quest.completeFor(p);
            }
          }
        } else if (getStatus(quest).equals(QuestStatus.ON_COOLDOWN)) {
          if (tracker.getQuestRefresh(quest) <= System.currentTimeMillis()) {
            tracker.setQuestStatus(quest, QuestStatus.AVAILABLE);
          }
        }
      }
    }
  }

  @Override
  public QuestStatus getStatus(IQuest quest) {
    Player p = asOnline(playerUUID);
    String worldName = p.getWorld().getName();

    if (!PlayerTools.checkPermission(p, quest.getPermission())) {
      return QuestStatus.LOCKED_NO_PERM;
    }
    if (p.getLevel() < quest.getLevelRequirement()) {
      return QuestStatus.LOCKED_LEVEL;
    }
    if (quest.getParent() != null && !hasFinished(quest.getParent())) {
      return QuestStatus.LOCKED_PARENT;
    }
    if (!quest.getWorldEnabled(worldName) || !quest.getCategory().isWorldEnabled(worldName)) {
      return QuestStatus.LOCKED_WORLD;
    }

    return tracker.getQuestStatus(quest);
  }

  @Override
  public boolean hasFinished(IQuest quest) {
    return tracker.isQuestFinished(quest);
  }

  @Override
  public boolean hasCompletedTask(IMission task) {
    return getProgress(task) >= task.getAmount();
  }

  @Override
  public boolean hasUnlockedTask(IMission task) {
    if (!task.getQuest().getOrdered()) {
      return true;
    }

    List<? extends IMission> tasks = task.getQuest().getOrderedMissions();
    int index = tasks.indexOf(task) - 1;
    if (index < 0 || hasCompletedTask(task)) {
      return true;
    } else {
      return !inDialogue && hasCompletedTask(tasks.get(index));
    }
  }

  @Override
  public int getProgress(IMission task) {
    int progress = tracker.getMissionProgress(task);

    return Math.min(progress, task.getAmount());
  }

  @Override
  public double getProgress(IQuest quest) {
    double progress = 0;
    for (IMission task : quest.getMissions()) {
      if (hasCompletedTask(task)) {
        progress++;
      } else {
        progress += (double) getProgress(task) / task.getAmount();
      }
    }

    return progress;
  }

  @Override
  public int getProgress(ICategory category) {
    int progress = 0;
    for (IQuest quest : category.getQuests()) {
      if (quest.isEnabled() && hasFinished(quest)) {
        ++progress;
      }
    }

    return progress;
  }

  @Override
  public String progressString(IQuest quest, float progress) {
    QuestStatus status = getStatus(quest);
    if (status == QuestStatus.FINISHED || status == QuestStatus.ON_COOLDOWN || progress >= 0.9998) {
      return Text.progressBar(10, 10, StringUtils.EMPTY);
    }
    return Text.progressBar(progress, StringUtils.EMPTY);
  }

  @Override
  public String progressString() {
    int done = 0;
    int total = 0;

    for (ICategory category : QuestWorld.getFacade().getCategories()) {
      total += category.getQuests().size();

      for (IQuest quest : category.getQuests()) {
        if (hasFinished(quest)) {
          ++done;
        }
      }
    }

    return Text.progressBar(done, total, StringUtils.EMPTY);
  }

  public void addProgress(IMission task, int amount) {
    int newProgress = Math.max(getProgress(task) + amount, 0);
    setProgress(task, Math.min(task.getAmount(), newProgress));
    Player player = (Player) getPlayer();
    player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1, 1);
  }

  public void setProgress(IMission task, int amount) {
    setSingleProgress(task, amount);

    Party party = SnazzyPartiesPlugin.getInstance().getPartyManager().getParty(playerUUID);

    if (task.getPartySupport() && party != null) {
      Location location = getPlayer().getPlayer().getLocation();
      Set<Player> members = SnazzyPartiesPlugin.getInstance().getPartyManager()
          .getNearbyPlayers(party, location, 35D);
      for (Player p : members) {
        if (p.getUniqueId().equals(playerUUID)) {
          continue;
        }
        PlayerStatus partyMemberStatus = of(p.getUniqueId());
        if (partyMemberStatus.getStatus(task.getQuest().getState()) == QuestStatus.AVAILABLE) {
          partyMemberStatus.setSingleProgress(task, amount);
        }
      }
    }
  }

  private static void updateQuestWaypoint(IMission task, Player player) {
    if (StringUtils.isNotBlank(task.getWaypointerId())) {
      WaypointerPlugin.getInstance().getWaypointManager().setWaypoint(player, task.getWaypointerId());
    }
  }

  public static IMission getNextTask(IMission currentTask) {
    if (!currentTask.getQuest().getOrdered()) {
      return null;
    }
    Iterator iterator = currentTask.getQuest().getOrderedMissions().iterator();
    while (iterator.hasNext()) {
      IMission task = (IMission) iterator.next();
      if (currentTask == task) {
        if (iterator.hasNext()) {
          return (IMission) iterator.next();
        }
      }
    }
    return null;
  }

  private void setSingleProgress(IMission task, int amount) {
    amount = Math.min(amount, task.getAmount());
    if (!updateTimeframe(task, amount)) {
      return;
    }

    tracker.setMissionProgress(task, amount);

    boolean complete = amount == task.getAmount();

    if (task.getActionBarUpdates()) {
      String actionBar;
      if (complete) {
        actionBar = "&a[Task Complete!] " + ChatColor.stripColor(task.getQuest().getName());
      } else {
        actionBar = "&b[Task] " + ChatColor.stripColor(task.getQuest().getName()) + " " +
            Text.progressBar(getProgress(task), task.getAmount(), StringUtils.EMPTY) + " " + ChatColor.AQUA +
            amount + "/" + task.getAmount();
      }
      Bukkit.getScheduler().runTaskLater(QuestWorld.getPlugin(),
          () -> AdvancedActionBarUtil.addOverrideMessage((Player) getPlayer(), "QUEST", actionBar, 20), 2L);
    }

    if (complete) {
      Bukkit.getPluginManager().callEvent(new MissionCompletedEvent(task));
      sendDialogue(this, task, task.getDialogue().iterator());
    }

    TransientPermissionUtil.updateTransientPerms(getPlayer(), this, task.getQuest());
  }

  public static void sendDialogue(PlayerStatus status, IMission task, Iterator<String> dialogue) {
    of(status.playerUUID).inDialogue = false;
    ifOnline(status.playerUUID).ifPresent(player -> {

      if (dialogue.hasNext()) {
        sendDialogueComponent(player, dialogue.next());
        of(status.playerUUID).inDialogue = true;
        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 2);
        Bukkit.getScheduler().scheduleSyncDelayedTask(QuestWorld.getPlugin(),
            () -> sendDialogue(status, task, dialogue), 50L);
      } else {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
        PlayerTools
            .sendTranslation(player, false, Translation.NOTIFY_COMPLETED, task.getQuest().getName(), task.getText());
        status.update();
        IMission nextTask = getNextTask(task);
        if (nextTask != null) {
          MessageUtils.sendMessage(player, "&e&lNew Objective! &e" + ChatColor.stripColor(nextTask.getDisplayName()));
          Bukkit.getScheduler().runTaskLater(QuestWorld.getPlugin(), () -> updateQuestWaypoint(nextTask, player), 50L);
        }
      }
    });
  }

  private static void sendDialogueComponent(Player player, String line) {
    line = line.replace("@p", player.getName());

    // TODO: remove
    line = line.replace("<player>", player.getName());

    if (line.startsWith("/")) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.substring(1));
    } else {
      line = Text.deserializeNewline(
          Text.colorize(QuestWorld.getPlugin().getConfig().getString("dialogue.prefix"))) + line;

      player.sendMessage(line);
    }
  }

  public void completeQuest(IQuest quest) {
    if (quest.getRawCooldown() < 0) {
      tracker.setQuestStatus(quest, QuestStatus.FINISHED);
    } else {
      if (quest.getRawCooldown() == 0) {
        tracker.setQuestStatus(quest, QuestStatus.AVAILABLE);
      } else {
        tracker.setQuestStatus(quest, QuestStatus.ON_COOLDOWN);
        tracker.setQuestRefresh(quest, System.currentTimeMillis() + quest.getRawCooldown());
      }

      for (IMission task : quest.getMissions()) {
        setProgress(task, 0);
      }
    }
    updateQuestPoints();
    TransientPermissionUtil.updateTransientPerms(getPlayer(), this, quest);
  }

  public ProgressTracker getTracker() {
    return tracker;
  }

  public static void clearAllCategoryData(ICategory category) {
    clearDataImpl(category);
  }

  // Right, so this function USED to loop through every file in data-storage/Quest
  // World on
  // the main thread. W H A T
  public static void clearAllQuestData(IQuest quest) {
    clearDataImpl(quest);
  }

  public static void clearAllMissionData(IMission mission) {
    clearDataImpl(mission);
  }

  private static void clearDataImpl(Object object) {
    Consumer<ProgressTracker> callback;

    if (object instanceof IQuest) {
      callback = tracker -> tracker.clearQuest((IQuest) object);
    } else if (object instanceof ICategory) {
      callback = tracker -> tracker.clearCategory((ICategory) object);
    } else if (object instanceof IMission) {
      callback = tracker -> tracker.clearMission((IMission) object);
    } else {
      throw new IllegalArgumentException(
          "clearData called with: " + object.getClass().getSimpleName());
    }

    QuestingImpl api = (QuestingImpl) QuestWorld.getAPI();

    Bukkit.getScheduler().runTaskAsynchronously(api.getPlugin(), () -> {
      // First: clear all the quest data on a new thread
      for (File file : Directories.listFiles(api.getDataFolders().playerdata,
          (file, name) -> name.endsWith(".yml"))) {
        String uuid = file.getName().substring(0, file.getName().length() - 4);
        try {
          ProgressTracker t = new ProgressTracker(UUID.fromString(uuid));
          callback.accept(t);
          t.onSave();
        }
        // File name was not
        catch (IllegalArgumentException e) {
        }
      }

      // Second: go back to the main thread and make sure all player managers know
      // what happened
      Bukkit.getScheduler().callSyncMethod(api.getPlugin(), () -> {
        for (Player player : Bukkit.getOnlinePlayers()) {
          callback.accept(of(player).getTracker());
        }

        return false;
      });
    });
  }

  @Override
  public boolean hasDeathEvent(IMission mission) {
    return ifOnline(playerUUID).map(player -> {
      IQuest quest = mission.getQuest();
      String playerWorld = player.getWorld().getName();

      return !hasCompletedTask(mission) &&
          getStatus(quest).equals(QuestStatus.AVAILABLE) &&
          mission.getDeathReset() &&
          quest.getWorldEnabled(playerWorld) &&
          quest.getCategory().isWorldEnabled(playerWorld);
    }).orElse(false);
  }
}

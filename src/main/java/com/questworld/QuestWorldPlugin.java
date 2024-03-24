package com.questworld;

import com.questworld.ampmenu.main.QuestMenu;
import com.questworld.ampmenu.missions.MissionListMenu;
import com.questworld.ampmenu.quests.QuestListMenu;
import com.questworld.ampmenu.reward.RewardMenu;
import com.questworld.ampmenu.reward.RewardPickerMenu;
import com.questworld.api.contract.ICategory;
import com.questworld.api.contract.IPlayerStatus.DeluxeCategory;
import com.questworld.api.contract.IQuest;
import com.questworld.api.contract.QuestingAPI;
import com.questworld.command.ClickCommand;
import com.questworld.command.DeluxeQuestsCommand;
import com.questworld.command.EditorCommand;
import com.questworld.command.QuestProgressCommand;
import com.questworld.listener.MenuListener;
import com.questworld.listener.MoneyDropListener;
import com.questworld.listener.PlayerListener;
import com.questworld.listener.SpawnerListener;
import com.questworld.util.Log;
import com.questworld.util.TransientPermissionUtil;
import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import info.faceland.loot.utils.MaterialUtil;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import land.face.potions.PotionPlugin;
import land.face.potions.data.Potion;
import lombok.Getter;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class QuestWorldPlugin extends JavaPlugin implements Listener {

  private static QuestWorldPlugin _INSTANCE;
  public static final DecimalFormat INT_FORMAT = new DecimalFormat("###,###,###");

  private QuestingImpl api;
  private static Permission perms = null;

  private int autosaveHandle = -1;
  private int questCheckHandle = -1;
  private int questCheckHandleCooldown = -1;

  private SpawnerListener spawnListener;

  @Getter
  private QuestMenu questMenu;
  private final Map<Player, QuestListMenu> questListMenus = new WeakHashMap<>();
  private final Map<Player, MissionListMenu> missionListMenus = new WeakHashMap<>();

  @Getter
  private RewardMenu rewardMenu;
  @Getter
  private RewardPickerMenu noSelectionPicker, selectionPickerOne, selectionPickerTwo, selectionPickerThree,
      selectionPickerFour, selectionPickerFive;

  public static QuestWorldPlugin get() {
    return _INSTANCE;
  }

  public static QuestingImpl getAPI() {
    return _INSTANCE.api;
  }

  @Override
  public void onLoad() {
    saveDefaultConfig();
    Log.setLogger(getLogger());
  }

  @Override
  public void onEnable() {
    _INSTANCE = this;
    api = new QuestingImpl(this);
    api.load();

    questListMenus.clear();
    missionListMenus.clear();

    loadConfigs();

    getCommand("quests").setExecutor(new DeluxeQuestsCommand());
    getCommand("questeditor").setExecutor(new EditorCommand(api));
    getCommand("q-external").setExecutor(new QuestProgressCommand(api));

    getServer().getServicesManager().register(QuestingAPI.class, api, this, ServicePriority.Normal);

    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
      new QuestPlaceholders().register();
    }

    new PlayerListener(api);
    new MenuListener(this);
    new MoneyDropListener(this);
    spawnListener = new SpawnerListener(this);
    new ClickCommand(this);

    setupPermissions();

    for (Player p : Bukkit.getOnlinePlayers()) {
      TransientPermissionUtil.updateTransientPerms(p);
    }

    questMenu = new QuestMenu(this);

    rewardMenu = new RewardMenu(this);

    noSelectionPicker = new RewardPickerMenu(this, 0);
    selectionPickerOne = new RewardPickerMenu(this, 1);
    selectionPickerTwo = new RewardPickerMenu(this, 2);
    selectionPickerThree = new RewardPickerMenu(this, 3);
    selectionPickerFour = new RewardPickerMenu(this, 4);
    selectionPickerFive = new RewardPickerMenu(this, 5);

    Bukkit.getScheduler().runTaskLater(this, () -> {
      long current = System.currentTimeMillis();
      Bukkit.getLogger().info("[FaceQuest] Running Loot updateItem pass...");
      for (ICategory c : QuestWorldPlugin.getAPI().getFacade().getCategories()) {
        for (IQuest q : c.getQuests()) {
          for (ItemStack stack : q.getRewards()) {
            try {
              MaterialUtil.updateItem(stack);
              Potion potion = PotionPlugin.getInstance().getPotionManager().getPotionFromStack(stack);
              if (potion != null) {
                PotionPlugin.getInstance().getPotionManager().updateItemStack(stack, potion);
              }
            } catch (Exception e) {
              Bukkit.getLogger().warning("[FaceQuest] Exception updating item for " + q.getName());
              Bukkit.getLogger().warning("[FaceQuest] stack: " + e);
            }
          }
        }
      }
      long diff = System.currentTimeMillis() - current;
      Bukkit.getLogger().info("[FaceQuest] Complete! Loot updateItem finished in " + diff + "ms");
    }, 1200);
  }

  public void openNewPicker(Player player, IQuest quest) {
    if (quest.getRewards()[4] != null || quest.getRewards()[5] != null || quest.getRewards()[6] != null ||
        quest.getRewards()[7] != null || quest.getRewards()[8] != null) {
      noSelectionPicker.setQuest(player, quest);
      noSelectionPicker.open(player);
    } else {
      rewardMenu.setQuest(player, quest);
      rewardMenu.open(player);
    }
  }

  public static List<String> rewardLines(IQuest quest) {
    List<String> lines = new ArrayList<>();
    if (quest.getRewards()[0] != null) {
      ItemStack stack = quest.getRewards()[0];
      lines.add(FaceColor.WHITE.s() + stack.getAmount() + "x " + ItemStackExtensionsKt.getDisplayName(stack));
    }
    if (quest.getRewards()[1] != null) {
      ItemStack stack = quest.getRewards()[1];
      lines.add(FaceColor.WHITE.s() + stack.getAmount() + "x " + ItemStackExtensionsKt.getDisplayName(stack));
    }
    if (quest.getRewards()[2] != null) {
      ItemStack stack = quest.getRewards()[2];
      lines.add(FaceColor.WHITE.s() + stack.getAmount() + "x " + ItemStackExtensionsKt.getDisplayName(stack));
    }
    if (quest.getRewards()[3] != null) {
      ItemStack stack = quest.getRewards()[3];
      lines.add(FaceColor.WHITE.s() + stack.getAmount() + "x " + ItemStackExtensionsKt.getDisplayName(stack));
    }
    if (quest.getRewards()[4] != null || quest.getRewards()[5] != null || quest.getRewards()[6] != null ||
        quest.getRewards()[7] != null || quest.getRewards()[8] != null) {
      lines.add(FaceColor.ORANGE + "[ A Choice of Items ]");
    }
    return lines;
  }

  public void loadConfigs() {
    reloadConfig();

    if (questCheckHandle != -1) {
      getServer().getScheduler().cancelTask(questCheckHandle);
    }
    if (questCheckHandleCooldown != -1) {
      getServer().getScheduler().cancelTask(questCheckHandleCooldown);
    }

    int questCheckDelay = getConfig().getInt("options.quest-check-delay");
    questCheckHandle = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
      for (Player p : getServer().getOnlinePlayers()) {
        api.getPlayerStatus(p).updateTicking(p);
      }
    }, 0L, questCheckDelay);
    questCheckHandleCooldown = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
      for (Player p : getServer().getOnlinePlayers()) {
        api.getPlayerStatus(p).updateCooldowns();
      }
    }, questCheckDelay / 2, questCheckDelay);

    int autosave = getConfig().getInt("options.autosave-interval") * 20 * 60; // minutes to ticks

    if (autosaveHandle != -1) {
      getServer().getScheduler().cancelTask(autosaveHandle);
      autosaveHandle = -1;
    }

    if (autosave > 0) {
      autosaveHandle = getServer().getScheduler()
          .scheduleSyncRepeatingTask(this, api::onSave, autosave, autosave);
    }
  }

  @Override
  public void onDisable() {
    api.onSave();
    api.onDiscard();

    for (Player p : getServer().getOnlinePlayers()) {
      p.removeMetadata(Constants.MD_LAST_MENU, this);
      p.removeMetadata(Constants.MD_PAGES, this);
    }

    autosaveHandle = -1;
    questCheckHandle = -1;
    questCheckHandleCooldown = -1;

    Log.setLogger(null);

    getServer().getServicesManager().unregisterAll(this);
    getServer().getScheduler().cancelTasks(this);
  }

  public void openQuestList(Player player, DeluxeCategory deluxeCategory) {
    if (!questListMenus.containsKey(player)) {
      questListMenus.put(player, new QuestListMenu(this));
    }
    else {
      if (questListMenus.get(player).getDeluxeCategory() != deluxeCategory) {
        questListMenus.get(player).setCurrentPage(0);
      }
    }
    questListMenus.get(player).setCurrentPage(0);
    questListMenus.get(player).setDeluxeCategory(deluxeCategory);
    questListMenus.get(player).resort(player);
    questListMenus.get(player).open(player);
  }

  public void openMissionList(Player player, DeluxeCategory deluxeCategory, IQuest quest) {
    if (!missionListMenus.containsKey(player)) {
      missionListMenus.put(player, new MissionListMenu(this));
    } else {
      if (missionListMenus.get(player).getSelectedCategory() != deluxeCategory) {
        missionListMenus.get(player).setCurrentPage(0);
      }
    }
    missionListMenus.get(player).setSelectedQuest(quest);
    missionListMenus.get(player).setSelectedCategory(deluxeCategory);
    missionListMenus.get(player).resort(player);
    missionListMenus.get(player).open(player);
  }

  private boolean setupPermissions() {
    RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager()
        .getRegistration(Permission.class);
    perms = rsp.getProvider();
    return perms != null;
  }

  public static Permission getPermissions() {
    return perms;
  }

  public SpawnerListener getSpawnListener() {
    return spawnListener;
  }
}

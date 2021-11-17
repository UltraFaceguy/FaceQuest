package com.questworld;

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
import java.text.DecimalFormat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class QuestWorldPlugin extends JavaPlugin implements Listener {

  private static QuestWorldPlugin _INSTANCE;
  private QuestingImpl api;
  private static Permission perms = null;

  private int autosaveHandle = -1;
  private int questCheckHandle = -1;

  private SpawnerListener spawnListener;

  public static final DecimalFormat INT_FORMAT = new DecimalFormat("###,###,###");

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
  }

  public void loadConfigs() {
    reloadConfig();

    if (questCheckHandle != -1) {
      getServer().getScheduler().cancelTask(questCheckHandle);
    }

    questCheckHandle = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
      for (Player p : getServer().getOnlinePlayers()) {
        api.getPlayerStatus(p).update(true);
      }
    }, 0L, getConfig().getInt("options.quest-check-delay"));

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

    Log.setLogger(null);

    getServer().getServicesManager().unregisterAll(this);
    getServer().getScheduler().cancelTasks(this);
  }

  private boolean setupPermissions() {
    RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
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

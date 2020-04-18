package com.questworld.quest;

import static com.questworld.util.PlayerTools.getStrifeExpFromPercentage;

import com.tealcube.minecraft.bukkit.TextUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import land.face.strife.StrifePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.questworld.QuestWorldPlugin;
import com.questworld.api.QuestWorld;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IQuest;
import com.questworld.api.contract.IQuestState;
import com.questworld.api.event.CancellableEvent;
import com.questworld.api.event.QuestCompleteEvent;
import com.questworld.util.ItemBuilder;
import com.questworld.util.Text;

class Quest extends UniqueObject implements IQuestState {

  private WeakReference<Category> category;
  private YamlConfiguration config;

  private boolean autoclaim = false;
  private boolean enabled = true;
  private List<String> commands = new ArrayList<>();
  private long cooldown = -1;
  private int id = -1;
  private ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
  private int money = 0;
  private String name = "";
  private String rewardsLore = "";
  private boolean ordered = false;
  private WeakReference<Quest> parent = new WeakReference<>(null);
  private String permission = "";
  private List<ItemStack> rewards = new ArrayList<>();
  private Map<Integer, Mission> tasks = new HashMap<>(9);
  private List<String> world_blacklist = new ArrayList<>();
  private int weight = 0;
  private int levelRequirement = 0;
  private int questPoints = 0;
  private double xp = 0;

  // Internal
  protected Quest(Quest quest) {
    copy(quest);
  }

  protected void copy(Quest source) {
    category = source.category;
    id = source.id;
    config = YamlConfiguration.loadConfiguration(Facade.fileFor(this));
    cooldown = source.cooldown;
    name = source.name;
    rewardsLore = source.rewardsLore;
    item = source.item.clone();

    tasks.clear();
    tasks.putAll(source.tasks);
    commands.clear();
    commands.addAll(source.commands);
    world_blacklist.clear();
    world_blacklist.addAll(source.world_blacklist);
    rewards.clear();
    rewards.addAll(source.rewards);

    money = source.money;
    xp = source.xp;
    ordered = source.ordered;
    autoclaim = source.autoclaim;
    enabled = source.enabled;
    parent = source.parent;
    permission = source.permission;
    weight = source.weight;
    levelRequirement = source.levelRequirement;
    questPoints = source.questPoints;
  }

  protected void copyTo(Quest dest) {
    dest.copy(this);
  }

  private long fromMaybeString(Object o) {
    if (o instanceof Long || o instanceof Integer) {
      return ((Number) o).longValue();
    }
    if (o instanceof String) {
      return Long.parseLong((String) o);
    }

    throw new IllegalArgumentException(
        "Expected (Long) Integer or String, got " + o.getClass().getSimpleName());
  }

  // Package
  Quest(int id, YamlConfiguration config, Category category) {
    this.id = id;
    this.config = config;
    this.category = new WeakReference<>(category);

    setUniqueId(config.getString("uniqueId", null));

    cooldown = fromMaybeString(config.get("cooldown"));
    ordered = config.getBoolean("in-order");
    autoclaim = config.getBoolean("auto-claim");

    // Quests missing the 'enabled' param were made when everything was enabled
    if (config.contains("enabled")) {
      enabled = config.getBoolean("enabled", true);
    } else {
      enabled = true;
      updateLastModified();
    }

    name = Text.colorize(config.getString("name"));
    rewardsLore = Text.colorize(config.getString("rewards-lore", ""));
    ItemStack i2 = config.getItemStack("item", item);

    rewards = loadRewards();
    money = config.getInt("rewards.money");
    xp = config.getDouble("rewards.xp");

    commands = config.getStringList("rewards.commands");
    world_blacklist = config.getStringList("world-blacklist");

    permission = config.getString("permission", "");
    weight = config.getInt("weight", 0);
    levelRequirement = config.getInt("level-requirement", 0);
    questPoints = config.getInt("quest-points", 0);

    if (i2.getType() != Material.AIR) {
      item = i2;
    }

    loadMissions();
  }

  // External
  public Quest(String name, int id, Category category) {
    this.id = id;
    this.category = new WeakReference<>(category);
    this.name = name;
    this.enabled = QuestWorld.getPlugin().getConfig().getBoolean("options.quests-default-enabled");

    config = YamlConfiguration.loadConfiguration(Facade.fileFor(this));
  }

  public void refreshParent() {
    String parentId = config.getString("parentId", null);
    if (parentId != null) {
      parent = new WeakReference<>(getCategory().getFacade().getQuest(UUID.fromString(parentId)));
    }
  }

  private void loadMissions() {
    ArrayList<Mission> arr = new ArrayList<>();
    List<Map<?, ?>> sa = config.getMapList("missions");

    if (!sa.isEmpty()) {
      for (Map<?, ?> map : sa) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) map;

        data.put("quest", this);

        Mission m = new Mission(data);
        m.validate();
        arr.add(m);
      }
    } else {
      ConfigurationSection missions = config.getConfigurationSection("missions");
      if (missions == null) {
        return;
      }

      int index = 0;
      for (String key : missions.getKeys(false)) {
        // TODO mess
        Map<String, Object> data = missions.getConfigurationSection(key).getValues(false);
        // getValues wont recurse through sections, so we have to manually map to... map
        data.put("location", ((ConfigurationSection) data.get("location")).getValues(false));

        data.put("index", index++);

        data.put("quest", this);

        Mission m = new Mission(data);
        m.validate();
        arr.add(m);
      }
    }

    QuestState state = getState();
    for (Mission m : arr) {
      state.directAddMission(m);
    }
    state.apply();
  }

  void save() {
    config.set("uniqueId", getUniqueId().toString());
    config.set("categoryId", getCategory().getUniqueId().toString());
    config.set("id", id);
    config.set("category", getCategory().getID());
    config.set("cooldown", cooldown);
    config.set("name", Text.escapeColor(name));
    config.set("rewards-lore", Text.escapeColor(rewardsLore));
    config.set("item", new ItemStack(item));
    config.set("rewards.items", null);
    config.set("rewards.money", money);
    config.set("rewards.xp", xp);
    config.set("rewards.commands", commands);
    config.set("missions", null);
    config.set("permission", permission);
    config.set("in-order", ordered);
    config.set("auto-claim", autoclaim);
    config.set("enabled", enabled);
    config.set("world-blacklist", world_blacklist);
    config.set("weight", weight);
    config.set("level-requirement", levelRequirement);
    config.set("quest-points", questPoints);

    config.set("rewards.items", rewards);

    List<Map<String, Object>> missions = new ArrayList<>(tasks.size());
    for (Mission mission : getOrderedMissions()) {
      Map<String, Object> data = mission.serialize();
      data.remove("quest");
      missions.add(data);
    }
    config.set("missions", missions);

    Quest parent = getParent();
    if (parent != null) {
      config.set("parentId", parent.getUniqueId().toString());
    }

    try {
      config.save(Facade.fileFor(this));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public int getID() {
    return id;
  }

  public ItemStack getItem() {
    return item.clone();
  }

  public Category getCategory() {
    return category.get();
  }

  public List<Mission> getOrderedMissions() {
    List<Mission> missions = new ArrayList<>(tasks.values());
    missions.sort(Comparator.comparingInt(Mission::getIndex));
    return missions;
  }

  public Collection<Mission> getMissions() {
    return tasks.values();
  }

  private List<ItemStack> loadRewards() {
    @SuppressWarnings("unchecked")
    List<ItemStack> newItems = (List<ItemStack>) config.getList("rewards.items");

    if (newItems != null) {
      return newItems;
    }

    List<ItemStack> oldItems = new ArrayList<>();

    ConfigurationSection rewards = config.getConfigurationSection("rewards.items");
    if (rewards == null) {
      return oldItems;
    }

    for (String key : rewards.getKeys(false)) {
      oldItems.add(rewards.getItemStack(key));
    }

    return oldItems;
  }

  public void setItemRewards(Player p) {
    rewards.clear();
    for (int i = 0; i <= 4; i++) {
      ItemStack item = p.getInventory().getItem(i);
      if (item != null && item.getType() != Material.AIR) {
        rewards.add(item.clone());
      }
    }
  }

  public void setItem(ItemStack item) {
    this.item = item.clone();
  }

  public void toggleWorld(String world) {
    if (world_blacklist.contains(world)) {
      world_blacklist.remove(world);
    } else {
      world_blacklist.add(world);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  public void setRewardsLore(String rewardsLore) {
    this.rewardsLore = rewardsLore;
  }

  @Override
  public String getRewardsLore() {
    return rewardsLore;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<ItemStack> getRewards() {
    return rewards;
  }

  public Mission getMission(int i) {
    return tasks.get(i);
  }

  public void addMission(int index) {
    tasks.put(index, getCategory().getFacade().createMission(index, getSource()));
  }

  public void directAddMission(Mission m) {
    tasks.put(m.getIndex(), m);
  }

  public void removeMission(IMission mission) {
    tasks.remove(mission.getIndex());
  }

  public long getRawCooldown() {
    return cooldown;
  }

  public void setRawCooldown(long cooldown) {
    this.cooldown = cooldown;
  }

  public long getCooldown() {
    return cooldown / COOLDOWN_SCALE;
  }

  public void setCooldown(long cooldown) {
    this.cooldown = cooldown * COOLDOWN_SCALE;
  }

  public int getMoney() {
    return money;
  }

  public double getXP() {
    return xp;
  }

  public int getWeight() {
    return weight;
  }

  public int getModifiedWeight() {
    if (weight == 0) {
      int currWeight = weight;
      IQuest loopQuest = this;
      while (loopQuest.getParent() != null && currWeight == 0) {
        loopQuest = loopQuest.getParent();
        currWeight = loopQuest.getWeight();
      }
      return currWeight;
    }
    return weight;
  }

  public int getLevelRequirement() {
    return levelRequirement;
  }

  public int getModifiedLevelRequirement() {
    if (levelRequirement == 0) {
      int currReq = levelRequirement;
      IQuest loopQuest = this;
      while (loopQuest.getParent() != null && currReq == 0) {
        loopQuest = loopQuest.getParent();
        currReq = loopQuest.getLevelRequirement();
      }
      return currReq;
    }
    return levelRequirement;
  }

  public int getQuestPoints() {
    return questPoints;
  }

  public void setMoney(int money) {
    this.money = money;
  }

  public void setXP(double xp) {
    this.xp = xp;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }

  public void setLevelRequirement(int levelRequirement) {
    this.levelRequirement = levelRequirement;
  }

  public void setQuestPoints(int questPoints) {
    this.questPoints = questPoints;
  }

  @Override
  public boolean completeFor(Player p) {
    if (CancellableEvent.send(new QuestCompleteEvent(getSource(), p))) {
      handoutReward(p);
      QuestWorldPlugin.getAPI().getPlayerStatus(p).completeQuest(this);
      return true;
    }

    return false;
  }

  @Override
  public ItemStack generateRewardInfo() {
    ItemStack info = new ItemStack(Material.PAPER);
    ItemStackExtensionsKt.setDisplayName(info, TextUtils.color("&f&lQuest Rewards!"));
    List<String> lore = new ArrayList<>();
    if (questPoints > 0) {
      lore.add("&b" + questPoints + " Quest Point(s)");
    }
    if (money > 0) {
      lore.add("&e" + money + " Bits");
    }
    if (xp > 0) {
      lore.add("&a" + (int) xp + " Experience");
    } else if (xp < 0) {
      lore.add("&a" + getStrifeExpFromPercentage(getModifiedLevelRequirement(), xp) + " Experience");
    }
    if (StringUtils.isNotBlank(rewardsLore)) {
      lore.addAll(Arrays.asList(rewardsLore.split("\\|")));
    }
    ItemStackExtensionsKt.setLore(info, TextUtils.color(lore));
    ItemStackExtensionsKt.setCustomModelData(info, 50);
    return info;
  }

  private void handoutReward(Player p) {

    QuestWorld.getSounds().QUEST_REWARD.playTo(p);

    ItemStack[] itemReward = rewards.toArray(new ItemStack[rewards.size()]);

    // addItem can modify item stacks, apparently. We don't want that ever.
    for (int i = 0; i < itemReward.length; ++i) {
      itemReward[i] = ItemBuilder.clone(itemReward[i]);
    }

    for (ItemStack item : p.getInventory().addItem(itemReward).values()) {
      p.getWorld().dropItemNaturally(p.getLocation(), item);
    }

    if (questPoints > 0) {
      MessageUtils.sendMessage(p, "&f&lYou were awarded &b&l" + questPoints + " QuestPoints&f&l!");
    }

    if (money > 0) {
      QuestWorld.getEconomy().ifPresent(economy -> economy.depositPlayer(p, money));
      MessageUtils.sendMessage(p, "&6You were awarded &f" + money + " Bits&6!");
    }

    if (xp > 0) {
      StrifePlugin.getInstance().getExperienceManager().addExperience(p, xp, true);
    } else if (xp < 0) {
      StrifePlugin.getInstance().getExperienceManager()
          .addExperience(p, getStrifeExpFromPercentage(getModifiedLevelRequirement(), xp), true);
    }

    for (String command : commands) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("@p", p.getName()));
    }
  }

  public String getFormattedCooldown() {
    long cooldown = getRawCooldown();
    if (cooldown < 0) {
      return "Single Use";
    }

    if (cooldown == 0) {
      return "Repeating";
    }

    return Text.timeFromNum(cooldown / COOLDOWN_SCALE);
  }

  public Quest getParent() {
    return parent.get();
  }

  @Override
  public void setParent(IQuest quest) {
    parent = new WeakReference<>(quest != null ? ((Quest) quest).getSource() : null);
  }

  public List<String> getCommands() {
    return commands;
  }

  public void removeCommand(int i) {
    commands.remove(i);
  }

  public void addCommand(String command) {
    commands.add(command);
  }

  public void addCommand(int index, String command) {
    commands.add(index, command);
  }

  @Override
  public String getPermission() {
    return permission;
  }

  @Override
  public void setPermission(String permission) {
    this.permission = permission;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean getOrdered() {
    return ordered;
  }

  public void setOrdered(boolean ordered) {
    this.ordered = ordered;
  }

  public boolean getAutoClaimed() {
    return autoclaim;
  }

  public void setAutoClaim(boolean autoclaim) {
    this.autoclaim = autoclaim;
  }

  public boolean getWorldEnabled(String world) {
    return !world_blacklist.contains(world);
  }

  @Override
  public QuestState getState() {
    return new QuestState(this);
  }

  @Override
  public boolean apply() {
    return true;
  }

  @Override
  public boolean discard() {
    return false;
  }

  @Override
  public Quest getSource() {
    return this;
  }

  @Override
  public boolean hasChange(Member field) {
    return true;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  /*
   * @Deprecated Quest(Map<String, Object> data) { loadMap(data); }
   *
   * @Deprecated public Map<String, Object> serialize() { return null; }
   *
   * @Deprecated private void loadMap(Map<String, Object> data) {
   *
   * }
   */
}

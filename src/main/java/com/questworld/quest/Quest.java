package com.questworld.quest;

import static com.questworld.util.PlayerTools.getStrifeExpFromPercentage;

import com.questworld.QuestWorldPlugin;
import com.questworld.api.QuestWorld;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IQuest;
import com.questworld.api.contract.IQuestState;
import com.questworld.api.event.CancellableEvent;
import com.questworld.api.event.QuestCompleteEvent;
import com.questworld.util.ItemBuilder;
import com.questworld.util.Text;
import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import com.tealcube.minecraft.bukkit.facecore.utilities.ItemUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.TextUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.ToastUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.ToastUtils.ToastStyle;
import com.tealcube.minecraft.bukkit.facecore.utilities.UnicodeUtil;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import land.face.strife.StrifePlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

class Quest extends UniqueObject implements IQuestState {

  public static DecimalFormat FORMAT = new DecimalFormat("###,###,###,###");

  private WeakReference<Category> category;
  private YamlConfiguration config;

  private boolean autoclaim = false;
  private boolean enabled = true;
  private List<String> commands = new ArrayList<>();
  private long cooldown = -1;
  private int id = -1;
  private ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
  @Getter
  private int money = 0;
  private String name = "";
  private String rewardsLore = "";
  private boolean ordered = false;
  private WeakReference<Quest> parent = new WeakReference<>(null);
  private String permission = "";
  @Getter
  private ItemStack[] rewards = new ItemStack[9];
  private Map<Integer, Mission> tasks = new HashMap<>(9);
  private List<String> world_blacklist = new ArrayList<>();
  @Getter
  private int weight = 0;
  @Getter
  private int levelRequirement = 0;
  @Getter
  private int questPoints = 0;
  @Getter
  private boolean hiddenUntilStarted = false;
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
    rewards = source.rewards.clone();

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
    hiddenUntilStarted = source.hiddenUntilStarted;
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

    hiddenUntilStarted = config.getBoolean("hide-until-started", false);

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
    enabled = QuestWorld.getPlugin().getConfig().getBoolean("options.quests-default-enabled");
    hiddenUntilStarted = false;
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
    config.set("hide-until-started", hiddenUntilStarted);
    config.set("world-blacklist", world_blacklist);
    config.set("weight", weight);
    config.set("level-requirement", levelRequirement);
    config.set("quest-points", questPoints);

    config.set("rewards.items", rewards);
    config.set("rewards.item-pick-1", rewards[4]);
    config.set("rewards.item-pick-2", rewards[5]);
    config.set("rewards.item-pick-3", rewards[6]);
    config.set("rewards.item-pick-4", rewards[7]);
    config.set("rewards.item-pick-5", rewards[8]);

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
    if (item == null || item.getType() == Material.AIR) {
      return new ItemStack(Material.WRITABLE_BOOK);
    }
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

  private ItemStack[] loadRewards() {
    @SuppressWarnings("unchecked")
    List<ItemStack> items = (List<ItemStack>) config.getList("rewards.items");
    ItemStack[] rewards = new ItemStack[9];
    if (items == null) {
      return rewards;
    }
    int i = 0;
    for (ItemStack item : items) {
      if (i > 3) {
        break;
      }
      rewards[i] = item;
      i++;
    }

    ItemStack pickOne = config.getItemStack("rewards.item-pick-1");
    ItemStack pickTwo = config.getItemStack("rewards.item-pick-2");
    ItemStack pickThree = config.getItemStack("rewards.item-pick-3");
    ItemStack pickFour = config.getItemStack("rewards.item-pick-4");
    ItemStack pickFive = config.getItemStack("rewards.item-pick-5");
    if (pickOne != null) {
      rewards[4] = pickOne;
    }
    if (pickOne != null) {
      rewards[5] = pickTwo;
    }
    if (pickOne != null) {
      rewards[6] = pickThree;
    }
    if (pickOne != null) {
      rewards[7] = pickFour;
    }
    if (pickOne != null) {
      rewards[8] = pickFive;
    }
    return rewards;
  }

  public void setItemRewards(Player p) {
    rewards = new ItemStack[9];
    for (int i = 0; i <= 8; i++) {
      ItemStack item = p.getInventory().getItem(i);
      if (item != null && item.getType() != Material.AIR) {
        rewards[i] = item.clone();
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

  public double getXP() {
    return xp;
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

  public void setHiddenUntilStarted(boolean hiddenUntilStarted) {
    this.hiddenUntilStarted = hiddenUntilStarted;
  }

  @Override
  public boolean completeFor(Player p, int selectedSlot) {
    if (CancellableEvent.send(new QuestCompleteEvent(getSource(), p))) {
      handoutReward(p, selectedSlot);
      QuestWorldPlugin.getAPI().getPlayerStatus(p).completeQuest(this);
      return true;
    }

    return false;
  }

  @Override
  public ItemStack generateRewardInfo() {
    ItemStack info = new ItemStack(Material.PAPER);
    ItemStackExtensionsKt.setCustomModelData(info, 996);
    ItemStackExtensionsKt.setDisplayName(info, TextUtils.color("&f&lQuest Rewards!"));
    List<String> lore = new ArrayList<>();
    if (questPoints > 0) {
      lore.add(FaceColor.CYAN.s() + questPoints + " Quest Point(s)");
    }
    if (money > 0) {
      lore.add(FaceColor.YELLOW.s() + FORMAT.format(money) + ChatColor.YELLOW + "◎");
    }
    if (xp > 0) {
      lore.add(FaceColor.LIGHT_GREEN.s() + FORMAT.format((int) xp) + " Combat XP");
    } else if (xp < 0) {
      lore.add(FaceColor.LIGHT_GREEN.s() + FORMAT.format(getStrifeExpFromPercentage(getModifiedLevelRequirement(), xp))
          + " Combat XP");
    }
    if (StringUtils.isNotBlank(rewardsLore)) {
      lore.addAll(TextUtils.color(Arrays.asList(rewardsLore.split("\\|"))));
    }
    if (lore.isEmpty()) {
      lore.add(FaceColor.LIGHT_GRAY + "Uhhh...");
      lore.add(FaceColor.LIGHT_GRAY + "A good adventure is");
      lore.add(FaceColor.LIGHT_GRAY + "its own reward..?");
    }
    TextUtils.setLore(info, TextUtils.color(lore));
    return info;
  }

  private void handoutReward(Player p, int selectedSlot) {
    QuestWorld.getSounds().QUEST_REWARD.playTo(p);
    if (questPoints > 0) {
      ToastUtils.sendToast(p, FaceColor.NO_SHADOW +
          UnicodeUtil.unicodePlacehold("<toast_questpoints>"), ItemUtils.BLANK, ToastStyle.INFO);
    }
    try {
      List<ItemStack> stacks = new ArrayList<>();
      ItemStack[] itemReward = rewards.clone();
      // addItem can modify item stacks, apparently. We don't want that ever.
      for (int i = 0; i < itemReward.length && i < 4; i++) {
        if (itemReward[i] == null) {
          continue;
        }
        stacks.add(ItemBuilder.clone(itemReward[i]));
      }
      if (selectedSlot > 0) {
        if (itemReward[selectedSlot + 3] != null) {
          stacks.add(ItemBuilder.clone(itemReward[selectedSlot + 3]));
        }
      }
      for (ItemStack item : stacks) {
        HashMap<Integer, ItemStack> remainder = p.getInventory().addItem(item);
        if (!remainder.isEmpty()) {
          for (ItemStack dropItem : remainder.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), dropItem);
          }
        }
      }
      if (questPoints > 0) {
        MessageUtils.sendMessage(p, "&f&lYou were awarded &b&l" + questPoints + " QuestPoints&f&l!");
      }
    } catch (Exception e) {
      Bukkit.getLogger().severe("[FaceQuest] FAILED TO GIVE ITEMS FOR " + p.getName());
      e.printStackTrace();
    }
    try {
      if (money > 0) {
        QuestWorld.getEconomy().ifPresent(economy -> economy.depositPlayer(p, money));
        MessageUtils.sendMessage(p, "&e  +" + money + "◎");
      }
    } catch (Exception e) {
      Bukkit.getLogger().severe("[FaceQuest] FAILED TO GIVE MONEY FOR " + p.getName());
      e.printStackTrace();
    }
    try {
      if (xp > 0) {
        StrifePlugin.getInstance().getExperienceManager().addExperience(p, xp, true);
      } else if (xp < 0) {
        StrifePlugin.getInstance().getExperienceManager()
            .addExperience(p, getStrifeExpFromPercentage(getModifiedLevelRequirement(), xp), true);
      }
    } catch (Exception e) {
      Bukkit.getLogger().severe("[FaceQuest] FAILED TO GIVE XP FOR " + p.getName());
      e.printStackTrace();
    }

    try {
      for (String command : commands) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("@p", p.getName()));
      }
    } catch (Exception e) {
      Bukkit.getLogger().severe("[FaceQuest] FAILED TO GIVE COMMANDS FOR " + p.getName());
      e.printStackTrace();
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

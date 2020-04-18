package com.questworld.api.menu;

import com.questworld.QuestingImpl;
import com.questworld.api.QuestStatus;
import com.questworld.api.QuestWorld;
import com.questworld.api.contract.IPlayerStatus.DeluxeCategory;
import com.questworld.api.contract.IQuest;
import com.questworld.api.contract.QuestingAPI;
import com.questworld.manager.PlayerStatus;
import com.questworld.util.Text;
import com.tealcube.minecraft.bukkit.TextUtils;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

public class DeluxeCategoryPicker implements InventoryHolder {

  private Inventory inv;
  private Map<DeluxeCategory, List<IQuest>> quests;

  private static final ItemStack openQuestsItem = buildOpenItem();
  private static final ItemStack currentQuestsItem = buildUnfinishedItem();
  private static final ItemStack dailyQuestsItem = buildDailyItem();
  private static final ItemStack rewardQuestsItem = buildRewardItem();
  private static final ItemStack doneQuestsItem = buildCompletedItem();

  DeluxeCategoryPicker(Map<DeluxeCategory, List<IQuest>> quests, Player player,
      String title) {
    this.quests = quests;
    inv = Bukkit.createInventory(this, InventoryType.HOPPER, Text.colorize(title));

    inv.setItem(0,
        applyStatusText(openQuestsItem.clone(), quests.get(DeluxeCategory.OPEN_QUESTS), null));
    inv.setItem(1,
        applyStatusText(currentQuestsItem.clone(), quests.get(DeluxeCategory.UNFINISHED_QUESTS), null));
    inv.setItem(2,
        applyStatusText(dailyQuestsItem.clone(), quests.get(DeluxeCategory.DAILY_QUESTS), null));
    inv.setItem(3,
        applyStatusText(rewardQuestsItem.clone(), quests.get(DeluxeCategory.UNCLAIMED_REWARDS), null));
    inv.setItem(4,
        applyStatusText(doneQuestsItem.clone(), quests.get(DeluxeCategory.COMPLETED_QUESTS), player));

    player.openInventory(inv);
  }

  public void openCategory(Player p, int slot) {
    switch (slot) {
      case 0:
        DeluxeQuestBook.openCategory(p, DeluxeCategory.OPEN_QUESTS,
            quests.get(DeluxeCategory.OPEN_QUESTS), true);
        return;
      case 1:
        DeluxeQuestBook.openCategory(p, DeluxeCategory.UNFINISHED_QUESTS,
            quests.get(DeluxeCategory.UNFINISHED_QUESTS), true);
        return;
      case 2:
        DeluxeQuestBook.openCategory(p, DeluxeCategory.DAILY_QUESTS,
            quests.get(DeluxeCategory.DAILY_QUESTS), true);
        return;
      case 3:
        DeluxeQuestBook.openCategory(p, DeluxeCategory.UNCLAIMED_REWARDS,
            quests.get(DeluxeCategory.UNCLAIMED_REWARDS), true);
        return;
      case 4:
        DeluxeQuestBook.openCategory(p, DeluxeCategory.COMPLETED_QUESTS,
            quests.get(DeluxeCategory.COMPLETED_QUESTS), true);
    }
  }

  @NotNull
  @Override
  public Inventory getInventory() {
    return inv;
  }

  private static ItemStack applyStatusText(ItemStack stack, List<IQuest> quests, Player player) {
    String name = ItemStackExtensionsKt.getDisplayName(stack);
    int quantity = quests.size();
    ItemStackExtensionsKt.setDisplayName(stack, name + " (" + quantity + ")");
    if (player != null) {
      int questPoints = QuestWorld.getAPI().getPlayerStatus(player).getQuestPoints();
      List<String> lore = new ArrayList<>();
      for (String s : ItemStackExtensionsKt.getLore(stack)) {
        lore.add(s.replace("{qp}", Integer.toString(questPoints)));
      }
      ItemStackExtensionsKt.setLore(stack, lore);
    }
    return stack;
  }

  private static ItemStack buildOpenItem() {
    ItemStack stack = new ItemStack(Material.COMPASS);
    ItemStackExtensionsKt.setDisplayName(stack, TextUtils.color("&aAvailable Quests"));
    List<String> lore = new ArrayList<>();
    lore.add("&7Click to view quests that");
    lore.add("&7you can start!");
    lore = TextUtils.color(lore);
    ItemStackExtensionsKt.setLore(stack, lore);
    return stack;
  }

  private static ItemStack buildUnfinishedItem() {
    ItemStack stack = new ItemStack(Material.WRITABLE_BOOK);
    ItemStackExtensionsKt.setDisplayName(stack, TextUtils.color("&2Current Quests"));
    List<String> lore = new ArrayList<>();
    lore.add("&7Click to view quests that");
    lore.add("&7you have already started!");
    lore = TextUtils.color(lore);
    ItemStackExtensionsKt.setLore(stack, lore);
    return stack;
  }

  private static ItemStack buildDailyItem() {
    ItemStack stack = new ItemStack(Material.CLOCK);
    ItemStackExtensionsKt.setDisplayName(stack, TextUtils.color("&eDaily Quests"));
    List<String> lore = new ArrayList<>();
    lore.add("&7Click to view quests that");
    lore.add("&7can be repeatedly done!");
    lore = TextUtils.color(lore);
    ItemStackExtensionsKt.setLore(stack, lore);
    return stack;
  }

  private static ItemStack buildRewardItem() {
    ItemStack stack = new ItemStack(Material.DIAMOND);
    ItemStackExtensionsKt.setDisplayName(stack, TextUtils.color("&5Unclaimed Rewards"));
    List<String> lore = new ArrayList<>();
    lore.add("&7Click to view quests you");
    lore.add("&7have completed, but have");
    lore.add("&7unclaimed items!");
    lore = TextUtils.color(lore);
    ItemStackExtensionsKt.setLore(stack, lore);
    return stack;
  }

  private static ItemStack buildCompletedItem() {
    ItemStack stack = new ItemStack(Material.NETHER_STAR);
    ItemStackExtensionsKt.setDisplayName(stack, TextUtils.color("&8Completed Quests"));
    List<String> lore = new ArrayList<>();
    lore.add("&f&lQuestPoints: {qp}");
    lore.add("&7Click to check quests that");
    lore.add("&7you have already done!");
    lore = TextUtils.color(lore);
    ItemStackExtensionsKt.setLore(stack, lore);
    return stack;
  }
}

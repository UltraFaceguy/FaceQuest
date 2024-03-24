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
package com.questworld.ampmenu.main;

import com.questworld.api.QuestWorld;
import com.questworld.api.contract.IPlayerStatus;
import com.questworld.api.contract.IPlayerStatus.DeluxeCategory;
import com.questworld.api.contract.IQuest;
import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import com.tealcube.minecraft.bukkit.facecore.utilities.PaletteUtil;
import com.tealcube.minecraft.bukkit.facecore.utilities.TextUtils;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.md_5.bungee.api.ChatColor;
import ninja.amp.ampmenus.menus.ItemMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class QuestMenu extends ItemMenu {

  private final Map<Player, Long> lastRequest = new WeakHashMap<>();

  private final Map<Player, ItemStack> newIcon = new WeakHashMap<>();
  private final Map<Player, ItemStack> currentIcon = new WeakHashMap<>();
  private final Map<Player, ItemStack> dailyIcon = new WeakHashMap<>();
  private final Map<Player, ItemStack> rewardsIcon = new WeakHashMap<>();
  private final Map<Player, ItemStack> completeIcon = new WeakHashMap<>();

  private final ItemStack baseNewIcon = buildNewBase();
  private final ItemStack baseCurrentIcon = buildCurrentBase();
  private final ItemStack baseDailyIcon = buildDailyBase();
  private final ItemStack baseRewardsIcon = buildRewardsBase();
  private final ItemStack baseCompleteIcon = buildCompleteBase();

  private final String quantityString = PaletteUtil.color(
      "|lgray||b|Contains |orange||b|{n} |lgray||b|Quests!");
  private final String clickString = PaletteUtil.color(
      "|white||b|[ Click To View This Category! ]");

  /*
  00 01 02 03 04 05 06 07 08
  09 10 11 12 13 14 15 16 17
  18 19 20 21 22 23 24 25 26
  27 28 29 30 31 32 33 34 35
  36 37 38 39 40 41 42 43 44
  45 46 47 48 49 50 51 52 53
  */
  public QuestMenu(JavaPlugin plugin) {
    super(ChatColor.WHITE + plugin.getConfig().getString("options.quest-title"), Size.SIX_LINE, plugin);

    setItem(0, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(1, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(2, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(3, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(4, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(5, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(6, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(7, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(8, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(9, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(10, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(11, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(12, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(13, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(14, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(15, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(16, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));
    setItem(17, new CategoryOpenIcon(this, DeluxeCategory.OPEN_QUESTS));

    setItem(18, new CategoryOpenIcon(this, DeluxeCategory.UNFINISHED_QUESTS));
    setItem(19, new CategoryOpenIcon(this, DeluxeCategory.UNFINISHED_QUESTS));
    setItem(20, new CategoryOpenIcon(this, DeluxeCategory.UNFINISHED_QUESTS));
    setItem(21, new CategoryOpenIcon(this, DeluxeCategory.UNFINISHED_QUESTS));
    setItem(27, new CategoryOpenIcon(this, DeluxeCategory.UNFINISHED_QUESTS));
    setItem(28, new CategoryOpenIcon(this, DeluxeCategory.UNFINISHED_QUESTS));
    setItem(29, new CategoryOpenIcon(this, DeluxeCategory.UNFINISHED_QUESTS));
    setItem(30, new CategoryOpenIcon(this, DeluxeCategory.UNFINISHED_QUESTS));

    setItem(23, new CategoryOpenIcon(this, DeluxeCategory.DAILY_QUESTS));
    setItem(24, new CategoryOpenIcon(this, DeluxeCategory.DAILY_QUESTS));
    setItem(25, new CategoryOpenIcon(this, DeluxeCategory.DAILY_QUESTS));
    setItem(26, new CategoryOpenIcon(this, DeluxeCategory.DAILY_QUESTS));
    setItem(32, new CategoryOpenIcon(this, DeluxeCategory.DAILY_QUESTS));
    setItem(33, new CategoryOpenIcon(this, DeluxeCategory.DAILY_QUESTS));
    setItem(34, new CategoryOpenIcon(this, DeluxeCategory.DAILY_QUESTS));
    setItem(35, new CategoryOpenIcon(this, DeluxeCategory.DAILY_QUESTS));

    setItem(36, new CategoryOpenIcon(this, DeluxeCategory.UNCLAIMED_REWARDS));
    setItem(37, new CategoryOpenIcon(this, DeluxeCategory.UNCLAIMED_REWARDS));
    setItem(38, new CategoryOpenIcon(this, DeluxeCategory.UNCLAIMED_REWARDS));
    setItem(39, new CategoryOpenIcon(this, DeluxeCategory.UNCLAIMED_REWARDS));
    setItem(45, new CategoryOpenIcon(this, DeluxeCategory.UNCLAIMED_REWARDS));
    setItem(46, new CategoryOpenIcon(this, DeluxeCategory.UNCLAIMED_REWARDS));
    setItem(47, new CategoryOpenIcon(this, DeluxeCategory.UNCLAIMED_REWARDS));
    setItem(48, new CategoryOpenIcon(this, DeluxeCategory.UNCLAIMED_REWARDS));

    setItem(41, new CategoryOpenIcon(this, DeluxeCategory.COMPLETED_QUESTS));
    setItem(42, new CategoryOpenIcon(this, DeluxeCategory.COMPLETED_QUESTS));
    setItem(43, new CategoryOpenIcon(this, DeluxeCategory.COMPLETED_QUESTS));
    setItem(44, new CategoryOpenIcon(this, DeluxeCategory.COMPLETED_QUESTS));
    setItem(50, new CategoryOpenIcon(this, DeluxeCategory.COMPLETED_QUESTS));
    setItem(51, new CategoryOpenIcon(this, DeluxeCategory.COMPLETED_QUESTS));
    setItem(52, new CategoryOpenIcon(this, DeluxeCategory.COMPLETED_QUESTS));
    setItem(53, new CategoryOpenIcon(this, DeluxeCategory.COMPLETED_QUESTS));
  }

  public ItemStack getIcon(Player p, DeluxeCategory category) {
    request(p);
    return switch (category) {
      case OPEN_QUESTS -> newIcon.get(p);
      case UNFINISHED_QUESTS -> currentIcon.get(p);
      case DAILY_QUESTS -> dailyIcon.get(p);
      case UNCLAIMED_REWARDS -> rewardsIcon.get(p);
      case COMPLETED_QUESTS -> completeIcon.get(p);
    };
  }

  public void request(Player player) {
    if (lastRequest.containsKey(player) && lastRequest.get(player) > System.currentTimeMillis()) {
      return;
    }
    lastRequest.put(player, System.currentTimeMillis() + 12000);
    List<String> lore;

    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(player);

    Map<DeluxeCategory, List<IQuest>> quests = playerStatus.getQuests();

    ItemStack newStack = baseNewIcon.clone();
    lore = TextUtils.getLore(newStack);
    lore.add("");
    lore.add(quantityString.replace("{n}",
        Integer.toString(quests.get(DeluxeCategory.OPEN_QUESTS).size())));
    lore.add("");
    lore.add(clickString);
    TextUtils.setLore(newStack, new ArrayList<>(lore), false);
    newIcon.put(player, newStack);

    ItemStack currentStack = baseCurrentIcon.clone();
    lore = TextUtils.getLore(currentStack);
    lore.add("");
    lore.add(quantityString.replace("{n}",
        Integer.toString(quests.get(DeluxeCategory.UNFINISHED_QUESTS).size())));
    lore.add("");
    lore.add(clickString);
    TextUtils.setLore(currentStack, new ArrayList<>(lore), false);
    currentIcon.put(player, currentStack);

    ItemStack dailyStack = baseDailyIcon.clone();
    lore = TextUtils.getLore(dailyStack);
    lore.add("");
    lore.add(quantityString.replace("{n}",
        Integer.toString(quests.get(DeluxeCategory.DAILY_QUESTS).size())));
    lore.add("");
    lore.add(clickString);
    TextUtils.setLore(dailyStack, new ArrayList<>(lore), false);
    dailyIcon.put(player, dailyStack);

    ItemStack rewardsStack = baseRewardsIcon.clone();
    lore = TextUtils.getLore(rewardsStack);
    lore.add("");
    lore.add(quantityString.replace("{n}",
        Integer.toString(quests.get(DeluxeCategory.UNCLAIMED_REWARDS).size())));
    lore.add("");
    lore.add(clickString);
    TextUtils.setLore(rewardsStack, new ArrayList<>(lore), false);
    rewardsIcon.put(player, rewardsStack);

    ItemStack completeStack = baseCompleteIcon.clone();
    lore = TextUtils.getLore(completeStack);
    lore.add("");
    lore.add(quantityString.replace("{n}",
        Integer.toString(quests.get(DeluxeCategory.COMPLETED_QUESTS).size())));
    lore.add("");
    lore.add(clickString);
    TextUtils.setLore(completeStack, new ArrayList<>(lore), false);
    completeIcon.put(player, completeStack);
  }

  private ItemStack buildNewBase() {
    ItemStack icon = new ItemStack(Material.BARRIER);
    ItemStackExtensionsKt.setCustomModelData(icon, 50);
    ItemStackExtensionsKt.setDisplayName(icon, FaceColor.CYAN + FaceColor.BOLD.s() + "Let's Start An Adventure!");

    List<String> lore = PaletteUtil.color(List.of(
        "",
        "|lgray|View |cyan||b|NEW |lgray|and |cyan||b|AVAILABLE|lgray| quests!",
        "",
        "|lgray|Pick one and head out to explore the",
        "|lgray|world!"
    ));

    TextUtils.setLore(icon, lore, false);
    icon.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    return icon;
  }

  private ItemStack buildCurrentBase() {
    ItemStack icon = new ItemStack(Material.BARRIER);
    ItemStackExtensionsKt.setCustomModelData(icon, 50);
    ItemStackExtensionsKt.setDisplayName(icon, FaceColor.LIGHT_GREEN + FaceColor.BOLD.s() + "Continue Your Quest!");

    List<String> lore = PaletteUtil.color(List.of(
        "",
        "|lgray|View |lgreen||b|ONGOING|lgray| quests!",
        "",
        "|lgray|Pick up where you left off and resume",
        "|lgray|your adventure!"
    ));

    TextUtils.setLore(icon, lore, false);
    icon.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    return icon;
  }

  private ItemStack buildDailyBase() {
    ItemStack icon = new ItemStack(Material.BARRIER);
    ItemStackExtensionsKt.setCustomModelData(icon, 50);
    ItemStackExtensionsKt.setDisplayName(icon, FaceColor.YELLOW + FaceColor.BOLD.s() + "Gettin' That Bread!");

    List<String> lore = PaletteUtil.color(List.of(
        "",
        "|lgray|View |yellow||b|DAILY|lgray| and |yellow||b|REPEATABLE|lgray| quests!",
        "",
        "|lgray|Get rewards every day by completing",
        "|lgray|these quests as many times as you",
        "|lgray|want!"
    ));

    TextUtils.setLore(icon, lore, false);
    icon.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    return icon;
  }

  private ItemStack buildRewardsBase() {
    ItemStack icon = new ItemStack(Material.BARRIER);
    ItemStackExtensionsKt.setCustomModelData(icon, 50);
    ItemStackExtensionsKt.setDisplayName(icon, FaceColor.PURPLE + FaceColor.BOLD.s() + "Claim Your Rewards!");

    List<String> lore = PaletteUtil.color(List.of(
        "",
        "|lgray|View |purple||b|UNCLAIMED REWARDS|lgray|!",
        "",
        "|lgray|If your inventory was full or you",
        "|lgray|declined a reward, it goes in here,",
        "|lgray|to be claimed later!"
    ));

    TextUtils.setLore(icon, lore, false);
    icon.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    return icon;
  }

  private ItemStack buildCompleteBase() {
    ItemStack icon = new ItemStack(Material.BARRIER);
    ItemStackExtensionsKt.setCustomModelData(icon, 50);
    ItemStackExtensionsKt.setDisplayName(icon, FaceColor.LIGHT_GRAY + FaceColor.BOLD.s() + "Review Completed Quests");

    List<String> lore = PaletteUtil.color(List.of(
        "",
        "|lgray|View |lgray||b|COMPLETED|lgray| quests!",
        "",
        "|lgray|Take a trip down memory lane and",
        "|lgray|see how far you've come!"
    ));

    TextUtils.setLore(icon, lore, false);
    icon.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    return icon;
  }

}

package com.questworld.api.menu;

import com.questworld.api.contract.IQuest;
import com.questworld.util.Text;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class RewardsPrompt implements InventoryHolder {

  private final Inventory inv;
  private final IQuest quest;

  public RewardsPrompt(IQuest quest, Player player) {
    this.quest = quest;
    inv = Bukkit.createInventory(this, InventoryType.HOPPER, Text.colorize("&0Rewards!"));
    int offset = 0;
    if (quest.getQuestPoints() > 0 || quest.getMoney() > 0 || quest.getXP() != 0 || StringUtils
        .isNotBlank(quest.getRewardsLore())) {
      inv.setItem(0, quest.generateRewardInfo());
      offset = 1;
    }
    for (int i = 0; i <= 4 - offset; i++) {
      if (quest.getRewards() == null || quest.getRewards().size() <= i) {
        continue;
      }
      inv.setItem(i + offset, quest.getRewards().get(i).clone());
    }
    player.openInventory(inv);
  }

  public void execute(Player player) {
    if (getEmptySlots(player) < quest.getRewards().size()) {
      MessageUtils.sendMessage(player,
          "&e[!] You do not have enough inventory space to accept this reward. Clear some space, then check this quest in &f/quests &eto claim.");
      player.closeInventory();
      return;
    }
    quest.completeFor(player);
    player.closeInventory();
  }

  public static int getEmptySlots(Player p) {
    PlayerInventory inventory = p.getInventory();
    ItemStack[] cont = inventory.getContents();
    int i = 0;
    for (ItemStack item : cont) {
      if (item == null || item.getType() == Material.AIR) {
        i++;
      }
    }
    return i;
  }

  @Override
  public Inventory getInventory() {
    return inv;
  }
}

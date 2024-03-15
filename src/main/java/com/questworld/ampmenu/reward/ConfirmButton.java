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
package com.questworld.ampmenu.reward;

import com.questworld.QuestWorldPlugin;
import com.questworld.api.contract.IQuest;
import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.Map;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import ninja.amp.ampmenus.menus.ItemMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ConfirmButton extends MenuItem {

  private final Map<Player, IQuest> parentQuestMap;
  private final int selectionId;

  public ConfirmButton(Map<Player, IQuest> parentQuestMap, int selectionId) {
    super("", new ItemStack(Material.BARRIER));
    ItemStackExtensionsKt.setCustomModelData(getIcon(), 50);
    ItemStackExtensionsKt.setDisplayName(getIcon(), FaceColor.GREEN + "Collect Rewards!");
    this.parentQuestMap = parentQuestMap;
    this.selectionId = selectionId;
  }

  @Override
  public ItemStack getFinalIcon(Player player) {
    return getIcon();
  }

  @Override
  public void onItemClick(ItemClickEvent event) {
    super.onItemClick(event);
    event.setWillClose(false);
    event.setWillUpdate(false);
    if (selectionId == 0) {
      MessageUtils.sendMessage(event.getPlayer(), "&e[!] Select a reward first!");
      return;
    }
    event.setWillClose(true);
    execute(parentQuestMap.get(event.getPlayer()), event.getPlayer());
  }

  public void execute(IQuest quest, Player player) {
    if (getEmptySlots(player) < QuestWorldPlugin.countRewards(quest)) {
      MessageUtils.sendMessage(player,
          "&e[!] You do not have enough inventory space to accept this reward. Clear some space, then check this quest in &f/quests &eto claim.");
      return;
    }
    quest.completeFor(player, selectionId);
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
}

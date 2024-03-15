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

import com.questworld.api.contract.IQuest;
import java.util.Map;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import ninja.amp.ampmenus.menus.ItemMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RewardIcon extends MenuItem {

  private final ItemMenu menu;
  private final Map<Player, IQuest> parentQuestMap;
  private final int slot;

  public RewardIcon(ItemMenu menu, Map<Player, IQuest> parentQuestMap, int slot) {
    super("", new ItemStack(Material.TOTEM_OF_UNDYING));
    this.menu = menu;
    this.parentQuestMap = parentQuestMap;
    this.slot = slot;
  }

  @Override
  public ItemStack getFinalIcon(Player player) {
    IQuest quest = parentQuestMap.get(player);
    if (quest.getRewards()[slot] == null) {
      return new ItemStack(Material.AIR);
    }
    return quest.getRewards()[slot];
  }

  @Override
  public void onItemClick(ItemClickEvent event) {
    super.onItemClick(event);
    event.setWillClose(false);
    event.setWillUpdate(false);
  }
}

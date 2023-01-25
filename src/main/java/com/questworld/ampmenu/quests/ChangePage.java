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
package com.questworld.ampmenu.quests;

import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ChangePage extends MenuItem {

  private final QuestListMenu menu;
  private final boolean forward;

  public ChangePage(QuestListMenu menu, boolean forward) {
    super("", new ItemStack(Material.PAPER));
    this.menu = menu;
    this.forward = forward;
  }

  @Override
  public ItemStack getFinalIcon(Player player) {
    ItemStack stack = getIcon().clone();
    int currentPage = menu.getCurrentPage();
    if (forward) {
      if (menu.getSortedQuests().size() / 21 > currentPage) {
        ItemStackExtensionsKt.setCustomModelData(stack, 993);
      } else {
        return new ItemStack(Material.AIR);
      }
    } else {
      if (currentPage > 0) {
        ItemStackExtensionsKt.setCustomModelData(stack, 997);
      } else {
        return new ItemStack(Material.AIR);
      }
    }
    return stack;
  }

  @Override
  public void onItemClick(ItemClickEvent event) {
    super.onItemClick(event);
    event.setWillClose(false);
    event.setWillUpdate(false);
    int currentPage = menu.getCurrentPage();
    if (forward) {
      if (menu.getSortedQuests().size() / 21 > currentPage) {
        menu.resort(event.getPlayer());
        menu.setCurrentPage(currentPage + 1);
        event.setWillUpdate(true);
      }
    } else {
      if (currentPage > 0) {
        menu.resort(event.getPlayer());
        menu.setCurrentPage(currentPage - 1);
        event.setWillUpdate(true);
      }
    }
  }
}

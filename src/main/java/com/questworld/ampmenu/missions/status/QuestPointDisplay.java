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
package com.questworld.ampmenu.missions.status;

import com.questworld.ampmenu.missions.MissionListMenu;
import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import com.tealcube.minecraft.bukkit.facecore.utilities.PaletteUtil;
import com.tealcube.minecraft.bukkit.facecore.utilities.TextUtils;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.List;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class QuestPointDisplay extends MenuItem {

  private final MissionListMenu menu;

  public QuestPointDisplay(MissionListMenu menu) {
    super("", new ItemStack(Material.TOTEM_OF_UNDYING));
    this.menu = menu;
  }

  @Override
  public ItemStack getFinalIcon(Player player) {
    int qp = menu.getSelectedQuest().getQuestPoints();
    if (qp > 0) {
      ItemStack stack = new ItemStack(Material.PAPER);
      ItemStackExtensionsKt.setCustomModelData(stack, 1010);
      ItemStackExtensionsKt.setDisplayName(stack, FaceColor.CYAN + FaceColor.BOLD.s() +
          "QuestPoint Reward");
      TextUtils.setLore(stack, PaletteUtil.color(List.of(
          "|lgray|When completed, this quest will",
          "|lgray|award you with |white|" + qp + "|lgray| QuestPoint(s)!"
      )), false);
      return stack;
    }
    return new ItemStack(Material.AIR);
  }

  @Override
  public void onItemClick(ItemClickEvent event) {
    super.onItemClick(event);
    event.setWillClose(false);
    event.setWillUpdate(false);
  }
}

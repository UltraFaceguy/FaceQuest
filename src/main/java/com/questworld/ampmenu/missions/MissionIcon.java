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
package com.questworld.ampmenu.missions;

import static com.questworld.api.menu.DeluxeQuestBook.lockedSlot;

import com.questworld.api.Manual;
import com.questworld.api.QuestWorld;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IPlayerStatus;
import com.questworld.util.ItemBuilder;
import com.questworld.util.Text;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import land.face.waypointer.WaypointerPlugin;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class MissionIcon extends MenuItem {

  private final MissionListMenu menu;
  private final int index;

  MissionIcon(MissionListMenu menu, int index) {
    super("", new ItemStack(Material.TOTEM_OF_UNDYING));
    this.menu = menu;
    this.index = index;
  }

  @Override
  public ItemStack getFinalIcon(Player player) {
    IMission mission = menu.getMission(index);
    IPlayerStatus manager = QuestWorld.getPlayerStatus(player);
    if (mission == null) {
      return new ItemStack(Material.AIR);
    }
    if (!manager.hasUnlockedTask(mission)) {
      return lockedSlot.clone();
    }
    ItemBuilder itemBuilder = new ItemBuilder(mission.getDisplayItem());
    int current = manager.getProgress(mission);
    int total = mission.getAmount();

    String progress = Text.progressBar(current, total);
    String number = Text.progressString(manager.getProgress(mission), mission.getAmount());

    if (manager.getProgress(mission) >= mission.getAmount()) {
      itemBuilder.type(Material.PAPER);
      itemBuilder.modelData(999);
    }

    if (StringUtils.isBlank(mission.getWaypointerId())) {
      if (mission.getType() instanceof Manual) {
        itemBuilder.wrapText(mission.getText(), "", progress, number, "",
            ((Manual) mission.getType()).getLabel());
      } else {
        itemBuilder.wrapText(mission.getText(), "", progress, number);
      }
    } else {
      if (mission.getType() instanceof Manual) {
        itemBuilder.wrapText(mission.getText(), "", progress, number, "",
            "&b> Right-click to set waypoint! <", ((Manual) mission.getType()).getLabel());
      } else {
        itemBuilder.wrapText(mission.getText(), "", progress, number, "",
            "&b> Right-click to set waypoint! <");
      }
    }
    return itemBuilder.get();
  }

  @Override
  public void onItemClick(ItemClickEvent event) {
    super.onItemClick(event);
    event.setWillClose(false);
    event.setWillUpdate(false);
    IMission mission = menu.getMission(index);
    if (mission == null) {
      return;
    }
    if (event.getClickType() == ClickType.RIGHT &&
        !StringUtils.isBlank(mission.getWaypointerId())) {
      WaypointerPlugin.getInstance().getWaypointManager()
          .setWaypoint(event.getPlayer(), mission.getWaypointerId());
    }
  }
}

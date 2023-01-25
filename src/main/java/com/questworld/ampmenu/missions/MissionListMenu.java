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

import com.questworld.ampmenu.missions.status.CooldownDisplay;
import com.questworld.ampmenu.missions.status.ExperienceDisplay;
import com.questworld.ampmenu.missions.status.LootDisplay;
import com.questworld.ampmenu.missions.status.QuestPointDisplay;
import com.questworld.ampmenu.missions.status.TimeLimitDisplay;
import com.questworld.api.QuestWorld;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IPlayerStatus;
import com.questworld.api.contract.IPlayerStatus.DeluxeCategory;
import com.questworld.api.contract.IQuest;
import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import ninja.amp.ampmenus.menus.ItemMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MissionListMenu extends ItemMenu {

  @Getter @Setter
  private int currentPage = 0;
  @Getter @Setter
  private IQuest selectedQuest;
  @Getter @Setter
  private DeluxeCategory selectedCategory;
  @Getter
  private final List<IMission> sortedMissions = new ArrayList<>();

  /*
  00 01 02 03 04 05 06 07 08
  09 10 11 12 13 14 15 16 17
  18 19 20 21 22 23 24 25 26
  27 28 29 30 31 32 33 34 35
  36 37 38 39 40 41 42 43 44
  45 46 47 48 49 50 51 52 53
  */
  public MissionListMenu(JavaPlugin plugin) {
    super("", Size.FIVE_LINE, plugin);

    setItem(1, new QuestPointDisplay(this));
    setItem(2, new ExperienceDisplay(this));
    setItem(3, new LootDisplay(this));
    setItem(6, new TimeLimitDisplay(this));
    setItem(7, new CooldownDisplay(this));

    setItem(9, new ChangePage(this, false));
    setItem(17, new ChangePage(this, true));

    // ROW1
    setItem(10, new MissionIcon(this, 0));
    setItem(11, new MissionIcon(this, 1));
    setItem(12, new MissionIcon(this, 2));
    setItem(13, new MissionIcon(this, 3));
    setItem(14, new MissionIcon(this, 4));
    setItem(15, new MissionIcon(this, 5));
    setItem(16, new MissionIcon(this, 6));
    // ROW2
    setItem(19, new MissionIcon(this, 7));
    setItem(20, new MissionIcon(this, 8));
    setItem(21, new MissionIcon(this, 9));
    setItem(22, new MissionIcon(this, 10));
    setItem(23, new MissionIcon(this, 11));
    setItem(24, new MissionIcon(this, 12));
    setItem(25, new MissionIcon(this, 13));
    // ROW3
    setItem(28, new MissionIcon(this, 14));
    setItem(29, new MissionIcon(this, 15));
    setItem(30, new MissionIcon(this, 16));
    setItem(31, new MissionIcon(this, 17));
    setItem(32, new MissionIcon(this, 18));
    setItem(33, new MissionIcon(this, 19));
    setItem(34, new MissionIcon(this, 20));

    setItem(39, new ReturnToQuestIcon(this));
    setItem(40, new ReturnToQuestIcon(this));
    setItem(41, new ReturnToQuestIcon(this));
  }

  @Override
  public void open(Player player) {
    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(player);
    playerStatus.update();
    setName(ChatColor.WHITE + "砳" + FaceColor.BLACK + FaceColor.BOLD.s() +
        ChatColor.stripColor(selectedQuest.getName()));
    super.open(player);
  }

  public IMission getMission(int index) {
    List<IMission> missions = sortedMissions;
    index = currentPage * 21 + index;
    if (missions.size() > index) {
      return missions.get(index);
    } else {
      return null;
    }
  }

  public void resort(Player player) {
    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(player);
    playerStatus.update();
    sortedMissions.clear();
    sortedMissions.addAll(getSelectedQuest().getOrderedMissions());
  }
}

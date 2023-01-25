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

import com.questworld.api.QuestWorld;
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

public class QuestListMenu extends ItemMenu {

  @Getter @Setter
  private DeluxeCategory deluxeCategory;
  @Getter @Setter
  private int currentPage = 0;
  @Getter
  private final List<IQuest> sortedQuests = new ArrayList<>();

  /*
  00 01 02 03 04 05 06 07 08
  09 10 11 12 13 14 15 16 17
  18 19 20 21 22 23 24 25 26
  27 28 29 30 31 32 33 34 35
  36 37 38 39 40 41 42 43 44
  45 46 47 48 49 50 51 52 53
  */
  public QuestListMenu(JavaPlugin plugin) {
    super("", Size.FOUR_LINE, plugin);

    setItem(9, new ChangePage(this, false));
    setItem(17, new ChangePage(this, true));

    // ROW1
    setItem(1, new QuestIcon(this, 0));
    setItem(2, new QuestIcon(this, 1));
    setItem(3, new QuestIcon(this, 2));
    setItem(4, new QuestIcon(this, 3));
    setItem(5, new QuestIcon(this, 4));
    setItem(6, new QuestIcon(this, 5));
    setItem(7, new QuestIcon(this, 6));
    // ROW2
    setItem(10, new QuestIcon(this, 7));
    setItem(11, new QuestIcon(this, 8));
    setItem(12, new QuestIcon(this, 9));
    setItem(13, new QuestIcon(this, 10));
    setItem(14, new QuestIcon(this, 11));
    setItem(15, new QuestIcon(this, 12));
    setItem(16, new QuestIcon(this, 13));
    // ROW3
    setItem(19, new QuestIcon(this, 14));
    setItem(20, new QuestIcon(this, 15));
    setItem(21, new QuestIcon(this, 16));
    setItem(22, new QuestIcon(this, 17));
    setItem(23, new QuestIcon(this, 18));
    setItem(24, new QuestIcon(this, 19));
    setItem(25, new QuestIcon(this, 20));

    setItem(30, new ReturnToCategoryIcon());
    setItem(31, new ReturnToCategoryIcon());
    setItem(32, new ReturnToCategoryIcon());
  }

  @Override
  public void open(Player player) {
    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(player);
    playerStatus.update();
    String title = switch (deluxeCategory) {
      case OPEN_QUESTS -> "New Quests";
      case UNFINISHED_QUESTS -> "Current Quests";
      case DAILY_QUESTS -> "Daily Quests";
      case UNCLAIMED_REWARDS -> "Claim Rewards!";
      case COMPLETED_QUESTS -> "ayyy u dun it";
    };
    setName(ChatColor.WHITE + "砲" + FaceColor.BLACK + FaceColor.BOLD.s() + title);
    super.open(player);
  }

  public IQuest getQuest(int index) {
    List<IQuest> quests = sortedQuests;
    index = currentPage * 21 + index;
    if (quests.size() > index) {
      return quests.get(index);
    } else {
      return null;
    }
  }

  public void resort(Player player) {
    IPlayerStatus playerStatus = QuestWorld.getPlayerStatus(player);
    playerStatus.update();
    sortedQuests.clear();
    sortedQuests.addAll(playerStatus.getQuests().get(deluxeCategory));
  }
}

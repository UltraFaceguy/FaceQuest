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
import java.util.WeakHashMap;
import net.md_5.bungee.api.ChatColor;
import ninja.amp.ampmenus.menus.ItemMenu;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RewardMenu extends ItemMenu {

  private static final Map<Player, IQuest> selectedQuest = new WeakHashMap<>();

  /*
  00 01 02 03 04 05 06 07 08
  09 10 11 12 13 14 15 16 17
  18 19 20 21 22 23 24 25 26
  27 28 29 30 31 32 33 34 35
  36 37 38 39 40 41 42 43 44
  45 46 47 48 49 50 51 52 53
  */
  public RewardMenu(JavaPlugin plugin) {
    super(ChatColor.WHITE + plugin.getConfig().getString("options.new-quest-reward-title"), Size.THREE_LINE, plugin);

    setItem(2, new XpRewardIcon(this, selectedQuest));
    setItem(3, new RewardIcon(this, selectedQuest, 0));
    setItem(4, new RewardIcon(this, selectedQuest, 1));
    setItem(5, new RewardIcon(this, selectedQuest, 2));
    setItem(6, new RewardIcon(this, selectedQuest, 3));

    setItem(21, new ConfirmButton(selectedQuest, -1));
    setItem(22, new ConfirmButton(selectedQuest, -1));
    setItem(23, new ConfirmButton(selectedQuest, -1));
  }

  public void setQuest(Player player, IQuest quest) {
    selectedQuest.put(player, quest);
  }
}

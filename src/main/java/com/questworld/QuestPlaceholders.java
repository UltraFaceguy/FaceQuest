package com.questworld;

import static com.questworld.QuestWorldPlugin.INT_FORMAT;

import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class QuestPlaceholders extends PlaceholderExpansion {

  @Override
  public boolean register() {
    if (!canRegister()) {
      return false;
    }
    return PlaceholderAPI.registerPlaceholderHook(getIdentifier(), this);
  }

  @Override
  public String getAuthor() {
    return "Faceguy";
  }

  @Override
  public String getIdentifier() {
    return "quest";
  }

  @Override
  public String getVersion() {
    return "1.0.0";
  }

  @Override
  public boolean persist(){
    return true;
  }

  @Override
  public String onPlaceholderRequest(Player p, String identifier) {
    if (p == null || StringUtils.isBlank(identifier)) {
      return "";
    }
    switch (identifier) {
      case "max_points":
        return INT_FORMAT.format(QuestWorldPlugin.getAPI().getMaxQuestPoints());
      case "points":
        return INT_FORMAT.format(QuestWorldPlugin.getAPI().getPlayerStatus(p).getQuestPoints());
    }
    return null;
  }
}

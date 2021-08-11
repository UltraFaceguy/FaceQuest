package com.questworld.util;

import com.questworld.QuestWorldPlugin;
import com.questworld.api.QuestStatus;
import com.questworld.api.contract.ICategory;
import com.questworld.api.contract.IPlayerStatus;
import com.questworld.api.contract.IQuest;
import org.bukkit.OfflinePlayer;

public class TransientPermissionUtil {

  public static void updateTransientPerms(OfflinePlayer p) {
    IPlayerStatus playerStatus = QuestWorldPlugin.getAPI().getPlayerStatus(p);
    for (ICategory c : QuestWorldPlugin.getAPI().getFacade().getCategories()) {
      for (IQuest q : c.getQuests()) {
        updateTransientPerms(p, playerStatus, q);
      }
    }
  }

  public static void updateTransientPerms(OfflinePlayer p, IPlayerStatus playerStatus, IQuest q) {
    QuestStatus status = playerStatus.getStatus(q);
    if (status == QuestStatus.FINISHED || status == QuestStatus.ON_COOLDOWN) {
      QuestWorldPlugin.getPermissions().playerAddTransient(p, "QC." + q.getUniqueId());
      QuestWorldPlugin.getPermissions().playerAddTransient(p, "QS." + q.getUniqueId());
    } else if (playerStatus.getProgress(q) > 0) {
      QuestWorldPlugin.getPermissions().playerAddTransient(p, "QS." + q.getUniqueId());
    } else {
      QuestWorldPlugin.getPermissions().playerRemoveTransient(p, "QC." + q.getUniqueId());
      QuestWorldPlugin.getPermissions().playerRemoveTransient(p, "QS." + q.getUniqueId());
    }
  }
}

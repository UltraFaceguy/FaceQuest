package com.questworld.api.contract;

import com.questworld.api.QuestStatus;
import com.questworld.api.annotation.NoImpl;
import com.questworld.api.annotation.Nullable;
import java.util.List;
import java.util.Map;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@NoImpl
public interface IPlayerStatus {
	boolean hasDeathEvent(IMission mission);
	
	public OfflinePlayer getPlayer();

	int countQuests(@Nullable ICategory category, @Nullable QuestStatus status);

	int getQuestPoints();

	boolean isInDialogue();

	void updateQuestPoints();

	Map<DeluxeCategory, List<IQuest>> getQuests();

	boolean hasFinished(IQuest quest);

	int getProgress(IMission mission);

	double getProgress(IQuest quest);

	int getProgress(ICategory category);

	void sendProgressStatus(IMission task, Player player);

	QuestStatus getStatus(IQuest quest);

	long getCooldownEnd(IQuest quest);

	boolean isMissionActive(IMission mission);

	boolean hasCompletedTask(IMission mission);

	boolean hasUnlockedTask(IMission mission);

	void checkComplete(IQuest quest, Player p);

	void updateTicking(Player p);

	void updateCooldowns();

	enum DeluxeCategory {
		OPEN_QUESTS,
		UNFINISHED_QUESTS,
		DAILY_QUESTS,
		UNCLAIMED_REWARDS,
		COMPLETED_QUESTS
	}
}

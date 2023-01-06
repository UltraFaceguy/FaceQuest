package com.questworld.api.contract;

import com.questworld.api.QuestStatus;
import com.questworld.api.annotation.NoImpl;
import com.questworld.api.annotation.Nullable;
import java.util.List;
import java.util.Map;
import org.bukkit.OfflinePlayer;

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

	QuestStatus getStatus(IQuest quest);

	long getCooldownEnd(IQuest quest);

	boolean isMissionActive(IMission mission);

	boolean hasCompletedTask(IMission mission);

	boolean hasUnlockedTask(IMission mission);

	void update();

	enum DeluxeCategory {
		OPEN_QUESTS,
		UNFINISHED_QUESTS,
		DAILY_QUESTS,
		UNCLAIMED_REWARDS,
		COMPLETED_QUESTS
	}
}

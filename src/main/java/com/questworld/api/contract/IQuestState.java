package com.questworld.api.contract;

import com.questworld.api.annotation.NoImpl;
import com.questworld.util.BitFlag.BitString;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@NoImpl
public interface IQuestState extends IQuest {

	void setItemRewards(Player p);

	void setItem(ItemStack item);

	void toggleWorld(String world);

	void setName(String name);

	void setRewardsLore(String rewardsLore);

	void addMission(int id);

	void removeMission(IMission mission);

	void setRawCooldown(long cooldown);

	void setCooldown(long cooldown);

	void setMoney(int money);

	void setXP(double xp);

	void setWeight(int weight);

	void setLevelRequirement(int levelRequirement);

	void setQuestPoints(int questPoints);

	void setHiddenUntilStarted(boolean hiddenUntilStarted);

	void setEnabled(boolean enabled);

	void setParent(IQuest object);

	void removeCommand(int i);

	void addCommand(String command);

	void addCommand(int index, String command);

	void setPermission(String permission);

	void setOrdered(boolean ordered);

	void setAutoClaim(boolean autoclaim);

	boolean apply();

	boolean discard();

	IQuest getSource();

	void refreshParent();

	enum Member implements BitString {
		CATEGORY,
		ID,
		COOLDOWN,
		NAME,
		ITEM,
		TASKS,
		COMMANDS,
		WORLD_BLACKLIST,
		REWARDS,
		MONEY,
		XP,
		ENABLED,
		PARTYSIZE,
		DISABLEPARTIES,
		ORDERED,
		AUTOCLAIM,
		PARENT,
		PERMISSION,
		WEIGHT,
		LEVEL,
		QUEST_POINTS,
		REWARDS_LORE,
		HIDDEN_UNTIL_STARTED
	}

	boolean hasChange(Member field);
}

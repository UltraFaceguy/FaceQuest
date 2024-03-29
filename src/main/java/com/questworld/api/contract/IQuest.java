package com.questworld.api.contract;

import com.questworld.api.annotation.NoImpl;
import com.questworld.manager.PlayerStatus;
import java.util.Collection;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@NoImpl
public interface IQuest extends DataObject {

	long COOLDOWN_SCALE = 60 * 1000;

	int getID();

	List<? extends IMission> getOrderedMissions();

	Collection<? extends IMission> getMissions();

	ItemStack getItem();

	ICategory getCategory();

	ItemStack[] getRewards();

	IMission getMission(int i);

	long getRawCooldown();

	long getCooldown();

	int getMoney();

	double getXP();

	boolean isEnabled();

	List<String> getCommands();

	boolean getOrdered();

	boolean getAutoClaimed();

	boolean getWorldEnabled(String world);

	String getFormattedCooldown();

	String getPermission();

	IQuest getParent();

	String getName();

	String getRewardsLore();

	boolean completeFor(Player p, int selectedSlot);

	IQuestState getState();

	int getWeight();

	int getModifiedWeight();

	int getLevelRequirement();

	int getModifiedLevelRequirement();

	int getQuestPoints();

	boolean isHiddenUntilStarted();

	ItemStack generateRewardInfo();
}

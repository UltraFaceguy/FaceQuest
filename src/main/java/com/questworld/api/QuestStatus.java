package com.questworld.api;

/**
 * Possible quest statuses.
 * 
 * @author mrCookieSlime
 */
public enum QuestStatus {
	/**
	 * The parent quest has not been completed.
	 */
	LOCKED_PARENT,

	/**
	 * The player lacks the required permission.
	 */
	LOCKED_NO_PERM,

	/**
	 * The player's world is not allowed for this quest
	 */
	LOCKED_WORLD,

	/**
	 * The player's level is too low for this quest
	 */
	LOCKED_LEVEL,

	/**
	 * The quest is able to be completed. It may have been previously completed, in
	 * which case either the cooldown expired or it had no cooldown.
	 */
	AVAILABLE,

	/**
	 * The quests has been completed, but rewards have not been claimed.
	 */
	REWARD_CLAIMABLE,

	/**
	 * The quest is on cooldown after all rewards have been claimed.
	 */
	ON_COOLDOWN,

	/**
	 * The quest was <tt>"Single-Use"</tt> and has been completed.
	 */
	FINISHED,

	/**
	 * Something bad happened.
	 */
	@Deprecated
	UNKNOWN,
}

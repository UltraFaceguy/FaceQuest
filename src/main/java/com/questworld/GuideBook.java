package com.questworld;

import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.questworld.api.QuestWorld;
import com.questworld.api.Translation;
import com.questworld.util.ItemBuilder;

public class GuideBook {
	private static HashSet<Integer> pastBooks = new HashSet<>();
	private static volatile GuideBook instance = null;

	private final ItemStack guide;

	public static GuideBook instance() {
		if (instance == null)
			instance = new GuideBook();

		return instance;
	}

	public ItemStack item() {
		return guide.clone();
	}

	public static void reset() {
		instance = null;
	}

	public static boolean isGuide(ItemStack item) {
		return item != null && pastBooks.contains(item.hashCode());
	}

	private GuideBook() {
		guide = new ItemBuilder(Material.ENCHANTED_BOOK)
				.wrapText(QuestWorld.translate(Translation.GUIDE_BOOK).split("\n")).get();

		pastBooks.add(guide.hashCode());
	}
}

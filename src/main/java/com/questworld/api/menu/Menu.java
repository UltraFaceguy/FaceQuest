package com.questworld.api.menu;

import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import com.questworld.util.ItemBuilder;
import com.questworld.util.Text;

public class Menu implements InventoryHolder {

	private static final int ROW_WIDTH = 9;
	private Inventory inv;
	private Consumer<InventoryClickEvent>[] handlers;
	
	// There is no reason I should have to keep track of this, butresizing needs
	// to create a new inventory.
	// Bukkit offers no way to get the old title without an open view.
	// Why.
	private final String title;

	@SuppressWarnings("unchecked")
	public Menu(int rows, String title) {
		int cells = rows * ROW_WIDTH;
		this.title = Text.colorize(title);
		inv = makeInv(cells);
		handlers = new Consumer[cells];
	}

	private Inventory makeInv(int cells) {
		Inventory inv = Bukkit.createInventory(this, cells, StringUtils.isBlank(title) ? "" : title);
		return inv;
	}

	public void resize(int rows) {
		int cells = rows * ROW_WIDTH;
		
		Inventory inv2 = makeInv(cells);

		inv2.setContents(Arrays.copyOf(inv.getContents(), cells));
		handlers = Arrays.copyOf(handlers, cells);

		for (HumanEntity e : inv.getViewers()) {
			e.closeInventory();
			e.openInventory(inv2);
		}
		inv = inv2;
	}

	public void remove(int slot) {
		inv.clear(slot);
		handlers[slot] = null;
	}

	public void put(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
		if (slot >= inv.getSize())
			resize(slot / ROW_WIDTH + 1);
		if (item == null)
			inv.clear(slot);
		else
			// Arbitrary data to make double-click not pick up items from upper inv
			inv.setItem(slot, new ItemBuilder(item).flag(ItemFlag.HIDE_PLACED_ON).get());
		handlers[slot] = handler;
	}

	public Consumer<InventoryClickEvent> getHandler(int slot) {
		if (slot >= inv.getSize())
			return null;

		return handlers[slot];
	}

	public ItemStack getItem(int slot) {
		if (slot >= inv.getSize())
			return null;

		return inv.getItem(slot);
	}

	public void openFor(HumanEntity... viewers) {
		for (HumanEntity v : viewers)
			v.openInventory(inv);
	}

	public boolean requestCancel(Inventory inv, int slot) {
		return slot < inv.getSize();
	}

	public boolean click(InventoryClickEvent event) {
		// Ignore creative and clicking out of the inv
		if (event.getClick().isCreativeAction() || event.getRawSlot() < 0)
			return false;
		else if (event.getRawSlot() < event.getInventory().getSize()) {
			Consumer<InventoryClickEvent> handler = handlers[event.getSlot()];
			if (handler != null)
				handler.accept(event);

			return true;
		}
		// Allow non-transfer clicks
		return event.getClick().isShiftClick();
	}

	@Override
	public Inventory getInventory() {
		return inv;
	}
}

package com.questworld.extension.builtin;

import com.questworld.api.MissionType;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IMissionState;
import com.questworld.api.menu.MissionButton;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ExternalCommandMission extends MissionType implements Listener {

  public ExternalCommandMission() {
    super("EXTERNAL_COMMAND", true, new ItemStack(Material.COMMAND_BLOCK));
  }

  @Override
  public ItemStack userDisplayItem(IMission instance) {
    return new ItemStack(Material.CLOCK);
  }

  @Override
  protected String userInstanceDescription(IMission instance) {
    return "&7Have Command Run " + instance.getAmount() + " times";
  }

  // Progress increment logic is withing Commands.class

  @Override
  protected void layoutMenu(IMissionState changes) {
    putButton(12, MissionButton.commandId(changes));
    putButton(17, MissionButton.amount(changes));
  }
}

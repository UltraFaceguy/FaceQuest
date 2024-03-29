package com.questworld.api.menu;

import static com.questworld.util.json.Prop.BLUE;
import static com.questworld.util.json.Prop.CLICK_RUN;
import static com.questworld.util.json.Prop.DARK_GREEN;
import static com.questworld.util.json.Prop.DARK_RED;
import static com.questworld.util.json.Prop.FUSE;
import static com.questworld.util.json.Prop.GRAY;
import static com.questworld.util.json.Prop.HOVER_TEXT;
import static com.questworld.util.json.Prop.WHITE;

import com.questworld.QuestWorldPlugin;
import com.questworld.api.QuestWorld;
import com.questworld.api.SinglePrompt;
import com.questworld.api.Translation;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IMissionState;
import com.questworld.manager.PlayerStatus;
import com.questworld.manager.ProgressTracker;
import com.questworld.util.EntityTools;
import com.questworld.util.ItemBuilder;
import com.questworld.util.PlayerTools;
import com.questworld.util.Text;
import com.questworld.util.json.JsonBlob;
import com.questworld.util.json.Prop;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import land.face.portrait.PortraitGenerator;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MissionButton {

  public static MenuData item(IMissionState changes) {
    return simpleButton(changes,
        new ItemBuilder(changes.getItem()).wrapText(
            Text.itemName(changes.getItem()),
            "",
            "&e> Click to change the mission item").get(),
        event -> {
          Player p = (Player) event.getWhoClicked();
          ItemStack hand = p.getInventory().getItemInMainHand();
					if (hand.getType() != Material.AIR) {
						changes.setItem(hand.clone());
					}
        }
    );
  }

  public static MenuData amount(IMissionState changes) {
    return amount(changes, 16);
  }

  public static MenuData amount(IMissionState changes, int groupSize) {
    return simpleButton(changes,
        new ItemBuilder(Material.REDSTONE).wrapText(
            "&7Amount: &b" + changes.getAmount(),
            "",
            "&rLeft click: &e+1",
            "&rRight click: &e-1",
            "&rShift left click: &e+" + groupSize,
            "&rShift right click: &e-" + groupSize).get(),
        event -> {
          int amt = clickNumber(changes.getAmount(), groupSize, event);
          changes.setAmount(Math.max(amt, 1));
        }
    );
  }

  public static MenuData actionBarUpdates(IMissionState changes) {
    return simpleButton(changes,
        new ItemBuilder(Material.NOTE_BLOCK).wrapText(
            "&bAction Bar Updates: &f" + changes.getActionBarUpdates()).get(),
        event -> changes.setActionBarUpdates(!changes.getActionBarUpdates())
    );
  }

  public static MenuData waypointer(IMissionState changes) {
    String waypointerId = changes.getWaypointerId();

    return new MenuData(
        new ItemBuilder(Material.CARTOGRAPHY_TABLE).wrapText(
            "&7Current Waypoint ID: &e" + waypointerId,
            "",
            "&e> Click to change!").get(),
        event -> QBDialogue.openQuestMissionWaypointEditor((Player) event.getWhoClicked(), changes)
    );
  }

  public static MenuData entity(IMissionState changes) {
    EntityType entity = changes.getEntity();

    return new MenuData(
        EntityTools.getEntityDisplay(entity).wrapText(
            "&7Entity Type: &e" + EntityTools.nameOf(entity),
            "",
            "&e> Click to change the entity").get(),
        event -> QBDialogue.openQuestMissionEntityEditor((Player) event.getWhoClicked(), changes)
    );
  }

  public static MenuData entityName(IMissionState changes) {
    String name = changes.getCustomString();
    return new MenuData(
        new ItemBuilder(Material.NAME_TAG).wrapText(
            "&7Entity name: &r&o" + (name.length() > 0 ? name : "-none-"),
            "",
            "&e> Click to change the Name").get(),
        event -> {
          Player p = (Player) event.getWhoClicked();

          p.closeInventory();
          PlayerTools.promptInput(p, new SinglePrompt(
              PlayerTools.makeTranslation(true, Translation.KILLMISSION_NAME_EDIT), (c, s) -> {
            changes.setCustomString(Text.deserializeNewline(Text.colorize(s)));
            if (changes.apply()) {
              PlayerTools.sendTranslation(p, true, Translation.KILLMISSION_NAME_SET);
            }
            QuestBook.openQuestMissionEditor(p, changes);
            return true;
          }
          ));
        }
    );
  }

  public static MenuData commandId(IMissionState changes) {
    String id = changes.getCustomString();
    return new MenuData(
        new ItemBuilder(Material.NAME_TAG).wrapText(
            "&7ID: &r&o" + (id.length() > 0 ? id : "-none-"),
            "",
            "&e> Click to change the id").get(),
        event -> {
          Player p = (Player) event.getWhoClicked();

          p.closeInventory();
          PlayerTools.promptInput(p, new SinglePrompt("Enter the external command ID to check for. cancel() to abort", (c, s) -> {
            changes.setCustomString(s);
            if (changes.apply()) {
              MessageUtils.sendMessage(p, "set!");
            }
            QuestBook.openQuestMissionEditor(p, changes);
            return true;
          }
          ));
        }
    );
  }

  public static MenuData missionName(IMissionState changes) {
    return new MenuData(
        new ItemBuilder(Material.NAME_TAG).wrapText(
            "&7" + changes.getText(),
            "",
            "&rLeft click: Edit mission name",
            "&rRight click: Reset mission name").get(),
        event -> {
          Player p = (Player) event.getWhoClicked();

          if (event.getClick().isRightClick()) {
            changes.setDisplayName("");
            apply(event, changes);
          } else {
            p.closeInventory();
            PlayerTools.promptInput(p, new SinglePrompt(
                PlayerTools.makeTranslation(true, Translation.MISSION_NAME_EDIT), (c, s) -> {
              changes.setDisplayName(Text.deserializeNewline(Text.colorize(s)));
              if (changes.apply()) {
                PlayerTools.sendTranslation(p, true, Translation.MISSION_NAME_SET);
                QuestBook.openQuestMissionEditor(p, changes.getSource());
              }
              return true;
            }
            ));
          }
        }
    );
  }

  public static MenuData timeframe(IMissionState changes) {
    return simpleButton(changes, new ItemBuilder(Material.CLOCK).wrapText(
        "&7Complete mission within: &b" + Text.timeFromNum(changes.getTimeframe()),
        "",
        "&rLeft click: &e+1m",
        "&rRight click: &e-1m",
        "&rShift left click: &e+1h",
        "&rShift right click: &e-1h").get(),
        event -> {
          int amt = clickNumber(changes.getTimeframe(), 60, event);
          changes.setTimeframe(Math.max(amt, 0));
        }
    );
  }

  public static MenuData deathReset(IMissionState changes) {
    return simpleButton(changes,
        new ItemBuilder(Material.PLAYER_HEAD).wrapText(
            "&7Resets on death: " + Text.booleanBadge(changes.getDeathReset()),
            "",
            "&e> Click to change whether this Mission's Progress resets when a Player dies").get(),
        event -> {
          changes.setDeathReset(!changes.getDeathReset());
        }
    );
  }

  public static MenuData spawnersAllowed(IMissionState changes) {
    return simpleButton(changes, new ItemBuilder(Material.SPAWNER).wrapText(
        "&7Allow Mobs from Spawners: " + Text.booleanBadge(changes.getSpawnerSupport()),
        "",
        "&e> Click to change whether this Mission will also count Mobs which were spawned by a Mob Spawner")
            .get(), event -> {
          changes.setSpawnerSupport(!changes.getSpawnerSupport());
        }
    );
  }

  private static void dialogueThing2(Player p, IMission mission, int index, List<String> dialogue) {
    int endoff = dialogue.size() - index;

    PlayerTools.promptInputOrCommand(p,
        new SinglePrompt(PlayerTools.makeTranslation(true, Translation.MISSION_DIALOG_ADD), null,
            (c, s) -> {
              if (s.equalsIgnoreCase("exit()") || s.equalsIgnoreCase("/exit")) {
                IMissionState state = mission.getState();
                state.setDialogue(dialogue);
                if (state.apply()) {
                  String filename = ProgressTracker.dialogueFile(state.getSource()).getName();
                  PlayerTools.sendTranslation(p, true, Translation.MISSION_DIALOG_SET, filename);
                  dialogueThing(p, mission);
                }
                return true;
              }

              Translation translator = s.startsWith("/") ? Translation.MISSION_COMMAND_ADDED
                  : Translation.MISSION_DIALOG_ADDED;
              dialogue.add(dialogue.size() - endoff, Text.deserializeNewline(Text.colorize(s)));
              SinglePrompt.setNextDisplay(c, PlayerTools.makeTranslation(true, translator, s));
              QuestWorld.getSounds().DIALOG_ADD.playTo(p);

              return false;
            }
        ));
  }

  private static void dialogueThing(Player p, IMission mission) {

    List<String> dialogue = new ArrayList<>(mission.getDialogue());

    p.sendMessage(Text.colorize("&7&m----------------------------"));
    int size = dialogue.size();
    for (int i = 0; i < size; ++i) {
      String s = dialogue.get(i);
      int index = i;
      Prop remove = FUSE(
          HOVER_TEXT("Click to remove " + (s.startsWith("/") ? "command" : "dialogue")),
          CLICK_RUN(p, () -> {
                dialogue.remove(index);

                IMissionState state = mission.getState();
                state.setDialogue(dialogue);
						if (state.apply()) {
							dialogueThing(p, mission);
						}
              }
          ));

      Prop above = FUSE(HOVER_TEXT("Click to insert above", GRAY), CLICK_RUN(p, () -> {
        dialogueThing2(p, mission, index, dialogue);
      }));

      PlayerTools.tellraw(p, new JsonBlob("^ ", DARK_GREEN, above).add("X ", DARK_RED, remove)
          .addLegacy(s, WHITE, remove).toString());
    }

    Prop add = FUSE(HOVER_TEXT("Click to add dialogue", GRAY), CLICK_RUN(p, () -> {
      dialogueThing2(p, mission, dialogue.size(), dialogue);
    }));

    PlayerTools.tellraw(p,
        new JsonBlob("+ ", DARK_GREEN, add).add("Add more dialogue", GRAY, add).toString());

    Prop back = FUSE(HOVER_TEXT("Open mission editor", GRAY),
        CLICK_RUN(p, () -> QuestBook.openQuestMissionEditor(p, mission)));

    PlayerTools.tellraw(p,
        new JsonBlob("< ", BLUE, back).add("Return to mission editor", GRAY, back).toString());

    p.sendMessage(Text.colorize("&7&m----------------------------"));
  }

  public static MenuData dialogue(IMission mission) {
    IMissionState changes = mission.getState();
    List<String> lore = new ArrayList<>(List.of(
        "&7Dialogue",
        "",
        "&fLeft Click: Edit the Dialogue",
        "&fRight Click: Demo Dialogue",
        ""
    ));
    lore.addAll(changes.getDialogue());
    return new MenuData(new ItemBuilder(Material.PAPER)
        .wrapText(lore.toArray(new String[0])).get(),
        event -> {
          Player p = (Player) event.getWhoClicked();

          if (event.getClick().isRightClick()) {
						if (changes.getDialogue().isEmpty()) {
							p.sendMessage(Text.colorize("&4No Dialogue found!"));
						} else {
              if ("CITIZENS_INTERACT".equals(changes.getType().toString())) {
                boolean success = PortraitGenerator.getInstance().getDialogueManager()
                    .display(p, mission.getUniqueId().toString());
                if (!success) {
                  p.sendMessage(Text.colorize("&4Failed to display dialogue!"));
                } else {
                  p.sendMessage(Text.colorize("&aOpening dialogue menu..."));
                }
              } else {
                p.sendMessage(Text.colorize("&aShowing dialogue..."));
                PlayerStatus.sendDialogue(QuestWorldPlugin.getAPI().getPlayerStatus(p), changes,
                    changes.getDialogue().iterator());
              }
						}
            return;
          }

          dialogueThing(p, changes.getSource());
          p.closeInventory();
        }
    );
  }

  public static void apply(InventoryClickEvent event, IMissionState changes) {
    if (changes.apply()) {
      QuestBook.openQuestMissionEditor((Player) event.getWhoClicked(), changes.getSource());
    }
  }

  public static MenuData simpleButton(IMissionState changes, ItemStack item,
      Consumer<InventoryClickEvent> action) {
    return new MenuData(item, action.andThen(event -> apply(event, changes)));
  }

  public static int clickNumber(int initial, int groupSize, InventoryClickEvent event) {
    switch (event.getClick()) {
      case RIGHT:
        initial -= 1;
        break;
      case SHIFT_RIGHT:
        initial -= groupSize;
        break;
      case LEFT:
        initial += 1;
        break;
      case SHIFT_LEFT:
        initial += groupSize;
        break;
      case NUMBER_KEY:
        initial = initial * 10 + event.getHotbarButton() + 1;
        break;
      case DROP:
        initial /= 10;
        break;
      default:
        break;
    }

    return initial;
  }
}

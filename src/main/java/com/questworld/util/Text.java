package com.questworld.util;

import com.questworld.api.QuestWorld;
import com.questworld.api.Translation;
import com.questworld.api.annotation.Nullable;
import com.tealcube.minecraft.bukkit.facecore.utilities.FaceColor;
import com.tealcube.minecraft.bukkit.facecore.utilities.PaletteUtil;
import com.tealcube.minecraft.bukkit.facecore.utilities.TextUtils;
import io.pixeloutlaw.minecraft.spigot.hilt.ItemStackExtensionsKt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public final class Text {

  // TODO: Probably make all of this better and then comment
  public final static char dummyChar = '&';
  public final static char colorChar = ChatColor.COLOR_CHAR;

  private static final String greenCheck = colorize("&2&l\u2714");
  private static final String redX = colorize("&4&l\u2718");

  private final static String cap = FaceColor.DARK_GRAY + "▌\uF801";
  private static final List<String> PROGRESS_BARS = buildBars();

  /**
   * Colors a string
   *
   * @param input A string with "&1" style colors
   * @return Colored string
   */
  public static String colorize(@Nullable("Returns null") String input) {
    if (input == null) {
      return null;
    }

    return ChatColor.translateAlternateColorCodes(dummyChar, input);
  }

  public static String escapeColor(String input) {
    return input.replace(colorChar, dummyChar);
  }

  public static String colorize(String... inputs) {
    StringBuilder sb = new StringBuilder(inputs.length);

    for (String input : inputs) {
      sb.append(colorize(input));
    }

    return sb.toString();
  }

  public static String[] colorizeList(String... inputs) {
    String[] output = new String[inputs.length];

    for (int i = 0; i < inputs.length; ++i) {
      output[i] = colorize(inputs[i]);
    }

    return output;
  }

  public static String decolor(@Nullable("Returns null") String input) {
    if (input == null) {
      return null;
    }

    return ChatColor.stripColor(input);
  }

  public static String decolor(String... inputs) {
    StringBuilder sb = new StringBuilder(inputs.length);

    for (String input : inputs) {
      sb.append(decolor(input));
    }

    return sb.toString();
  }

  public static String[] decolorList(String... inputs) {
    String[] output = new String[inputs.length];

    for (int i = 0; i < inputs.length; ++i) {
      output[i] = decolor(inputs[i]);
    }

    return output;
  }

  /**
   * Transforms a boolean into a nice text badge.
   *
   * @param state A boolean value
   * @return <font color="green"><b>&#x2714;</b></font> if true;
   * <font color="red"><b>&#x2718;</b></font> if false
   */
  public static String booleanBadge(boolean state) {
    return state ? greenCheck : redX;
  }

  public static String serializeNewline(@Nullable("Returns null") String input) {
    if (input == null) {
      return null;
    }

    return input.replace("\\", "\\\\").replace("\n", "\\n");
  }

  public static String deserializeNewline(@Nullable("Returns null") String input) {
    if (input == null) {
      return null;
    }

    return input.replaceAll("(?i)(?<!\\\\)((?:\\\\\\\\)*)\\\\n", "$1\n").replace("\\\\", "\\");
  }

  public static String stringOf(Location location) {
    if (location.getWorld() != null) {
      return QuestWorld.translate(Translation.WORLD_FMT, String.valueOf(location.getBlockX()),
          String.valueOf(location.getBlockY()), String.valueOf(location.getBlockZ()),
          location.getWorld().getName());
    }

    return QuestWorld.translate(Translation.UNKNOWN_WORLD);
  }

  public static String stringOf(Location location, int radius) {
    if (location.getWorld() != null) {
      return QuestWorld.translate(Translation.RANGE_FMT, String.valueOf(location.getBlockX()),
          String.valueOf(location.getBlockY()), String.valueOf(location.getBlockZ()),
          location.getWorld().getName(), String.valueOf(radius));
    }

    return QuestWorld.translate(Translation.UNKNOWN_WORLD);
  }

  public static UUID toUniqueId(String uuidString) {
    if (uuidString != null) {
      try {
        return UUID.fromString(uuidString);
      } catch (IllegalArgumentException e) {
      }
    }

    return null;
  }

  static Pattern firstLetter = Pattern.compile("\\b\\S");

  public static String niceName(String input) {
    input = input.replace('_', ' ').trim().toLowerCase(Locale.getDefault());

    StringBuilder sb = new StringBuilder(input.length());

    Matcher m = firstLetter.matcher(input);
    while (m.find()) {
      m.appendReplacement(sb, m.group().toUpperCase(Locale.getDefault()));
    }

    m.appendTail(sb);

    return sb.toString();
  }

  public static String timeFromNum(long minutes) {
    long hours = minutes / 60;
    minutes = minutes - hours * 60;

    return QuestWorld.translate(Translation.TIME_FMT, String.valueOf(hours), String.valueOf(minutes));
  }

  private static List<String> buildBars() {
    return List.of(
        FaceColor.TRUE_WHITE +"砀",
        FaceColor.TRUE_WHITE +"砟",
        FaceColor.TRUE_WHITE +"砂",
        FaceColor.TRUE_WHITE +"砃",
        FaceColor.TRUE_WHITE +"砄",
        FaceColor.TRUE_WHITE +"砅",
        FaceColor.TRUE_WHITE +"砆",
        FaceColor.TRUE_WHITE +"砇",
        FaceColor.TRUE_WHITE +"砈",
        FaceColor.TRUE_WHITE +"砉",
        FaceColor.TRUE_WHITE +"砊",
        FaceColor.TRUE_WHITE +"砋",
        FaceColor.TRUE_WHITE +"砌",
        FaceColor.TRUE_WHITE +"砍",
        FaceColor.TRUE_WHITE +"砎",
        FaceColor.TRUE_WHITE +"砏",
        FaceColor.TRUE_WHITE +"砐",
        FaceColor.TRUE_WHITE +"砑",
        FaceColor.TRUE_WHITE +"砒",
        FaceColor.TRUE_WHITE +"砓",
        FaceColor.TRUE_WHITE +"研"
    );
  }

  public static String progressString(int current, int total) {
    return progressString(current, total, false);
  }

  public static String progressString(int current, int total, boolean showPercent) {
    if (!showPercent) {
      return FaceColor.WHITE + "[ " + current + " / " + total + " ]";
    } else {
      float percent = (float) Math.floor((current * 100) / (float) total);
      return FaceColor.WHITE + "[ " + current + " / " + total + " ]  " + percent + "%";
    }
  }

  public static String progressBar(float current, float total) {
    return progressBar(current / total);
  }

  public static String progressBar(float percent) {
    if (percent > 0.995) {
      return PROGRESS_BARS.get(PROGRESS_BARS.size() - 1);
    }
    return PROGRESS_BARS.get((int) Math.floor(percent * PROGRESS_BARS.size()));
  }

  public static String itemName(ItemStack stack) {
    return ItemStackExtensionsKt.getDisplayName(stack);
  }

  private static final Map<String, String> cachedObjectiveNames = new HashMap<>();

  public static String convertToSuperscript(String original) {
    if (cachedObjectiveNames.containsKey(original)) {
      return cachedObjectiveNames.get(original);
    }
    String s = original.toLowerCase();
    s = s
        .replaceAll("a", "ᵃ\uF801")
        .replaceAll("b", "ᵇ\uF801")
        .replaceAll("c", "ᶜ\uF801")
        .replaceAll("d", "ᵈ\uF801")
        .replaceAll("e", "ᵉ\uF801")
        .replaceAll("f", "ᶠ\uF801")
        .replaceAll("g", "ᵍ\uF801")
        .replaceAll("h", "ʰ\uF801")
        .replaceAll("i", "ᶦ\uF801")
        .replaceAll("j", "ʲ\uF801")
        .replaceAll("k", "ᵏ\uF801")
        .replaceAll("l", "ˡ\uF801")
        .replaceAll("m", "ᵐ\uF801")
        .replaceAll("n", "ⁿ\uF801")
        .replaceAll("o", "ᵒ\uF801")
        .replaceAll("p", "ᵖ\uF801")
        .replaceAll("q", "ᵠ\uF801")
        .replaceAll("r", "ʳ\uF801")
        .replaceAll("s", "ˢ\uF801")
        .replaceAll("t", "ᵗ\uF801")
        .replaceAll("u", "ᵘ\uF801")
        .replaceAll("v", "ᵛ\uF801")
        .replaceAll("w", "ʷ\uF801")
        .replaceAll("x", "ˣ\uF801")
        .replaceAll("y", "ʸ\uF801")
        .replaceAll("z", "ᶻ\uF801")
        .replaceAll("1", "¹\uF801")
        .replaceAll("2", "²\uF801")
        .replaceAll("3", "³\uF801")
        .replaceAll("4", "⁴\uF801")
        .replaceAll("5", "⁵\uF801")
        .replaceAll("6", "⁶\uF801")
        .replaceAll("7", "⁷\uF801")
        .replaceAll("8", "⁸\uF801")
        .replaceAll("9", "⁹\uF801")
        .replaceAll("0", "⁰\uF801")
        .replaceAll("-", "⁻\uF801")
        .replaceAll("/", "⃫\uF801")
        .replaceAll("\\(", "⁽\uF801")
        .replaceAll("\\)", "⁾\uF801")
        .replaceAll("'", "՚\uF801")
        .replaceAll("!", "ᵎ\uF801")
        .replaceAll("\\?", "ˀ\uF801");
    cachedObjectiveNames.put(original, s);
    return s;
  }

  public static ArrayList<String> wrapAndColor(int maxLineLength, String... input) {
    ArrayList<String> output = new ArrayList<>();

    for (String line : input) {
      String[] words = line.split("\\s");
      StringBuilder currentLine = new StringBuilder(FaceColor.LIGHT_GRAY.s());
      int length = 0;
      for (String word : words) {
        String coloredWord = PaletteUtil.color(TextUtils.color(word));
        String s = net.md_5.bungee.api.ChatColor.stripColor(word);
        length += s.length();
        if (length > maxLineLength) {
          length = 0;
          output.add(currentLine.toString());
          currentLine = new StringBuilder(FaceColor.LIGHT_GRAY + coloredWord + " ");
        } else {
          currentLine.append(coloredWord).append(" ");
        }
      }
      output.add(currentLine.toString());
    }
    return output;
  }
}

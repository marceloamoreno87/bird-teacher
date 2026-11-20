package br.birdteacher.app;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;

final class Appearance {
  static final String[] PALETTES = {"petroleo", "mata", "ardosia", "cobre", "lagoa"};
  static final String[] PALETTE_NAMES = {"Petróleo", "Floresta", "Ardósia", "Cobre", "Lagoa"};
  private static final Properties colors = new Properties();

  static {
    try (var input = Appearance.class.getResourceAsStream("palette.properties")) {
      colors.load(input);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static final Preferences prefs = Preferences.userNodeForPackage(Appearance.class);
  static String palette = prefs.get("palette", legacyPalette(prefs.get("bird", "")));

  private static String legacyPalette(String previous) {
    return switch (previous) {
      case "caninde" -> "mata";
      case "lear" -> "ardosia";
      case "vermelha" -> "cobre";
      case "maracana" -> "lagoa";
      default -> colors.getProperty("defaultTheme").split("-")[0];
    };
  }

  static boolean light = prefs.getBoolean("light", false);
  static final Font BODY = font("NunitoSans-Regular"),
      BOLD = font("NunitoSans-Bold"),
      DISPLAY = font("Anton-Regular");
  private static final Map<String, Image> images = new HashMap<>();

  private static Font font(String file) {
    try (var input = Appearance.class.getResourceAsStream("fonts/" + file + ".ttf")) {
      var loaded = Font.createFont(Font.TRUETYPE_FONT, input);
      GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loaded);
      return loaded;
    } catch (Exception e) {
      throw new IllegalStateException(file, e);
    }
  }

  static Color color(String role) {
    return Color.decode(hex(role));
  }

  private static String hex(String role) {
    return colors.getProperty(palette + (light ? "-light." : "-dark.") + role);
  }

  static Image art(String file) {
    return images.computeIfAbsent(
        file,
        k ->
            new ImageIcon(Objects.requireNonNull(Appearance.class.getResource("art/" + k + ".png")))
                .getImage());
  }

  static java.util.List<Image> emblemSizes() {
    return java.util.List.of(
        art("falcao/icone-16"),
        art("falcao/icone-32"),
        art("falcao/icone-64"),
        art("falcao/icone-256"));
  }

  static Icon icon(String name, int size, JComponent component) {
    var icon = new FlatSVGIcon("br/birdteacher/app/icons/" + name + ".svg", size, size);
    icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> component.getForeground()));
    return icon;
  }

  static void install() {
    if (!Arrays.asList(PALETTES).contains(palette)) palette = PALETTES[0];
    var defaults = new HashMap<String, String>();
    defaults.put("@background", hex("background"));
    defaults.put("@componentBackground", hex("surface"));
    defaults.put("@foreground", hex("ink"));
    defaults.put("@accentColor", hex("speaker"));
    defaults.put("@selectionBackground", hex("selected"));
    defaults.put("@selectionForeground", hex("ink"));
    defaults.put("Component.borderColor", hex("line"));
    defaults.put("Component.focusedBorderColor", hex("speaker"));
    defaults.put("Button.background", hex("surface"));
    defaults.put("Table.background", hex("surface"));
    defaults.put("TableHeader.background", hex("surface"));
    defaults.put("TableHeader.foreground", hex("muted"));
    defaults.put("Table.gridColor", hex("line"));
    defaults.put(
        "[style]Button.primary",
        "background: "
            + hex("accent")
            + "; foreground: "
            + hex("on-accent")
            + "; hoverBackground: "
            + hex("accent")
            + "; pressedBackground: "
            + hex("accent")
            + "; borderWidth: 0; arc: 14");
    defaults.put(
        "[style]ToggleButton.navigation",
        "background: " + hex("surface") + "; foreground: " + hex("muted")
            + "; selectedBackground: " + hex("selected") + "; selectedForeground: " + hex("speaker")
            + "; disabledSelectedBackground: " + hex("selected") + "; disabledSelectedForeground: " + hex("speaker")
            + "; focusedBackground: " + hex("surface") + "; hoverBackground: " + hex("surface")
            + "; hoverForeground: " + hex("ink") + "; borderWidth: 0; arc: 14");
    FlatLaf.setGlobalExtraDefaults(defaults);
    if (light) FlatLightLaf.setup();
    else FlatDarkLaf.setup();
    UIManager.put("defaultFont", BODY.deriveFont(14f));
    UIManager.put("Button.arc", 12);
    UIManager.put("Component.arc", 10);
    UIManager.put("TextComponent.arc", 10);
    UIManager.put("ScrollBar.width", 9);
  }

  static void change(String selectedPalette, boolean bright) {
    palette = selectedPalette;
    light = bright;
    prefs.put("palette", palette);
    prefs.putBoolean("light", light);
    install();
    FlatLaf.updateUI();
    for (var w : java.awt.Window.getWindows()) {
      apply(w);
      w.repaint();
    }
  }

  static void tint(JComponent c, String fg, String bg) {
    c.putClientProperty("bird.fg", fg);
    c.putClientProperty("bird.bg", bg);
    paintColors(c);
  }

  private static void paintColors(JComponent c) {
    if (c.getClientProperty("bird.fg") instanceof String role) c.setForeground(color(role));
    if (c.getClientProperty("bird.bg") instanceof String role) c.setBackground(color(role));
  }

  private static void apply(Component c) {
    if (c instanceof JComponent j) paintColors(j);
    if (c instanceof Container parent) for (var child : parent.getComponents()) apply(child);
  }
}

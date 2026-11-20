package br.birdteacher.app;

import static br.birdteacher.app.Widgets.*;

import br.birdteacher.domain.Transcript;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** A small window for use over a call. All commands go through the main window's actions. */
final class Companion {
  private final Window owner;
  private final JDialog dialog;
  private final JLabel state = label("", Appearance.BOLD, 15, "speaker"),
      source = label("", Appearance.BOLD, 13, "muted");
  private final JTextArea text = new JTextArea(3, 27);
  private final JButton toggle = button("Iniciar gravação", "mic"), finish = button("Encerrar e revisar", null);
  private TrayIcon tray;
  private MenuItem trayToggle, trayFinish;

  Companion(Window owner) {
    this.owner = owner;
    dialog = new JDialog((Frame) null, "Bird Teacher · Modo compacto", false);
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.setAlwaysOnTop(true);
    dialog.setAutoRequestFocus(false);
    dialog.addWindowListener(
        new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            full();
          }
        });
    var body = card(new BorderLayout(0, 8), "surface", 12);
    var top = panel(new BorderLayout());
    top.add(state);
    var restore = button("Janela completa", null);
    restore.addActionListener(e -> full());
    top.add(restore, BorderLayout.EAST);
    body.add(top, BorderLayout.NORTH);
    var lines = panel(new BorderLayout(0, 4));
    lines.add(source, BorderLayout.NORTH);
    text.setEditable(false);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setFont(Appearance.BODY.deriveFont(16f));
    Appearance.tint(text, "ink", "surface");
    lines.add(scroll(text));
    body.add(lines);
    toggle.addActionListener(e -> owner.toggle.doClick());
    finish.addActionListener(e -> owner.finish.doClick());
    body.add(row(toggle, finish), BorderLayout.SOUTH);
    dialog.setContentPane(body);
    dialog.pack();
    owner.compact.addActionListener(e -> compact());
    dialog.setIconImages(Appearance.emblemSizes());
  }

  void installTray() {
    if (!SystemTray.isSupported()) return;
    var menu = new PopupMenu();
    item(menu, "Abrir Bird Teacher", this::full);
    item(menu, "Modo compacto", this::compact);
    menu.addSeparator();
    trayToggle = item(menu, "Iniciar gravação", () -> owner.toggle.doClick());
    trayFinish = item(menu, "Encerrar e revisar", () -> owner.finish.doClick());
    menu.addSeparator();
    item(
        menu,
        "Sair",
        () -> {
          full();
          owner.dispatchEvent(new WindowEvent(owner, WindowEvent.WINDOW_CLOSING));
        });
    tray =
        new TrayIcon(
            new java.awt.image.BaseMultiResolutionImage(
                Appearance.emblemSizes().toArray(Image[]::new)),
            "Bird Teacher",
            menu);
    tray.setImageAutoSize(true);
    tray.addActionListener(e -> SwingUtilities.invokeLater(this::full));
    try {
      SystemTray.getSystemTray().add(tray);
    } catch (AWTException e) {
      tray = null;
    }
  }

  private MenuItem item(PopupMenu menu, String name, Runnable action) {
    var item = new MenuItem(name);
    item.addActionListener(e -> SwingUtilities.invokeLater(action));
    menu.add(item);
    return item;
  }

  private void compact() {
    if (!dialog.isVisible()) {
      var bounds = owner.getGraphicsConfiguration().getBounds();
      var insets = Toolkit.getDefaultToolkit().getScreenInsets(owner.getGraphicsConfiguration());
      dialog.setLocation(
          bounds.x + bounds.width - insets.right - dialog.getWidth() - 18,
          bounds.y + insets.top + 18);
    }
    dialog.setVisible(true);
    owner.setVisible(false);
  }

  void full() {
    dialog.setVisible(false);
    owner.setVisible(true);
    owner.setExtendedState(owner.getExtendedState() & ~Frame.ICONIFIED);
    owner.toFront();
  }

  void sync(String status) {
    state.setText(status);
    toggle.setText(owner.toggle.getText());
    toggle.setEnabled(owner.toggle.isEnabled());
    finish.setEnabled(owner.finish.isEnabled());
    if (tray != null) {
      tray.setToolTip("Bird Teacher · " + status);
      trayToggle.setLabel(toggle.getText());
      trayToggle.setEnabled(toggle.isEnabled());
      trayFinish.setEnabled(finish.isEnabled());
    }
  }

  void line(Transcript last) {
    source.setText(last == null ? "Transcrição" : last.clip().source().label);
    String value =
        last == null
            ? "Aguardando falas…"
            : last.status() == Transcript.Status.QUEUED
                ? "Transcrevendo…"
                : last.text() + (last.status() == Transcript.Status.PREVIEW ? " …" : "");
    text.setText(value);
    text.setCaretPosition(text.getDocument().getLength());
  }

  void close() {
    dialog.dispose();
    if (tray != null) SystemTray.getSystemTray().remove(tray);
  }
}

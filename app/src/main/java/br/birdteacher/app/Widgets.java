package br.birdteacher.app;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

final class Widgets {
    static JPanel panel(LayoutManager layout) {
        var p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    static JPanel row(Component... children) {
        var p = panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (var child : children) p.add(child);
        return p;
    }

    static JLabel label(String text, Font font, float size, String role) {
        var l = new JLabel(text);
        l.setFont(font.deriveFont(size));
        Appearance.tint(l, role, null);
        return l;
    }

    static JButton button(String text, String icon) {
        var b = new JButton(text);
        if (icon != null) b.setIcon(Appearance.icon(icon, 16, b));
        b.setIconTextGap(8);
        b.setMargin(new Insets(6, 11, 6, 11));
        return b;
    }

    static JToggleButton navigationButton(String text, String icon) {
        var button = new JToggleButton(text);
        if (icon != null) button.setIcon(Appearance.icon(icon, 16, button));
        button.setIconTextGap(8);
        button.setMargin(new Insets(6, 11, 6, 11));
        button.putClientProperty("FlatLaf.styleClass", "navigation");
        return button;
    }

    static JPanel card(LayoutManager layout, String fill, int pad) {
        var p =
                new JPanel(layout) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        var g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Appearance.color(fill));
                        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                        g2.setColor(Appearance.color("line"));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                        g2.dispose();
                    }
                };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(pad, pad, pad, pad));
        return p;
    }

    static <T> JList<T> fittedList() {
        return new JList<>() {
            @Override public boolean getScrollableTracksViewportWidth() { return true; }
        };
    }

    static JScrollPane settingsScroll(JComponent content) {
        class Form extends JPanel implements Scrollable {
            Form() {
                super(new BorderLayout());
                setOpaque(false);
                add(content, BorderLayout.NORTH);
            }
            public boolean getScrollableTracksViewportWidth() { return true; }
            public boolean getScrollableTracksViewportHeight() { return false; }
            public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
            public int getScrollableUnitIncrement(Rectangle area, int axis, int direction) { return 16; }
            public int getScrollableBlockIncrement(Rectangle area, int axis, int direction) { return Math.max(16, area.height - 16); }
        }
        return scroll(new Form());
    }

    static JScrollPane scroll(Component c) {
        var s = new JScrollPane(c);
        s.setBorder(BorderFactory.createEmptyBorder());
        Appearance.tint(s.getViewport(), null, "surface");
        return s;
    }
}

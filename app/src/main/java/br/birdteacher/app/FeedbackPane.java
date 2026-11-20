package br.birdteacher.app;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.*;

/** Style the saved plain-text report without changing its content or file format. */
final class FeedbackPane extends JTextPane {
    FeedbackPane() {
        setEditable(false);
        setMargin(new Insets(16, 22, 16, 22));
        Appearance.tint(this, "ink", "surface");
    }

    record Citation(long seconds, String words) {}

    private static final java.util.regex.Pattern CITATION =
            java.util.regex.Pattern.compile("(?m)^(\\d+):(\\d{2})[^\\n]*Você\\s*\\n“([^\\n]+)”");

    Citation citation() {
        var matcher = CITATION.matcher(getText());
        Citation found = null;
        while (matcher.find() && matcher.start() <= getCaretPosition()) {
            found =
                    new Citation(
                            60 * Long.parseLong(matcher.group(1))
                                    + Long.parseLong(matcher.group(2)),
                            matcher.group(3));
        }
        return found;
    }

    void showReport(String report) {
        setText(report);
        StyledDocument doc = getStyledDocument();
        int pos = 0;
        for (String line : report.split("\n", -1)) {
            boolean time = line.matches("\\d+:\\d+.*"), heading = line.equals("Experimente dizer");
            var style = new SimpleAttributeSet();
            StyleConstants.setFontFamily(style, Appearance.BODY.getFamily());
            StyleConstants.setBold(style, time || heading);
            StyleConstants.setFontSize(style, time ? 17 : 15);
            StyleConstants.setForeground(
                    style, Appearance.color(time ? "speaker" : heading ? "other" : "ink"));
            doc.setCharacterAttributes(pos, line.length(), style, true);
            pos += line.length() + 1;
        }
        setCaretPosition(0);
    }
}

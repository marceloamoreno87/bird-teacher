package br.birdteacher.app;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/** The pose communicates the session state even when animation is disabled. */
final class BirdScene extends JComponent {
  private String pose = "pouso";

  BirdScene() {
    setPreferredSize(new Dimension(202, 118));
    setMinimumSize(new Dimension(96, 72));
  }

  void state(boolean listening, boolean working) {
    pose = working ? "mergulho" : listening ? "planeio" : "pouso";
    repaint();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    int w = getWidth(), h = getHeight();
    if (w < 1 || h < 1) return;
    var g = (Graphics2D) graphics.create();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.clip(new RoundRectangle2D.Double(0, 0, w, h, 18, 18));
      g.setColor(Appearance.color("sky"));
      g.fillRect(0, 0, w, h);
      g.scale(w / 202.0, h / 118.0);
      g.setComposite(AlphaComposite.SrcOver.derive(.38f));
      g.setColor(Appearance.color("accent"));
      g.fillOval(133, 9, 41, 41);
      g.setComposite(AlphaComposite.SrcOver.derive(.55f));
      g.setColor(Appearance.color("surface"));
      var ridge = new Path2D.Double();
      ridge.moveTo(0, 98);
      ridge.quadTo(32, 76, 65, 86);
      ridge.curveTo(109, 99, 145, 52, 202, 66);
      ridge.lineTo(202, 118);
      ridge.lineTo(0, 118);
      ridge.closePath();
      g.fill(ridge);
      g.scale(202.0 / w, 118.0 / h);
      g.setComposite(AlphaComposite.SrcOver);
      Image image = Appearance.art("falcao/" + pose);
      double scale = Math.min(w * .91 / image.getWidth(null), h * .91 / image.getHeight(null));
      int width = (int) Math.round(image.getWidth(null) * scale);
      int height = (int) Math.round(image.getHeight(null) * scale);
      g.drawImage(image, (w - width) / 2, (h - height) / 2, width, height, null);
    } finally {
      g.dispose();
    }
  }
}

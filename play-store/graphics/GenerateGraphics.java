import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Draws the Play Store icon (512x512) and feature graphic (1024x500) from the same shapes as the
 * app's launcher icon (res/drawable/ic_launcher_*.xml).
 * Run from this folder:  java GenerateGraphics.java
 */
public class GenerateGraphics {
    static final Color VIOLET = new Color(0x7C3AED);
    static final Color MAGENTA = new Color(0xC026D3);
    static final Color ROSE = new Color(0xF43F5E);
    static final Color AMBER = new Color(0xFFD54F);

    public static void main(String[] args) throws Exception {
        writeIcon(new File("icon-512.png"));
        writeFeatureGraphic(new File("feature-graphic.png"));
        System.out.println("Wrote icon-512.png and feature-graphic.png");
    }

    static Graphics2D smooth(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    static Paint brandGradient(float x0, float y0, float x1, float y1) {
        return new LinearGradientPaint(new Point2D.Float(x0, y0), new Point2D.Float(x1, y1),
                new float[]{0f, 0.55f, 1f}, new Color[]{VIOLET, MAGENTA, ROSE});
    }

    /**
     * The ring chart from ic_launcher_foreground.xml, scaled from its 108-unit canvas so that
     * one unit = [unit] pixels, centred at (cx, cy).
     */
    static void drawRing(Graphics2D g, float cx, float cy, float unit) {
        float r = 22 * unit;
        g.setStroke(new BasicStroke(10 * unit, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        Arc2D.Float box = new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 0, 0, Arc2D.OPEN);
        // Java measures angles anticlockwise from 3 o'clock; the icon goes clockwise from 12 o'clock
        g.setColor(Color.WHITE);
        box.setAngleStart(90);
        box.setAngleExtent(-250);
        g.draw(box);
        g.setColor(AMBER);
        box.setAngleStart(90 - 265);
        box.setAngleExtent(-80);
        g.draw(box);
    }

    static void writeIcon(File out) throws Exception {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(image);
        // Full square: Play rounds the corners itself
        g.setPaint(brandGradient(0, 0, size, size));
        g.fillRect(0, 0, size, size);
        drawRing(g, size / 2f, size / 2f, size / 108f);
        g.dispose();
        ImageIO.write(image, "png", out);
    }

    static void writeFeatureGraphic(File out) throws Exception {
        int w = 1024, h = 500;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = smooth(image);
        g.setPaint(brandGradient(0, 0, w, h));
        g.fillRect(0, 0, w, h);

        // Soft glow circles for depth
        g.setColor(new Color(255, 255, 255, 22));
        g.fillOval(-120, 260, 420, 420);
        g.fillOval(760, -160, 420, 420);

        // Icon tile on the left
        int tile = 240, tx = 90, ty = (h - tile) / 2;
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(new RoundRectangle2D.Float(tx, ty, tile, tile, 64, 64));
        drawRing(g, tx + tile / 2f, ty + tile / 2f, tile / 80f);

        // Text on the right
        int textX = tx + tile + 64;
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 62));
        g.drawString("Expenses Manager", textX, 215);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        g.drawString("Where did your money go?", textX, 280);
        g.setColor(new Color(255, 255, 255, 215));
        g.setFont(new Font("Segoe UI", Font.BOLD, 26));
        g.drawString("TRACK  ·  COMPARE  ·  WRAPPED", textX, 345);
        g.dispose();
        ImageIO.write(image, "png", out);
    }
}

package lk.ac.is7111.client.ui;

import java.awt.*;
import javax.swing.JPanel;
import lk.ac.is7111.client.model.TrafficState.Light;

/** Draws all three lamps. Active color comes only from a backend snapshot. */
public final class SignalPanel extends JPanel {
    private Light active;
    private boolean connected;
    public SignalPanel() {
        setPreferredSize(new Dimension(270, 440));
        setBackground(new Color(20, 31, 48));
        getAccessibleContext().setAccessibleName("Traffic signal: waiting for backend");
    }
    public void display(Light active, boolean connected) {
        this.active = active;
        this.connected = connected;
        getAccessibleContext().setAccessibleName("Traffic signal: " + (connected ? active : "connection lost, last known " + active));
        repaint();
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = (getWidth() - 132) / 2;
        int top = Math.max(46, (getHeight() - 348) / 2);
        g.setColor(new Color(9, 17, 30));
        g.fillRoundRect(x, top, 132, 326, 48, 48);
        Light[] lights = {Light.RED, Light.YELLOW, Light.GREEN};
        Color[] colors = {new Color(255, 91, 102), new Color(255, 199, 70), new Color(53, 218, 154)};
        for (int i = 0; i < lights.length; i++) {
            int y = top + 22 + i * 100;
            boolean on = connected && active == lights[i];
            if (on) {
                g.setColor(new Color(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), 35));
                g.fillOval(x + 12, y - 4, 108, 108);
            }
            g.setColor(on ? colors[i] : new Color(42, 53, 68));
            g.fillOval(x + 20, y + 4, 92, 92);
            g.setColor(on ? new Color(20, 31, 48) : new Color(143, 155, 173));
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String text = lights[i].name();
            g.drawString(text, x + (132 - g.getFontMetrics().stringWidth(text)) / 2, y + 55);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(connected ? new Color(179, 196, 217) : new Color(255, 199, 70));
        String text = connected ? "LIVE SIGNAL" : "SIGNAL UNAVAILABLE";
        g.drawString(text, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2, top + 358);
        g.dispose();
    }
}

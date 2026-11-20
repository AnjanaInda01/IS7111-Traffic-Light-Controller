package lk.ac.is7111.client.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import lk.ac.is7111.client.ClientCoordinator;
import lk.ac.is7111.client.model.TrafficState;
import lk.ac.is7111.client.model.TrafficState.Mode;

public final class TrafficFrame extends JFrame implements ClientCoordinator.View {
    private static final Color INK = new Color(23, 39, 61);
    private static final Color MUTED = new Color(92, 108, 128);
    private final SignalPanel signal = new SignalPanel();
    private final JLabel connection = label("CONNECTING", 12, MUTED);
    private final JLabel countdown = label("--", 72, INK);
    private final JLabel mode = label("WAITING", 15, INK);
    private final JLabel current = label("Waiting for controller", 18, INK);
    private final JLabel message = label("Connecting to the server...", 13, MUTED);
    private final JLabel feedback = label("Actions and errors appear here.", 12, MUTED);
    private final JLabel detail = label("Automatic refresh every second", 12, MUTED);
    private final Map<String, JButton> buttons = new LinkedHashMap<>();
    private Consumer<String> action = ignored -> { };
    private TrafficState last;
    private boolean connected;
    private boolean busy;

    public TrafficFrame(String serverUrl) {
        super("Traffic Light Controller | IS 7111");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(new Color(239, 243, 248));
        root.setBorder(new EmptyBorder(24, 26, 20, 26));
        setContentPane(root);

        JPanel header = transparent(new BorderLayout());
        JPanel titles = transparent(new GridLayout(2, 1, 0, 5));
        titles.add(label("Traffic Light Controller", 27, INK));
        titles.add(label("IS 7111  /  Single-junction signal simulator", 13, MUTED));
        header.add(titles, BorderLayout.CENTER);
        header.add(connection, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = transparent(new BorderLayout(22, 0));
        body.add(signal, BorderLayout.WEST);
        JPanel controls = new JPanel();
        controls.setBackground(Color.WHITE);
        controls.setBorder(new EmptyBorder(24, 26, 24, 26));
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        addRow(controls, label("CONTROLLER STATUS", 11, MUTED), 8);
        JPanel stateRow = transparent(new BorderLayout());
        stateRow.add(current, BorderLayout.WEST);
        stateRow.add(mode, BorderLayout.EAST);
        addRow(controls, stateRow, 0);
        JPanel timeRow = transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        timeRow.add(countdown);
        JLabel unit = label("  seconds remaining", 13, MUTED);
        timeRow.add(unit);
        addRow(controls, timeRow, 0);
        addRow(controls, message, 20);
        addRow(controls, new JSeparator(), 18);
        addRow(controls, label("AUTOMATIC OPERATION", 11, MUTED), 10);
        JPanel auto = transparent(new GridLayout(1, 3, 10, 0));
        auto.add(button("Start", "start", new Color(25, 105, 201), Color.WHITE));
        auto.add(button("Pause", "pause", new Color(230, 237, 247), INK));
        auto.add(button("Resume", "resume", new Color(230, 237, 247), INK));
        addRow(controls, auto, 12);
        JPanel stopReset = transparent(new GridLayout(1, 2, 10, 0));
        stopReset.add(button("Stop", "stop", new Color(230, 237, 247), INK));
        stopReset.add(button("Reset", "reset", new Color(230, 237, 247), INK));
        addRow(controls, stopReset, 20);
        addRow(controls, label("MANUAL SELECTION  /  Stop the cycle first", 11, MUTED), 10);
        JPanel manual = transparent(new GridLayout(1, 3, 10, 0));
        manual.add(button("Red", "manual/RED", new Color(255, 232, 234), new Color(159, 40, 54)));
        manual.add(button("Yellow", "manual/YELLOW", new Color(255, 244, 212), new Color(126, 88, 12)));
        manual.add(button("Green", "manual/GREEN", new Color(219, 247, 237), new Color(16, 112, 77)));
        addRow(controls, manual, 20);
        addRow(controls, button("EMERGENCY  -  Force red", "emergency", new Color(182, 40, 61), Color.WHITE), 8);
        addRow(controls, label("Emergency stays locked until Reset.", 12, MUTED), 0);
        body.add(controls, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel footer = transparent(new GridLayout(3, 1, 0, 5));
        footer.add(feedback);
        footer.add(detail);
        footer.add(label("Server: " + serverUrl + "   |   RED 10s  >  GREEN 8s  >  YELLOW 3s", 12, MUTED));
        root.add(footer, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(980, 790));
        setSize(1040, 820);
        setLocationRelativeTo(null);
        updateButtons();
    }

    public void bind(Consumer<String> action, Runnable close) {
        this.action = action;
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { close.run(); }
        });
    }

    private JButton button(String text, String command, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(background.darker()),
                new EmptyBorder(11, 10, 11, 10)));
        button.setFocusPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(command.equals("reset") ? "Clear manual/emergency state and return to RED / STOPPED / 0" : "Send " + command + " to the backend");
        button.addActionListener(event -> action.accept(command));
        buttons.put(command, button);
        return button;
    }

    @Override public void showState(TrafficState state) {
        boolean recovered = !connected;
        last = state;
        connected = true;
        connection.setText("CONNECTED");
        connection.setForeground(new Color(16, 126, 85));
        current.setText(state.currentLight().name() + " signal");
        mode.setText(state.mode().name());
        mode.setForeground(state.emergency() ? new Color(182, 40, 61) : INK);
        countdown.setText(String.format("%02d", state.remainingSeconds()));
        message.setText(state.message());
        detail.setText(state.emergency() ? "Emergency lock is active. Press Reset to release it." : "Live backend state - refreshed every second");
        signal.display(state.currentLight(), true);
        if (recovered) showFeedback("Connected to controller.", false);
        updateButtons();
    }

    @Override public void showOffline(String text) {
        connected = false;
        connection.setText("DISCONNECTED");
        connection.setForeground(new Color(182, 40, 61));
        countdown.setText("--");
        mode.setText("OFFLINE");
        mode.setForeground(MUTED);
        current.setText(last == null ? "No signal available" : "Last known: " + last.currentLight());
        message.setText("Live signal unavailable");
        detail.setText("Retrying automatically - the displayed state may be stale.");
        signal.display(last == null ? null : last.currentLight(), false);
        showFeedback(text, true);
        updateButtons();
    }

    @Override public void showFeedback(String text, boolean error) {
        feedback.setText(text);
        feedback.setForeground(error ? new Color(182, 40, 61) : new Color(16, 112, 77));
    }
    @Override public void setBusy(boolean busy) { this.busy = busy; updateButtons(); }

    private void updateButtons() {
        boolean ready = connected && !busy && last != null;
        boolean unlocked = ready && !last.emergency();
        for (JButton button : buttons.values()) button.setEnabled(ready);
        buttons.get("start").setEnabled(unlocked);
        buttons.get("pause").setEnabled(unlocked && last.mode() == Mode.AUTOMATIC);
        // Keep Resume available when connected so T4 can demonstrate a backend rejection.
        buttons.get("resume").setEnabled(unlocked);
        boolean manual = unlocked && (last.mode() == Mode.STOPPED || last.mode() == Mode.MANUAL);
        for (String light : new String[]{"RED", "YELLOW", "GREEN"}) buttons.get("manual/" + light).setEnabled(manual);
    }

    private static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setOpaque(false); return panel;
    }
    private static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", size >= 15 || size == 11 ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }
    private static void addRow(JPanel parent, JComponent component, int gap) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        parent.add(component);
        if (gap > 0) parent.add(Box.createVerticalStrut(gap));
    }
}

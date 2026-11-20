package lk.ac.is7111.client;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lk.ac.is7111.client.api.TrafficApi;
import lk.ac.is7111.client.ui.TrafficFrame;

public final class TrafficClientApplication {
    private TrafficClientApplication() { }
    public static void main(String[] args) {
        String url = args.length > 0 ? args[0] : "http://localhost:8080";
        SwingUtilities.invokeLater(() -> {
            try {
                TrafficApi api = new TrafficApi(url);
                TrafficFrame frame = new TrafficFrame(url);
                ClientCoordinator coordinator = new ClientCoordinator(api, frame);
                frame.bind(coordinator::command, coordinator::close);
                frame.setVisible(true);
                coordinator.start();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Invalid server URL", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

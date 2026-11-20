package lk.ac.is7111.client;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import lk.ac.is7111.client.api.TrafficApi;
import lk.ac.is7111.client.model.TrafficState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientCoordinatorTest {
    @Test void slowBackendDoesNotBlockEdtAndOfflineNotificationArrivesOnEdt() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch offline = new CountDownLatch(1);
        AtomicBoolean callbackOnEdt = new AtomicBoolean();
        server.createContext("/api/traffic/status", exchange -> {
            entered.countDown();
            try { release.await(4, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        var view = new ClientCoordinator.View() {
            public void showState(TrafficState state) { }
            public void showOffline(String detail) {
                callbackOnEdt.set(SwingUtilities.isEventDispatchThread());
                offline.countDown();
            }
            public void showFeedback(String text, boolean error) { }
            public void setBusy(boolean busy) { }
        };
        try (var coordinator = new ClientCoordinator(new TrafficApi("http://127.0.0.1:" + server.getAddress().getPort()), view)) {
            SwingUtilities.invokeAndWait(coordinator::start);
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            CountDownLatch uiEvent = new CountDownLatch(1);
            SwingUtilities.invokeLater(uiEvent::countDown);
            assertTrue(uiEvent.await(1, TimeUnit.SECONDS), "Swing must process events while HTTP is waiting");
            assertTrue(offline.await(4, TimeUnit.SECONDS));
            assertTrue(callbackOnEdt.get());
        } finally { release.countDown(); server.stop(0); }
    }
}

package lk.ac.is7111.client;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import lk.ac.is7111.client.api.ApiException;
import lk.ac.is7111.client.api.TrafficApi;
import lk.ac.is7111.client.model.TrafficState;

/** Serializes polls and commands so an older poll cannot overwrite a newer action. */
public final class ClientCoordinator implements AutoCloseable {
    public interface View {
        void showState(TrafficState state);
        void showOffline(String detail);
        void showFeedback(String text, boolean error);
        void setBusy(boolean busy);
    }
    private final TrafficApi api;
    private final View view;
    private final AtomicBoolean pending = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "traffic-http-worker");
        thread.setDaemon(true);
        return thread;
    });

    public ClientCoordinator(TrafficApi api, View view) { this.api = api; this.view = view; }
    public void start() { worker.scheduleAtFixedRate(() -> request(null), 0, 1, TimeUnit.SECONDS); }

    public void command(String action) {
        if (closed.get() || !pending.compareAndSet(false, true)) return;
        onEdt(() -> view.setBusy(true));
        worker.execute(() -> request(action));
    }

    private void request(String action) {
        try {
            TrafficState state = action == null ? api.status() : api.command(action);
            onEdt(() -> {
                view.showState(state);
                if (action != null) view.showFeedback("Command accepted: " + action.replace('/', ' '), false);
            });
        } catch (ApiException ex) {
            onEdt(() -> {
                if (ex.state() != null) view.showState(ex.state());
                view.showFeedback("HTTP " + ex.statusCode() + " - " + ex.getMessage(), true);
            });
        } catch (IOException ex) {
            onEdt(() -> view.showOffline("Cannot reach the controller. Check that the server is running."));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ex) {
            // A malformed response must not cancel future scheduled polls.
            onEdt(() -> view.showOffline("Controller response could not be read. Retrying automatically."));
        } finally {
            if (action != null) {
                pending.set(false);
                onEdt(() -> view.setBusy(false));
            }
        }
    }

    private void onEdt(Runnable task) {
        SwingUtilities.invokeLater(() -> { if (!closed.get()) task.run(); });
    }
    @Override public void close() { closed.set(true); worker.shutdownNow(); }
}

package lk.ac.is7111.client.api;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrafficApiTest {
    private HttpServer server;
    private TrafficApi api;
    private int code = 200;
    private String body = "{\"currentLight\":\"RED\",\"mode\":\"STOPPED\",\"remainingSeconds\":0,\"running\":false,\"emergency\":false,\"message\":\"Ready\"}";
    private final AtomicReference<String> request = new AtomicReference<>();
    @BeforeEach void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/traffic/", exchange -> {
            request.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        api = new TrafficApi("http://127.0.0.1:" + server.getAddress().getPort());
    }
    @AfterEach void tearDown() { server.stop(0); }
    @Test void statusUsesGetAndReadsContract() throws Exception {
        assertEquals("RED", api.status().currentLight().name());
        assertEquals("GET /api/traffic/status", request.get());
    }
    @Test void commandsUsePost() throws Exception {
        api.command("manual/GREEN");
        assertEquals("POST /api/traffic/manual/GREEN", request.get());
    }
    @Test void rejectionCarriesMessageAndState() {
        code = 409;
        body = "{\"message\":\"Pause first\",\"state\":" + body + "}";
        ApiException ex = assertThrows(ApiException.class, () -> api.command("resume"));
        assertEquals(409, ex.statusCode());
        assertEquals("Pause first", ex.getMessage());
        assertEquals("STOPPED", ex.state().mode().name());
    }
    @Test void nonJsonErrorStillExplainsHttpStatus() {
        code = 503; body = "Service unavailable";
        ApiException ex = assertThrows(ApiException.class, api::status);
        assertEquals(503, ex.statusCode());
    }
    @Test void malformedSuccessIsRejected() {
        body = "{}";
        assertThrows(java.io.IOException.class, api::status);
    }
    @Test void unavailableBackendFailsPromptly() {
        server.stop(0);
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(4), () ->
            assertThrows(java.io.IOException.class, api::status));
    }
    @Test void validatesServerOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new TrafficApi("file:///tmp/foo"));
        assertThrows(IllegalArgumentException.class, () -> new TrafficApi("http://localhost:8080/path"));
    }
}

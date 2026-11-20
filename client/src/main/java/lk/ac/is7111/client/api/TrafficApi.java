package lk.ac.is7111.client.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import lk.ac.is7111.client.model.TrafficState;

/** Blocking HTTP operations; the coordinator always invokes these off the Swing EDT. */
public class TrafficApi {
    private final URI base;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final Set<String> COMMANDS = Set.of("start", "pause", "resume", "stop", "reset",
            "emergency", "manual/RED", "manual/YELLOW", "manual/GREEN");

    public TrafficApi(String serverUrl) {
        URI url = URI.create(serverUrl);
        if ((!"http".equals(url.getScheme()) && !"https".equals(url.getScheme())) || url.getHost() == null
                || url.getRawQuery() != null || url.getFragment() != null || url.getUserInfo() != null
                || (url.getPath() != null && !url.getPath().isEmpty() && !url.getPath().equals("/"))) {
            throw new IllegalArgumentException("Use a server origin such as http://localhost:8080");
        }
        base = url.resolve("/api/traffic/");
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    public TrafficState status() throws IOException, InterruptedException { return send("status", false); }
    public TrafficState command(String action) throws IOException, InterruptedException {
        if (!COMMANDS.contains(action)) throw new IllegalArgumentException("Unsupported command");
        return send(action, true);
    }

    private TrafficState send(String path, boolean post) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(2)).header("Accept", "application/json");
        if (post) request.POST(HttpRequest.BodyPublishers.noBody()); else request.GET();
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = "Server rejected request (HTTP " + response.statusCode() + ").";
            TrafficState state = null;
            try {
                JsonNode error = json.readTree(response.body());
                if (error.hasNonNull("message")) message = error.get("message").asText();
                if (error.hasNonNull("state")) state = json.treeToValue(error.get("state"), TrafficState.class);
            } catch (IOException ignored) { /* A proxy may return plain text or HTML. */ }
            throw new ApiException(response.statusCode(), message, state);
        }
        JsonNode body = json.readTree(response.body());
        if (body == null || !body.hasNonNull("currentLight") || !body.hasNonNull("mode")
                || !body.path("remainingSeconds").isIntegralNumber() || !body.path("running").isBoolean()
                || !body.path("emergency").isBoolean() || !body.path("message").isTextual()) {
            throw new IOException("Invalid controller response.");
        }
        TrafficState state = json.treeToValue(body, TrafficState.class);
        if (state.remainingSeconds() < 0) throw new IOException("Invalid countdown in controller response.");
        return state;
    }
}

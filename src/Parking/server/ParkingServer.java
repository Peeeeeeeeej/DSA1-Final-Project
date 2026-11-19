package Parking.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Run this class from IntelliJ, then open http://localhost:8080 in a browser.
// It serves the web/ folder and answers the page's requests using ParkingSystem.
public class ParkingServer {

    private static final int PORT = 8080;
    private static final ParkingSystem system = new ParkingSystem();
    private static Path webDir;

    public static void main(String[] args) throws IOException {
        // optional: pass the web folder's path as the first argument
        webDir = args.length > 0 ? Paths.get(args[0]).toAbsolutePath().normalize() : findWebDir();
        if (webDir == null) {
            System.out.println("Could not find the web/ folder. Run this from the project folder (DSA1-Final-Project).");
            return;
        }

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
        } catch (IOException e) {
            System.out.println("Port " + PORT + " is already in use. Stop the other server (red square in IntelliJ) and run again.");
            return;
        }

        server.createContext("/api/state", ex -> send(ex, 200, "application/json", system.stateJson()));
        server.createContext("/api/version", ex -> send(ex, 200, "application/json", "{\"version\":" + system.getVersion() + "}"));
        server.createContext("/api/park", ex -> {
            Map<String, String> f = form(ex);
            boolean ev = "true".equals(f.get("ev"));
            boolean handicapped = "true".equals(f.get("handicapped"));
            int lot = toInt(f.get("lot"), -1);
            // lot "best" = Park me anywhere
            ParkingSystem.Result r = "best".equals(f.get("lot"))
                    ? system.parkBest(f.get("type"), f.get("plate"), f.get("designation"), ev, handicapped)
                    : system.park(lot, f.get("type"), f.get("plate"), f.get("designation"), ev, handicapped,
                                  f.getOrDefault("mode", ""));
            send(ex, 200, "application/json", ParkingSystem.resultJson(r));
        });
        server.createContext("/api/undo", ex -> send(ex, 200, "application/json", ParkingSystem.resultJson(system.undo())));
        server.createContext("/api/dev/leave", ex -> {
            int n = system.devLeave(clamp(toInt(form(ex).get("count"), 0)));
            send(ex, 200, "application/json", "{\"left\":" + n + "}");
        });
        server.createContext("/api/leave", ex ->
                send(ex, 200, "application/json", ParkingSystem.resultJson(system.leave(form(ex).get("plate")))));
        server.createContext("/api/search", ex ->
                send(ex, 200, "application/json", ParkingSystem.resultJson(system.search(form(ex).get("plate")))));
        server.createContext("/api/dev/add", ex -> {
            Map<String, String> f = form(ex);
            Map<String, Integer> forced = new LinkedHashMap<>();
            for (String d : new String[]{"student", "faculty", "visitor", "service"}) {
                forced.put(d, clamp(toInt(f.get(d), 0)));
            }
            int[] mix = {clamp(toInt(f.get("car"), 75)), clamp(toInt(f.get("truck"), 15)), clamp(toInt(f.get("motorcycle"), 10))};
            int[] res = system.devAdd(clamp(toInt(f.get("total"), 0)), forced, clamp(toInt(f.get("handicapped"), 0)), mix);
            send(ex, 200, "application/json",
                    "{\"added\":" + res[0] + ",\"parked\":" + res[1] + ",\"turnedAway\":" + res[2] + "}");
        });
        server.createContext("/api/dev/reset", ex -> {
            system.reset();
            send(ex, 200, "application/json", "{\"status\":\"reset\"}");
        });
        server.createContext("/", ParkingServer::serveFile);

        server.start();
        System.out.println("Parking website running. Open http://localhost:" + PORT + " in your browser.");
        System.out.println("Serving files from " + webDir);
    }

    // the web/ folder is next to src/ in the project
    private static Path findWebDir() {
        Path here = Paths.get("").toAbsolutePath();
        for (Path p = here; p != null; p = p.getParent()) {
            Path candidate = p.resolve("web");
            if (Files.isRegularFile(candidate.resolve("index.html"))) {
                return candidate;
            }
        }
        return null;
    }

    private static void serveFile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = webDir.resolve(path.substring(1)).normalize();
        if (!file.startsWith(webDir) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain", "Not found");
            return;
        }
        String type = "text/plain";
        String name = file.getFileName().toString();
        if (name.endsWith(".html")) type = "text/html";
        else if (name.endsWith(".css")) type = "text/css";
        else if (name.endsWith(".js")) type = "application/javascript";
        else if (name.endsWith(".svg")) type = "image/svg+xml";
        sendBytes(ex, 200, type, Files.readAllBytes(file));
    }

    // reads parameters from the URL query and (for POST) the form body
    private static Map<String, String> form(HttpExchange ex) throws IOException {
        Map<String, String> out = new HashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        if (query != null) parse(query, out);
        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
            parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), out);
        }
        return out;
    }

    private static void parse(String data, Map<String, String> out) {
        for (String pair : data.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            out.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
    }

    private static int toInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int clamp(int n) {
        return Math.max(0, Math.min(n, 1000));
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        sendBytes(ex, code, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendBytes(HttpExchange ex, int code, String type, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}

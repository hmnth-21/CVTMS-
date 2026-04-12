package com.cvtms.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.cvtms.dao.VehicleDAO;
import com.cvtms.model.EntryLog;
import com.cvtms.model.Role;
import com.cvtms.model.User;
import com.cvtms.model.Vehicle;
import com.cvtms.model.VehicleType;
import com.cvtms.service.AuthService;
import com.cvtms.service.IncidentService;
import com.cvtms.service.ReportingService;
import com.cvtms.service.TrafficService;
import com.cvtms.service.VehicleService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Lightweight REST API server using Java's built-in HttpServer.
 * Acts as a bridge between the HTML/JS frontend and existing service layer.
 * Does NOT duplicate any business logic — reuses existing services.
 */
public class ApiServer {

    private HttpServer server;
    private VehicleService vehicleService;
    private TrafficService trafficService;
    private IncidentService incidentService;
    private ReportingService reportingService;
    private VehicleDAO vehicleDAO;

    public ApiServer(int port) throws IOException {
        this.vehicleService = new VehicleService();
        this.trafficService = new TrafficService(vehicleService);
        this.incidentService = new IncidentService();
        this.reportingService = new ReportingService();
        this.vehicleDAO = new VehicleDAO();

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register endpoints
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/vehicles/inside", new VehiclesInsideHandler());
        server.createContext("/api/entry", new EntryHandler());
        server.createContext("/api/exit", new ExitHandler());
        server.createContext("/api/vehicle/register", new VehicleRegisterHandler());

        server.setExecutor(null); // default executor
    }

    public void start() {
        server.start();
        System.out.println("[API] Server running on http://localhost:" + server.getAddress().getPort());
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /** Add CORS headers to every response. */
    private static void addCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
    }

    /** Handle CORS preflight (OPTIONS). Returns true if it was a preflight request. */
    private static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    /** Send a JSON response with the given status code. */
    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /** Read the full request body as a string. */
    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Extract a string value from a simple flat JSON object.
     * Works for: {"key": "value", ...}
     * No external library needed.
     */
    static String jsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) return null;

        int valueStart = colonIndex + 1;
        // Skip whitespace
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) return null;

        if (json.charAt(valueStart) == '"') {
            // String value — find closing quote
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd == -1) return null;
            return json.substring(valueStart + 1, valueEnd);
        } else {
            // Non-string value (number, boolean, null)
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    /** Escape a string for use inside a JSON value. */
    static String esc(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // =========================================================================
    // Endpoint Handlers
    // =========================================================================

    /**
     * POST /api/login
     * Body: {"username": "...", "password": "..."}
     * Response: {"success": true, "username": "...", "role": "ADMIN|SECURITY"}
     *       or: {"success": false, "message": "..."}
     */
    class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            String username = jsonValue(body, "username");
            String password = jsonValue(body, "password");

            if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Username and password are required\"}");
                return;
            }

            // Create a fresh AuthService per request to avoid shared mutable state
            AuthService auth = new AuthService();
            boolean success = auth.login(username, password);

            if (success) {
                User user = auth.getCurrentUser();
                String json = "{\"success\":true,"
                    + "\"username\":\"" + esc(user.getUsername()) + "\","
                    + "\"role\":\"" + user.getRole().name() + "\"}";
                sendJson(exchange, 200, json);
            } else {
                sendJson(exchange, 401, "{\"success\":false,\"message\":\"Invalid username or password\"}");
            }
        }
    }

    /**
     * GET /api/vehicles/inside
     * Returns the list of vehicles currently inside campus AND the count.
     * Count is derived from the same list (single query) to guarantee consistency.
     * Response: {"success":true, "count":N, "data":[{"regNumber":"...","entryTime":"...","gate":"...","type":"..."}]}
     */
    class VehiclesInsideHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            List<EntryLog> inside = trafficService.viewVehiclesCurrentlyInside();

            StringBuilder data = new StringBuilder("[");
            for (int i = 0; i < inside.size(); i++) {
                EntryLog log = inside.get(i);
                Vehicle vehicle = vehicleDAO.findVehicleById(log.getVehicleId());
                String regNumber = (vehicle != null) ? vehicle.getRegistrationNumber() : "ID-" + log.getVehicleId();
                String type = (vehicle != null) ? vehicle.getType().name() : "UNKNOWN";
                String entryTime = log.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                if (i > 0) data.append(",");
                data.append("{")
                    .append("\"regNumber\":\"").append(esc(regNumber)).append("\",")
                    .append("\"entryTime\":\"").append(entryTime).append("\",")
                    .append("\"gate\":\"").append(esc(log.getGate())).append("\",")
                    .append("\"type\":\"").append(type).append("\"")
                    .append("}");
            }
            data.append("]");

            String json = "{\"success\":true,\"count\":" + inside.size() + ",\"data\":" + data.toString() + "}";
            sendJson(exchange, 200, json);
        }
    }

    /**
     * POST /api/register
     * Body: {"username": "...", "password": "..."}
     * Registers a new SECURITY user. Reuses AuthService.register() which
     * handles BCrypt hashing, duplicate checking, and input normalization.
     * Response: {"success": true, "message": "..."}
     *       or: {"success": false, "message": "..."}
     */
    class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            String username = jsonValue(body, "username");
            String password = jsonValue(body, "password");

            if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Username and password are required\"}");
                return;
            }

            // Reuse existing AuthService.register() — handles hashing + normalization
            AuthService auth = new AuthService();
            boolean success = auth.register(username, password, Role.SECURITY);

            if (success) {
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Security user registered successfully\"}");
            } else {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Registration failed. Username may already exist.\"}");
            }
        }
    }

    /**
     * POST /api/entry
     * Body: {"regNumber": "...", "gate": "...", "purpose": "..."}
     */
    class EntryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            String regNumber = jsonValue(body, "regNumber");
            String gate = jsonValue(body, "gate");
            String purpose = jsonValue(body, "purpose");

            if (regNumber == null || gate == null || regNumber.trim().isEmpty() || gate.trim().isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Registration number and gate are required\"}");
                return;
            }

            boolean success = trafficService.recordEntry(regNumber.trim().toUpperCase(), gate.trim(), purpose != null ? purpose.trim() : "");
            
            if (success) {
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Vehicle entry recorded.\"}");
            } else {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Entry failed. Vehicle may not exist or is already inside.\"}");
            }
        }
    }

    /**
     * POST /api/exit
     * Body: {"regNumber": "...", "justification": "..."}
     */
    class ExitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            String regNumber = jsonValue(body, "regNumber");
            String justification = jsonValue(body, "justification");

            if (regNumber == null || regNumber.trim().isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Registration number is required\"}");
                return;
            }

            boolean success = trafficService.recordExit(
                regNumber.trim().toUpperCase(),
                justification != null ? justification.trim() : ""
            );

            if (success) {
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Vehicle exit recorded.\"}");
            } else {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Exit failed. Vehicle may not be inside, may not exist, or overstay justification is required.\"}");
            }
        }
    }

    /**
     * POST /api/vehicle/register
     * Body: {"regNumber": "...", "ownerName": "...", "ownerContact": "...", "type": "..."}
     */
    class VehicleRegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            String regNumber = jsonValue(body, "regNumber");
            String ownerName = jsonValue(body, "ownerName");
            String ownerContact = jsonValue(body, "ownerContact");
            String typeStr = jsonValue(body, "type");

            if (regNumber == null || regNumber.trim().isEmpty() || typeStr == null || typeStr.trim().isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Registration number and type are required\"}");
                return;
            }

            VehicleType type;
            try {
                type = VehicleType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Invalid vehicle type\"}");
                return;
            }

            boolean success = vehicleService.registerVehicle(
                regNumber.trim().toUpperCase(),
                ownerName != null ? ownerName.trim() : "",
                ownerContact != null ? ownerContact.trim() : "",
                type
            );

            if (success) {
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Vehicle registered successfully\"}");
            } else {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Failed to register vehicle. It may already exist.\"}");
            }
        }
    }
}

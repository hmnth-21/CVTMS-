package com.cvtms.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cvtms.dao.DatabaseConnectionManager;
import com.cvtms.dao.LogDAO;
import com.cvtms.dao.UserDAO;
import com.cvtms.dao.VehicleDAO;
import com.cvtms.model.EntryLog;
import com.cvtms.model.Incident;
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
    private LogDAO logDAO;
    private UserDAO userDAO;

    public ApiServer(int port) throws IOException {
        this.vehicleService = new VehicleService();
        this.trafficService = new TrafficService(vehicleService);
        this.incidentService = new IncidentService();
        this.reportingService = new ReportingService();
        this.vehicleDAO = new VehicleDAO();
        this.logDAO = new LogDAO();
        this.userDAO = new UserDAO();

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register endpoints
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/vehicles/inside", new VehiclesInsideHandler());
        server.createContext("/api/entry", new EntryHandler());
        server.createContext("/api/exit", new ExitHandler());
        server.createContext("/api/vehicle/register", new VehicleRegisterHandler());
        server.createContext("/api/incidents", new IncidentsHandler());
        server.createContext("/api/vehicle/search", new VehicleSearchHandler());
        server.createContext("/api/vehicle/movement", new VehicleMovementHandler());
        server.createContext("/api/logs/search", new SearchLogsHandler());
        server.createContext("/api/dashboard/summary", new DashboardSummaryHandler());
        server.createContext("/api/users", new UsersHandler());

        server.setExecutor(null); // default executor
    }

    public void start() {
        server.start();
        System.out.println("[API] Server running on http://localhost:" + server.getAddress().getPort());
    }

    public static void main(String[] args) throws IOException {
        DatabaseConnectionManager.initializeDatabase();
        ApiServer apiServer = new ApiServer(8080);
        apiServer.start();
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

    /** Decode query parameters from the request URI. */
    static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return params;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isEmpty()) continue;

            String[] kv = pair.split("=", 2);
            String key = decodeUrl(kv[0]);
            String value = kv.length > 1 ? decodeUrl(kv[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private static String decodeUrl(String value) {
        if (value == null) return "";
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "";
        return timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
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
     * GET /api/users
     * Returns only username and role for admin visibility.
     */
    class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            List<User> users = userDAO.getAllUsers();
            StringBuilder data = new StringBuilder("[");

            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                if (i > 0) data.append(",");
                data.append("{")
                    .append("\"username\":\"").append(esc(user.getUsername())).append("\",")
                    .append("\"role\":\"").append(user.getRole().name()).append("\"")
                    .append("}");
            }
            data.append("]");

            String json = "{\"success\":true,\"count\":" + users.size() + ",\"data\":" + data.toString() + "}";
            sendJson(exchange, 200, json);
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

            String normalizedReg = regNumber.trim().toUpperCase();

            Vehicle vehicle = vehicleService.getVehicle(normalizedReg);
            if (vehicle == null) {
                sendJson(exchange, 404, "{\"success\":false,\"message\":\"Entry failed. Vehicle is not registered. Please register it first.\"}");
                return;
            }

            EntryLog activeLog = logDAO.findActiveEntryLogForVehicle(vehicle.getId());
            if (activeLog != null) {
                String activeEntryTime = activeLog.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String activeGate = activeLog.getGate() != null ? activeLog.getGate() : "Unknown Gate";
                sendJson(exchange, 409,
                    "{\"success\":false,\"message\":\"Entry failed. Vehicle is already inside (entered at "
                    + activeEntryTime + " via " + esc(activeGate)
                    + "). Record exit first.\"}");
                return;
            }

            boolean success = trafficService.recordEntry(normalizedReg, gate.trim(), purpose != null ? purpose.trim() : "");
            
            if (success) {
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Vehicle entry recorded.\"}");
            } else {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Entry failed due to an unexpected validation/database issue.\"}");
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
     * GET /api/incidents
     * POST /api/incidents
     */
    class IncidentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                List<Incident> incidents = incidentService.getAllIncidents();
                StringBuilder data = new StringBuilder("[");

                for (int i = 0; i < incidents.size(); i++) {
                    Incident incident = incidents.get(i);
                    Vehicle vehicle = vehicleDAO.findVehicleById(incident.getVehicleId());
                    String regNumber = (vehicle != null) ? vehicle.getRegistrationNumber() : "ID-" + incident.getVehicleId();

                    if (i > 0) data.append(",");
                    data.append("{")
                        .append("\"date\":\"").append(esc(formatTimestamp(incident.getTimestamp()))).append("\",")
                        .append("\"id\":\"").append(esc(regNumber)).append("\",")
                        .append("\"severity\":\"").append(esc(incident.getSeverity())).append("\",")
                        .append("\"desc\":\"").append(esc(incident.getDescription())).append("\"")
                        .append("}");
                }
                data.append("]");

                String json = "{\"success\":true,\"count\":" + incidents.size() + ",\"data\":" + data.toString() + "}";
                sendJson(exchange, 200, json);
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(exchange);
                String regNumber = jsonValue(body, "regNumber");
                String severity = jsonValue(body, "severity");
                String description = jsonValue(body, "description");

                if (regNumber == null || regNumber.trim().isEmpty()
                    || severity == null || severity.trim().isEmpty()
                    || description == null || description.trim().isEmpty()) {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Registration number, severity, and description are required\"}");
                    return;
                }

                boolean success = incidentService.recordIncident(
                    regNumber.trim().toUpperCase(),
                    description.trim(),
                    severity.trim().toUpperCase()
                );

                if (success) {
                    sendJson(exchange, 200, "{\"success\":true,\"message\":\"Incident recorded successfully\"}");
                } else {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Failed to record incident. Vehicle may not be registered.\"}");
                }
                return;
            }

            sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
        }
    }

    /**
     * GET /api/vehicle/search?regNumber=...
     */
    class VehicleSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = queryParams(exchange);
            String regNumber = params.get("regNumber");

            if (regNumber == null || regNumber.trim().isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"regNumber query parameter is required\"}");
                return;
            }

            String normalizedReg = regNumber.trim().toUpperCase();
            Vehicle vehicle = vehicleService.getVehicle(normalizedReg);
            if (vehicle == null) {
                sendJson(exchange, 404, "{\"success\":false,\"message\":\"Vehicle not found\"}");
                return;
            }

            EntryLog activeInsideLog = null;
            List<EntryLog> inside = trafficService.viewVehiclesCurrentlyInside();
            for (EntryLog log : inside) {
                if (log.getVehicleId() == vehicle.getId()) {
                    activeInsideLog = log;
                    break;
                }
            }

            if (activeInsideLog != null) {
                String json = "{\"success\":true,"
                    + "\"regNumber\":\"" + esc(vehicle.getRegistrationNumber()) + "\"," 
                    + "\"type\":\"" + vehicle.getType().name() + "\"," 
                    + "\"status\":\"INSIDE\"," 
                    + "\"entryTime\":\"" + activeInsideLog.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\"," 
                    + "\"gate\":\"" + esc(activeInsideLog.getGate()) + "\"}";
                sendJson(exchange, 200, json);
                return;
            }

            List<EntryLog> history = trafficService.getVehicleMovementHistory(normalizedReg);
            EntryLog latest = (history != null && !history.isEmpty()) ? history.get(0) : null;
            String status = (latest == null || latest.getExitTime() != null) ? "LEFT" : "INSIDE";
            String lastEntry = latest != null
                ? latest.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "";
            String lastExit = (latest != null && latest.getExitTime() != null)
                ? latest.getExitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "";

            String json = "{\"success\":true,"
                + "\"regNumber\":\"" + esc(vehicle.getRegistrationNumber()) + "\"," 
                + "\"type\":\"" + vehicle.getType().name() + "\"," 
                + "\"status\":\"" + status + "\"," 
                + "\"entryTime\":\"" + lastEntry + "\"," 
                + "\"exitTime\":\"" + lastExit + "\"}";
            sendJson(exchange, 200, json);
        }
    }

    /**
     * GET /api/vehicle/movement?regNumber=...
     */
    class VehicleMovementHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = queryParams(exchange);
            String regNumber = params.get("regNumber");

            if (regNumber == null || regNumber.trim().isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"regNumber query parameter is required\"}");
                return;
            }

            String normalizedReg = regNumber.trim().toUpperCase();
            List<EntryLog> history = trafficService.getVehicleMovementHistory(normalizedReg);
            if (history == null) {
                sendJson(exchange, 404, "{\"success\":false,\"message\":\"Vehicle not found\"}");
                return;
            }

            Vehicle vehicle = vehicleService.getVehicle(normalizedReg);
            String type = (vehicle != null) ? vehicle.getType().name() : "UNKNOWN";

            StringBuilder data = new StringBuilder("[");
            for (int i = 0; i < history.size(); i++) {
                EntryLog log = history.get(i);
                String entryTime = log.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String exitTime = (log.getExitTime() != null)
                    ? log.getExitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "";
                String status = (log.getExitTime() == null) ? "INSIDE" : "LEFT";

                if (i > 0) data.append(",");
                data.append("{")
                    .append("\"regNumber\":\"").append(esc(normalizedReg)).append("\",")
                    .append("\"entryTime\":\"").append(entryTime).append("\",")
                    .append("\"gate\":\"").append(esc(log.getGate())).append("\",")
                    .append("\"exitTime\":\"").append(exitTime).append("\",")
                    .append("\"status\":\"").append(status).append("\",")
                    .append("\"type\":\"").append(type).append("\"")
                    .append("}");
            }
            data.append("]");

            String json = "{\"success\":true,\"count\":" + history.size() + ",\"data\":" + data.toString() + "}";
            sendJson(exchange, 200, json);
        }
    }

    /**
     * GET /api/logs/search?regNumber=...&dateFrom=...&dateTo=...
     */
    class SearchLogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = queryParams(exchange);
            String regNumber = params.get("regNumber");
            String dateFrom = params.get("dateFrom");
            String dateTo = params.get("dateTo");

            String normalizedReg = (regNumber == null || regNumber.trim().isEmpty()) ? null : regNumber.trim().toUpperCase();
            String normalizedDateFrom = (dateFrom == null || dateFrom.trim().isEmpty()) ? null : dateFrom.trim().replace("T", " ");
            String normalizedDateTo = (dateTo == null || dateTo.trim().isEmpty()) ? null : dateTo.trim().replace("T", " ");

            List<EntryLog> logs = trafficService.searchTrafficLogs(normalizedReg, normalizedDateFrom, normalizedDateTo);

            StringBuilder data = new StringBuilder("[");
            for (int i = 0; i < logs.size(); i++) {
                EntryLog log = logs.get(i);
                Vehicle vehicle = vehicleDAO.findVehicleById(log.getVehicleId());
                String reg = (vehicle != null) ? vehicle.getRegistrationNumber() : "ID-" + log.getVehicleId();
                String entryTime = log.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String exitTime = (log.getExitTime() != null)
                    ? log.getExitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "";
                String status = (log.getExitTime() == null) ? "INSIDE" : "LEFT";

                if (i > 0) data.append(",");
                data.append("{")
                    .append("\"regNumber\":\"").append(esc(reg)).append("\",")
                    .append("\"entryTime\":\"").append(entryTime).append("\",")
                    .append("\"gate\":\"").append(esc(log.getGate())).append("\",")
                    .append("\"exitTime\":\"").append(exitTime).append("\",")
                    .append("\"status\":\"").append(status).append("\"")
                    .append("}");
            }
            data.append("]");

            String json = "{\"success\":true,\"count\":" + logs.size() + ",\"data\":" + data.toString() + "}";
            sendJson(exchange, 200, json);
        }
    }

    /**
     * GET /api/dashboard/summary
     */
    class DashboardSummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, Integer> daily = reportingService.getDailyStatistics();
            int entriesToday = daily.getOrDefault("entries", 0);
            int exitsToday = daily.getOrDefault("exits", 0);
            int overstayCount = reportingService.getOverstayCount();
            int currentlyInside = trafficService.viewVehiclesCurrentlyInside().size();

            Map<VehicleType, Integer> distribution = reportingService.getVehicleTypeDistribution();
            StringBuilder dist = new StringBuilder("{");
            VehicleType[] types = VehicleType.values();
            for (int i = 0; i < types.length; i++) {
                if (i > 0) dist.append(",");
                int count = distribution.getOrDefault(types[i], 0);
                dist.append("\"").append(types[i].name()).append("\":").append(count);
            }
            dist.append("}");

            String json = "{\"success\":true,"
                + "\"entriesToday\":" + entriesToday + ","
                + "\"exitsToday\":" + exitsToday + ","
                + "\"currentlyInside\":" + currentlyInside + ","
                + "\"overstayCount\":" + overstayCount + ","
                + "\"distribution\":" + dist.toString()
                + "}";
            sendJson(exchange, 200, json);
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

            String normalizedReg = regNumber.trim().toUpperCase();

            VehicleType type;
            try {
                type = VehicleType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Invalid vehicle type\"}");
                return;
            }

            Vehicle existing = vehicleService.getVehicle(normalizedReg);
            if (existing != null) {
                sendJson(exchange, 409, "{\"success\":false,\"message\":\"Vehicle is already registered\"}");
                return;
            }

            boolean success = vehicleService.registerVehicle(
                normalizedReg,
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

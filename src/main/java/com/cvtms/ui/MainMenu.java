package com.cvtms.ui;

import com.cvtms.dao.DatabaseConnectionManager;
import com.cvtms.model.EntryLog;
import com.cvtms.model.Role;
import com.cvtms.model.User;
import com.cvtms.model.VehicleType;
import com.cvtms.service.AuthService;
import com.cvtms.service.IncidentService;
import com.cvtms.service.ReportingService;
import com.cvtms.service.TrafficService;
import com.cvtms.service.VehicleService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainMenu {
    private Scanner scanner;
    private AuthService authService;
    private VehicleService vehicleService;
    private TrafficService trafficService;
    private IncidentService incidentService;
    private ReportingService reportingService;

    public MainMenu() {
        this.scanner = new Scanner(System.in);
        this.authService = new AuthService();
        this.vehicleService = new VehicleService();
        this.trafficService = new TrafficService(vehicleService);
        this.incidentService = new IncidentService();
        this.reportingService = new ReportingService();
    }

    public void start() {
        System.out.println("=========================================");
        System.out.println("  Campus Vehicular Traffic Management  ");
        System.out.println("=========================================");
        DatabaseConnectionManager.initializeDatabase();

        while (true) {
            if (!authService.isAuthenticated()) {
                showLoginMenu();
            } else {
                User currentUser = authService.getCurrentUser();
                if (currentUser.getRole() == Role.ADMIN) {
                    showAdminMenu();
                } else if (currentUser.getRole() == Role.SECURITY) {
                    showSecurityMenu();
                }
            }
        }
    }

    private void showLoginMenu() {
        System.out.println("\n=========================================");
        System.out.println("                LOGIN                    ");
        System.out.println("=========================================");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (authService.login(username, password)) {
            System.out.println("\n>>> Login successful! Welcome, " + username);
        } else {
            System.out.println("\n>>> Login failed. Please check your credentials.");
        }
        System.out.println("=========================================");
    }

    private void showAdminMenu() {
        System.out.println("\n=========================================");
        System.out.println("              ADMIN MENU                 ");
        System.out.println("=========================================");
        System.out.println("1. Register Security Personnel");
        System.out.println("2. View All Incidents");
        System.out.println("3. View Statistics and Reports");
        System.out.println("4. Logout");
        System.out.println("5. Exit System");
        System.out.println("-----------------------------------------");
        System.out.print("Select Option: ");
        
        String option = scanner.nextLine();
        switch (option) {
            case "1":
                System.out.print("New Username: ");
                String nUser = scanner.nextLine();
                System.out.print("New Password: ");
                String nPass = scanner.nextLine();
                if (authService.register(nUser, nPass, Role.SECURITY)) {
                    System.out.println("Security user registered successfully.");
                } else {
                    System.out.println("Failed to register.");
                }
                break;
            case "2":
                viewAllIncidents();
                break;
            case "3":
                viewStatistics();
                break;
            case "4":
                authService.logout();
                break;
            case "5":
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private void showSecurityMenu() {
        System.out.println("\n=========================================");
        System.out.println("            SECURITY MENU                ");
        System.out.println("=========================================");
        System.out.println("1. Register Vehicle");
        System.out.println("2. Record Vehicle Entry");
        System.out.println("3. Record Vehicle Exit");
        System.out.println("4. View Vehicles Currently Inside");
        System.out.println("5. Search Vehicle");
        System.out.println("6. Record Incident");
        System.out.println("7. View Vehicle Incidents");
        System.out.println("8. View Vehicle Movement History");
        System.out.println("9. Extended Traffic Log Search");
        System.out.println("10. Logout");
        System.out.println("11. Exit System");
        System.out.println("-----------------------------------------");
        System.out.print("Select Option: ");

        String option = scanner.nextLine();
        switch (option) {
            case "1":
                registerVehicle();
                break;
            case "2":
                recordEntry();
                break;
            case "3":
                recordExit();
                break;
            case "4":
                viewCurrentlyInside();
                break;
            case "5":
                searchVehicle();
                break;
            case "6":
                recordIncident();
                break;
            case "7":
                viewVehicleIncidents();
                break;
            case "8":
                viewMovementHistory();
                break;
            case "9":
                extendedSearch();
                break;
            case "10":
                authService.logout();
                break;
            case "11":
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private void registerVehicle() {
        System.out.println("\n--- Register New Vehicle ---");
        String regNumber = readNonEmptyString("Registration Number: ");
        System.out.println("Vehicle Types: CAMPUS, CAB, DELIVERY, WORK, EXTERNAL");
        System.out.print("Type: ");
        String typeStr = scanner.nextLine().toUpperCase();
        
        VehicleType type;
        try {
            type = VehicleType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            System.out.println(">>> Invalid Type.");
            return;
        }

        System.out.print("Owner Name (optional, press enter to skip): ");
        String ownerName = scanner.nextLine();
        String ownerContact = "";
        if (!ownerName.isEmpty()) {
            System.out.print("Owner Contact: ");
            ownerContact = scanner.nextLine();
        }

        if (vehicleService.registerVehicle(regNumber, ownerName, ownerContact, type)) {
            System.out.println(">>> Vehicle registered successfully.");
        }
        System.out.println("-----------------------------");
    }

    private void recordEntry() {
        System.out.println("\n--- Record Vehicle Entry ---");
        String regNumber = readNonEmptyString("Registration Number: ");
        String gate = readNonEmptyString("Gate: ");
        
        System.out.print("Purpose of visit (required only if EXTERNAL): ");
        String purpose = scanner.nextLine();
        
        trafficService.recordEntry(regNumber, gate, purpose);
        System.out.println("-----------------------------");
    }

    private void recordExit() {
        System.out.println("\n--- Record Vehicle Exit ---");
        String regNumber = readNonEmptyString("Registration Number: ");
        
        boolean success = trafficService.recordExit(regNumber, "");
        if (!success) {
            System.out.print("If overstay was detected, enter justification here: ");
            String justification = scanner.nextLine();
            if(!justification.trim().isEmpty()){
                trafficService.recordExit(regNumber, justification);
            }
        }
        System.out.println("-----------------------------");
    }

    private void viewCurrentlyInside() {
        List<EntryLog> inside = trafficService.viewVehiclesCurrentlyInside();
        System.out.println("\n--- Vehicles Currently Inside ---");
        if(inside.isEmpty()){
            System.out.println("No vehicles inside at the moment.");
            return;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (EntryLog log : inside) {
            System.out.println("Vehicle ID: " + log.getVehicleId() + " | Entry Time: " + log.getEntryTime().format(fmt) + " | Gate: " + log.getGate());
        }
        System.out.println("---------------------------------");
    }

    private void searchVehicle() {
        String regNumber = readNonEmptyString("Enter Registration Number: ");
        
        com.cvtms.model.Vehicle vehicle = vehicleService.getVehicle(regNumber);
        if (vehicle == null) {
            System.out.println("Vehicle not found.");
            return;
        }
        
        System.out.println("\n--- Vehicle Details ---");
        System.out.println("Registration: " + vehicle.getRegistrationNumber());
        System.out.println("Type: " + vehicle.getType());
        
        if (vehicle.getOwnerId() > 0) {
            com.cvtms.model.VehicleOwner owner = vehicleService.getVehicleOwner(vehicle.getOwnerId());
            if (owner != null) {
                System.out.println("Owner: " + owner.getName() + " | Contact: " + owner.getContact());
            }
        }
        System.out.println("---------------------------------");
    }

    private void recordIncident() {
        String regNumber = readNonEmptyString("Registration Number: ");
        String description = readNonEmptyString("Incident Description: ");
        String severity = readNonEmptyString("Severity (LOW, MEDIUM, HIGH): ").toUpperCase();

        if (incidentService.recordIncident(regNumber, description, severity)) {
            System.out.println("Incident recorded successfully.");
        } else {
            System.out.println("Failed to record incident.");
        }
    }

    private void viewVehicleIncidents() {
        String regNumber = readNonEmptyString("Registration Number: ");
        List<com.cvtms.model.Incident> incidents = incidentService.getVehicleIncidents(regNumber);
        
        if (incidents == null) {
            System.out.println("Vehicle not found.");
            return;
        }
        
        System.out.println("\n--- Incidents for " + regNumber + " ---");
        if (incidents.isEmpty()) {
            System.out.println("No incidents recorded for this vehicle.");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (com.cvtms.model.Incident incident : incidents) {
                System.out.println("[" + incident.getTimestamp().toLocalDateTime().format(fmt) + "] " + 
                                   "Severity: " + incident.getSeverity() + " | Description: " + incident.getDescription());
            }
        }
        System.out.println("---------------------------------");
    }

    private void viewAllIncidents() {
        List<com.cvtms.model.Incident> incidents = incidentService.getAllIncidents();
        System.out.println("\n--- All Recorded Incidents ---");
        if (incidents.isEmpty()) {
            System.out.println("No incidents recorded.");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (com.cvtms.model.Incident incident : incidents) {
                System.out.println("[" + incident.getTimestamp().toLocalDateTime().format(fmt) + "] " + 
                                   "Vehicle ID: " + incident.getVehicleId() + " | Severity: " + incident.getSeverity() + " | Description: " + incident.getDescription());
            }
        }
        System.out.println("---------------------------------");
    }

    private void viewStatistics() {
        System.out.println("\n--- Statistics and Reports ---");
        
        Map<String, Integer> dailyStats = reportingService.getDailyStatistics();
        System.out.println("Daily Stats (Today):");
        System.out.println("  Total Entries: " + dailyStats.getOrDefault("entries", 0));
        System.out.println("  Total Exits: " + dailyStats.getOrDefault("exits", 0));
        
        System.out.println("\nVehicle Type Distribution (Currently Inside):");
        Map<VehicleType, Integer> distribution = reportingService.getVehicleTypeDistribution();
        if (distribution.isEmpty()) {
            System.out.println("  No vehicles inside.");
        } else {
            for (Map.Entry<VehicleType, Integer> entry : distribution.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        System.out.println("\nOverstay Summary:");
        System.out.println("  Total Overstay Incidents: " + reportingService.getOverstayCount());
        
        System.out.println("---------------------------------");
    }

    private void viewMovementHistory() {
        String regNumber = readNonEmptyString("Registration Number: ");
        List<EntryLog> history = trafficService.getVehicleMovementHistory(regNumber);
        
        if (history == null) {
            System.out.println("Vehicle not found.");
            return;
        }
        
        System.out.println("\n--- Movement History for " + regNumber + " ---");
        if (history.isEmpty()) {
            System.out.println("No movement history found.");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (EntryLog log : history) {
                String exitStr = (log.getExitTime() != null) ? log.getExitTime().format(fmt) : "STILL INSIDE";
                System.out.println("[" + log.getEntryTime().format(fmt) + "] Gate: " + log.getGate() + 
                                   " | Exit: " + exitStr + " | Purpose: " + log.getExternalPurpose());
            }
        }
        System.out.println("---------------------------------");
    }

    private void extendedSearch() {
        System.out.println("\n--- Extended Traffic Log Search ---");
        System.out.print("Registration Number (optional, enter to skip): ");
        String regNumber = scanner.nextLine();
        System.out.print("Date From (YYYY-MM-DD HH:MM, optional): ");
        String dateFrom = scanner.nextLine();
        System.out.print("Date To (YYYY-MM-DD HH:MM, optional): ");
        String dateTo = scanner.nextLine();
        
        List<EntryLog> results = trafficService.searchTrafficLogs(regNumber, dateFrom, dateTo);
        
        System.out.println("\n--- Search Results ---");
        if (results.isEmpty()) {
            System.out.println("No logs found matching the criteria.");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (EntryLog log : results) {
                String exitStr = (log.getExitTime() != null) ? log.getExitTime().format(fmt) : "STILL INSIDE";
                System.out.println("[" + log.getEntryTime().format(fmt) + "] Vehicle ID: " + log.getVehicleId() + 
                                   " | Gate: " + log.getGate() + " | Exit: " + exitStr + " | Purpose: " + log.getExternalPurpose());
            }
        }
        System.out.println("---------------------------------");
    }

    private String readNonEmptyString(String prompt) {
        String input;
        do {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Input cannot be empty. Please try again.");
            }
        } while (input.isEmpty());
        return input;
    }

    public static void main(String[] args) {
        new MainMenu().start();
    }
}

package com.cvtms.ui;

import com.cvtms.dao.DatabaseConnectionManager;
import com.cvtms.model.EntryLog;
import com.cvtms.model.Role;
import com.cvtms.model.User;
import com.cvtms.model.VehicleType;
import com.cvtms.service.AuthService;
import com.cvtms.service.TrafficService;
import com.cvtms.service.VehicleService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class MainMenu {
    private Scanner scanner;
    private AuthService authService;
    private VehicleService vehicleService;
    private TrafficService trafficService;

    public MainMenu() {
        this.scanner = new Scanner(System.in);
        this.authService = new AuthService();
        this.vehicleService = new VehicleService();
        this.trafficService = new TrafficService(vehicleService);
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
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (authService.login(username, password)) {
            System.out.println("Login successful! Welcome, " + username);
        } else {
            System.out.println("Login failed.");
        }
    }

    private void showAdminMenu() {
        System.out.println("\n--- Admin Menu ---");
        System.out.println("1. Register Security Personnel");
        System.out.println("2. Logout");
        System.out.println("3. Exit System");
        System.out.print("Select: ");
        
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
                authService.logout();
                break;
            case "3":
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private void showSecurityMenu() {
        System.out.println("\n--- Security Menu ---");
        System.out.println("1. Register Vehicle");
        System.out.println("2. Record Vehicle Entry");
        System.out.println("3. Record Vehicle Exit");
        System.out.println("4. View Vehicles Currently Inside");
        System.out.println("5. Search Vehicle");
        System.out.println("6. Logout");
        System.out.println("7. Exit System");
        System.out.print("Select: ");

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
                authService.logout();
                break;
            case "7":
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private void registerVehicle() {
        System.out.print("Registration Number: ");
        String regNumber = scanner.nextLine();
        System.out.println("Vehicle Types: CAMPUS, CAB, DELIVERY, WORK, EXTERNAL");
        System.out.print("Type: ");
        String typeStr = scanner.nextLine().toUpperCase();
        
        VehicleType type;
        try {
            type = VehicleType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid Type.");
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
            System.out.println("Vehicle registered successfully.");
        }
    }

    private void recordEntry() {
        System.out.print("Registration Number: ");
        String regNumber = scanner.nextLine();
        System.out.print("Gate: ");
        String gate = scanner.nextLine();
        
        System.out.print("Purpose of visit (required only if EXTERNAL): ");
        String purpose = scanner.nextLine();
        
        trafficService.recordEntry(regNumber, gate, purpose);
    }

    private void recordExit() {
        System.out.print("Registration Number: ");
        String regNumber = scanner.nextLine();
        
        boolean success = trafficService.recordExit(regNumber, "");
        if (!success) {
            System.out.print("If overstay was detected, enter justification here: ");
            String justification = scanner.nextLine();
            if(!justification.trim().isEmpty()){
                trafficService.recordExit(regNumber, justification);
            }
        }
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
        System.out.print("Enter Registration Number: ");
        String regNumber = scanner.nextLine();
        
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

    public static void main(String[] args) {
        new MainMenu().start();
    }
}

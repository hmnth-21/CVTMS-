package com.cvtms.dao;

import com.cvtms.model.Incident;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IncidentDAO {

    public boolean createIncident(int vehicleId, String description, String severity) {
        String query = "INSERT INTO incidents (vehicle_id, description, severity) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, vehicleId);
            pstmt.setString(2, description);
            pstmt.setString(3, severity);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Incident> getIncidentsByVehicleId(int vehicleId) {
        List<Incident> incidents = new ArrayList<>();
        String query = "SELECT id, vehicle_id, description, timestamp, severity FROM incidents WHERE vehicle_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, vehicleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                incidents.add(new Incident(
                    rs.getInt("id"),
                    rs.getInt("vehicle_id"),
                    rs.getString("description"),
                    rs.getTimestamp("timestamp"),
                    rs.getString("severity")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return incidents;
    }

    public List<Incident> getAllIncidents() {
        List<Incident> incidents = new ArrayList<>();
        String query = "SELECT id, vehicle_id, description, timestamp, severity FROM incidents ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                incidents.add(new Incident(
                    rs.getInt("id"),
                    rs.getInt("vehicle_id"),
                    rs.getString("description"),
                    rs.getTimestamp("timestamp"),
                    rs.getString("severity")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return incidents;
    }
}

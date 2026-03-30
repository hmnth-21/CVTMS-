package com.cvtms.service;

import com.cvtms.dao.DatabaseConnectionManager;
import com.cvtms.model.VehicleType;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ReportingService {

    public Map<String, Integer> getDailyStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        String entryQuery = "SELECT COUNT(*) FROM entry_logs WHERE entry_time::date = CURRENT_DATE";
        String exitQuery = "SELECT COUNT(*) FROM exit_logs WHERE exit_time::date = CURRENT_DATE";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rsEntry = stmt.executeQuery(entryQuery);
            if (rsEntry.next()) {
                stats.put("entries", rsEntry.getInt(1));
            }

            ResultSet rsExit = stmt.executeQuery(exitQuery);
            if (rsExit.next()) {
                stats.put("exits", rsExit.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public Map<VehicleType, Integer> getVehicleTypeDistribution() {
        Map<VehicleType, Integer> distribution = new HashMap<>();
        String query = "SELECT v.type, COUNT(*) FROM entry_logs el " +
                       "JOIN vehicles v ON el.vehicle_id = v.id " +
                       "LEFT JOIN exit_logs ex ON el.id = ex.entry_log_id " +
                       "WHERE ex.id IS NULL " +
                       "GROUP BY v.type";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                distribution.put(VehicleType.valueOf(rs.getString(1)), rs.getInt(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return distribution;
    }

    public int getOverstayCount() {
        String query = "SELECT COUNT(*) FROM overstays";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}

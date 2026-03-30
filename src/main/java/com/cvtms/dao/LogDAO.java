package com.cvtms.dao;

import com.cvtms.model.EntryLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

public class LogDAO {

    public int createEntryLog(int vehicleId, String gate, String externalPurpose) {
        String query = "INSERT INTO entry_logs (vehicle_id, gate, external_purpose) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, vehicleId);
            pstmt.setString(2, gate);
            pstmt.setString(3, externalPurpose);
            
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public EntryLog findActiveEntryLogForVehicle(int vehicleId) {
        String query = "SELECT el.id, el.vehicle_id, el.entry_time, el.gate, el.external_purpose " +
                       "FROM entry_logs el " +
                       "LEFT JOIN exit_logs ex ON el.id = ex.entry_log_id " +
                       "WHERE el.vehicle_id = ? AND ex.id IS NULL " +
                       "ORDER BY el.entry_time DESC LIMIT 1";
                       
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, vehicleId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("entry_time");
                return new EntryLog(
                    rs.getInt("id"),
                    rs.getInt("vehicle_id"),
                    ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                    rs.getString("gate"),
                    rs.getString("external_purpose")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int createExitLog(int entryLogId) {
        String query = "INSERT INTO exit_logs (entry_log_id) VALUES (?)";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, entryLogId);
            
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean createOverstayRecord(int exitLogId, String justification) {
        String query = "INSERT INTO overstays (exit_log_id, justification) VALUES (?, ?)";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, exitLogId);
            pstmt.setString(2, justification);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // For Reports "vehicles currently inside" (Week 1 allowed)
    public List<EntryLog> getVehiclesCurrentlyInside() {
        List<EntryLog> list = new ArrayList<>();
        String query = "SELECT el.id, el.vehicle_id, el.entry_time, el.gate, el.external_purpose " +
                       "FROM entry_logs el " +
                       "LEFT JOIN exit_logs ex ON el.id = ex.entry_log_id " +
                       "WHERE ex.id IS NULL";
                       
        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
             
            while (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("entry_time");
                list.add(new EntryLog(
                    rs.getInt("id"),
                    rs.getInt("vehicle_id"),
                    ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                    rs.getString("gate"),
                    rs.getString("external_purpose")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<EntryLog> getMovementHistory(int vehicleId) {
        List<EntryLog> history = new ArrayList<>();
        String query = "SELECT el.id, el.vehicle_id, el.entry_time, el.gate, el.external_purpose, ex.exit_time " +
                       "FROM entry_logs el " +
                       "LEFT JOIN exit_logs ex ON el.id = ex.entry_log_id " +
                       "WHERE el.vehicle_id = ? ORDER BY el.entry_time DESC";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, vehicleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                java.sql.Timestamp tsEntry = rs.getTimestamp("entry_time");
                java.sql.Timestamp tsExit = rs.getTimestamp("exit_time");
                history.add(new EntryLog(
                    rs.getInt("id"),
                    rs.getInt("vehicle_id"),
                    tsEntry != null ? tsEntry.toLocalDateTime() : LocalDateTime.now(),
                    rs.getString("gate"),
                    rs.getString("external_purpose"),
                    tsExit != null ? tsExit.toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public List<EntryLog> searchLogs(String regNumber, String dateFrom, String dateTo) {
        List<EntryLog> results = new ArrayList<>();
        StringBuilder query = new StringBuilder(
            "SELECT el.id, el.vehicle_id, el.entry_time, el.gate, el.external_purpose, ex.exit_time " +
            "FROM entry_logs el " +
            "JOIN vehicles v ON el.vehicle_id = v.id " +
            "LEFT JOIN exit_logs ex ON el.id = ex.entry_log_id " +
            "WHERE 1=1"
        );
        
        if (regNumber != null && !regNumber.isEmpty()) {
            query.append(" AND v.registration_number = ?");
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            query.append(" AND el.entry_time >= ?::timestamp");
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            query.append(" AND el.entry_time <= ?::timestamp");
        }
        
        query.append(" ORDER BY el.entry_time DESC");

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
            
            int paramIndex = 1;
            if (regNumber != null && !regNumber.isEmpty()) {
                pstmt.setString(paramIndex++, regNumber);
            }
            if (dateFrom != null && !dateFrom.isEmpty()) {
                pstmt.setString(paramIndex++, dateFrom);
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                pstmt.setString(paramIndex++, dateTo);
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                java.sql.Timestamp tsEntry = rs.getTimestamp("entry_time");
                java.sql.Timestamp tsExit = rs.getTimestamp("exit_time");
                results.add(new EntryLog(
                    rs.getInt("id"),
                    rs.getInt("vehicle_id"),
                    tsEntry != null ? tsEntry.toLocalDateTime() : LocalDateTime.now(),
                    rs.getString("gate"),
                    rs.getString("external_purpose"),
                    tsExit != null ? tsExit.toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
}

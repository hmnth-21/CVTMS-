package com.cvtms.dao;

import com.cvtms.model.Vehicle;
import com.cvtms.model.VehicleOwner;
import com.cvtms.model.VehicleType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class VehicleDAO {

    public int createOwner(String name, String contact) {
        String query = "INSERT INTO owners (name, contact) VALUES (?, ?)";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, contact);
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
    
    public VehicleOwner findOwnerById(int id) {
        String query = "SELECT id, name, contact FROM owners WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new VehicleOwner(rs.getInt("id"), rs.getString("name"), rs.getString("contact"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createVehicle(String registrationNumber, int ownerId, VehicleType type) {
        String query = "INSERT INTO vehicles (registration_number, owner_id, type) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
             
            pstmt.setString(1, registrationNumber);
            if (ownerId > 0) {
                pstmt.setInt(2, ownerId);
            } else {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            }
            pstmt.setString(3, type.name());
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Vehicle findVehicleByRegistrationNumber(String registrationNumber) {
        String query = "SELECT id, registration_number, owner_id, type FROM vehicles WHERE registration_number = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
             
            pstmt.setString(1, registrationNumber);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Vehicle(
                    rs.getInt("id"),
                    rs.getString("registration_number"),
                    rs.getInt("owner_id"),
                    VehicleType.valueOf(rs.getString("type"))
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public Vehicle findVehicleById(int id) {
        String query = "SELECT id, registration_number, owner_id, type FROM vehicles WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
             
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Vehicle(
                    rs.getInt("id"),
                    rs.getString("registration_number"),
                    rs.getInt("owner_id"),
                    VehicleType.valueOf(rs.getString("type"))
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

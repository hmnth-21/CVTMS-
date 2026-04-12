package com.cvtms.service;

import com.cvtms.dao.UserDAO;
import com.cvtms.model.Role;
import com.cvtms.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private UserDAO userDAO;
    private User currentUser;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.currentUser = null;
    }

    public boolean register(String username, String password, Role role) {
        // Normalize inputs to prevent case/whitespace mismatches
        username = username.trim().toLowerCase();
        password = password.trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password cannot be empty.");
            return false;
        }

        // Check if user exists
        if (userDAO.findByUsername(username) != null) {
            System.out.println("Username already exists.");
            return false;
        }

        // Hash password
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        return userDAO.createUser(username, hashedPassword, role);
    }

    public boolean login(String username, String password) {
        // Normalize inputs to match registration format
        username = username.trim().toLowerCase();
        password = password.trim();

        User user = userDAO.findByUsername(username);
        if (user != null) {
            if (BCrypt.checkpw(password, user.getPasswordHash())) {
                this.currentUser = user;
                return true;
            } else {
                System.out.println("Invalid password.");
            }
        } else {
            System.out.println("User not found.");
        }
        return false;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }
}

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

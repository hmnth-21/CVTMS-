package com.cvtms.service;

import com.cvtms.model.Role;
import com.cvtms.model.User;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthServiceTest {
    private AuthService authService;

    @Before
    public void setUp() {
        com.cvtms.dao.DatabaseConnectionManager.initializeDatabase();
        authService = new AuthService();
    }

    @Test
    public void testLoginLogout() {
        // Assuming 'admin' exists from database initialization
        boolean loginResult = authService.login("admin", "admin");
        assertTrue(loginResult);
        assertTrue(authService.isAuthenticated());
        assertNotNull(authService.getCurrentUser());

        authService.logout();
        assertFalse(authService.isAuthenticated());
        assertNull(authService.getCurrentUser());
    }

    @Test
    public void testRegister() {
        String testUser = "testuser_" + System.currentTimeMillis();
        boolean regResult = authService.register(testUser, "password", Role.SECURITY);
        assertTrue(regResult);
        
        boolean loginResult = authService.login(testUser, "password");
        assertTrue(loginResult);
        assertEquals(Role.SECURITY, authService.getCurrentUser().getRole());
    }

    @Test
    public void testLoginWithWrongPassword() {
        boolean result = authService.login("admin", "wrong_password");
        assertFalse("Login should fail with wrong password", result);
    }

    @Test
    public void testLoginWithNonExistentUser() {
        boolean result = authService.login("non_existent_user", "password");
        assertFalse("Login should fail for non-existent user", result);
    }
}

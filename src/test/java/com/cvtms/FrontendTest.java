package com.cvtms;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.Assert.assertTrue;

public class FrontendTest {

    private WebDriver driver;
    private String baseUrl = "http://localhost:3000";

    @Before
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
    }

    @Test
    public void testLoginAndDashboard() {
        driver.get(baseUrl);

        // Wait for login form to be visible
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-form")));

        // Perform login
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("admin");
        driver.findElement(By.cssSelector("#login-form button[type='submit']")).click();

        // Wait for dashboard to be visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("app-view")));

        // Verify that the dashboard is displayed
        WebElement appView = driver.findElement(By.id("app-view"));
        assertTrue("Dashboard should be visible after login", appView.isDisplayed());

        // Verify role badge
        WebElement roleBadge = driver.findElement(By.id("user-role-badge"));
        wait.until(ExpectedConditions.textToBePresentInElement(roleBadge, "ADMIN"));
        assertTrue("Role badge should contain 'ADMIN'", roleBadge.getText().contains("ADMIN"));
    }

    @Test
    public void testSecurityLogin() {
        driver.get(baseUrl);

        // Wait for login form
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-form")));

        // Login as admin to register a security user
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("admin");
        driver.findElement(By.cssSelector("#login-form button[type='submit']")).click();

        // Navigate to Register Security page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a[data-target='admin-register']")));
        driver.findElement(By.cssSelector("a[data-target='admin-register']")).click();

        // Register new security user
        String secUser = "sec_test_" + System.currentTimeMillis();
        driver.findElement(By.id("new-sec-user")).sendKeys(secUser);
        driver.findElement(By.id("new-sec-pass")).sendKeys("password123");
        driver.findElement(By.cssSelector("#register-security-form button")).click();

        // Wait for success toast (optional, but good for stability)
        // For simplicity, we'll just logout and try to login with the new user
        
        // Logout
        driver.findElement(By.id("logout-btn")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-view")));

        // Login as new security user
        driver.findElement(By.id("username")).sendKeys(secUser);
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("#login-form button[type='submit']")).click();

        // Wait for security dashboard
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("app-view")));

        // Verify role badge shows SECURITY
        WebElement roleBadge = driver.findElement(By.id("user-role-badge"));
        wait.until(ExpectedConditions.textToBePresentInElement(roleBadge, "SECURITY"));
        assertTrue("Role badge should contain 'SECURITY'", roleBadge.getText().contains("SECURITY"));

        // Verify security-specific nav is visible
        WebElement securityNav = driver.findElement(By.id("security-nav"));
        assertTrue("Security navigation should be visible", securityNav.isDisplayed());
    }

    @Test
    public void testInvalidLogin() {
        driver.get(baseUrl);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-form")));

        // Try to login with wrong credentials
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("wrong_password");
        driver.findElement(By.cssSelector("#login-form button[type='submit']")).click();

        // Wait for error toast or check that we are still on the login page
        // The app shows a toast, but we can also check that the login view is still active
        WebElement loginView = driver.findElement(By.id("login-view"));
        assertTrue("Login view should still be active after failed login", loginView.getAttribute("class").contains("active"));
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}

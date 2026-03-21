package com.guardian.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Login Page — Fluent Page Object Model.
 *
 * Design Pattern: Method chaining ("Fluent Interface") makes tests
 * read like user stories:
 *
 *   new LoginPage(driver)
 *       .enterCredentials("admin@clinic.com", "SecurePass123")
 *       .clickLogin()
 *       .verifyDashboardLoaded();
 */
public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ─── Locators ──────────────────────────────────────────────
    private static final By EMAIL_FIELD    = By.id("loginEmail");
    private static final By PASSWORD_FIELD = By.id("loginPassword");
    private static final By LOGIN_BUTTON   = By.id("loginBtn");
    private static final By ERROR_MESSAGE  = By.id("loginError");
    private static final By ROLE_SELECTOR  = By.id("loginRole");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // ─── Actions (Fluent) ──────────────────────────────────────

    public LoginPage navigateTo(String baseUrl) {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
        return this;
    }

    public LoginPage enterCredentials(String email, String password) {
        WebElement emailEl = wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
        emailEl.clear();
        emailEl.sendKeys(email);

        WebElement passEl = driver.findElement(PASSWORD_FIELD);
        passEl.clear();
        passEl.sendKeys(password);

        return this;
    }

    public LoginPage selectRole(String role) {
        WebElement roleSelect = driver.findElement(ROLE_SELECTOR);
        new org.openqa.selenium.support.ui.Select(roleSelect).selectByValue(role);
        return this;
    }

    public DashboardPage clickLogin() {
        driver.findElement(LOGIN_BUTTON).click();
        return new DashboardPage(driver);
    }

    public LoginPage clickLoginExpectingError() {
        driver.findElement(LOGIN_BUTTON).click();
        return this;
    }

    // ─── Assertions ────────────────────────────────────────────

    public LoginPage verifyErrorMessage(String expectedText) {
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_MESSAGE));
        String actual = error.getText();
        assert actual.contains(expectedText)
                : "Expected error '" + expectedText + "' but got '" + actual + "'";
        return this;
    }

    public boolean isErrorDisplayed() {
        try {
            return driver.findElement(ERROR_MESSAGE).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}

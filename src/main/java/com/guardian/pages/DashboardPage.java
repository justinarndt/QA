package com.guardian.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Dashboard Page — Central navigation hub after login.
 *
 * Validates role-based element visibility (RBAC) and provides
 * navigation to sub-pages (Scheduler, Billing, Patient Notes).
 */
public class DashboardPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ─── Locators ──────────────────────────────────────────────
    private static final By WELCOME_BANNER  = By.id("welcomeBanner");
    private static final By NAV_SCHEDULER   = By.id("navScheduler");
    private static final By NAV_BILLING     = By.id("navBilling");
    private static final By NAV_NOTES       = By.id("navClinicalNotes");
    private static final By NAV_PATIENTS    = By.id("navPatients");
    private static final By USER_ROLE_BADGE = By.id("userRoleBadge");
    private static final By LOGOUT_BTN      = By.id("logoutBtn");
    private static final By ACCESS_DENIED   = By.id("accessDenied");

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // ─── Verification ──────────────────────────────────────────

    public DashboardPage verifyDashboardLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(WELCOME_BANNER));
        return this;
    }

    public String getWelcomeText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(WELCOME_BANNER)).getText();
    }

    public String getUserRole() {
        return driver.findElement(USER_ROLE_BADGE).getText();
    }

    // ─── RBAC Checks ───────────────────────────────────────────

    public boolean isSchedulerVisible() {
        return isElementVisible(NAV_SCHEDULER);
    }

    public boolean isBillingVisible() {
        return isElementVisible(NAV_BILLING);
    }

    public boolean isClinicalNotesVisible() {
        return isElementVisible(NAV_NOTES);
    }

    public boolean isPatientsVisible() {
        return isElementVisible(NAV_PATIENTS);
    }

    public boolean isAccessDeniedVisible() {
        return isElementVisible(ACCESS_DENIED);
    }

    private boolean isElementVisible(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

    // ─── Navigation (Fluent) ───────────────────────────────────

    public SchedulerPage navigateToScheduler() {
        wait.until(ExpectedConditions.elementToBeClickable(NAV_SCHEDULER)).click();
        return new SchedulerPage(driver);
    }

    public BillingPage navigateToBilling() {
        wait.until(ExpectedConditions.elementToBeClickable(NAV_BILLING)).click();
        return new BillingPage(driver);
    }

    public PatientNotesPage navigateToClinicalNotes() {
        wait.until(ExpectedConditions.elementToBeClickable(NAV_NOTES)).click();
        return new PatientNotesPage(driver);
    }

    public DashboardPage clickClinicalNotesExpectDenied() {
        try {
            driver.findElement(NAV_NOTES).click();
        } catch (Exception ignored) {
            // Element may not be clickable — that's the RBAC point
        }
        return this;
    }

    public void logout() {
        driver.findElement(LOGOUT_BTN).click();
    }
}

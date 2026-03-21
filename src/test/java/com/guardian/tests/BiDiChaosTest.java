package com.guardian.tests;

import com.guardian.base.BaseTest;
import com.guardian.config.ConfigManager;
import com.guardian.pages.LoginPage;
import com.guardian.utils.RetryAnalyzer;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

import static org.testng.Assert.*;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * BIDI CHAOS TEST — Network Failure Injection & Resiliency Validation
 * ═══════════════════════════════════════════════════════════════════════
 *
 * This is the test that proves you're not just testing the "Happy Path."
 *
 * Using Selenium 4 BiDi (Bidirectional WebSocket protocol), this test:
 * 1. Enables network interception via Chrome DevTools Protocol.
 * 2. Injects simulated failures (blocked requests, network errors).
 * 3. Verifies the UI displays a GRACEFUL error message instead of crashing.
 *
 * Why it matters:
 * - In production, APIs fail. Networks hiccup. Servers return 500.
 * - Traditional Selenium only tests what you CAN see.
 * - BiDi tests what GOES WRONG underneath.
 *
 * This catches "silent" failures that would otherwise reach patients.
 */
@Epic("Resiliency & Chaos Engineering")
@Feature("Network Failure Handling")
public class BiDiChaosTest extends BaseTest {

    // ═════════════════════════════════════════════════════════════
    //  TEST: Network Interception Detects API Failures
    // ═════════════════════════════════════════════════════════════

    @Test(description = "BiDi: Detect 4xx/5xx API errors during normal page load",
          retryAnalyzer = RetryAnalyzer.class)
    @Story("Silent Error Detection")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Enables BiDi network monitoring, navigates the application, and "
               + "verifies that no silent API failures (4xx/5xx) occur during page load. "
               + "Catches errors that traditional Selenium would miss entirely.")
    public void testNetworkInterceptionDetectsErrors() {
        // Enable BiDi network monitoring BEFORE navigation
        enableNetworkInterception();

        // Navigate and interact normally
        new LoginPage(getDriver())
                .navigateTo(ConfigManager.baseUrl())
                .enterCredentials("admin@clinic.com", "SecurePass123")
                .clickLogin()
                .verifyDashboardLoaded();

        // Assert: No silent failures should have occurred
        Allure.step("Verifying no silent 4xx/5xx errors during page load");
        assertNoNetworkErrors();

        captureScreenshot("NetworkClean");
    }

    // ═════════════════════════════════════════════════════════════
    //  TEST: Graceful Error Handling Under Network Failure
    // ═════════════════════════════════════════════════════════════

    @Test(description = "BiDi: Verify graceful error UI when API fails",
          retryAnalyzer = RetryAnalyzer.class)
    @Story("Graceful Degradation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Uses DevTools to block API requests, then verifies the application "
               + "displays a user-friendly error message instead of a blank page or crash. "
               + "This is the difference between 'works' and 'production-ready.'")
    public void testGracefulErrorOnNetworkFailure() {
        // ── Inject chaos: block API calls ──────────────────────
        Allure.step("CHAOS: Injecting network failure for API requests");
        injectNetworkFailure("*/api/*", 500);

        // ── Navigate — the API calls should fail ───────────────
        Allure.step("Navigating to app with blocked API");
        getDriver().get(ConfigManager.baseUrl());

        // ── Verify: Error message should appear, not a crash ───
        Allure.step("Verifying graceful error message is displayed");
        WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(10));

        // Look for any kind of graceful error indicator
        boolean hasGracefulError = false;

        // Check for error banner
        List<WebElement> errorBanners = getDriver().findElements(By.id("errorBanner"));
        if (!errorBanners.isEmpty() && errorBanners.get(0).isDisplayed()) {
            hasGracefulError = true;
            Allure.step("✅ Error banner displayed: " + errorBanners.get(0).getText());
        }

        // Check for error modal
        List<WebElement> errorModals = getDriver().findElements(By.cssSelector(".error-modal"));
        if (!errorModals.isEmpty() && errorModals.get(0).isDisplayed()) {
            hasGracefulError = true;
            Allure.step("✅ Error modal displayed");
        }

        // Check that login form still renders (graceful fallback)
        List<WebElement> loginForms = getDriver().findElements(By.id("loginEmail"));
        if (!loginForms.isEmpty() && loginForms.get(0).isDisplayed()) {
            hasGracefulError = true;
            Allure.step("✅ Login form still functional despite API failure");
        }

        captureScreenshot("ChaosTest_GracefulError");

        assertTrue(hasGracefulError,
                "Application should display a graceful error message or maintain functionality "
                + "when the API is unreachable. Instead, the page may have crashed or gone blank.");
    }

    // ═════════════════════════════════════════════════════════════
    //  TEST: Console Error Monitoring
    // ═════════════════════════════════════════════════════════════

    @Test(description = "BiDi: Monitor browser console for JavaScript errors",
          retryAnalyzer = RetryAnalyzer.class)
    @Story("Client-Side Error Detection")
    @Severity(SeverityLevel.NORMAL)
    @Description("Navigates through the application and monitors the browser console "
               + "for unhandled JavaScript errors that could indicate client-side bugs.")
    public void testNoJavaScriptConsolErrors() {
        enableNetworkInterception();

        new LoginPage(getDriver())
                .navigateTo(ConfigManager.baseUrl())
                .enterCredentials("admin@clinic.com", "SecurePass123")
                .clickLogin()
                .verifyDashboardLoaded();

        Allure.step("Checking for JavaScript console errors");

        // BiDi captures network-level errors. For console.error(),
        // we check the browser log entries.
        var logs = getDriver().manage().logs().get("browser");
        long severeCount = logs.getAll().stream()
                .filter(entry -> entry.getLevel().getName().equals("SEVERE"))
                .count();

        captureScreenshot("ConsoleErrorCheck");

        assertEquals(severeCount, 0,
                "Found " + severeCount + " SEVERE JavaScript console errors during navigation.");
    }
}

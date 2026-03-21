package com.guardian.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Scheduler Page — Patient scheduling and appointment verification.
 *
 * Used in Hybrid Workflow Tests:
 *   1. API creates patient (200ms)
 *   2. This page verifies patient appears in the calendar (Selenium)
 */
public class SchedulerPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ─── Locators ──────────────────────────────────────────────
    private static final By SCHEDULER_VIEW   = By.id("schedulerView");
    private static final By SEARCH_INPUT     = By.id("patientSearch");
    private static final By SEARCH_BUTTON    = By.id("searchBtn");
    private static final By SEARCH_RESULTS   = By.cssSelector(".search-result");
    private static final By NO_RESULTS       = By.id("noResults");
    private static final By PATIENT_NAME     = By.cssSelector(".patient-name");

    public SchedulerPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // ─── Actions ───────────────────────────────────────────────

    public SchedulerPage verifyLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(SCHEDULER_VIEW));
        return this;
    }

    public SchedulerPage searchForPatient(String patientNameOrId) {
        WebElement searchBox = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
        searchBox.clear();
        searchBox.sendKeys(patientNameOrId);
        driver.findElement(SEARCH_BUTTON).click();

        // Wait for results to populate
        wait.until(driver -> {
            List<WebElement> results = driver.findElements(SEARCH_RESULTS);
            List<WebElement> noResult = driver.findElements(NO_RESULTS);
            return !results.isEmpty() || !noResult.isEmpty();
        });

        return this;
    }

    // ─── Assertions ────────────────────────────────────────────

    public SchedulerPage verifyPatientVisible(String expectedName) {
        List<WebElement> results = driver.findElements(PATIENT_NAME);
        boolean found = results.stream()
                .anyMatch(el -> el.getText().contains(expectedName));

        assert found : "Patient '" + expectedName + "' not found in scheduler. "
                + "Found: " + results.stream().map(WebElement::getText).toList();
        return this;
    }

    public boolean isPatientInResults(String name) {
        List<WebElement> results = driver.findElements(PATIENT_NAME);
        return results.stream().anyMatch(el -> el.getText().contains(name));
    }
}

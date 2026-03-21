package com.guardian.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Billing Page — Insurance billing calculation verification.
 *
 * Used in Data-Driven tests to validate complex billing math:
 * copays, deductibles, co-insurance percentages, and total due amounts.
 */
public class BillingPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ─── Locators ──────────────────────────────────────────────
    private static final By BILLING_VIEW        = By.id("billingView");
    private static final By COPAY_INPUT         = By.id("copayAmount");
    private static final By DEDUCTIBLE_INPUT    = By.id("deductibleAmount");
    private static final By COINSURANCE_INPUT   = By.id("coinsurancePercent");
    private static final By SERVICE_COST_INPUT  = By.id("serviceCost");
    private static final By CALCULATE_BTN       = By.id("calculateBtn");
    private static final By TOTAL_DUE           = By.id("totalDue");
    private static final By INSURANCE_PAYS      = By.id("insurancePays");
    private static final By PATIENT_PAYS        = By.id("patientPays");
    private static final By ERROR_BANNER        = By.id("billingError");

    public BillingPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // ─── Actions ───────────────────────────────────────────────

    public BillingPage verifyLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(BILLING_VIEW));
        return this;
    }

    public BillingPage enterBillingData(String copay, String deductible,
                                        String coinsurance, String serviceCost) {
        clearAndType(COPAY_INPUT, copay);
        clearAndType(DEDUCTIBLE_INPUT, deductible);
        clearAndType(COINSURANCE_INPUT, coinsurance);
        clearAndType(SERVICE_COST_INPUT, serviceCost);
        return this;
    }

    public BillingPage clickCalculate() {
        driver.findElement(CALCULATE_BTN).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(TOTAL_DUE));
        return this;
    }

    // ─── Assertions ────────────────────────────────────────────

    public String getTotalDue() {
        return driver.findElement(TOTAL_DUE).getText().replace("$", "").trim();
    }

    public String getInsurancePays() {
        return driver.findElement(INSURANCE_PAYS).getText().replace("$", "").trim();
    }

    public String getPatientPays() {
        return driver.findElement(PATIENT_PAYS).getText().replace("$", "").trim();
    }

    public BillingPage verifyTotalDue(String expectedAmount) {
        String actual = getTotalDue();
        assert actual.equals(expectedAmount)
                : "Expected total due $" + expectedAmount + " but got $" + actual;
        return this;
    }

    public BillingPage verifyPatientPays(String expectedAmount) {
        String actual = getPatientPays();
        assert actual.equals(expectedAmount)
                : "Expected patient pays $" + expectedAmount + " but got $" + actual;
        return this;
    }

    public boolean isErrorDisplayed() {
        try {
            return driver.findElement(ERROR_BANNER).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Helpers ───────────────────────────────────────────────

    private void clearAndType(By locator, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        el.clear();
        el.sendKeys(text);
    }
}

package com.guardian.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardian.base.BaseTest;
import com.guardian.config.ConfigManager;
import com.guardian.pages.BillingPage;
import com.guardian.pages.LoginPage;
import com.guardian.utils.RetryAnalyzer;
import io.qameta.allure.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * BILLING DATA-DRIVEN TEST — The Insurance Math Engine
 * ═══════════════════════════════════════════════════════════════════════
 *
 * TherapyNotes handles complex billing: copays, deductibles, co-insurance.
 * A single math error means incorrect patient charges or insurance claims.
 *
 * This test reads 50+ billing scenarios from billing_data.json and:
 * 1. Enters the billing data into the UI form.
 * 2. Calculates the EXPECTED total using Java math (independent verification).
 * 3. Compares it to the UI's displayed "Total Due."
 *
 * If the UI total doesn't match the calculated total, we have a billing bug.
 */
@Epic("Billing & Insurance")
@Feature("Billing Calculation Accuracy")
public class BillingDataDrivenTest extends BaseTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ═════════════════════════════════════════════════════════════
    //  DATA PROVIDER: 50+ billing scenarios from JSON
    // ═════════════════════════════════════════════════════════════

    @DataProvider(name = "billingScenarios")
    public Object[][] billingDataProvider() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("billing_data.json");
        List<Map<String, Object>> scenarios = mapper.readValue(is, new TypeReference<>() {});

        Object[][] data = new Object[scenarios.size()][1];
        for (int i = 0; i < scenarios.size(); i++) {
            data[i][0] = scenarios.get(i);
        }
        return data;
    }

    // ═════════════════════════════════════════════════════════════
    //  TEST: UI Total vs Calculated Total
    // ═════════════════════════════════════════════════════════════

    @Test(dataProvider = "billingScenarios",
          description = "Verify billing math: UI total matches independent calculation",
          retryAnalyzer = RetryAnalyzer.class)
    @Story("Insurance Claim Accuracy")
    @Severity(SeverityLevel.CRITICAL)
    @Description("For each billing scenario, enters copay/deductible/co-insurance into "
               + "the billing form, triggers calculation, and compares the UI result against "
               + "an independently calculated expected total. Catches billing math errors.")
    public void testBillingCalculation(@SuppressWarnings("unchecked") Map<String, Object> scenario) {
        String scenarioName = (String) scenario.get("scenario");
        double copay = toDouble(scenario.get("copay"));
        double deductible = toDouble(scenario.get("deductible"));
        double coinsurancePercent = toDouble(scenario.get("coinsurancePercent"));
        double serviceCost = toDouble(scenario.get("serviceCost"));

        Allure.step("Testing billing scenario: " + scenarioName);

        // ── Independent Calculation ────────────────────────────
        double afterDeductible = Math.max(0, serviceCost - deductible);
        double insurancePays = afterDeductible * (coinsurancePercent / 100.0);
        double expectedPatientPays = copay + (afterDeductible - insurancePays);
        String expectedTotal = String.format("%.2f", expectedPatientPays);

        Allure.step("Expected patient pays: $" + expectedTotal
                + " (Service: $" + serviceCost
                + ", Copay: $" + copay
                + ", Deductible: $" + deductible
                + ", Co-insurance: " + coinsurancePercent + "%)");

        // ── UI Interaction ─────────────────────────────────────
        BillingPage billingPage = new LoginPage(getDriver())
                .navigateTo(ConfigManager.baseUrl())
                .enterCredentials("billing@clinic.com", "SecurePass123")
                .clickLogin()
                .verifyDashboardLoaded()
                .navigateToBilling()
                .verifyLoaded();

        billingPage.enterBillingData(
                String.valueOf(copay),
                String.valueOf(deductible),
                String.valueOf(coinsurancePercent),
                String.valueOf(serviceCost)
        ).clickCalculate();

        // ── Assertion ──────────────────────────────────────────
        String actualPatientPays = billingPage.getPatientPays();

        captureScreenshot("Billing_" + scenarioName);

        assertEquals(actualPatientPays, expectedTotal,
                "Billing mismatch for scenario '" + scenarioName + "': "
                + "UI shows $" + actualPatientPays + " but expected $" + expectedTotal);
    }

    // ─── Helper ────────────────────────────────────────────────

    private double toDouble(Object value) {
        if (value instanceof Number num) return num.doubleValue();
        return Double.parseDouble(value.toString());
    }
}

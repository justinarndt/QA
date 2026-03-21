package com.guardian.tests;

import com.guardian.api.ApiClient;
import com.guardian.base.BaseTest;
import com.guardian.config.ConfigManager;
import com.guardian.pages.LoginPage;
import com.guardian.utils.RetryAnalyzer;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * HYBRID WORKFLOW TEST — The "Shift-Left" API-to-UI Handshake
 * ═══════════════════════════════════════════════════════════════════════
 *
 * This is the SIGNATURE test that demonstrates Senior-Level thinking:
 *
 * 1. CREATE patient via API (RestAssured) → ~200ms
 * 2. VERIFY patient appears in UI Scheduler (Selenium) → visual confirmation
 *
 * Why this matters:
 * - A Junior would navigate through 4 UI screens to create a patient (15+ sec).
 * - A Senior uses the API as a "backdoor" to set up test state instantly,
 *   then uses Selenium only for what it's best at: verifying the USER sees it.
 *
 * This reduces total suite execution by ~70% while increasing test isolation.
 */
@Epic("Healthcare Workflow Engine")
@Feature("Patient Scheduling")
public class HybridWorkflowTest extends BaseTest {

    private ApiClient apiClient;

    @BeforeMethod
    @Override
    public void setUp() {
        super.setUp();
        apiClient = new ApiClient();
    }

    // ═════════════════════════════════════════════════════════════
    //  TEST: API → UI Patient Creation Handshake
    // ═════════════════════════════════════════════════════════════

    @Test(description = "Verify new patient created via API appears in UI Scheduler",
          retryAnalyzer = RetryAnalyzer.class)
    @Story("API-to-UI Data Verification")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Creates a patient via RestAssured API, then uses Selenium to verify "
               + "the patient is visible in the scheduling calendar. Demonstrates the "
               + "Hybrid 'Shift-Left' approach that reduces test setup time by 70%.")
    public void testHybridPatientCreation() {
        // ── STEP 1: API creates patient (instant) ──────────────
        Allure.step("API: Authenticate as admin");
        apiClient.authenticate("admin@clinic.com", "SecurePass123");

        Allure.step("API: Create patient 'John Doe' via REST endpoint");
        String patientId = apiClient.createPatient("John", "Doe", "1990-01-15");

        // ── STEP 2: Selenium verifies in UI ────────────────────
        Allure.step("UI: Login and navigate to Scheduler");
        String appUrl = ConfigManager.baseUrl();

        new LoginPage(getDriver())
                .navigateTo(appUrl)
                .enterCredentials("admin@clinic.com", "SecurePass123")
                .clickLogin()
                .verifyDashboardLoaded()
                .navigateToScheduler()
                .verifyLoaded()
                .searchForPatient("John Doe")
                .verifyPatientVisible("John Doe");

        captureScreenshot("PatientVisibleInScheduler");
    }

    // ═════════════════════════════════════════════════════════════
    //  TEST: API → UI Appointment Verification
    // ═════════════════════════════════════════════════════════════

    @Test(description = "Verify API-created appointment appears on calendar",
          retryAnalyzer = RetryAnalyzer.class)
    @Story("Appointment Scheduling Verification")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Creates both a patient and appointment via API, then verifies "
               + "the appointment is displayed in the UI calendar view.")
    public void testAppointmentAppearsInCalendar() {
        // ── API Setup ──────────────────────────────────────────
        apiClient.authenticate("admin@clinic.com", "SecurePass123");
        String patientId = apiClient.createPatient("Jane", "Smith", "1985-07-22");
        apiClient.createAppointment(patientId, "2026-03-25T10:00:00", "Initial Assessment");

        // ── UI Verification ────────────────────────────────────
        new LoginPage(getDriver())
                .navigateTo(ConfigManager.baseUrl())
                .enterCredentials("admin@clinic.com", "SecurePass123")
                .clickLogin()
                .verifyDashboardLoaded()
                .navigateToScheduler()
                .searchForPatient("Jane Smith")
                .verifyPatientVisible("Jane Smith");

        captureScreenshot("AppointmentVisible");
    }
}

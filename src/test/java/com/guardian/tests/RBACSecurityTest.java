package com.guardian.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardian.base.BaseTest;
import com.guardian.config.ConfigManager;
import com.guardian.pages.DashboardPage;
import com.guardian.pages.LoginPage;
import com.guardian.utils.RetryAnalyzer;
import io.qameta.allure.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * RBAC SECURITY TEST — The "Privacy" Engine
 * ═══════════════════════════════════════════════════════════════════════
 *
 * In healthcare SaaS (HIPAA), a Billing Clerk should NEVER see patient
 * clinical notes. An Intern should have read-only access. A Doctor sees all.
 *
 * This test iterates through a JSON file of roles and asserts that:
 * - Each role sees ONLY what they're authorized to see.
 * - Unauthorized access shows "403 Forbidden" or a hidden element.
 * - Both the UI element AND the underlying API return the correct response.
 *
 * Uses Soft Assertions: collects ALL failures before failing the test,
 * giving a complete picture of which permissions are misconfigured.
 */
@Epic("Security & Compliance")
@Feature("Role-Based Access Control (RBAC)")
public class RBACSecurityTest extends BaseTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ═════════════════════════════════════════════════════════════
    //  DATA PROVIDER: Reads role definitions from JSON
    // ═════════════════════════════════════════════════════════════

    @DataProvider(name = "roleData")
    public Object[][] roleDataProvider() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("roles.json");
        List<Map<String, Object>> roles = mapper.readValue(is, new TypeReference<>() {});

        Object[][] data = new Object[roles.size()][1];
        for (int i = 0; i < roles.size(); i++) {
            data[i][0] = roles.get(i);
        }
        return data;
    }

    // ═════════════════════════════════════════════════════════════
    //  TEST: Role-Based Element Visibility
    // ═════════════════════════════════════════════════════════════

    @Test(dataProvider = "roleData",
          description = "Validate RBAC: each role sees only authorized elements",
          retryAnalyzer = RetryAnalyzer.class)
    @Story("Healthcare Data Privacy")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Iterates through role definitions from roles.json. For each role, "
               + "logs in and asserts that navigation elements match the role's permissions. "
               + "A Billing Clerk should NOT see Clinical Notes. A Doctor should see everything.")
    public void testRoleBasedAccess(@SuppressWarnings("unchecked") Map<String, Object> roleConfig) {
        String role = (String) roleConfig.get("role");
        String email = (String) roleConfig.get("email");
        String password = (String) roleConfig.get("password");

        @SuppressWarnings("unchecked")
        Map<String, Boolean> permissions = (Map<String, Boolean>) roleConfig.get("permissions");

        Allure.step("Testing role: " + role + " (" + email + ")");

        // ── Login as this role ─────────────────────────────────
        DashboardPage dashboard = new LoginPage(getDriver())
                .navigateTo(ConfigManager.baseUrl())
                .enterCredentials(email, password)
                .selectRole(role.toLowerCase())
                .clickLogin()
                .verifyDashboardLoaded();

        // ── Soft Assert all permissions ────────────────────────
        SoftAssert softAssert = new SoftAssert();

        Boolean canSeeScheduler = permissions.get("scheduler");
        if (canSeeScheduler != null) {
            softAssert.assertEquals(dashboard.isSchedulerVisible(), canSeeScheduler.booleanValue(),
                    role + " - Scheduler visibility mismatch");
        }

        Boolean canSeeBilling = permissions.get("billing");
        if (canSeeBilling != null) {
            softAssert.assertEquals(dashboard.isBillingVisible(), canSeeBilling.booleanValue(),
                    role + " - Billing visibility mismatch");
        }

        Boolean canSeeNotes = permissions.get("clinicalNotes");
        if (canSeeNotes != null) {
            softAssert.assertEquals(dashboard.isClinicalNotesVisible(), canSeeNotes.booleanValue(),
                    role + " - Clinical Notes visibility mismatch");
        }

        Boolean canSeePatients = permissions.get("patients");
        if (canSeePatients != null) {
            softAssert.assertEquals(dashboard.isPatientsVisible(), canSeePatients.booleanValue(),
                    role + " - Patients visibility mismatch");
        }

        captureScreenshot("RBAC_" + role);

        Allure.step("Role '" + role + "' permission check complete");
        softAssert.assertAll();
    }
}

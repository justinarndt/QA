package com.guardian.api;

import com.guardian.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * RestAssured API Client — The "Shift-Left" Engine.
 *
 * Instead of spending 15 seconds navigating through the UI to create
 * test data, this client hits the API directly in ~200ms.
 *
 * Features:
 * - Auth token caching: authenticates once, reuses across all calls.
 * - Patient CRUD: create, fetch, delete patients via API.
 * - Appointment management: schedule appointments for UI verification.
 *
 * This is the key differentiator between a "Senior" and a "Mid-Level" approach.
 */
public class ApiClient {

    private String authToken;
    private final String baseUrl;

    public ApiClient() {
        this.baseUrl = ConfigManager.apiBaseUrl();
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // ═══════════════════════════════════════════════════════════════
    //  AUTHENTICATION — Token Caching
    // ═══════════════════════════════════════════════════════════════

    /**
     * Authenticate once and cache the token for subsequent API calls.
     * Avoids re-authenticating for every API request (performance + realism).
     */
    public ApiClient authenticate(String username, String password) {
        Response response = RestAssured.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", username,
                        "password", password
                ))
                .post("/api/auth/login");

        if (response.statusCode() == 200) {
            this.authToken = response.jsonPath().getString("token");
            System.out.println("✅ API Auth: Token acquired for " + username);
        } else {
            System.err.println("❌ API Auth Failed: HTTP " + response.statusCode());
        }
        return this;
    }

    // ═══════════════════════════════════════════════════════════════
    //  PATIENT CRUD — High-Speed Test Data Seeding
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a patient via API (bypassing the slow UI form).
     * Returns the patient ID for downstream UI verification.
     */
    public String createPatient(String firstName, String lastName, String dob) {
        Response response = authorizedRequest()
                .body(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "dateOfBirth", dob,
                        "status", "active"
                ))
                .post("/api/patients");

        String patientId = response.jsonPath().getString("id");
        System.out.println("✅ API: Patient created → ID=" + patientId
                + " (" + firstName + " " + lastName + ")");
        return patientId;
    }

    /**
     * Fetch patient details via API — used for data validation assertions.
     */
    public Response getPatient(String patientId) {
        return authorizedRequest()
                .get("/api/patients/" + patientId);
    }

    /**
     * Delete patient via API — test data cleanup to avoid pollution.
     */
    public void deletePatient(String patientId) {
        authorizedRequest()
                .delete("/api/patients/" + patientId);
        System.out.println("🗑️ API: Patient " + patientId + " deleted (cleanup).");
    }

    // ═══════════════════════════════════════════════════════════════
    //  APPOINTMENTS — Schedule for UI Verification
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create an appointment via API for a given patient.
     * The UI Scheduler test then verifies this appears in the calendar.
     */
    public String createAppointment(String patientId, String dateTime, String type) {
        Response response = authorizedRequest()
                .body(Map.of(
                        "patientId", patientId,
                        "dateTime", dateTime,
                        "type", type,
                        "duration", "50min"
                ))
                .post("/api/appointments");

        String appointmentId = response.jsonPath().getString("id");
        System.out.println("✅ API: Appointment created → ID=" + appointmentId);
        return appointmentId;
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private RequestSpecification authorizedRequest() {
        return RestAssured.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken);
    }

    /**
     * Health check — validates the API is reachable before running tests.
     */
    public boolean isApiHealthy() {
        try {
            Response response = RestAssured.given()
                    .baseUri(baseUrl)
                    .get("/api/health");
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("⚠ API Health Check Failed: " + e.getMessage());
            return false;
        }
    }
}

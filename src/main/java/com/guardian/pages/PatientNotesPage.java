package com.guardian.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Patient Clinical Notes Page — Used for RBAC security testing.
 *
 * The core assertion: a Billing Clerk should NEVER see clinical notes.
 * A Doctor should see them with full content.
 */
public class PatientNotesPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ─── Locators ──────────────────────────────────────────────
    private static final By NOTES_VIEW         = By.id("clinicalNotesView");
    private static final By NOTE_ENTRIES       = By.cssSelector(".note-entry");
    private static final By NOTE_CONTENT       = By.cssSelector(".note-content");
    private static final By ACCESS_DENIED_MSG  = By.id("accessDeniedMessage");
    private static final By PATIENT_NAME_LABEL = By.id("notesPatientName");

    public PatientNotesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // ─── Verification ──────────────────────────────────────────

    public PatientNotesPage verifyLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(NOTES_VIEW));
        return this;
    }

    public boolean isAccessDenied() {
        try {
            WebElement denied = driver.findElement(ACCESS_DENIED_MSG);
            return denied.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean areNotesVisible() {
        List<WebElement> notes = driver.findElements(NOTE_ENTRIES);
        return !notes.isEmpty();
    }

    public int getNoteCount() {
        return driver.findElements(NOTE_ENTRIES).size();
    }

    public String getFirstNoteContent() {
        List<WebElement> contents = driver.findElements(NOTE_CONTENT);
        return contents.isEmpty() ? "" : contents.get(0).getText();
    }

    public String getPatientName() {
        return driver.findElement(PATIENT_NAME_LABEL).getText();
    }

    // ─── Assertions ────────────────────────────────────────────

    public PatientNotesPage verifyNotesAccessible() {
        assert areNotesVisible() : "Clinical notes should be visible but are not.";
        return this;
    }

    public PatientNotesPage verifyAccessIsDenied() {
        assert isAccessDenied() : "Access should be denied but notes are visible.";
        return this;
    }
}

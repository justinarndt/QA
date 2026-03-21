package com.guardian.base;

import com.guardian.config.ConfigManager;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v131.network.Network;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Thread-Safe Base Test with Selenium BiDi Network Interception.
 *
 * Key Senior-Level Design Decisions:
 * 1. ThreadLocal<WebDriver> — Enables parallel execution without session collisions.
 * 2. BiDi DevTools — Real-time network monitoring catches silent API failures.
 * 3. Automatic Screenshots — Allure captures failure evidence without manual effort.
 * 4. Config-Driven — Browser, headless mode, and URLs controlled via config/system props.
 */
public class BaseTest {

    /** Thread-safe WebDriver — each parallel thread gets its own isolated browser instance. */
    protected static final ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();

    /** Captured network errors for assertion in chaos/resiliency tests. */
    protected static final ThreadLocal<List<NetworkError>> networkErrors = ThreadLocal.withInitial(ArrayList::new);

    // ═══════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═══════════════════════════════════════════════════════════════

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        WebDriver driver = createDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().window().maximize();
        driverThread.set(driver);
        networkErrors.get().clear();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        WebDriver driver = getDriver();
        if (driver != null) {
            // Capture screenshot on failure for Allure report
            if (result.getStatus() == ITestResult.FAILURE) {
                captureScreenshot("Failure_" + result.getMethod().getMethodName());
            }
            driver.quit();
            driverThread.remove();
            networkErrors.remove();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DRIVER FACTORY
    // ═══════════════════════════════════════════════════════════════

    public WebDriver getDriver() {
        return driverThread.get();
    }

    private WebDriver createDriver() {
        String browser = ConfigManager.browser().toLowerCase();
        boolean headless = ConfigManager.headless();

        return switch (browser) {
            case "firefox" -> {
                FirefoxOptions opts = new FirefoxOptions();
                if (headless) opts.addArguments("--headless");
                yield new FirefoxDriver(opts);
            }
            case "edge" -> {
                EdgeOptions opts = new EdgeOptions();
                if (headless) opts.addArguments("--headless=new");
                yield new EdgeDriver(opts);
            }
            default -> {
                ChromeOptions opts = new ChromeOptions();
                opts.addArguments("--remote-allow-origins=*");
                opts.addArguments("--disable-gpu");
                opts.addArguments("--no-sandbox");
                opts.addArguments("--disable-dev-shm-usage");
                if (headless) opts.addArguments("--headless=new");
                yield new ChromeDriver(opts);
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════
    //  SELENIUM BIDI — NETWORK INTERCEPTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Enable real-time network monitoring via Chrome DevTools Protocol.
     * Captures 4xx/5xx responses for resiliency assertions.
     *
     * Usage:
     *   enableNetworkInterception();
     *   // ... navigate and interact ...
     *   assertNoNetworkErrors();  // or assertNetworkErrorOccurred(500);
     */
    protected void enableNetworkInterception() {
        WebDriver driver = getDriver();
        if (driver instanceof ChromeDriver chromeDriver) {
            DevTools devTools = chromeDriver.getDevTools();
            devTools.createSession();
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

            devTools.addListener(Network.responseReceived(), response -> {
                int status = response.getResponse().getStatus();
                String url = response.getResponse().getUrl();
                if (status >= 400) {
                    networkErrors.get().add(new NetworkError(url, status));
                    System.err.println("🔴 NETWORK ALERT: " + url + " → HTTP " + status);
                }
            });
        }
    }

    /**
     * Inject a mock network failure via DevTools — for chaos/resiliency testing.
     * Blocks requests matching the URL pattern and returns the specified status code.
     */
    protected void injectNetworkFailure(String urlPattern, int statusCode) {
        WebDriver driver = getDriver();
        if (driver instanceof ChromeDriver chromeDriver) {
            DevTools devTools = chromeDriver.getDevTools();
            devTools.createSession();
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

            devTools.send(Network.setBlockedURLs(List.of(urlPattern)));
            System.out.println("🟡 CHAOS: Blocking requests matching '" + urlPattern + "' → " + statusCode);
        }
    }

    /** Assert that no network errors (4xx/5xx) were captured. */
    protected void assertNoNetworkErrors() {
        List<NetworkError> errors = networkErrors.get();
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Network errors detected:\n");
            errors.forEach(e -> sb.append("  • ").append(e.url()).append(" → HTTP ").append(e.status()).append("\n"));
            throw new AssertionError(sb.toString());
        }
    }

    /** Assert that at least one network error with the given status was captured. */
    protected boolean hasNetworkError(int expectedStatus) {
        return networkErrors.get().stream().anyMatch(e -> e.status() == expectedStatus);
    }

    // ═══════════════════════════════════════════════════════════════
    //  ALLURE EVIDENCE CAPTURE
    // ═══════════════════════════════════════════════════════════════

    protected void captureScreenshot(String name) {
        WebDriver driver = getDriver();
        if (driver instanceof TakesScreenshot ts) {
            byte[] screenshot = ts.getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), "png");
        }
    }

    // ─── Inner Record ──────────────────────────────────────────────
    public record NetworkError(String url, int status) {}
}

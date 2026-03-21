package com.guardian.utils;

import com.guardian.config.ConfigManager;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Automatic Retry for Flaky Tests.
 *
 * If a test fails, it retries up to N times (configurable via -Dretry.count=2).
 * If it passes on retry, Allure marks it as "Flaky" rather than "Failed" —
 * preventing environmental glitches from blocking the pipeline while still
 * flagging the instability for investigation.
 *
 * Usage: @Test(retryAnalyzer = RetryAnalyzer.class)
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        int maxRetries = ConfigManager.retryCount();
        if (retryCount < maxRetries) {
            retryCount++;
            System.out.println("🔄 RETRY: Re-running '" + result.getName()
                    + "' — attempt " + retryCount + "/" + maxRetries);
            return true;
        }
        return false;
    }
}

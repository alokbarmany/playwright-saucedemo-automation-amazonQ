package com.saucedemo.hooks;

import com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter;
import com.saucedemo.context.PlaywrightManager;
import com.saucedemo.dataprovider.JsonDataReader;
import com.saucedemo.reports.ExtentReportManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Hooks {

    /**
     * Order 1: Load test data and set current row using rowIndex set by TestRunner
     */
    @Before(order = 1)
    public void loadTestData(Scenario scenario) {
        PlaywrightManager.loadNextRowIndex();
        ExtentReportManager.setEnvironmentInfo();

        String env = System.getProperty("env", com.saucedemo.utils.ConfigReader.get("default.env"));
        Collection<String> tags = scenario.getSourceTagNames();

        for (String tag : tags) {
            if (tag.startsWith("@dataFile:")) {
                JsonDataReader.loadTestData(tag.substring("@dataFile:".length()).replace("{env}", env));
                List<Map<String, String>> allData = JsonDataReader.getAllTestData();
                int rowIndex = PlaywrightManager.getCurrentRowIndex();
                JsonDataReader.setCurrentRow(allData.get(rowIndex));

                ExtentReportManager.setTest(ExtentCucumberAdapter.getCurrentScenario());

                String resolvedDataFile = tag.substring("@dataFile:".length()).replace("{env}", env);
                ExtentReportManager.getTest().info("Data File: " + resolvedDataFile);
                ExtentReportManager.getTest().info(
                        "Data Row [" + (rowIndex + 1) + "/" + allData.size() + "] — " +
                                allData.get(rowIndex).toString());
                break;
            }
        }
    }

    /**
     * Order 2: Launch browser after test data is loaded
     */
    @Before(order = 2)
    public void launchBrowser() {
        PlaywrightManager.initBrowser();
        if (ExtentReportManager.getTest() != null) {
            ExtentReportManager.getTest().info(
                    "Browser launched: " + com.saucedemo.utils.ConfigReader.get("browser.name"));
        }
    }

    /**
     * Order 2: Close browser and capture screenshot on failure
     */
    @After(order = 2)
    public void closeBrowser(Scenario scenario) {
        // Take screenshot BEFORE closing browser
        if (scenario.isFailed()) {
            captureScreenshot(scenario);
        }
        PlaywrightManager.closeBrowser();
    }

    private void captureScreenshot(Scenario scenario) {
        try {
            byte[] screenshot = PlaywrightManager.getPage()
                    .screenshot(new com.microsoft.playwright.Page.ScreenshotOptions().setFullPage(true));
            String base64 = "data:image/png;base64," +
                    java.util.Base64.getEncoder().encodeToString(screenshot);
            // Attach to Cucumber scenario for Cucumber HTML report
            scenario.attach(screenshot, "image/png", "Failure Screenshot");
            // Attach to ExtentReport
            if (ExtentReportManager.getTest() != null) {
                ExtentReportManager.getTest().fail("Scenario failed: " + scenario.getName());
                ExtentReportManager.getTest().addScreenCaptureFromBase64String(base64, "Failure Screenshot");
            }
        } catch (Exception e) {
            if (ExtentReportManager.getTest() != null) {
                ExtentReportManager.getTest().warning("Screenshot unavailable: " + e.getMessage());
            }
        }
    }

    /**
     * Order 1: Clean up test data after browser is closed
     */
    @After(order = 1)
    public void cleanUpData() {
        PlaywrightManager.resetRowIndex();
        JsonDataReader.clearTestData();
        ExtentReportManager.removeTest();
    }
}

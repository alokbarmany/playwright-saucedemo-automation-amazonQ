package com.saucedemo.reports;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.service.ExtentService;
import com.saucedemo.utils.ConfigReader;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExtentReportManager {

    private static final ThreadLocal<ExtentTest> testTL      = new ThreadLocal<>();
    private static final AtomicBoolean           envInfoSet  = new AtomicBoolean(false);

    /**
     * Adds dynamic environment info to the report once per test run.
     * Called from Hooks @Before(order=1) on first scenario.
     */
    public static void setEnvironmentInfo() {
        if (envInfoSet.compareAndSet(false, true)) {
            String env = System.getProperty("env", ConfigReader.get("default.env"));
            ExtentService.getInstance().setSystemInfo("Environment",  env.toUpperCase(Locale.ROOT));
            ExtentService.getInstance().setSystemInfo("Base URL",     ConfigReader.get("base.url"));
            ExtentService.getInstance().setSystemInfo("Browser",      ConfigReader.get("browser.name"));
            ExtentService.getInstance().setSystemInfo("Headless",     ConfigReader.get("execution.headless"));
            ExtentService.getInstance().setSystemInfo("OS",           System.getProperty("os.name"));
            ExtentService.getInstance().setSystemInfo("Java Version", System.getProperty("java.version"));
            ExtentService.getInstance().setSystemInfo("User",         System.getProperty("user.name"));
        }
    }

    public static void setTest(ExtentTest test) {
        testTL.set(test);
    }

    public static ExtentTest getTest() {
        return testTL.get();
    }

    public static void removeTest() {
        testTL.remove();
    }
}

package com.saucedemo.steps;

import com.aventstack.extentreports.Status;
import com.microsoft.playwright.Page;
import com.saucedemo.context.PlaywrightManager;
import com.saucedemo.dataprovider.JsonDataReader;
import com.saucedemo.reports.ExtentReportManager;
import com.saucedemo.utils.ConfigReader;

public class BaseSteps {

    protected Page getPage() {
        return PlaywrightManager.getPage();
    }

    protected String resolve(String placeholder) {
        return JsonDataReader.resolvePlaceholder(placeholder);
    }

    protected String getBaseUrl() {
        return ConfigReader.get("base.url");
    }

    protected String getData(String key) {
        return JsonDataReader.get(key);
    }

    protected void log(String message) {
        ExtentReportManager.getTest().log(Status.INFO, message);
    }

    protected void logPass(String message) {
        ExtentReportManager.getTest().log(Status.PASS, message);
    }

    protected void logFail(String message) {
        ExtentReportManager.getTest().log(Status.FAIL, message);
    }
}

package com.saucedemo.steps;

import com.saucedemo.pages.LoginPage;
import io.cucumber.java.en.*;

import java.util.Map;

import static org.testng.Assert.*;

public class LoginSteps extends BaseSteps {

    private LoginPage loginPage() {
        return new LoginPage(getPage());
    }

    @Given("the user is on the login page")
    public void theUserIsOnTheLoginPage() {
        log("Navigating to: " + getBaseUrl());
        loginPage().navigate(getBaseUrl());
        logPass("Login page loaded successfully");
    }

    @When("the user logs in with username {string} and password {string}")
    public void theUserLogsIn(String username, String password) {
        String resolvedUsername = resolve(username);
        log("Logging in with username: " + resolvedUsername);
        loginPage().login(resolvedUsername, resolve(password));
        logPass("Login submitted for: " + resolvedUsername);
    }

    @Then("the user should be on the inventory page")
    public void theUserShouldBeOnTheInventoryPage() {
        log("Verifying inventory page is loaded");
        assertTrue(loginPage().isLoaded(), "Inventory page did not load");
        logPass("Inventory page loaded successfully");
    }

    @Then("user should see the dashboard header text {string}")
    public void user_should_see_the_dashboard_header_text(String expectedText) {
        log("Verifying dashboard header text: " + expectedText);
        assertTrue(loginPage().isLoaded(), "Dashboard not loaded");
        assertEquals(loginPage().getPageTitle(), expectedText);
        logPass("Dashboard header verified: " + expectedText);
    }

    @Then("user print address information from {string} section of the data file")
    public void user_print_address_information_from_section(String section) {
        String resolvedSection = section.replace("${", "").replace("}", "");

        Map<String, String> addressData = Map.of(
            "Street", getData(resolvedSection + ".street"),
            "City",   getData(resolvedSection + ".city"),
            "State",  getData(resolvedSection + ".state"),
            "Zip",    getData(resolvedSection + ".zip")
        );

        log("Address Information:");
        addressData.forEach((key, value) -> log("&nbsp;&nbsp;" + key + ": " + value));
        logPass("Address information printed successfully");
    }

    @Then("an error message {string} should be displayed")
    public void anErrorMessageShouldBeDisplayed(String expectedError) {
        log("Verifying error message contains: " + expectedError);
        assertTrue(loginPage().isErrorDisplayed(), "Error message not visible");
        assertTrue(loginPage().getErrorMessage().contains(expectedError));
        logPass("Error message verified: " + expectedError);
    }
}

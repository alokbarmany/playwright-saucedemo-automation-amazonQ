package com.saucedemo.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class LoginPage extends BasePage {

    // Login locators
    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator errorMessage;

    // Inventory locators
    private final Locator pageTitle;
    private final Locator inventoryItems;
    private final Locator menuButton;
    private final Locator logoutLink;

    public LoginPage(Page page) {
        super(page);
        this.usernameInput  = page.locator("#user-name");
        this.passwordInput  = page.locator("#password");
        this.loginButton    = page.locator("#login-button");
        this.errorMessage   = page.locator("[data-test='error']");
        this.pageTitle      = page.locator(".title");
        this.inventoryItems = page.locator(".inventory_item");
        this.menuButton     = page.locator("#react-burger-menu-btn");
        this.logoutLink     = page.locator("#logout_sidebar_link");
    }

    public void login(String username, String password) {
        usernameInput.fill(username);
        passwordInput.fill(password);
        loginButton.click();
        page.waitForTimeout(3000);
    }

    public String getErrorMessage() {
        return errorMessage.textContent();
    }

    public boolean isErrorDisplayed() {
        return errorMessage.isVisible();
    }

    public boolean isLoaded() {
        return pageTitle.isVisible() && pageTitle.textContent().equals("Products");
    }

    public String getPageTitle() {
        return pageTitle.textContent();
    }

    public int getInventoryItemCount() {
        return inventoryItems.count();
    }

    public void logout() {
        menuButton.click();
        logoutLink.click();
    }
}

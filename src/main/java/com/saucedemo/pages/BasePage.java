package com.saucedemo.pages;

import com.microsoft.playwright.Page;

public class BasePage {

    protected final Page page;

    public BasePage(Page page) {
        this.page = page;
    }

    public void navigate(String url) {
        page.navigate(url);
    }

    public void waitForPageLoad() {
        page.waitForLoadState();
    }

    public boolean isVisible(String selector) {
        return page.locator(selector).isVisible();
    }

    public String getText(String selector) {
        return page.locator(selector).textContent();
    }
}

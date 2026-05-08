package com.saucedemo.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class ProductsPageObjects extends BasePage {

    private final Locator productNames;
    private final Locator sortDropdown;
    private final Locator cartBadge;

    public ProductsPageObjects(Page page) {
        super(page);
        this.productNames = page.locator(".inventory_item_name");
        this.sortDropdown = page.locator(".product_sort_container");
        this.cartBadge    = page.locator(".shopping_cart_badge");
    }

    public int getProductCount() {
        return productNames.count();
    }

    public void sortProductsBy(String sortOption) {
        sortDropdown.selectOption(sortOption);
    }

    public boolean isProductDisplayed(String productName) {
        return page.locator(".inventory_item_name", new Page.LocatorOptions()
                .setHasText(productName)).isVisible();
    }

    public String getProductPrice(String productName) {
        return page.locator(".inventory_item")
                .filter(new Locator.FilterOptions().setHasText(productName))
                .locator(".inventory_item_price")
                .textContent();
    }

    public void addProductToCart(String productName) {
        page.locator(".inventory_item")
                .filter(new Locator.FilterOptions().setHasText(productName))
                .locator("button[data-test^='add-to-cart']")
                .click();
    }

    public String getCartBadgeCount() {
        return cartBadge.textContent();
    }

    public boolean isCartBadgeVisible() {
        return cartBadge.isVisible();
    }
}

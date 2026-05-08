package com.saucedemo.context;

import com.microsoft.playwright.*;
import com.saucedemo.utils.ConfigReader;

public class PlaywrightManager {

    private static final ThreadLocal<Playwright> playwrightTL = new ThreadLocal<>();
    private static final ThreadLocal<Browser> browserTL = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> contextTL = new ThreadLocal<>();
    private static final ThreadLocal<Page> pageTL = new ThreadLocal<>();
    private static final ThreadLocal<Integer> rowIndexTL = ThreadLocal.withInitial(() -> 0);
    private static final java.util.concurrent.ConcurrentLinkedQueue<Integer> rowIndexQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public static void enqueueRowIndex(int index) {
        rowIndexQueue.add(index);
    }

    public static void loadNextRowIndex() {
        Integer next = rowIndexQueue.poll();
        if (next != null)
            rowIndexTL.set(next);
    }

    public static void initBrowser() {
        Playwright playwright = Playwright.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(ConfigReader.getBoolean("execution.headless"));

        String browserName = ConfigReader.get("browser.name").toLowerCase();
        Browser browser;
        if (browserName.equals("firefox")) {
            browser = playwright.firefox().launch(options);
        } else if (browserName.equals("webkit")) {
            browser = playwright.webkit().launch(options);
        } else if (browserName.equals("safari")) {
            browser = playwright.webkit().launch(options);
        } else if ("edge".equalsIgnoreCase(browserName)) {
            browser = playwright.chromium().launch(options.setChannel("msedge"));
        } else if ("chrome".equalsIgnoreCase(browserName)) {
            browser = playwright.chromium().launch(options.setChannel("chrome"));
        } else {
            browser = playwright.chromium().launch(options);
        }

        playwrightTL.set(playwright);
        browserTL.set(browser);
        BrowserContext context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        contextTL.set(context);
        pageTL.set(context.newPage());
    }

    public static Page getPage() {
        return pageTL.get();
    }

    public static void closeBrowser() {
        if (pageTL.get() != null)
            pageTL.get().close();
        if (contextTL.get() != null)
            contextTL.get().close();
        if (browserTL.get() != null)
            browserTL.get().close();
        if (playwrightTL.get() != null)
            playwrightTL.get().close();
        pageTL.remove();
        contextTL.remove();
        browserTL.remove();
        playwrightTL.remove();
    }

    public static int getCurrentRowIndex() {
        return rowIndexTL.get();
    }

    public static void setRowIndex(int index) {
        rowIndexTL.set(index);
    }

    public static void resetRowIndex() {
        rowIndexTL.set(0);
    }
}

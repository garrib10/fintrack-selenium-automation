package dev.portfolio.fintrack.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class DashboardPage extends BasePage {

    private static final By PRIMARY_NAVIGATION =
            By.cssSelector("nav[aria-label='Primary navigation']");

    private static final By CURRENT_BALANCE =
            By.cssSelector("[data-testid='summary-current-balance']");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public DashboardPage waitUntilLoaded() {
        waitUntilVisible(PRIMARY_NAVIGATION);
        waitUntilVisible(CURRENT_BALANCE);
        return this;
    }

    public DashboardPage refresh() {
        driver.navigate().refresh();
        return waitUntilLoaded();
    }

    public boolean isPrimaryNavigationDisplayed() {
        return waitUntilVisible(PRIMARY_NAVIGATION).isDisplayed();
    }

    public boolean isCurrentBalanceDisplayed() {
        return waitUntilVisible(CURRENT_BALANCE).isDisplayed();
    }
}
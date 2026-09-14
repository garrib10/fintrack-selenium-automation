package dev.portfolio.fintrack.components;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import dev.portfolio.fintrack.pages.BasePage;
import dev.portfolio.fintrack.pages.LoginPage;

public final class AppHeader extends BasePage {

    private static final By PRIMARY_NAVIGATION =
            By.cssSelector("nav[aria-label='Primary navigation']");

    private static final By LOGOUT_BUTTON =
            By.xpath("//button[normalize-space(.)='Logout']");

    public AppHeader(WebDriver driver) {
        super(driver);
    }

    public AppHeader waitUntilAuthenticated() {
        waitUntilVisible(PRIMARY_NAVIGATION);
        waitUntilClickable(LOGOUT_BUTTON);
        return this;
    }

    public boolean isDisplayed() {
        return waitUntilVisible(PRIMARY_NAVIGATION).isDisplayed();
    }

    public LoginPage logout() {
        click(LOGOUT_BUTTON);
        return new LoginPage(driver).waitUntilLoaded();
    }
}
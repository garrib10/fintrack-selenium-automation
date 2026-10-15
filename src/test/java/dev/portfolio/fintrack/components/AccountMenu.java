package dev.portfolio.fintrack.components;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import dev.portfolio.fintrack.pages.BasePage;
import dev.portfolio.fintrack.pages.LoginPage;

/**
 * The signed-in account menu at the bottom of the navigation. Profile,
 * Account Settings and Logout exist only while the panel is open, and every
 * action is looked up inside that panel, never globally.
 */
public final class AccountMenu extends BasePage {

    private static final By TRIGGER = By.id("account-menu-trigger");

    private static final By PANEL = By.id("account-menu-panel");

    private static final By PROFILE_LINK = panelControl(
            "a", "Profile");

    private static final By ACCOUNT_SETTINGS_LINK = panelControl(
            "a", "Account Settings");

    private static final By LOGOUT_BUTTON = panelControl(
            "button", "Logout");

    public AccountMenu(WebDriver driver) {
        super(driver);
    }

    /** Opens the panel unless it is already open. */
    public AccountMenu open() {
        if (!isOpen()) {
            click(TRIGGER);
            firstVisible(PANEL);
        }

        return this;
    }

    public boolean isOpen() {
        return driver.findElements(PANEL)
                .stream()
                .anyMatch(BasePage::isDisplayedSafely);
    }

    /** The trigger's accessible label, "Open account menu for {name}" while closed. */
    public String triggerLabel() {
        return firstVisible(TRIGGER).getDomAttribute("aria-label");
    }

    public void openProfile() {
        open();
        click(PROFILE_LINK);
        waitForPage("Profile");
    }

    public void openAccountSettings() {
        open();
        click(ACCOUNT_SETTINGS_LINK);
        waitForPage("Account Settings");
    }

    public LoginPage logout() {
        open();
        click(LOGOUT_BUTTON);
        return new LoginPage(driver).waitUntilLoaded();
    }

    private static By panelControl(String tagName, String name) {
        return By.xpath(
                "//*[@id='account-menu-panel']//"
                        + tagName
                        + "[normalize-space(.)="
                        + xpathLiteral(name)
                        + "]");
    }
}

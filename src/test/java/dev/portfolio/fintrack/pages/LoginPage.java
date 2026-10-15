package dev.portfolio.fintrack.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class LoginPage extends BasePage {

    private static final String PATH = "/login";
    private static final String HEADING = "Login";
    private static final By EMAIL_INPUT = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By SUBMIT_BUTTON = By.cssSelector("form button[type='submit']");
    private static final By ERROR_ALERT = By.cssSelector("p[role='alert']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        openPath(PATH);
        return waitUntilLoaded();
    }

    public LoginPage waitUntilLoaded() {
        waitUntilPathIs(PATH);
        waitForPage(HEADING);
        waitUntilVisible(EMAIL_INPUT);
        waitUntilVisible(PASSWORD_INPUT);
        return this;
    }

    public void login(String email, String password) {
        enterText(EMAIL_INPUT, email);
        enterText(PASSWORD_INPUT, password);
        click(SUBMIT_BUTTON);
    }

    public boolean isEmailFieldDisplayed() {
        return waitUntilVisible(EMAIL_INPUT).isDisplayed();
    }

    /** The visible login error, such as "Invalid email or password". */
    public String errorMessage() {
        return waitForVisibleText(ERROR_ALERT);
    }
}

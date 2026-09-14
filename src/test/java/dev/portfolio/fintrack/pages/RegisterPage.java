package dev.portfolio.fintrack.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class RegisterPage extends BasePage {

    private static final String PATH = "/register";

    private static final By FIRST_NAME_INPUT = By.id("firstName");
    private static final By LAST_NAME_INPUT = By.id("lastName");
    private static final By EMAIL_INPUT = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");

    private static final By SUBMIT_BUTTON =
            By.cssSelector("form button[type='submit']");

    private static final By ERROR_ALERT =
            By.cssSelector("[role='alert']");

    public RegisterPage(WebDriver driver) {
        super(driver);
    }

    public RegisterPage open() {
        openPath(PATH);
        return waitUntilLoaded();
    }

    public RegisterPage waitUntilLoaded() {
        waitUntilPathIs(PATH);
        waitUntilVisible(FIRST_NAME_INPUT);
        waitUntilVisible(EMAIL_INPUT);
        return this;
    }

    public void register(
            String firstName,
            String lastName,
            String email,
            String password
    ) {
        enterText(FIRST_NAME_INPUT, firstName);
        enterText(LAST_NAME_INPUT, lastName);
        enterText(EMAIL_INPUT, email);
        enterText(PASSWORD_INPUT, password);
        click(SUBMIT_BUTTON);
    }

    public String errorMessage() {
        return readText(ERROR_ALERT);
    }

    public String emailValidationMessage() {
        return readDomProperty(EMAIL_INPUT, "validationMessage");
    }
}
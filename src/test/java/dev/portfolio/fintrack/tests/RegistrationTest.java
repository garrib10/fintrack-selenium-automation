package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.BaseTest;
import dev.portfolio.fintrack.pages.RegisterPage;

class RegistrationTest extends BaseTest {

    @Test
    void duplicateEmailDisplaysExpectedRegistrationError() {
        RegisterPage registerPage = new RegisterPage(driver).open();

        registerPage.register(
                "Selenium",
                "Duplicate",
                TestConfig.testEmail(),
                TestConfig.testPassword()
        );

        assertAll(
                () -> assertEquals(
                        "/register",
                        registerPage.currentPath(),
                        "Expected duplicate registration to remain on registration"
                ),
                () -> assertEquals(
                        "Email is already registered",
                        registerPage.errorMessage(),
                        "Expected the duplicate-email error message"
                )
        );
    }

    @Test
    void malformedEmailIsRejectedByBrowserValidation() {
        RegisterPage registerPage = new RegisterPage(driver).open();

        registerPage.register(
                "Selenium",
                "Invalid",
                "not-an-email",
                "ValidTestPassword123!"
        );

        assertAll(
                () -> assertEquals(
                        "/register",
                        registerPage.currentPath(),
                        "Expected invalid registration to remain on registration"
                ),
                () -> assertFalse(
                        registerPage.emailValidationMessage().isBlank(),
                        "Expected browser validation for the malformed email"
                )
        );
    }
}
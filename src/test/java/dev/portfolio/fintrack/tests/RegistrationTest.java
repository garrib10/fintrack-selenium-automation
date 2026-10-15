package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.components.AppShell;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.BaseTest;
import dev.portfolio.fintrack.pages.LoginPage;
import dev.portfolio.fintrack.pages.RegisterPage;

/**
 * Server-side registration rejection. Successful registration is not
 * automated because accounts cannot be deleted.
 */
@Tag("authentication")
@Tag("regression")
@Tag("read-only")
class RegistrationTest extends BaseTest {

    /**
     * Valid by FinTrack's password rules, so the request reaches the
     * duplicate-email check; never the automation account's real password.
     */
    private static final String DUPLICATE_ATTEMPT_PASSWORD =
            "Unused-duplicate-attempt-2026";

    @Test
    void duplicateEmailIsRejectedAndUserStaysSignedOut() {
        RegisterPage registerPage = new RegisterPage(driver).open();

        registerPage.register(
                "Selenium",
                "Duplicate",
                TestConfig.testEmail(),
                DUPLICATE_ATTEMPT_PASSWORD);

        String error = registerPage.errorMessage();

        assertAll(
                () -> assertEquals(
                        "Email is already registered",
                        error.replaceFirst("[.!]$", ""),
                        "Expected the duplicate-email error"),
                () -> assertEquals("/register", registerPage.currentPath(),
                        "Expected a rejected registration to stay on Register"),
                () -> assertEquals("Create Account", registerPage.visibleHeading()),
                () -> assertFalse(new AppShell(driver).isCurrentlyDisplayed(),
                        "Expected no signed-in navigation"));

        driver.get(TestConfig.baseUrl() + "/");

        LoginPage loginPage = new LoginPage(driver).waitUntilLoaded();

        assertEquals("/login", loginPage.currentPath(),
                "Expected no session after a rejected registration");
    }
}

package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.BaseTest;
import dev.portfolio.fintrack.pages.LoginPage;
import org.junit.jupiter.api.Tag;

@Tag("smoke")
@Tag("regression")
class FinTrackSmokeTest extends BaseTest {

    @Test
    void deployedApplicationLoadsLoginPage() {
        LoginPage loginPage = new LoginPage(driver).open();

        assertAll(
                () -> assertTrue(
                        loginPage.currentUrl().endsWith("/login"),
                        "Expected the browser to remain on the login route"
                ),
                () -> assertTrue(
                        loginPage.isEmailFieldDisplayed(),
                        "Expected the login email field to be visible"
                )
        );
    }
}
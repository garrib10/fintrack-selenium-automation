package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.BaseTest;
import dev.portfolio.fintrack.pages.LoginPage;
import org.junit.jupiter.api.Tag;

@Tag("authentication")
@Tag("regression")
class ProtectedRouteTest extends BaseTest {

    @ParameterizedTest(name = "Unauthenticated visit to {0} redirects to login")
    @ValueSource(strings = {
            "/",
            "/transactions",
            "/budgets"
    })
    void unauthenticatedUserIsRedirectedToLogin(String protectedPath) {
        driver.get(TestConfig.baseUrl() + protectedPath);

        LoginPage loginPage = new LoginPage(driver).waitUntilLoaded();

        assertAll(
                () -> assertEquals(
                        "/login",
                        loginPage.currentPath(),
                        "Expected protected route to redirect to login"
                ),
                () -> assertTrue(
                        loginPage.isEmailFieldDisplayed(),
                        "Expected the login form to be visible after redirect"
                )
        );
    }
}
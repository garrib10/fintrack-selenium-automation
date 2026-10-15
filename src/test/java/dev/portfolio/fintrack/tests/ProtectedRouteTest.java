package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import dev.portfolio.fintrack.components.AppShell;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.BaseTest;
import dev.portfolio.fintrack.pages.LoginPage;

/** Each execution starts in its own signed-out browser. */
@Tag("authentication")
@Tag("regression")
@Tag("smoke")
@Tag("read-only")
class ProtectedRouteTest extends BaseTest {

    @ParameterizedTest(name = "Unauthenticated visit to {0} redirects to login")
    @ValueSource(strings = {
            "/",
            "/transactions",
            "/budgets",
            "/categories"
    })
    void unauthenticatedUserIsRedirectedToLogin(String protectedPath) {
        driver.get(TestConfig.baseUrl() + protectedPath);

        LoginPage loginPage = new LoginPage(driver).waitUntilLoaded();

        assertAll(
                () -> assertEquals(
                        "/login",
                        loginPage.currentPath(),
                        "Expected protected route to redirect to login"),
                () -> assertEquals("Login", loginPage.visibleHeading()),
                () -> assertFalse(new AppShell(driver).isCurrentlyDisplayed(),
                        "Expected no signed-in navigation while signed out"));
    }
}

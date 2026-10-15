package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.components.AppShell;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.BaseTest;
import dev.portfolio.fintrack.pages.BudgetsPage;
import dev.portfolio.fintrack.pages.DashboardPage;
import dev.portfolio.fintrack.pages.LoginPage;

/**
 * Signed-in session behavior through the real Login form. Neither test
 * creates or changes application records.
 */
@Tag("authentication")
@Tag("regression")
@Tag("smoke")
@Tag("read-only")
class AuthenticationTest extends BaseTest {

    /** A well-formed address that is not an account, so failed attempts never lock the automation account. */
    private static final String UNREGISTERED_EMAIL =
            "not-a-fintrack-account@example.com";

    private static final String WRONG_PASSWORD =
            "DefinitelyWrongPassword123!";

    @Test
    void sessionPersistsAndProtectedReturnPathWorks() {
        LoginPage loginPage = new LoginPage(driver).open();

        assertEquals("Login", loginPage.visibleHeading());

        loginPage.login(
                TestConfig.testEmail(),
                TestConfig.testPassword());

        DashboardPage dashboardPage =
                new DashboardPage(driver).waitUntilLoaded();

        assertAll(
                () -> assertEquals("/", dashboardPage.currentPath(),
                        "Expected login to open the dashboard"),
                () -> assertEquals("Dashboard", dashboardPage.visibleHeading()),
                () -> assertTrue(dashboardPage.isPrimaryNavigationDisplayed(),
                        "Expected the signed-in navigation"));

        dashboardPage.refresh();

        assertAll(
                () -> assertEquals("/", dashboardPage.currentPath(),
                        "Expected refresh to keep the dashboard route"),
                () -> assertEquals("Dashboard", dashboardPage.visibleHeading(),
                        "Expected the session to survive a refresh"),
                () -> assertTrue(dashboardPage.isCurrentBalanceDisplayed(),
                        "Expected dashboard data after refresh"));

        LoginPage loggedOutPage =
                dashboardPage.appShell().accountMenu().logout();

        assertAll(
                () -> assertEquals("/login", loggedOutPage.currentPath()),
                () -> assertEquals("Login", loggedOutPage.visibleHeading()),
                () -> assertFalse(new AppShell(driver).isCurrentlyDisplayed(),
                        "Expected the signed-in navigation to be gone after logout"));

        driver.get(TestConfig.baseUrl() + "/budgets");

        LoginPage redirectedPage = new LoginPage(driver).waitUntilLoaded();

        assertAll(
                () -> assertEquals("/login", redirectedPage.currentPath(),
                        "Expected a signed-out visit to /budgets to require login"),
                () -> assertEquals("Login", redirectedPage.visibleHeading()));

        redirectedPage.login(
                TestConfig.testEmail(),
                TestConfig.testPassword());

        BudgetsPage budgetsPage = new BudgetsPage(driver).waitUntilLoaded();

        assertAll(
                () -> assertEquals("/budgets", budgetsPage.currentPath(),
                        "Expected login to return to the requested route"),
                () -> assertEquals("Budgets", budgetsPage.visibleHeading()));
    }

    @Test
    void invalidCredentialsKeepUserOnLoginWithAnError() {
        LoginPage loginPage = new LoginPage(driver).open();

        loginPage.login(UNREGISTERED_EMAIL, WRONG_PASSWORD);

        String error = loginPage.errorMessage();

        assertAll(
                () -> assertEquals(
                        "Invalid email or password",
                        error.replaceFirst("[.!]$", ""),
                        "Expected the invalid-credentials error"),
                () -> assertEquals("/login", loginPage.currentPath()),
                () -> assertEquals("Login", loginPage.visibleHeading()),
                () -> assertFalse(new AppShell(driver).isCurrentlyDisplayed(),
                        "Expected no signed-in navigation after a failed login"));
    }
}

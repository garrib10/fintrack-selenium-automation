package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.components.AppHeader;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.BaseTest;
import dev.portfolio.fintrack.pages.DashboardPage;
import dev.portfolio.fintrack.pages.LoginPage;
import org.junit.jupiter.api.Tag;

@Tag("authentication")
@Tag("regression")
class AuthenticationTest extends BaseTest {

        @Test
        @Tag("smoke")
        void registeredUserCanLoginAndRemainAuthenticatedAfterRefresh() {
                LoginPage loginPage = new LoginPage(driver).open();

                loginPage.login(
                                TestConfig.testEmail(),
                                TestConfig.testPassword());

                DashboardPage dashboardPage = new DashboardPage(driver).waitUntilLoaded();

                assertAll(
                                () -> assertEquals(
                                                "/",
                                                dashboardPage.currentPath(),
                                                "Expected successful login to open the dashboard"),
                                () -> assertTrue(
                                                dashboardPage.isPrimaryNavigationDisplayed(),
                                                "Expected authenticated navigation to be visible"),
                                () -> assertTrue(
                                                dashboardPage.isCurrentBalanceDisplayed(),
                                                "Expected the dashboard balance to be visible"));

                dashboardPage.refresh();

                assertAll(
                                () -> assertEquals(
                                                "/",
                                                dashboardPage.currentPath(),
                                                "Expected refresh to preserve the dashboard route"),
                                () -> assertTrue(
                                                dashboardPage.isPrimaryNavigationDisplayed(),
                                                "Expected the authenticated session to survive refresh"));
        }

        @Test
        void invalidPasswordKeepsUserOnLoginPageAndDisplaysError() {
                LoginPage loginPage = new LoginPage(driver).open();

                loginPage.login(
                                TestConfig.testEmail(),
                                "DefinitelyWrongPassword123!");

                assertAll(
                                () -> assertEquals(
                                                "/login",
                                                loginPage.currentPath(),
                                                "Expected invalid credentials to remain on the login route"),
                                () -> assertEquals(
                                                "Invalid email or password",
                                                loginPage.errorMessage(),
                                                "Expected the invalid-credentials error message"));
        }

        @Test
        void logoutEndsSessionAndRestoresProtectedRouteBehavior() {
                LoginPage loginPage = new LoginPage(driver).open();

                loginPage.login(
                                TestConfig.testEmail(),
                                TestConfig.testPassword());

                DashboardPage dashboardPage = new DashboardPage(driver).waitUntilLoaded();

                AppHeader appHeader = new AppHeader(driver).waitUntilAuthenticated();

                assertAll(
                                () -> assertEquals(
                                                "/",
                                                dashboardPage.currentPath(),
                                                "Expected successful login to open the dashboard"),
                                () -> assertTrue(
                                                appHeader.isDisplayed(),
                                                "Expected authenticated navigation before logout"));

                LoginPage loggedOutLoginPage = appHeader.logout();

                assertAll(
                                () -> assertEquals(
                                                "/login",
                                                loggedOutLoginPage.currentPath(),
                                                "Expected logout to return the user to login"),
                                () -> assertTrue(
                                                loggedOutLoginPage.isEmailFieldDisplayed(),
                                                "Expected the login form after logout"));

                driver.get(TestConfig.baseUrl() + "/transactions");
                loggedOutLoginPage.waitUntilLoaded();

                assertEquals(
                                "/login",
                                loggedOutLoginPage.currentPath(),
                                "Expected the logged-out user to remain blocked from protected routes");
        }
}
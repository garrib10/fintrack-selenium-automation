package dev.portfolio.fintrack.core;

import org.junit.jupiter.api.BeforeEach;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.pages.DashboardPage;
import dev.portfolio.fintrack.pages.LoginPage;

public abstract class AuthenticatedTest extends BaseTest {

    protected DashboardPage dashboardPage;

    @BeforeEach
    protected void logInAsTestUser() {
        LoginPage loginPage = new LoginPage(driver).open();

        loginPage.login(
                TestConfig.testEmail(),
                TestConfig.testPassword()
        );

        dashboardPage = new DashboardPage(driver).waitUntilLoaded();
    }
}
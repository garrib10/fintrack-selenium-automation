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
import dev.portfolio.fintrack.core.DriverFactory;
import dev.portfolio.fintrack.pages.DashboardPage;
import dev.portfolio.fintrack.pages.LoginPage;
import dev.portfolio.fintrack.pages.TransactionsPage;

/**
 * Optional: navigation and logout in the phone layout (390×844), using only
 * the mobile drawer dialog. Signs in and out; changes no records.
 */
@Tag("authentication")
@Tag("extended")
@Tag("read-only")
class MobileNavigationTest extends BaseTest {

    @Test
    void mobileDrawerNavigatesAndLogsOut() {
        DriverFactory.emulateMobileViewport(driver, 390, 844);

        LoginPage loginPage = new LoginPage(driver).open();
        loginPage.login(TestConfig.testEmail(), TestConfig.testPassword());

        DashboardPage dashboardPage = new DashboardPage(driver).waitUntilLoaded();
        AppShell appShell = dashboardPage.appShell();

        assertAll(
                () -> assertTrue(appShell.isMobileLayout(),
                        "Expected the menu button and no sidebar at 390px"),
                () -> assertFalse(appShell.isMobileNavigationOpen(),
                        "Expected the drawer to start closed"));

        appShell.openMobileNavigation();

        assertTrue(appShell.isMobileNavigationOpen(), "Expected the drawer to open");

        TransactionsPage transactionsPage = appShell.openTransactions();

        assertAll(
                () -> assertEquals("/transactions", transactionsPage.currentPath()),
                () -> assertEquals("Transactions", transactionsPage.visibleHeading()),
                () -> assertFalse(appShell.isMobileNavigationOpen(),
                        "Expected following a link to close the drawer"));

        appShell.openMobileNavigation();

        assertTrue(appShell.isAccountMenuInMobileNavigation(),
                "Expected the account menu inside the drawer");

        LoginPage loggedOutPage = appShell.accountMenu().logout();

        assertAll(
                () -> assertEquals("/login", loggedOutPage.currentPath()),
                () -> assertEquals("Login", loggedOutPage.visibleHeading()),
                () -> assertFalse(appShell.isCurrentlyDisplayed(),
                        "Expected no signed-in shell after logout"));
    }
}

package dev.portfolio.fintrack.core;

import java.time.YearMonth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.CleanupRegistry.Bucket;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.BudgetsPage;
import dev.portfolio.fintrack.pages.CategoriesPage;
import dev.portfolio.fintrack.pages.DashboardPage;
import dev.portfolio.fintrack.pages.LoginPage;
import dev.portfolio.fintrack.pages.TransactionsPage;

/**
 * A signed-in test. Register cleanup immediately after creating a record; it
 * runs in {@link #runRegisteredCleanup()}, which JUnit calls after
 * {@link FailureEvidenceExtension} has captured a failure and before
 * {@link BaseTest} quits the browser.
 */
public abstract class AuthenticatedTest extends BaseTest {

    protected DashboardPage dashboardPage;

    private final CleanupRegistry cleanupRegistry = new CleanupRegistry();

    @BeforeEach
    protected void logInAsTestUser() {
        LoginPage loginPage = new LoginPage(driver).open();

        loginPage.login(
                TestConfig.testEmail(),
                TestConfig.testPassword()
        );

        dashboardPage = new DashboardPage(driver).waitUntilLoaded();
    }

    /*
     * Named differently from BaseTest.tearDownDriver so both run: JUnit runs
     * subclass @AfterEach methods before superclass ones, so cleanup still
     * has a browser.
     */
    @AfterEach
    protected void runRegisteredCleanup() {
        if (driver == null) {
            cleanupRegistry.clear();
            return;
        }

        cleanupRegistry.runAll();
    }

    /** Deletes this run's transaction after the test, wherever the test stopped. */
    protected void cleanUpTransactionAfterTest(String description) {
        requireCurrentRunRecord(description);

        cleanupRegistry.register(
                Bucket.TRANSACTIONS,
                "transaction " + description,
                () -> {
                    restoreSignedInSession();
                    new TransactionsPage(driver)
                            .open()
                            .deleteIfPresent(description);
                });
    }

    /**
     * Deletes a budget after the test. Only the configured automation budget
     * category or a category created by this run may be cleaned up.
     */
    protected void cleanUpBudgetAfterTest(
            String category,
            YearMonth period) {

        if (!category.equals(TestConfig.testBudgetCategory())
                && !TestData.belongsToCurrentRun(category)) {
            throw new IllegalArgumentException(
                    "Budget cleanup is limited to the automation budget"
                            + " category and this run's categories");
        }

        cleanupRegistry.register(
                Bucket.BUDGETS,
                "budget " + category + " " + period,
                () -> {
                    restoreSignedInSession();
                    new BudgetsPage(driver)
                            .open()
                            .deleteIfPresent(category, period);
                });
    }

    /** Deletes this run's custom category after its transactions and budgets. */
    protected void cleanUpCategoryAfterTest(String name) {
        requireCurrentRunRecord(name);

        cleanupRegistry.register(
                Bucket.CATEGORIES,
                "category " + name,
                () -> {
                    restoreSignedInSession();
                    new CategoriesPage(driver)
                            .open()
                            .deleteIfPresent(name);
                });
    }

    /** A failed test may have logged out or left any page open; start from a known state. */
    private void restoreSignedInSession() {
        if (!new DashboardPage(driver).openIfSignedIn()) {
            logInAsTestUser();
        }
    }

    private static void requireCurrentRunRecord(String name) {
        if (!TestData.belongsToCurrentRun(name)) {
            throw new IllegalArgumentException(
                    "Cleanup is limited to records created by this run: "
                            + name);
        }
    }
}

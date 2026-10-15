package dev.portfolio.fintrack.core;

import java.time.YearMonth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.CleanupRegistry.Bucket;
import dev.portfolio.fintrack.core.CleanupRegistry.Registration;
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

    /**
     * Deletes this run's transaction after the test, wherever the test stopped.
     * Cleanup matches by exact description, so register the new description
     * before saving a rename and cancel the old registration once it is saved.
     */
    protected Registration cleanUpTransactionAfterTest(String description) {
        requireCurrentRunRecord(description);

        return cleanupRegistry.register(
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
    protected Registration cleanUpBudgetAfterTest(
            String category,
            YearMonth period) {

        if (!category.equals(TestConfig.testBudgetCategory())
                && !TestData.belongsToCurrentRun(category)) {
            throw new IllegalArgumentException(
                    "Budget cleanup is limited to the automation budget"
                            + " category and this run's categories");
        }

        return cleanupRegistry.register(
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
    protected Registration cleanUpCategoryAfterTest(String name) {
        requireCurrentRunRecord(name);

        return cleanupRegistry.register(
                Bucket.CATEGORIES,
                "category " + name,
                () -> {
                    restoreSignedInSession();
                    new CategoriesPage(driver)
                            .open()
                            .deleteIfPresent(name);
                });
    }

    /**
     * Cancels registered cleanup the test no longer needs: the test deleted the
     * record itself, or a rename was saved and the old name no longer exists.
     */
    protected void cleanupNoLongerNeeded(Registration registration) {
        cleanupRegistry.unregister(registration);
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

package dev.portfolio.fintrack.core;

import java.time.YearMonth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.CleanupRegistry.Bucket;
import dev.portfolio.fintrack.core.CleanupRegistry.Registration;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.AccountSettingsPage;
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

    /** Restores the "Transactions per page" preference recorded before the test changed it. */
    protected Registration restorePageSizeAfterTest(int originalPageSize) {
        return cleanupRegistry.register(
                Bucket.ACCOUNT_SETTINGS,
                "transactions per page " + originalPageSize,
                () -> {
                    restoreSignedInSession();
                    AccountSettingsPage settings = new AccountSettingsPage(driver).open();

                    if (settings.transactionPageSize() != originalPageSize) {
                        settings.saveTransactionPageSize(originalPageSize);
                    }
                });
    }

    /**
     * Removes the profile photo after the test. Register it only after
     * confirming the account had no photo, so a photo the test did not
     * upload is never removed.
     */
    protected Registration removeProfilePhotoAfterTest() {
        return cleanupRegistry.register(
                Bucket.ACCOUNT_SETTINGS,
                "profile photo",
                () -> {
                    restoreSignedInSession();
                    AccountSettingsPage settings = new AccountSettingsPage(driver).open();

                    if (settings.hasPhoto()) {
                        settings.removePhoto();
                    }
                });
    }

    /*
     * Budget periods reserved for automation on the dedicated account, all in
     * the automation budget category (FINTRACK_TEST_BUDGET_CATEGORY, Travel):
     *
     *   current month     Budget-to-Dashboard integration
     *   next month        budget create, edit and delete
     *   month after next  duplicate-budget check (staging only)
     *
     * Each test uses its own period, so serial runs never collide.
     */

    protected static YearMonth dashboardBudgetPeriod() {
        return YearMonth.now();
    }

    protected static YearMonth crudBudgetPeriod() {
        return YearMonth.now().plusMonths(1);
    }

    protected static YearMonth duplicateBudgetPeriod() {
        return YearMonth.now().plusMonths(2);
    }

    /**
     * Opens Budgets on a reserved period and fails if a budget already exists
     * there. A budget cannot carry this run's ID, so one found here was not
     * created by this run and is never deleted automatically; remove it by
     * hand before rerunning.
     */
    protected BudgetsPage requireFreeBudgetPeriod(
            String category,
            YearMonth period) {

        BudgetsPage budgetsPage = new BudgetsPage(driver)
                .open()
                .selectPeriod(period);

        if (budgetsPage.isBudgetPresent(category, period)) {
            throw new IllegalStateException(
                    "A "
                            + category
                            + " budget already exists for "
                            + period
                            + ". This run did not create it, so it was left"
                            + " untouched; remove it by hand and rerun.");
        }

        return budgetsPage;
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

package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.BudgetsPage;
import dev.portfolio.fintrack.pages.DashboardPage;
import dev.portfolio.fintrack.pages.DashboardPage.DashboardBudget;
import dev.portfolio.fintrack.pages.DashboardPage.DashboardSummary;
import dev.portfolio.fintrack.pages.TransactionsPage;

/**
 * A current-month expense in the automation budget category must move the
 * Dashboard totals and that category's Monthly Budgets card. Assertions
 * compare before-and-after values, so other account data does not matter.
 */
@Tag("budgets")
@Tag("dashboard")
@Tag("integration")
@Tag("regression")
@Tag("mutating")
class BudgetDashboardIntegrationTest extends AuthenticatedTest {

    private static final BigDecimal BUDGET_LIMIT =
            new BigDecimal("200.00");

    private static final BigDecimal EXPENSE_AMOUNT =
            new BigDecimal("40.00");

    @Test
    void currentMonthExpenseUpdatesDashboardAndBudgetProgress() {
        String category = TestConfig.testBudgetCategory();
        YearMonth currentPeriod = dashboardBudgetPeriod();

        // Today, never a later day, so the expense always counts this month.
        LocalDate transactionDate = LocalDate.now();

        String description =
                TestData.transactionDescription("dashboard-expense");

        BudgetsPage budgetsPage =
                requireFreeBudgetPeriod(category, currentPeriod);

        // Only after confirming the period was free, so cleanup can never
        // remove a budget this run did not create.
        cleanUpBudgetAfterTest(category, currentPeriod);
        budgetsPage.createBudget(
                category,
                BUDGET_LIMIT.toPlainString(),
                currentPeriod);

        DashboardPage dashboardPage =
                new DashboardPage(driver).open();

        DashboardSummary baselineSummary =
                dashboardPage.summary();

        DashboardBudget baselineBudget =
                dashboardPage.budget(category);

        cleanUpTransactionAfterTest(description);
        new TransactionsPage(driver)
                .open()
                .addTransaction(
                        "EXPENSE",
                        category,
                        EXPENSE_AMOUNT.toPlainString(),
                        description,
                        transactionDate);

        dashboardPage.open();

        DashboardSummary updatedSummary =
                dashboardPage.summary();

        DashboardBudget updatedBudget =
                dashboardPage.budget(category);

        BigDecimal expectedUtilizationIncrease =
                EXPENSE_AMOUNT
                        .multiply(new BigDecimal("100"))
                        .divide(BUDGET_LIMIT, 1, RoundingMode.HALF_UP);

        assertAll(
                () -> assertMoneyEquals(
                        baselineSummary.totalExpenses().add(EXPENSE_AMOUNT),
                        updatedSummary.totalExpenses(),
                        "Expected total expenses to increase"),
                () -> assertMoneyEquals(
                        baselineSummary.monthlyExpenses().add(EXPENSE_AMOUNT),
                        updatedSummary.monthlyExpenses(),
                        "Expected monthly expenses to increase"),
                () -> assertMoneyEquals(
                        baselineSummary.currentBalance().subtract(EXPENSE_AMOUNT),
                        updatedSummary.currentBalance(),
                        "Expected current balance to decrease"),
                () -> assertMoneyEquals(
                        BUDGET_LIMIT,
                        updatedBudget.monthlyLimit(),
                        "Expected the card to show this test's budget"),
                () -> assertMoneyEquals(
                        baselineBudget.spent().add(EXPENSE_AMOUNT),
                        updatedBudget.spent(),
                        "Expected budget spending to increase"),
                () -> assertMoneyEquals(
                        baselineBudget.remaining().subtract(EXPENSE_AMOUNT),
                        updatedBudget.remaining(),
                        "Expected the remaining budget to decrease"),
                () -> assertNumberEquals(
                        baselineBudget.usedPercentage().add(expectedUtilizationIncrease),
                        updatedBudget.usedPercentage(),
                        "Expected budget utilization to increase"));

        BigDecimal used = updatedBudget.usedPercentage();
        BigDecimal clampedUsed = used.max(BigDecimal.ZERO).min(new BigDecimal("100"));

        assertAll(
                () -> assertEquals(
                        category + " budget utilization",
                        updatedBudget.progressAccessibleName(),
                        "Expected the progress bar to be named for its category"),
                () -> assertEquals("0", updatedBudget.progressValueMin()),
                () -> assertEquals("100", updatedBudget.progressValueMax()),
                () -> assertNumberEquals(
                        clampedUsed,
                        new BigDecimal(updatedBudget.progressValueNow()),
                        "Expected aria-valuenow to match the Used figure"),
                () -> assertEquals(
                        used.setScale(1, RoundingMode.HALF_UP).toPlainString() + "% used",
                        updatedBudget.progressValueText(),
                        "Expected accessible progress text"));
    }

    private static void assertMoneyEquals(
            BigDecimal expected,
            BigDecimal actual,
            String message) {

        assertEquals(
                0,
                expected.compareTo(actual),
                message + ": expected " + expected + " but was " + actual);
    }

    /** The card shows one decimal; aria-valuenow may carry more. */
    private static void assertNumberEquals(
            BigDecimal expected,
            BigDecimal actual,
            String message) {

        assertEquals(
                0,
                expected.setScale(1, RoundingMode.HALF_UP)
                        .compareTo(actual.setScale(1, RoundingMode.HALF_UP)),
                message + ": expected " + expected + " but was " + actual);
    }
}

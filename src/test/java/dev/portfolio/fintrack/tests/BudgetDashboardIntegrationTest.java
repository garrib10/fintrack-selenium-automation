package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.pages.BudgetsPage;
import dev.portfolio.fintrack.pages.DashboardPage;
import dev.portfolio.fintrack.pages.DashboardPage.DashboardBudget;
import dev.portfolio.fintrack.pages.DashboardPage.DashboardSummary;
import dev.portfolio.fintrack.pages.TransactionsPage;
import org.junit.jupiter.api.Tag;

@Tag("budgets")
@Tag("dashboard")
@Tag("integration")
@Tag("regression")
class BudgetDashboardIntegrationTest extends AuthenticatedTest {

    private static final BigDecimal BUDGET_LIMIT =
            new BigDecimal("200.00");

    private static final BigDecimal EXPENSE_AMOUNT =
            new BigDecimal("40.00");

    @Test
    void currentMonthExpenseUpdatesDashboardAndBudgetProgress() {
        String category =
                TestConfig.testBudgetCategory();

        YearMonth currentPeriod =
                YearMonth.now();

        LocalDate transactionDate =
                currentPeriod.atDay(15);

        String description =
                "selenium-dashboard-expense-"
                        + UUID.randomUUID();

        BudgetsPage budgetsPage =
                new BudgetsPage(driver).open();

        /*
         * Travel is reserved for this suite. Remove only a stale
         * automation budget from an interrupted previous run.
         */
        budgetsPage.deleteIfPresent(
                category,
                currentPeriod);

        try {
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

            new TransactionsPage(driver)
                    .open()
                    .addTransactionWithCategory(
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

            assertMoneyEquals(
                    baselineSummary.totalExpenses()
                            .add(EXPENSE_AMOUNT),
                    updatedSummary.totalExpenses(),
                    "Expected total expenses to increase");

            assertMoneyEquals(
                    baselineSummary.monthlyExpenses()
                            .add(EXPENSE_AMOUNT),
                    updatedSummary.monthlyExpenses(),
                    "Expected monthly expenses to increase");

            assertMoneyEquals(
                    baselineSummary.currentBalance()
                            .subtract(EXPENSE_AMOUNT),
                    updatedSummary.currentBalance(),
                    "Expected current balance to decrease");

            assertMoneyEquals(
                    baselineBudget.spent()
                            .add(EXPENSE_AMOUNT),
                    updatedBudget.spent(),
                    "Expected Travel budget spending to increase");

            BigDecimal expectedUtilizationIncrease =
                    EXPENSE_AMOUNT
                            .multiply(new BigDecimal("100"))
                            .divide(BUDGET_LIMIT);

            assertPercentageEquals(
                    baselineBudget.usedPercentage()
                            .add(expectedUtilizationIncrease),
                    updatedBudget.usedPercentage(),
                    "Expected budget utilization to increase");

            assertEquals(
                    category + " budget utilization",
                    category
                            + " budget utilization",
                    "Expected progress semantics for the category");

            assertEquals(
                    updatedBudget.usedPercentage()
                            .toPlainString()
                            + "% used",
                    updatedBudget.progressValueText(),
                    "Expected accessible progress text");
        } finally {
            /*
             * A budget does not delete its transactions, so remove
             * the transaction first and the budget second.
             */
            try {
                new TransactionsPage(driver)
                        .open()
                        .deleteIfPresent(description);
            } finally {
                new BudgetsPage(driver)
                        .open()
                        .deleteIfPresent(
                                category,
                                currentPeriod);
            }
        }
    }

    private static void assertMoneyEquals(
            BigDecimal expected,
            BigDecimal actual,
            String message) {

        assertEquals(
                0,
                expected.compareTo(actual),
                message);
    }

    private static void assertPercentageEquals(
            BigDecimal expected,
            BigDecimal actual,
            String message) {

        assertEquals(
                0,
                expected.compareTo(actual),
                message);
    }
}
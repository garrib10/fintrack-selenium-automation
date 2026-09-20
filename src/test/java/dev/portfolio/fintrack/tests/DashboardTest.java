package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.pages.DashboardPage;
import dev.portfolio.fintrack.pages.DashboardPage.DashboardSummary;
import org.junit.jupiter.api.Tag;


@Tag("dashboard")
@Tag("smoke")
@Tag("regression")
class DashboardTest extends AuthenticatedTest {

    @Test
    void dashboardDisplaysConsistentFinancialSummary() {
        DashboardPage dashboardPage =
                new DashboardPage(driver).open();

        DashboardSummary summary =
                dashboardPage.summary();

        assertTrue(
                dashboardPage.areAllSummaryValuesDisplayed(),
                "Expected all five financial summary values");

        BigDecimal expectedBalance =
                summary.totalIncome()
                        .subtract(summary.totalExpenses());

        assertMoneyEquals(
                expectedBalance,
                summary.currentBalance(),
                "Expected balance to equal income minus expenses");

        assertTrue(
                summary.monthlyIncome()
                        .compareTo(BigDecimal.ZERO) >= 0,
                "Expected monthly income to be non-negative");

        assertTrue(
                summary.monthlyExpenses()
                        .compareTo(BigDecimal.ZERO) >= 0,
                "Expected monthly expenses to be non-negative");
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
}
package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.BudgetsPage;
import dev.portfolio.fintrack.pages.CategoriesPage;
import dev.portfolio.fintrack.pages.TransactionsPage;

/**
 * Manual maintenance: removes records left by interrupted runs. It needs both
 * the maintenance tag (excluded by default) and FINTRACK_RUN_MAINTENANCE=true.
 *
 * <p>Only names this suite writes are deleted: transactions matching
 * {@code selenium-<runId>-…} or the v1.0.0 {@code selenium-…-<UUID>} format,
 * then categories named exactly {@code Sel <runId>}. Budgets cannot carry a
 * run ID, so Travel budgets in the reserved months are reported, never
 * deleted. Built-in categories are never touched.
 */
@Tag("maintenance")
@Tag("mutating")
class MaintenanceSweepTest extends AuthenticatedTest {

    /** Each pass clears at most one page of results. */
    private static final int MAX_TRANSACTION_PASSES = 20;

    @Test
    void removeRecordsLeftByEarlierRuns() {
        assumeTrue(
                TestConfig.maintenanceEnabled(),
                "Set FINTRACK_RUN_MAINTENANCE=true to run the maintenance sweep");

        List<String> problems = new ArrayList<>();

        int deletedTransactions = sweepTransactions(problems);
        int deletedCategories = sweepCategories(problems);
        List<String> reservedBudgets = reportReservedBudgets();

        System.out.println("Maintenance sweep: deleted "
                + deletedTransactions + " transaction(s) and "
                + deletedCategories + " category(ies); Travel budgets left in place for "
                + (reservedBudgets.isEmpty() ? "no reserved month" : reservedBudgets));

        assertTrue(problems.isEmpty(), "Maintenance sweep problems: " + problems);
    }

    private int sweepTransactions(List<String> problems) {
        TransactionsPage transactionsPage = new TransactionsPage(driver).open();
        int deleted = 0;

        for (int pass = 0; pass < MAX_TRANSACTION_PASSES; pass++) {
            List<String> owned = transactionsPage
                    .searchByDescription("selenium-")
                    .visibleTransactionDescriptions()
                    .stream()
                    .filter(TestData::isSeleniumTransaction)
                    .distinct()
                    .toList();

            if (owned.isEmpty()) {
                return deleted;
            }

            for (String description : owned) {
                try {
                    if (transactionsPage.deleteIfPresent(description)) {
                        deleted++;
                    }
                } catch (RuntimeException failure) {
                    problems.add("transaction " + description + ": " + failure.getMessage());
                    return deleted;
                }
            }
        }

        problems.add("Selenium transactions remained after "
                + MAX_TRANSACTION_PASSES + " passes");
        return deleted;
    }

    private int sweepCategories(List<String> problems) {
        CategoriesPage categoriesPage = new CategoriesPage(driver).open();
        int deleted = 0;

        List<String> owned = categoriesPage.categoryNamesMatching("Sel ")
                .stream()
                .filter(TestData::isSeleniumCategory)
                .distinct()
                .toList();

        for (String name : owned) {
            try {
                if (categoriesPage.isDeleteDisabled(name)) {
                    problems.add("category " + name + " is still in use: "
                            + categoriesPage.deleteBlockedReason(name));
                } else if (categoriesPage.deleteIfPresent(name)) {
                    deleted++;
                }
            } catch (RuntimeException failure) {
                problems.add("category " + name + ": " + failure.getMessage());
            }
        }

        return deleted;
    }

    private List<String> reportReservedBudgets() {
        String category = TestConfig.testBudgetCategory();
        BudgetsPage budgetsPage = new BudgetsPage(driver).open();
        List<String> present = new ArrayList<>();

        for (YearMonth period : List.of(
                dashboardBudgetPeriod(),
                crudBudgetPeriod(),
                duplicateBudgetPeriod())) {

            if (budgetsPage.selectPeriod(period).isBudgetPresent(category, period)) {
                present.add(period.toString());
            }
        }

        return present;
    }
}

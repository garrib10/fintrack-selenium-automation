package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.pages.BudgetsPage;
import org.junit.jupiter.api.Tag;

@Tag("budgets")
@Tag("regression")
class BudgetValidationTest extends AuthenticatedTest {

    @Test
    void missingCategoryIsRejected() {
        YearMonth period = YearMonth.now().plusMonths(1);

        BudgetsPage budgetsPage = new BudgetsPage(driver)
                .open()
                .fillBudgetFormWithoutCategory(
                        "250.00",
                        period)
                .submitCreate();

        assertEquals(
                "Please select a category.",
                budgetsPage.categoryErrorMessage());
    }

    @Test
    void nonPositiveMonthlyLimitIsRejected() {
        String category = TestConfig.testBudgetCategory();
        YearMonth period = YearMonth.now().plusMonths(1);

        BudgetsPage budgetsPage = new BudgetsPage(driver)
                .open()
                .fillBudgetForm(
                        category,
                        "0",
                        period)
                .submitCreate();

        assertFalse(
                budgetsPage.monthlyLimitValidationMessage()
                        .isBlank(),
                "Expected browser validation to reject a zero limit");
    }

    @Test
    void duplicateBudgetIsRejected() {
        String category = TestConfig.testBudgetCategory();
        YearMonth period = YearMonth.now().plusMonths(2);

        BudgetsPage budgetsPage = new BudgetsPage(driver).open();

        boolean budgetExists = false;

        try {
            budgetsPage.deleteIfPresent(category, period);

            budgetsPage.createBudget(
                    category,
                    "200.00",
                    period);

            budgetExists = true;

            budgetsPage
                    .fillBudgetForm(
                            category,
                            "300.00",
                            period)
                    .submitCreate();

            assertEquals(
                    "Budget already exists for this category and month",
                    budgetsPage.submissionErrorMessage());
        } finally {
            if (budgetExists) {
                budgetsPage.deleteIfPresent(
                        category,
                        period);
            }
        }
    }
}
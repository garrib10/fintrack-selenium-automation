package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.pages.BudgetsPage;
import dev.portfolio.fintrack.pages.BudgetsPage.BudgetCard;

class BudgetTest extends AuthenticatedTest {

    @Test
    void userCanCreateEditCancelUpdateAndDeleteBudget() {
        String category = TestConfig.testBudgetCategory();
        YearMonth period = YearMonth.now().plusMonths(1);

        BudgetsPage budgetsPage = new BudgetsPage(driver).open();

        boolean budgetExists = false;

        try {
            budgetsPage.deleteIfPresent(category, period);

            budgetsPage.createBudget(
                    category,
                    "250.00",
                    period);

            budgetExists = true;

            BudgetCard createdBudget =
                    budgetsPage.budget(category, period);

            assertAll(
                    () -> assertEquals(
                            category,
                            createdBudget.category(),
                            "Expected the created budget category"),
                    () -> assertEquals(
                            "$250.00",
                            createdBudget.monthlyLimit(),
                            "Expected the created monthly limit"));

            budgetsPage
                    .startEditing(category, period)
                    .changeMonthlyLimitDuringEdit("275.00")
                    .cancelEdit();

            BudgetCard afterCancel =
                    budgetsPage.budget(category, period);

            assertEquals(
                    "$250.00",
                    afterCancel.monthlyLimit(),
                    "Cancel should not persist the edited limit");

            budgetsPage
                    .startEditing(category, period)
                    .updateBudget(
                            category,
                            "325.00",
                            period);

            BudgetCard updatedBudget =
                    budgetsPage.budget(category, period);

            assertEquals(
                    "$325.00",
                    updatedBudget.monthlyLimit(),
                    "Expected the updated monthly limit");

            String confirmation =
                    budgetsPage.deleteBudget(
                            category,
                            period);

            budgetExists = false;

            assertAll(
                    () -> assertEquals(
                            "Delete the " + category + " budget?",
                            confirmation,
                            "Expected the correct deletion confirmation"),
                    () -> assertFalse(
                            budgetsPage.isBudgetPresent(
                                    category,
                                    period),
                            "Expected the deleted budget to disappear"));
        } finally {
            if (budgetExists) {
                budgetsPage.deleteIfPresent(
                        category,
                        period);
            }
        }
    }
}
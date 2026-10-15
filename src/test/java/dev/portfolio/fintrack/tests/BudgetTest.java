package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.time.YearMonth;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.core.CleanupRegistry.Registration;
import dev.portfolio.fintrack.pages.BudgetsPage;
import dev.portfolio.fintrack.pages.BudgetsPage.BudgetCard;
import dev.portfolio.fintrack.pages.DeletionResult;

/**
 * One budget lifecycle in the automation category's reserved CRUD period
 * (next month). The test fails rather than deleting a budget it did not
 * create.
 */
@Tag("budgets")
@Tag("regression")
@Tag("mutating")
class BudgetTest extends AuthenticatedTest {

    @Test
    void userCanCreateEditAndDeleteBudget() {
        String category = TestConfig.testBudgetCategory();
        YearMonth period = crudBudgetPeriod();

        BudgetsPage budgetsPage = requireFreeBudgetPeriod(category, period);

        // Registered before saving: the period was free, so the only budget
        // cleanup could find is the one this test creates.
        Registration budgetCleanup = cleanUpBudgetAfterTest(category, period);

        String createdStatus = budgetsPage.createBudget(category, "250.00", period);
        BudgetCard created = budgetsPage.budget(category, period);

        assertAll(
                () -> assertEquals(BudgetsPage.CREATED_MESSAGE, createdStatus),
                () -> assertEquals(category, created.category()),
                () -> assertEquals("$250.00", created.monthlyLimit()));

        String updatedStatus = budgetsPage
                .startEditing(category, period)
                .updateBudget(category, "325.00", period);

        BudgetCard updated = budgetsPage.budget(category, period);

        assertAll(
                () -> assertEquals(BudgetsPage.UPDATED_MESSAGE, updatedStatus),
                () -> assertEquals("$325.00", updated.monthlyLimit()));

        DeletionResult deletion = budgetsPage.deleteBudget(category, period);

        assertAll(
                () -> assertEquals(
                        "Delete the " + category + " budget?",
                        deletion.confirmation()),
                () -> assertEquals(BudgetsPage.DELETED_MESSAGE, deletion.statusMessage()),
                () -> assertFalse(
                        budgetsPage.selectPeriod(period).isBudgetPresent(category, period),
                        "Expected the deleted budget to be gone"));

        cleanupNoLongerNeeded(budgetCleanup);
    }
}

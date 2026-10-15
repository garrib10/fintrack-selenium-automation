package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import java.time.YearMonth;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.pages.BudgetsPage;

/**
 * The server-side duplicate-budget rule. Staging or local only: it is
 * excluded from the default suite through the staging-only tag and skips
 * itself if pointed at production anyway.
 */
@Tag("budgets")
@Tag("staging-only")
@Tag("mutating")
class BudgetValidationTest extends AuthenticatedTest {

    @Test
    void duplicateBudgetForTheSamePeriodIsRejected() {
        assumeFalse(
                TestConfig.isProduction(),
                "Duplicate-budget coverage never runs against production");

        String category = TestConfig.testBudgetCategory();
        YearMonth period = duplicateBudgetPeriod();

        BudgetsPage budgetsPage = requireFreeBudgetPeriod(category, period);

        cleanUpBudgetAfterTest(category, period);
        budgetsPage.createBudget(category, "200.00", period);

        budgetsPage
                .fillBudgetForm(category, "300.00", period)
                .submitCreate();

        assertEquals(
                "Budget already exists for this category and month",
                budgetsPage.submissionErrorMessage());
    }
}

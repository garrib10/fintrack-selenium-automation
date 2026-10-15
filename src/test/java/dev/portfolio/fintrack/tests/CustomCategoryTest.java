package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.core.CleanupRegistry.Registration;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.CategoriesPage;
import dev.portfolio.fintrack.pages.DeletionResult;
import dev.portfolio.fintrack.pages.TransactionsPage;

/**
 * A custom category from inline creation to deletion: in-use protection,
 * the Categories-to-Transactions deep link, and filter persistence on
 * reload. Only this run's category and transaction are touched.
 */
@Tag("categories")
@Tag("transactions")
@Tag("regression")
class CustomCategoryTest extends AuthenticatedTest {

    @Test
    void customCategoryLifecycleFromTransactionToDeletion() {
        String categoryName = TestData.categoryName();
        String description = TestData.transactionDescription("custom-category");
        LocalDate date = LocalDate.now();

        TransactionsPage transactionsPage = new TransactionsPage(driver).open();

        // Both registered before saving; cleanup removes the transaction
        // first, then the category it was keeping in use.
        Registration categoryCleanup = cleanUpCategoryAfterTest(categoryName);
        Registration transactionCleanup = cleanUpTransactionAfterTest(description);

        String addedStatus = transactionsPage.addTransactionWithNewCategory(
                "EXPENSE",
                categoryName,
                "33.10",
                description,
                date);

        assertAll(
                () -> assertEquals(TransactionsPage.ADDED_MESSAGE, addedStatus),
                () -> assertTrue(transactionsPage.hasFilterCategory(categoryName),
                        "Expected the new category in the transaction filter"));

        CategoriesPage categoriesPage = new CategoriesPage(driver).open();

        boolean custom = categoriesPage.isCustom(categoryName);
        boolean deleteDisabled = categoriesPage.isDeleteDisabled(categoryName);
        String blockedReason = categoriesPage.deleteBlockedReason(categoryName);

        assertAll(
                () -> assertTrue(custom, "Expected a Custom badge"),
                () -> assertTrue(deleteDisabled,
                        "Expected Delete to be unavailable while the category is used"),
                () -> assertTrue(blockedReason.startsWith("Used by 1 transaction."),
                        "Expected the in-use reason, was: " + blockedReason));

        TransactionsPage filteredPage = categoriesPage.viewTransactions(categoryName);

        assertFilteredToCategory(filteredPage, categoryName, description);

        driver.navigate().refresh();
        TransactionsPage reloadedPage = new TransactionsPage(driver).waitUntilLoaded();

        assertFilteredToCategory(reloadedPage, categoryName, description);

        DeletionResult transactionDeletion = reloadedPage.deleteTransaction(description);

        assertEquals(
                "Delete \"" + description + "\"?",
                transactionDeletion.confirmation());

        cleanupNoLongerNeeded(transactionCleanup);

        CategoriesPage refreshedCategories = new CategoriesPage(driver).open();
        DeletionResult categoryDeletion = refreshedCategories.deleteCategory(categoryName);

        assertAll(
                () -> assertEquals(
                        "Delete “" + categoryName + "”? This cannot be undone.",
                        categoryDeletion.confirmation()),
                () -> assertEquals(
                        "“" + categoryName + "” was deleted successfully.",
                        categoryDeletion.statusMessage()),
                () -> assertFalse(refreshedCategories.isCategoryPresent(categoryName),
                        "Expected the category to be gone"));

        cleanupNoLongerNeeded(categoryCleanup);
    }

    /** The deep link: ?category={id}, that category selected, only this run's row. */
    private void assertFilteredToCategory(
            TransactionsPage page,
            String categoryName,
            String description) {

        String query = URI.create(page.currentUrl()).getQuery();
        List<String> descriptions = page.visibleTransactionDescriptions();

        assertAll(
                () -> assertEquals("/transactions", page.currentPath()),
                () -> assertTrue(query != null && query.matches("(.*&)?category=\\d+(&.*)?"),
                        "Expected ?category={id}, was: " + query),
                () -> assertEquals(categoryName, page.selectedFilterCategory()),
                () -> assertEquals(List.of(description), descriptions,
                        "Expected only this run's transaction"));
    }
}

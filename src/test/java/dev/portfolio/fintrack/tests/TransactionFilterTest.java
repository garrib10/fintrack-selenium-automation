package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.TransactionsPage;

/**
 * Search and type filtering against two records owned by this run.
 * Assertions only concern those two rows, never other account data,
 * row counts, or ordering. The cleanup registry removes both afterwards.
 */
@Tag("transactions")
@Tag("regression")
@Tag("mutating")
class TransactionFilterTest extends AuthenticatedTest {

    @Test
    void userCanSearchFilterByTypeAndResetFilters() {
        String searchKey = TestData.transactionDescription("filter");
        String incomeDescription = searchKey + "-income";
        String expenseDescription = searchKey + "-expense";

        LocalDate date = LocalDate.now();

        TransactionsPage transactionsPage =
                new TransactionsPage(driver).open();

        cleanUpTransactionAfterTest(incomeDescription);
        transactionsPage.addTransaction(
                "INCOME", "Income", "125.00", incomeDescription, date);

        cleanUpTransactionAfterTest(expenseDescription);
        transactionsPage.addTransaction(
                "EXPENSE", "Other", "45.00", expenseDescription, date);

        transactionsPage
                .searchByDescription(searchKey)
                .waitUntilTransactionPresent(incomeDescription)
                .waitUntilTransactionPresent(expenseDescription);

        assertAll(
                () -> assertTrue(transactionsPage.isTransactionPresent(incomeDescription),
                        "Expected the matching income transaction"),
                () -> assertTrue(transactionsPage.isTransactionPresent(expenseDescription),
                        "Expected the matching expense transaction"));

        transactionsPage
                .filterByType("INCOME")
                .waitUntilTransactionPresent(incomeDescription)
                .waitUntilTransactionAbsent(expenseDescription);

        assertAll(
                () -> assertTrue(transactionsPage.isTransactionPresent(incomeDescription),
                        "Expected the income transaction to remain"),
                () -> assertFalse(transactionsPage.isTransactionPresent(expenseDescription),
                        "Expected the expense transaction to be filtered out"));

        transactionsPage.resetFilters();

        assertAll(
                () -> assertEquals("", transactionsPage.searchText(),
                        "Expected Reset to clear the search"),
                () -> assertEquals("", transactionsPage.selectedTypeFilter(),
                        "Expected Reset to return the type filter to All"),
                () -> assertFalse(transactionsPage.isFilteredEmptyStateDisplayed(),
                        "Expected the history to leave the filtered state"));
    }
}

package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.TransactionsPage;

class TransactionFilterTest extends AuthenticatedTest {

    @Test
    void userCanSearchAndFilterTransactionsByType() {
        String searchKey =
                TestData.uniqueTransactionDescription("type-filter");

        String incomeDescription = searchKey + "-income";
        String expenseDescription = searchKey + "-expense";

        TransactionsPage transactionsPage =
                new TransactionsPage(driver).open();

        try {
            transactionsPage.addTransaction(
                    "INCOME",
                    "125.00",
                    incomeDescription,
                    LocalDate.now());

            transactionsPage.resetFilters();

            transactionsPage.addTransaction(
                    "EXPENSE",
                    "45.00",
                    expenseDescription,
                    LocalDate.now());

            transactionsPage
                    .resetFilters()
                    .searchByDescription(searchKey)
                    .waitUntilTransactionPresent(incomeDescription)
                    .waitUntilTransactionPresent(expenseDescription);

            assertAll(
                    () -> assertTrue(
                            transactionsPage.isTransactionPresent(incomeDescription),
                            "Expected the matching income transaction"),
                    () -> assertTrue(
                            transactionsPage.isTransactionPresent(expenseDescription),
                            "Expected the matching expense transaction"));

            transactionsPage
                    .filterByType("INCOME")
                    .waitUntilTransactionPresent(incomeDescription)
                    .waitUntilTransactionAbsent(expenseDescription);

            assertAll(
                    () -> assertTrue(
                            transactionsPage.isTransactionPresent(incomeDescription),
                            "Expected the income transaction to remain visible"),
                    () -> assertFalse(
                            transactionsPage.isTransactionPresent(expenseDescription),
                            "Expected the expense transaction to be excluded"));
        } finally {
            transactionsPage.resetFilters();
            transactionsPage.deleteIfPresent(incomeDescription);
            transactionsPage.deleteIfPresent(expenseDescription);
        }
    }
}
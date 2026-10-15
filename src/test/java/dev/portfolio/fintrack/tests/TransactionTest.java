package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.core.CleanupRegistry.Registration;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.DeletionResult;
import dev.portfolio.fintrack.pages.TransactionsPage;
import dev.portfolio.fintrack.pages.TransactionsPage.TransactionRow;

/**
 * One transaction lifecycle through the UI. The record carries this run's ID
 * and is cleaned up by the registry if the test stops early.
 */
@Tag("transactions")
@Tag("regression")
class TransactionTest extends AuthenticatedTest {

    /** A built-in category every account has. */
    private static final String CATEGORY = "Other";

    @Test
    void userCanCreateEditAndDeleteTransaction() {
        String originalDescription =
                TestData.transactionDescription("crud")
                        + " O'Brien \"QA\" & Co.";

        String updatedDescription =
                TestData.transactionDescription("crud-edited")
                        + " <b>Café</b> & \"Q'A\"";

        LocalDate date = LocalDate.now();

        TransactionsPage transactionsPage =
                new TransactionsPage(driver).open();

        // Registered before saving: cleanup is idempotent, and this also covers
        // a save that succeeds on the server but fails before confirmation.
        Registration originalCleanup =
                cleanUpTransactionAfterTest(originalDescription);

        String addedStatus = transactionsPage.addTransaction(
                "EXPENSE",
                CATEGORY,
                "125.50",
                originalDescription,
                date);

        TransactionRow created = transactionsPage
                .searchByDescription(originalDescription)
                .transaction(originalDescription);

        assertAll(
                () -> assertEquals(TransactionsPage.ADDED_MESSAGE, addedStatus),
                () -> assertEquals(originalDescription, created.description(),
                        "Expected special characters to be preserved"),
                () -> assertEquals(CATEGORY, created.category()),
                () -> assertTrue(created.type().equalsIgnoreCase("Expense"),
                        "Expected an Expense, was " + created.type()),
                () -> assertMoneyEquals("125.50", created.amountValue()),
                () -> assertTrue(created.showsDate(date),
                        "Expected the transaction date, was " + created.date()));

        // Track the new name before saving it, then drop the old one once
        // the rename is confirmed.
        Registration updatedCleanup =
                cleanUpTransactionAfterTest(updatedDescription);

        String updatedStatus = transactionsPage
                .startEditing(originalDescription)
                .updateTransaction(
                        "EXPENSE",
                        "84.25",
                        updatedDescription,
                        date);

        boolean originalStillListed = transactionsPage
                .searchByDescription(originalDescription)
                .isTransactionPresent(originalDescription);

        TransactionRow updated = transactionsPage
                .searchByDescription(updatedDescription)
                .transaction(updatedDescription);

        assertAll(
                () -> assertEquals(TransactionsPage.UPDATED_MESSAGE, updatedStatus),
                () -> assertFalse(originalStillListed,
                        "Expected the original description to be gone"),
                () -> assertEquals(updatedDescription, updated.description(),
                        "Expected special characters to be preserved"),
                () -> assertEquals(CATEGORY, updated.category(),
                        "Expected the category to be unchanged"),
                () -> assertTrue(updated.type().equalsIgnoreCase("Expense")),
                () -> assertMoneyEquals("84.25", updated.amountValue()));

        cleanupNoLongerNeeded(originalCleanup);

        DeletionResult deletion =
                transactionsPage.deleteTransaction(updatedDescription);

        boolean deletedStillListed = transactionsPage
                .searchByDescription(updatedDescription)
                .isTransactionPresent(updatedDescription);

        assertAll(
                () -> assertEquals(
                        "Delete \"" + updatedDescription + "\"?",
                        deletion.confirmation(),
                        "Expected the transaction-specific confirmation"),
                () -> assertEquals(TransactionsPage.DELETED_MESSAGE,
                        deletion.statusMessage()),
                () -> assertFalse(deletedStillListed,
                        "Expected the deleted transaction to be gone"));

        cleanupNoLongerNeeded(updatedCleanup);
    }

    private static void assertMoneyEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected amount " + expected + " but was " + actual);
    }
}

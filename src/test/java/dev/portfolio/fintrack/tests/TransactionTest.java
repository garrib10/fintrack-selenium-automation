package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.TransactionsPage;
import dev.portfolio.fintrack.pages.TransactionsPage.TransactionRow;

class TransactionTest extends AuthenticatedTest {

    @Test
    void userCanCreateVerifyAndDeleteIncomeTransaction() {
        String description = TestData.uniqueTransactionDescription("income-create");

        TransactionsPage transactionsPage = new TransactionsPage(driver).open();

        try {
            String category = transactionsPage.addTransaction(
                    "INCOME",
                    "125.50",
                    description,
                    LocalDate.now());

            TransactionRow transaction = transactionsPage.transaction(description);

            assertAll(
                    () -> assertEquals(
                            description,
                            transaction.description(),
                            "Expected the created income description"),
                    () -> assertEquals(
                            category,
                            transaction.category(),
                            "Expected the selected category"),
                    () -> assertTrue(
                            transaction.type().equalsIgnoreCase("INCOME"),
                            "Expected the transaction type to be Income"),
                    () -> assertTrue(
                            transaction.amount().contains("125.50"),
                            "Expected the displayed income amount"),
                    () -> assertFalse(
                            transaction.date().isBlank(),
                            "Expected the transaction date to be displayed"));

            String confirmation = transactionsPage.deleteTransaction(description);

            assertAll(
                    () -> assertEquals(
                            "Delete \"" + description + "\"?",
                            confirmation,
                            "Expected the transaction-specific confirmation"),
                    () -> assertFalse(
                            transactionsPage.isTransactionPresent(description),
                            "Expected the deleted income to disappear"));
        } finally {
            transactionsPage.deleteIfPresent(description);
        }
    }

    @Test
    void userCanCreateVerifyAndDeleteExpenseTransaction() {
        String description = TestData.uniqueTransactionDescription("expense-create");

        TransactionsPage transactionsPage = new TransactionsPage(driver).open();

        try {
            String category = transactionsPage.addTransaction(
                    "EXPENSE",
                    "42.75",
                    description,
                    LocalDate.now());

            TransactionRow transaction = transactionsPage.transaction(description);

            assertAll(
                    () -> assertEquals(
                            description,
                            transaction.description(),
                            "Expected the created expense description"),
                    () -> assertEquals(
                            category,
                            transaction.category(),
                            "Expected the selected category"),
                    () -> assertTrue(
                            transaction.type().equalsIgnoreCase("EXPENSE"),
                            "Expected the transaction type to be Expense"),
                    () -> assertTrue(
                            transaction.amount().contains("42.75"),
                            "Expected the displayed expense amount"),
                    () -> assertFalse(
                            transaction.date().isBlank(),
                            "Expected the transaction date to be displayed"));

            String confirmation = transactionsPage.deleteTransaction(description);

            assertAll(
                    () -> assertEquals(
                            "Delete \"" + description + "\"?",
                            confirmation,
                            "Expected the transaction-specific confirmation"),
                    () -> assertFalse(
                            transactionsPage.isTransactionPresent(description),
                            "Expected the deleted expense to disappear"));
        } finally {
            transactionsPage.deleteIfPresent(description);
        }
    }

    @Test
    void userCanCancelEditThenUpdateAndDeleteExpense() {
        String originalDescription = TestData.uniqueTransactionDescription("expense-original");

        String updatedDescription = TestData.uniqueTransactionDescription("expense-updated");

        TransactionsPage transactionsPage = new TransactionsPage(driver).open();

        try {
            String category = transactionsPage.addTransaction(
                    "EXPENSE",
                    "60.00",
                    originalDescription,
                    LocalDate.now());

            transactionsPage
                    .startEditing(originalDescription)
                    .changeDescriptionDuringEdit(updatedDescription)
                    .cancelEdit();

            TransactionRow unchangedTransaction = transactionsPage.transaction(originalDescription);

            assertAll(
                    () -> assertEquals(
                            originalDescription,
                            unchangedTransaction.description(),
                            "Expected cancel to preserve the original description"),
                    () -> assertFalse(
                            transactionsPage.isTransactionPresent(
                                    updatedDescription),
                            "Expected cancel not to save the edited description"));

            transactionsPage
                    .startEditing(originalDescription)
                    .updateTransaction(
                            "EXPENSE",
                            "84.25",
                            updatedDescription,
                            LocalDate.now());

            transactionsPage
                    .resetFilters()
                    .searchByDescription(updatedDescription);

            TransactionRow updatedTransaction = transactionsPage.transaction(updatedDescription);

            assertAll(
                    () -> assertEquals(
                            updatedDescription,
                            updatedTransaction.description(),
                            "Expected the updated description"),
                    () -> assertEquals(
                            category,
                            updatedTransaction.category(),
                            "Expected the category to remain unchanged"),
                    () -> assertTrue(
                            updatedTransaction.type()
                                    .equalsIgnoreCase("EXPENSE"),
                            "Expected the updated transaction to remain an expense"),
                    () -> assertTrue(
                            updatedTransaction.amount().contains("84.25"),
                            "Expected the updated amount"),
                    () -> assertFalse(
                            transactionsPage.isTransactionPresent(
                                    originalDescription),
                            "Expected the original description to disappear"));

            String confirmation = transactionsPage.deleteTransaction(updatedDescription);

            assertAll(
                    () -> assertEquals(
                            "Delete \"" + updatedDescription + "\"?",
                            confirmation,
                            "Expected confirmation for the updated transaction"),
                    () -> assertFalse(
                            transactionsPage.isTransactionPresent(
                                    updatedDescription),
                            "Expected the updated transaction to be deleted"));
        } finally {
            transactionsPage.deleteIfPresent(originalDescription);
            transactionsPage.deleteIfPresent(updatedDescription);
        }
    }

    @Test
    void transactionDescriptionSupportsSpecialCharacters() {
        String description = TestData.uniqueTransactionDescription("special-characters")
                + " O'Brien \"QA\" & Co.";

        TransactionsPage transactionsPage = new TransactionsPage(driver).open();

        try {
            transactionsPage.addTransaction(
                    "EXPENSE",
                    "19.99",
                    description,
                    LocalDate.now());

            TransactionsPage.TransactionRow transaction = transactionsPage.transaction(description);

            assertAll(
                    () -> assertEquals(
                            description,
                            transaction.description(),
                            "Expected special characters to be preserved"),
                    () -> assertTrue(
                            transactionsPage.isTransactionPresent(description),
                            "Expected the transaction to be located by its exact description"));
        } finally {
            transactionsPage.deleteIfPresent(description);
        }
    }
}
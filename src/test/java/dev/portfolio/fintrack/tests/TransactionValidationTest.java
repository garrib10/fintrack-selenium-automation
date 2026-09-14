package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.data.TestData;
import dev.portfolio.fintrack.pages.TransactionsPage;

class TransactionValidationTest extends AuthenticatedTest {

  @Test
  void amountBelowMinimumIsRejected() {
    String description = TestData.uniqueTransactionDescription("invalid-amount");

    TransactionsPage transactionsPage = new TransactionsPage(driver).open();

    transactionsPage
        .fillNewTransactionForm(
            "EXPENSE",
            "0.00",
            description,
            LocalDate.now())
        .submitNewTransaction();

    assertAll(
        () -> assertFalse(
            transactionsPage.amountValidationMessage().isBlank(),
            "Expected the amount field to report a validation error"),
        () -> assertEquals(
            "/transactions",
            transactionsPage.currentPath(),
            "Expected invalid input to remain on the transactions page"),
        () -> assertFalse(
            transactionsPage.isTransactionPresent(description),
            "Expected an invalid transaction not to be created"));
  }

  @Test
  void missingDescriptionIsRejected() {
    TransactionsPage transactionsPage = new TransactionsPage(driver).open();

    transactionsPage
        .fillNewTransactionForm(
            "EXPENSE",
            "25.00",
            "",
            LocalDate.now())
        .submitNewTransaction();

    assertAll(
        () -> assertFalse(
            transactionsPage.descriptionValidationMessage().isBlank(),
            "Expected the required description field to report an error"),
        () -> assertEquals(
            "/transactions",
            transactionsPage.currentPath(),
            "Expected invalid input to remain on the transactions page"));
  }
}
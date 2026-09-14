package dev.portfolio.fintrack.pages;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public final class TransactionsPage extends BasePage {

  private static final String PATH = "/transactions";

  private static final By CATEGORY_SELECT = By.id("transaction-category");

  private static final By TYPE_SELECT = By.id("transaction-type");

  private static final By AMOUNT_INPUT = By.id("transaction-amount");

  private static final By DESCRIPTION_INPUT = By.id("transaction-description");

  private static final By DATE_INPUT = By.id("transaction-date");

  private static final By SEARCH_INPUT = By.id("transaction-search");

  private static final By FILTER_TYPE_SELECT = By.id("filter-type");

  private static final By APPLY_FILTERS_BUTTON = By.xpath("//button[normalize-space(.)='Apply Filters']");

  private static final By RESET_FILTERS_BUTTON = By.xpath("//button[normalize-space(.)='Reset']");

  private static final By ADD_TRANSACTION_BUTTON = By.xpath("//button[normalize-space(.)='Add Transaction']");

  private static final By UPDATE_TRANSACTION_BUTTON = By.xpath("//button[normalize-space(.)='Update Transaction']");

  private static final By CANCEL_EDIT_BUTTON = By.xpath("//button[normalize-space(.)='Cancel']");

  private static final By NO_TRANSACTIONS_MESSAGE = By.xpath("//*[normalize-space(.)='No transactions found.']");

  private static final DateTimeFormatter DATE_INPUT_FORMAT = DateTimeFormatter.ofPattern("MMddyyyy");

  private static final By TRANSACTION_ROWS = By.cssSelector("tr[data-testid^='transaction-row-']");

  public TransactionsPage(WebDriver driver) {
    super(driver);
  }

  public TransactionsPage open() {
    openPath(PATH);
    return waitUntilLoaded();
  }

  public TransactionsPage waitUntilLoaded() {
    waitUntilPathIs(PATH);
    waitUntilVisible(CATEGORY_SELECT);
    waitUntilVisible(DESCRIPTION_INPUT);
    return this;
  }

  public String addTransaction(
      String type,
      String amount,
      String description,
      LocalDate date) {

    selectType(type);
    String category = selectFirstAvailableCategory();

    enterText(AMOUNT_INPUT, amount);
    enterText(DESCRIPTION_INPUT, description);
    enterText(
        DATE_INPUT,
        date.format(DATE_INPUT_FORMAT));

    click(ADD_TRANSACTION_BUTTON);

    waitUntil(ignored -> {
      String value = waitUntilVisible(DESCRIPTION_INPUT)
          .getDomProperty("value");

      return value == null || value.isEmpty();
    });

    searchByDescription(description);
    waitUntilVisible(rowByDescription(description));

    return category;
  }

  public TransactionsPage fillNewTransactionForm(
      String type,
      String amount,
      String description,
      LocalDate date) {

    selectType(type);
    selectFirstAvailableCategory();

    enterText(AMOUNT_INPUT, amount);
    enterText(DESCRIPTION_INPUT, description);
    enterText(
        DATE_INPUT,
        date.format(DATE_INPUT_FORMAT));

    return this;
  }

  public TransactionsPage submitNewTransaction() {
    click(ADD_TRANSACTION_BUTTON);
    return this;
  }

  public String amountValidationMessage() {
    return waitUntilVisible(AMOUNT_INPUT)
        .getDomProperty("validationMessage");
  }

  public String descriptionValidationMessage() {
    return waitUntilVisible(DESCRIPTION_INPUT)
        .getDomProperty("validationMessage");
  }

  public TransactionsPage searchByDescription(
      String searchTerm) {

    enterText(SEARCH_INPUT, searchTerm);
    click(APPLY_FILTERS_BUTTON);

    waitUntil(ignored -> {
      List<WebElement> rows = driver.findElements(TRANSACTION_ROWS);

      boolean noTransactionsDisplayed = !driver.findElements(NO_TRANSACTIONS_MESSAGE)
          .isEmpty();

      boolean displayedRowsMatchSearch = !rows.isEmpty()
          && rows.stream().allMatch(row -> {
            List<WebElement> cells = row.findElements(By.tagName("td"));

            return cells.size() >= 2
                && cells.get(1)
                    .getText()
                    .trim()
                    .contains(searchTerm);
          });

      return noTransactionsDisplayed
          || displayedRowsMatchSearch;
    });

    return this;
  }

  public TransactionsPage filterByType(String type) {
    Select filterTypeSelect = new Select(
        waitUntilVisible(FILTER_TYPE_SELECT));

    filterTypeSelect.selectByValue(type);
    click(APPLY_FILTERS_BUTTON);

    return this;
  }

  public TransactionsPage resetFilters() {
    click(RESET_FILTERS_BUTTON);

    waitUntil(ignored -> {
      String value = waitUntilVisible(SEARCH_INPUT)
          .getDomProperty("value");

      return value == null || value.isEmpty();
    });

    return this;
  }

  public TransactionsPage waitUntilTransactionPresent(
      String description) {

    waitUntil(ignored -> !driver.findElements(
        rowByDescription(description)).isEmpty());

    return this;
  }

  public TransactionsPage waitUntilTransactionAbsent(
      String description) {

    waitUntil(ignored -> driver.findElements(
        rowByDescription(description)).isEmpty());

    return this;
  }

  public TransactionRow transaction(String description) {
    WebElement row = waitUntilVisible(
        rowByDescription(description));

    List<WebElement> cells = row.findElements(By.tagName("td"));

    if (cells.size() < 5) {
      throw new IllegalStateException(
          "Transaction row did not contain the expected columns");
    }

    return new TransactionRow(
        cells.get(0).getText().trim(),
        cells.get(1).getText().trim(),
        cells.get(2).getText().trim(),
        cells.get(3).getText().trim(),
        cells.get(4).getText().trim());
  }

  public boolean isTransactionPresent(String description) {
    return !driver.findElements(
        rowByDescription(description)).isEmpty();
  }

  public TransactionsPage startEditing(
      String description) {

    WebElement row = waitUntilVisible(
        rowByDescription(description));

    clickButtonWithinRow(row, "Edit");
    waitUntilClickable(UPDATE_TRANSACTION_BUTTON);

    return this;
  }

  public TransactionsPage changeDescriptionDuringEdit(
      String description) {

    enterText(DESCRIPTION_INPUT, description);
    return this;
  }

  public TransactionsPage cancelEdit() {
    click(CANCEL_EDIT_BUTTON);
    waitUntilClickable(ADD_TRANSACTION_BUTTON);
    return this;
  }

  public TransactionsPage updateTransaction(
      String type,
      String amount,
      String description,
      LocalDate date) {

    selectType(type);
    enterText(AMOUNT_INPUT, amount);
    enterText(DESCRIPTION_INPUT, description);
    enterText(
        DATE_INPUT,
        date.format(DATE_INPUT_FORMAT));

    click(UPDATE_TRANSACTION_BUTTON);
    waitUntilClickable(ADD_TRANSACTION_BUTTON);

    return this;
  }

  public String deleteTransaction(String description) {
    WebElement row = waitUntilVisible(
        rowByDescription(description));

    clickButtonWithinRow(row, "Delete");

    Alert alert = waitUntil(
        ExpectedConditions.alertIsPresent());

    String confirmationText = alert.getText();
    alert.accept();

    waitUntil(ignored -> driver.findElements(
        rowByDescription(description)).isEmpty());

    return confirmationText;
  }

  public void deleteIfPresent(String description) {
    if (isTransactionPresent(description)) {
      deleteTransaction(description);
      return;
    }

    resetFilters();
    searchByDescription(description);

    if (isTransactionPresent(description)) {
      deleteTransaction(description);
    }
  }

  private void selectType(String type) {
    Select typeSelect = new Select(
        waitUntilVisible(TYPE_SELECT));

    typeSelect.selectByValue(type);
  }

  private String selectFirstAvailableCategory() {
    Select categorySelect = new Select(
        waitUntilVisible(CATEGORY_SELECT));

    WebElement categoryOption = categorySelect.getOptions()
        .stream()
        .filter(WebElement::isEnabled)
        .filter(option -> {
          String value = option.getDomAttribute("value");

          return value != null && !value.isBlank();
        })
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No transaction category was available"));

    String value = categoryOption.getDomAttribute("value");

    categorySelect.selectByValue(value);

    return categoryOption.getText().trim();
  }

  private void clickButtonWithinRow(
      WebElement row,
      String buttonName) {

    By button = By.xpath(
        ".//button[normalize-space(.)='"
            + buttonName
            + "']");

    row.findElement(button).click();
  }

  private static By rowByDescription(
      String description) {

    return By.xpath(
        "//tr[starts-with(@data-testid,'transaction-row-')]"
            + "[td[normalize-space()="
            + xpathLiteral(description)
            + "]]");
  }

  private static String xpathLiteral(String value) {
    if (!value.contains("'")) {
      return "'" + value + "'";
    }

    if (!value.contains("\"")) {
      return "\"" + value + "\"";
    }

    return "concat('"
        + value.replace("'", "', \"'\", '")
        + "')";
  }

  public record TransactionRow(
      String date,
      String description,
      String category,
      String type,
      String amount) {
  }
}
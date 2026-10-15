package dev.portfolio.fintrack.pages;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public final class TransactionsPage extends BasePage {

  private static final String PATH = "/transactions";

  private static final String HEADING = "Transactions";

  /** The category select option that switches the form to inline category creation. */
  public static final String CREATE_CUSTOM_CATEGORY_OPTION =
      "Create a custom category…";

  public static final String ADDED_MESSAGE = "Transaction added.";

  public static final String UPDATED_MESSAGE = "Transaction updated.";

  public static final String DELETED_MESSAGE = "Transaction deleted.";

  private static final By CATEGORY_SELECT =
      By.id("transaction-category");

  private static final By NEW_CATEGORY_NAME_INPUT =
      By.id("transaction-category-new-name");

  private static final By TYPE_SELECT =
      By.id("transaction-type");

  private static final By AMOUNT_INPUT =
      By.id("transaction-amount");

  private static final By DESCRIPTION_INPUT =
      By.id("transaction-description");

  private static final By DATE_INPUT =
      By.id("transaction-date");

  private static final By SEARCH_INPUT =
      By.id("transaction-search");

  private static final By FILTER_TYPE_SELECT =
      By.id("filter-type");

  private static final By FILTER_CATEGORY_SELECT =
      By.id("filter-category");

  private static final By FILTER_SORT_BY_SELECT =
      By.id("filter-sort-by");

  private static final By FILTER_SORT_DIRECTION_SELECT =
      By.id("filter-sort-direction");

  /** The add/edit form, found through its own description field. */
  private static final String TRANSACTION_FORM =
      "//form[.//*[@id='transaction-description']]";

  /** The filter form, found through its own search field. */
  private static final String FILTER_FORM =
      "//form[.//*[@id='transaction-search']]";

  private static final By ADD_TRANSACTION_BUTTON =
      By.xpath(TRANSACTION_FORM
          + "//button[@type='submit' and normalize-space(.)='Add transaction']");

  private static final By UPDATE_TRANSACTION_BUTTON =
      By.xpath(TRANSACTION_FORM
          + "//button[@type='submit' and normalize-space(.)='Update transaction']");

  private static final By CANCEL_EDIT_BUTTON =
      By.xpath(TRANSACTION_FORM
          + "//button[@type='button' and normalize-space(.)='Cancel']");

  private static final By APPLY_FILTERS_BUTTON =
      By.xpath(FILTER_FORM
          + "//button[@type='submit' and normalize-space(.)='Apply filters']");

  private static final By RESET_FILTERS_BUTTON =
      By.xpath(FILTER_FORM
          + "//button[normalize-space(.)='Reset']");

  private static final By FILTERED_EMPTY_MESSAGE =
      By.xpath("//p[normalize-space(.)='No transactions match these filters.']");

  /** Filtered and unfiltered empty history states. */
  private static final By EMPTY_HISTORY_MESSAGE =
      By.xpath("//p[normalize-space(.)='No transactions match these filters.'"
          + " or normalize-space(.)='No transactions found.']");

  private static final By TRANSACTION_ROWS =
      By.cssSelector("tr[data-testid^='transaction-row-']");

  /** Typed into the native date input; DriverFactory fixes Chrome to en-US. */
  private static final DateTimeFormatter DATE_INPUT_FORMAT =
      DateTimeFormatter.ofPattern("MMddyyyy");

  /** FinTrack's default MEDIUM date preference, e.g. "Oct 14, 2026". */
  private static final DateTimeFormatter MEDIUM_DISPLAY_DATE =
      DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

  /** A bound on deleting exact duplicates; one run should never create this many. */
  private static final int MAX_DUPLICATE_DELETIONS = 5;

  public TransactionsPage(WebDriver driver) {
    super(driver);
  }

  public TransactionsPage open() {
    openPath(PATH);
    return waitUntilLoaded();
  }

  public TransactionsPage waitUntilLoaded() {
    waitUntilPathIs(PATH);
    waitForPage(HEADING);
    waitUntilVisible(CATEGORY_SELECT);
    waitUntilVisible(DESCRIPTION_INPUT);
    return this;
  }

  /**
   * Adds a transaction with an explicitly named existing category and returns
   * the success banner text. It does not search for the new row.
   */
  public String addTransaction(
      String type,
      String category,
      String amount,
      String description,
      LocalDate date) {

    fillNewTransactionForm(type, category, amount, description, date);

    return submitAndConfirmAdded();
  }

  /**
   * Adds a transaction together with a new custom category, created inline
   * through "Create a custom category…", and returns the success banner text.
   */
  public String addTransactionWithNewCategory(
      String type,
      String newCategoryName,
      String amount,
      String description,
      LocalDate date) {

    selectType(type);
    selectByVisibleText(CATEGORY_SELECT, CREATE_CUSTOM_CATEGORY_OPTION);
    enterText(NEW_CATEGORY_NAME_INPUT, newCategoryName);
    fillAmountDescriptionAndDate(amount, description, date);

    return submitAndConfirmAdded();
  }

  private void fillNewTransactionForm(
      String type,
      String category,
      String amount,
      String description,
      LocalDate date) {

    selectType(type);
    selectCategory(category);
    fillAmountDescriptionAndDate(amount, description, date);
  }

  /**
   * Searches the history and waits until it shows only matching rows or an
   * empty state. An empty result is a normal outcome, not a timeout.
   */
  public TransactionsPage searchByDescription(
      String searchTerm) {

    enterText(SEARCH_INPUT, searchTerm);
    click(APPLY_FILTERS_BUTTON);

    String expected = searchTerm.toLowerCase(Locale.ROOT);

    waitUntil(ignored -> {
      if (isLoadingIndicatorDisplayed()) {
        return false;
      }

      if (isEmptyHistoryDisplayed()) {
        return true;
      }

      List<WebElement> rows = driver.findElements(TRANSACTION_ROWS);

      return !rows.isEmpty()
          && rows.stream().allMatch(row -> row.getText()
              .toLowerCase(Locale.ROOT)
              .contains(expected));
    });

    return this;
  }

  public TransactionsPage filterByType(String type) {
    selectByValue(FILTER_TYPE_SELECT, type);
    click(APPLY_FILTERS_BUTTON);
    waitForHistoryToSettle();
    return this;
  }

  public TransactionsPage filterByCategory(String category) {
    selectByVisibleText(FILTER_CATEGORY_SELECT, category);
    click(APPLY_FILTERS_BUTTON);
    waitForHistoryToSettle();
    return this;
  }

  /**
   * Whether the category filter offers {@code category}. Categories load
   * asynchronously, so this waits for the option before answering false.
   */
  public boolean hasFilterCategory(String category) {
    try {
      newWait().until(ignored -> new Select(waitUntilVisible(FILTER_CATEGORY_SELECT))
          .getOptions()
          .stream()
          .anyMatch(option -> category.equals(normalizedText(option))));

      return true;
    } catch (TimeoutException exception) {
      return false;
    }
  }

  /** Descriptions of the rows the history shows once it has finished loading. */
  public List<String> visibleTransactionDescriptions() {
    waitForHistoryToSettle();

    return driver.findElements(TRANSACTION_ROWS)
        .stream()
        .map(row -> normalizedText(row.findElements(By.tagName("td")).get(1)))
        .toList();
  }

  /** The visible name of the selected category filter, "All categories" when unset. */
  public String selectedFilterCategory() {
    return selectedText(FILTER_CATEGORY_SELECT);
  }

  /** The type filter's value; empty means "All". */
  public String selectedTypeFilter() {
    return selectedValue(FILTER_TYPE_SELECT);
  }

  public String searchText() {
    String value = waitUntilVisible(SEARCH_INPUT).getDomProperty("value");
    return value == null ? "" : value;
  }

  /** Whether the history shows the filtered empty state ("No transactions match these filters."). */
  public boolean isFilteredEmptyStateDisplayed() {
    return driver.findElements(FILTERED_EMPTY_MESSAGE)
        .stream()
        .anyMatch(BasePage::isDisplayedSafely);
  }

  public String selectedSortBy() {
    return selectedValue(FILTER_SORT_BY_SELECT);
  }

  public String selectedSortDirection() {
    return selectedValue(FILTER_SORT_DIRECTION_SELECT);
  }

  public TransactionsPage resetFilters() {
    click(RESET_FILTERS_BUTTON);

    waitUntil(ignored -> {
      String value =
          waitUntilVisible(SEARCH_INPUT)
              .getDomProperty("value");

      return value == null || value.isEmpty();
    });

    waitForHistoryToSettle();

    return this;
  }

  public TransactionsPage waitUntilTransactionPresent(
      String description) {

    waitUntil(ignored ->
        !driver.findElements(
            rowByDescription(description))
            .isEmpty());

    return this;
  }

  public TransactionsPage waitUntilTransactionAbsent(
      String description) {

    waitUntilAbsent(rowByDescription(description));

    return this;
  }

  public TransactionRow transaction(
      String description) {

    WebElement row = waitUntilVisible(
        rowByDescription(description));

    List<WebElement> cells =
        row.findElements(By.tagName("td"));

    if (cells.size() < 5) {
      throw new IllegalStateException(
          "Transaction row did not contain "
              + "the expected columns");
    }

    return new TransactionRow(
        normalizedText(cells.get(0)),
        normalizedText(cells.get(1)),
        normalizedText(cells.get(2)),
        normalizedText(cells.get(3)),
        normalizedText(cells.get(4)));
  }

  public boolean isTransactionPresent(
      String description) {

    return !driver.findElements(
        rowByDescription(description))
        .isEmpty();
  }

  public TransactionsPage startEditing(
      String description) {

    click(rowButton(description, "Edit"));

    waitUntilClickable(
        UPDATE_TRANSACTION_BUTTON);

    return this;
  }

  public TransactionsPage changeDescriptionDuringEdit(
      String description) {

    enterText(
        DESCRIPTION_INPUT,
        description);

    return this;
  }

  public TransactionsPage cancelEdit() {
    click(CANCEL_EDIT_BUTTON);

    waitUntilClickable(
        ADD_TRANSACTION_BUTTON);

    return this;
  }

  /** Saves the open edit and returns the success banner text. */
  public String updateTransaction(
      String type,
      String amount,
      String description,
      LocalDate date) {

    selectType(type);
    fillAmountDescriptionAndDate(amount, description, date);

    click(UPDATE_TRANSACTION_BUTTON);

    String status = captureStatusMessage(UPDATED_MESSAGE);

    waitUntilClickable(
        ADD_TRANSACTION_BUTTON);

    return status;
  }

  /**
   * Deletes the visible row with exactly this description through the native
   * confirmation, and returns the confirmation and success banner texts.
   */
  public DeletionResult deleteTransaction(
      String description) {

    String confirmation = deleteRow(description);
    String status = captureStatusMessage(DELETED_MESSAGE);

    return new DeletionResult(confirmation, status);
  }

  /**
   * Cleanup-safe deletion: searches for the exact description and deletes
   * only rows whose description matches it exactly. A missing row counts as
   * already cleaned up. Returns whether anything was deleted.
   */
  public boolean deleteIfPresent(
      String description) {

    searchByDescription(description);

    int deleted = 0;

    while (isTransactionPresent(description)) {
      if (deleted == MAX_DUPLICATE_DELETIONS) {
        throw new IllegalStateException(
            "More exact matches remained than expected for "
                + "transaction cleanup");
      }

      deleteRow(description);
      deleted++;
    }

    return deleted > 0;
  }

  /**
   * Deletes one exact-match row and waits for that row (by its test ID) to
   * disappear, so an identical description elsewhere is never mistaken for it.
   */
  private String deleteRow(String description) {
    WebElement row = waitUntilVisible(
        rowByDescription(description));

    String rowTestId = row.getDomAttribute("data-testid");

    click(By.xpath(
        "//tr[@data-testid="
            + xpathLiteral(rowTestId)
            + "]//button[normalize-space(.)='Delete']"));

    String confirmationText = acceptConfirm();

    waitUntilAbsent(By.cssSelector(
        "tr[data-testid='" + rowTestId + "']"));

    // The history reloads after a delete; read it only once that finishes.
    waitForHistoryToSettle();

    return confirmationText;
  }

  private String submitAndConfirmAdded() {
    click(ADD_TRANSACTION_BUTTON);

    String status = captureStatusMessage(ADDED_MESSAGE);

    waitForTransactionFormToReset();

    return status;
  }

  private void fillAmountDescriptionAndDate(
      String amount,
      String description,
      LocalDate date) {

    enterText(AMOUNT_INPUT, amount);
    enterText(DESCRIPTION_INPUT, description);
    enterText(
        DATE_INPUT,
        date.format(DATE_INPUT_FORMAT));
  }

  private void waitForTransactionFormToReset() {
    waitUntil(ignored -> {
      String value =
          waitUntilVisible(DESCRIPTION_INPUT)
              .getDomProperty("value");

      return value == null || value.isEmpty();
    });
  }

  /** Waits until the history shows rows or an empty state, not a loading message. */
  private void waitForHistoryToSettle() {
    waitUntil(ignored -> !isLoadingIndicatorDisplayed()
        && (isEmptyHistoryDisplayed()
            || !driver.findElements(TRANSACTION_ROWS).isEmpty()));
  }

  private boolean isEmptyHistoryDisplayed() {
    return driver.findElements(EMPTY_HISTORY_MESSAGE)
        .stream()
        .anyMatch(BasePage::isDisplayedSafely);
  }

  private void selectType(String type) {
    selectByValue(TYPE_SELECT, type);
  }

  /** Selects an existing category by name; inline creation has its own method. */
  private void selectCategory(String category) {
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException(
          "A transaction category must be named explicitly");
    }

    if (CREATE_CUSTOM_CATEGORY_OPTION.equals(category)) {
      throw new IllegalArgumentException(
          "Use addTransactionWithNewCategory to create a custom category");
    }

    selectByVisibleText(CATEGORY_SELECT, category);
  }

  private static By rowButton(
      String description,
      String buttonName) {

    return By.xpath(
        rowXPath(description)
            + "//button[normalize-space(.)="
            + xpathLiteral(buttonName)
            + "]");
  }

  private static By rowByDescription(
      String description) {

    return By.xpath(rowXPath(description));
  }

  private static String rowXPath(String description) {
    return "//tr[starts-with("
        + "@data-testid,'transaction-row-')]"
        + "[td[normalize-space()="
        + xpathLiteral(description)
        + "]]";
  }

  /**
   * Parses a displayed amount such as "$1,234.50" or "-$42.75" into its
   * magnitude, so assertions do not depend on currency formatting.
   */
  static BigDecimal parseAmount(String displayed) {
    String digits = displayed.replaceAll("[^0-9.]", "");

    if (digits.isEmpty()) {
      throw new IllegalArgumentException("No amount in displayed value");
    }

    return new BigDecimal(digits);
  }

  /**
   * Whether a date cell shows {@code date} in either FinTrack date preference:
   * MEDIUM ("Oct 14, 2026", the default) or ISO ("2026-10-14").
   */
  static boolean matchesDisplayedDate(String displayed, LocalDate date) {
    String normalized = displayed.trim().replaceAll("\\s+", " ");

    return normalized.equals(date.format(MEDIUM_DISPLAY_DATE))
        || normalized.equals(date.toString());
  }

  public record TransactionRow(
      String date,
      String description,
      String category,
      String type,
      String amount) {

    public BigDecimal amountValue() {
      return parseAmount(amount);
    }

    public boolean showsDate(LocalDate expected) {
      return matchesDisplayedDate(date, expected);
    }
  }
}

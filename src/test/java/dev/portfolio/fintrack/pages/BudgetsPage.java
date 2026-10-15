package dev.portfolio.fintrack.pages;

import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class BudgetsPage extends BasePage {

    private static final String PATH = "/budgets";

    private static final String HEADING = "Budgets";

    public static final String CREATED_MESSAGE = "Budget created.";

    public static final String UPDATED_MESSAGE = "Budget updated.";

    public static final String DELETED_MESSAGE = "Budget deleted.";

    private static final By CATEGORY_SELECT = By.id("budget-category");

    private static final By NEW_CATEGORY_NAME_INPUT =
            By.id("budget-category-new-name");

    private static final By MONTHLY_LIMIT_INPUT =
            By.id("budget-monthly-limit");

    private static final By FORM_MONTH_SELECT = By.id("budget-month");

    private static final By FORM_YEAR_SELECT = By.id("budget-year");

    /** Client-side category error shown under the category select. */
    private static final By CATEGORY_ERROR = By.id("budget-category-error");

    /** The create/edit form, found through its own category field. */
    private static final String BUDGET_FORM =
            "//form[.//*[@id='budget-category']]";

    private static final By CREATE_BUTTON = By.xpath(
            BUDGET_FORM
                    + "//button[@type='submit'"
                    + " and normalize-space(.)='Create budget']");

    private static final By UPDATE_BUTTON = By.xpath(
            BUDGET_FORM
                    + "//button[@type='submit'"
                    + " and normalize-space(.)='Update budget']");

    private static final By CANCEL_EDIT_BUTTON = By.xpath(
            BUDGET_FORM
                    + "//button[normalize-space(.)='Cancel edit']");

    /** Form-level error, such as a server-side duplicate, shown after the form. */
    private static final By SUBMISSION_ERROR = By.xpath(
            BUDGET_FORM + "/following-sibling::p[@role='alert']");

    private static final String PERIOD_FILTER_XPATH =
            "//*[@data-testid='budget-period-filter']";

    private static final By PERIOD_FILTER =
            By.cssSelector("[data-testid='budget-period-filter']");

    /** The Budget period selects have no IDs; each sits in a label naming it. */
    private static final By FILTER_MONTH_SELECT = By.xpath(
            PERIOD_FILTER_XPATH
                    + "//label[span[normalize-space(.)='Month']]//select");

    private static final By FILTER_YEAR_SELECT = By.xpath(
            PERIOD_FILTER_XPATH
                    + "//label[span[normalize-space(.)='Year']]//select");

    public BudgetsPage(WebDriver driver) {
        super(driver);
    }

    public BudgetsPage open() {
        openPath(PATH);
        return waitUntilLoaded();
    }

    /** Waits through lazy page loading and "Loading budgets for {Month Year}…". */
    public BudgetsPage waitUntilLoaded() {
        waitUntilPathIs(PATH);
        waitForPage(HEADING);

        waitUntilVisible(CATEGORY_SELECT);
        waitUntilVisible(PERIOD_FILTER);
        waitUntilVisible(CREATE_BUTTON);

        return this;
    }

    /** Shows the given reporting period and waits for its budgets to load. */
    public BudgetsPage selectPeriod(YearMonth period) {
        selectByValue(
                FILTER_MONTH_SELECT,
                Integer.toString(period.getMonthValue()));

        selectByValue(
                FILTER_YEAR_SELECT,
                Integer.toString(period.getYear()));

        waitUntilPeriodShown(period);

        return this;
    }

    public BudgetsPage fillBudgetForm(
            String category,
            String monthlyLimit,
            YearMonth period) {

        selectCategory(category);
        fillLimitAndPeriod(monthlyLimit, period);

        return this;
    }

    public BudgetsPage fillBudgetFormWithoutCategory(
            String monthlyLimit,
            YearMonth period) {

        selectByValue(CATEGORY_SELECT, "");
        fillLimitAndPeriod(monthlyLimit, period);

        return this;
    }

    /** Fills the form with a new custom category created inline. */
    public BudgetsPage fillBudgetFormWithNewCategory(
            String newCategoryName,
            String monthlyLimit,
            YearMonth period) {

        selectByVisibleText(
                CATEGORY_SELECT,
                TransactionsPage.CREATE_CUSTOM_CATEGORY_OPTION);

        enterText(NEW_CATEGORY_NAME_INPUT, newCategoryName);
        fillLimitAndPeriod(monthlyLimit, period);

        return this;
    }

    public BudgetsPage submitCreate() {
        click(CREATE_BUTTON);
        return this;
    }

    /**
     * Creates a budget and returns the success banner text. After saving, the
     * page shows the budget's own period.
     */
    public String createBudget(
            String category,
            String monthlyLimit,
            YearMonth period) {

        fillBudgetForm(category, monthlyLimit, period);
        submitCreate();

        String status = captureStatusMessage(CREATED_MESSAGE);

        waitUntilPeriodShown(period);
        waitUntilVisible(cardByCategoryAndPeriod(category, period));

        return status;
    }

    public BudgetCard budget(
            String category,
            YearMonth period) {

        WebElement card = waitUntilVisible(
                cardByCategoryAndPeriod(category, period));

        return new BudgetCard(
                normalizedText(card.findElement(By.tagName("h3"))),
                readCardValue(card, "Monthly limit:"));
    }

    public boolean isBudgetPresent(
            String category,
            YearMonth period) {

        return !driver.findElements(
                        cardByCategoryAndPeriod(category, period))
                .isEmpty();
    }

    public BudgetsPage startEditing(
            String category,
            YearMonth period) {

        click(cardButton(category, period, "Edit"));

        waitUntilClickable(UPDATE_BUTTON);

        return this;
    }

    public BudgetsPage changeMonthlyLimitDuringEdit(
            String monthlyLimit) {

        enterText(MONTHLY_LIMIT_INPUT, monthlyLimit);
        return this;
    }

    public BudgetsPage cancelEdit() {
        click(CANCEL_EDIT_BUTTON);
        waitUntilClickable(CREATE_BUTTON);
        return this;
    }

    /** Saves the open edit and returns the success banner text. */
    public String updateBudget(
            String category,
            String monthlyLimit,
            YearMonth period) {

        fillBudgetForm(category, monthlyLimit, period);
        click(UPDATE_BUTTON);

        String status = captureStatusMessage(UPDATED_MESSAGE);

        waitUntilClickable(CREATE_BUTTON);
        waitUntilPeriodShown(period);
        waitUntilVisible(cardByCategoryAndPeriod(category, period));

        return status;
    }

    /**
     * Deletes the budget through the native confirmation ("Delete the
     * {category} budget?") and returns the confirmation and banner texts.
     */
    public DeletionResult deleteBudget(
            String category,
            YearMonth period) {

        selectPeriod(period);

        String confirmation = deleteCard(category, period);
        String status = captureStatusMessage(DELETED_MESSAGE);

        return new DeletionResult(confirmation, status);
    }

    /**
     * Cleanup-safe deletion of exactly this category and period. A missing
     * budget counts as already cleaned up. Returns whether one was deleted.
     */
    public boolean deleteIfPresent(
            String category,
            YearMonth period) {

        selectPeriod(period);

        if (!isBudgetPresent(category, period)) {
            return false;
        }

        deleteCard(category, period);
        return true;
    }

    public String submissionErrorMessage() {
        return waitForVisibleText(SUBMISSION_ERROR);
    }

    public String categoryErrorMessage() {
        return waitForVisibleText(CATEGORY_ERROR);
    }

    public String monthlyLimitValidationMessage() {
        return waitUntilVisible(MONTHLY_LIMIT_INPUT)
                .getDomProperty("validationMessage");
    }

    private String deleteCard(
            String category,
            YearMonth period) {

        WebElement card = waitUntilVisible(
                cardByCategoryAndPeriod(category, period));

        String cardTestId = card.getDomAttribute("data-testid");

        click(By.xpath(
                "//article[@data-testid="
                        + xpathLiteral(cardTestId)
                        + "]//button[normalize-space(.)='Delete']"));

        String confirmationText = acceptConfirm();

        waitUntilAbsent(By.cssSelector(
                "article[data-testid='" + cardTestId + "']"));

        return confirmationText;
    }

    private void selectCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "A budget category must be named explicitly");
        }

        if (TransactionsPage.CREATE_CUSTOM_CATEGORY_OPTION.equals(category)) {
            throw new IllegalArgumentException(
                    "Use fillBudgetFormWithNewCategory to create a category");
        }

        selectByVisibleText(CATEGORY_SELECT, category);
    }

    private void fillLimitAndPeriod(
            String monthlyLimit,
            YearMonth period) {

        enterText(MONTHLY_LIMIT_INPUT, monthlyLimit);

        selectByValue(
                FORM_MONTH_SELECT,
                Integer.toString(period.getMonthValue()));

        selectByValue(
                FORM_YEAR_SELECT,
                Integer.toString(period.getYear()));
    }

    /**
     * Waits until Budget period shows {@code period} and its list has finished
     * loading: cards for that period, or an empty state naming it ("No budgets
     * found for …", or "No {category} budget for …" under a category filter).
     */
    private void waitUntilPeriodShown(YearMonth period) {
        String month = Integer.toString(period.getMonthValue());
        String year = Integer.toString(period.getYear());
        String periodLabel = periodLabel(period);

        By emptyState = By.xpath(
                "//p[starts-with(normalize-space(.), 'No ')"
                        + " and contains(normalize-space(.), "
                        + xpathLiteral(" for " + periodLabel + ".")
                        + ")]");

        By cardsForPeriod = By.xpath(
                "//article[starts-with(@data-testid,'budget-card-')]"
                        + "[.//span[normalize-space(.)="
                        + xpathLiteral(periodLabel)
                        + "]]");

        newWait()
                .withMessage(() -> "Budgets did not finish loading "
                        + periodLabel)
                .until(ignored -> month.equals(selectedValue(FILTER_MONTH_SELECT))
                        && year.equals(selectedValue(FILTER_YEAR_SELECT))
                        && !isLoadingIndicatorDisplayed()
                        && (!driver.findElements(emptyState).isEmpty()
                                || !driver.findElements(cardsForPeriod).isEmpty()));
    }

    private static String readCardValue(
            WebElement card,
            String label) {

        return normalizedText(card.findElement(By.xpath(
                ".//div[span["
                        + xpathTextEqualsIgnoringCase(".", label)
                        + "]]/strong")));
    }

    private static By cardButton(
            String category,
            YearMonth period,
            String buttonName) {

        return By.xpath(
                cardXPath(category, period)
                        + "//button[normalize-space(.)="
                        + xpathLiteral(buttonName)
                        + "]");
    }

    private static By cardByCategoryAndPeriod(
            String category,
            YearMonth period) {

        return By.xpath(cardXPath(category, period));
    }

    /** A card by its category heading and its own period line, e.g. "November 2026". */
    private static String cardXPath(
            String category,
            YearMonth period) {

        return "//article[starts-with("
                + "@data-testid,'budget-card-')]"
                + "[.//h3[normalize-space(.)="
                + xpathLiteral(category)
                + "]]"
                + "[.//span[normalize-space(.)="
                + xpathLiteral(periodLabel(period))
                + "]]";
    }

    /** FinTrack formats periods with Intl en-US "long" month and numeric year. */
    private static String periodLabel(YearMonth period) {
        return Month.of(period.getMonthValue())
                .getDisplayName(
                        TextStyle.FULL,
                        Locale.US)
                + " "
                + period.getYear();
    }

    public record BudgetCard(
            String category,
            String monthlyLimit) {
    }
}

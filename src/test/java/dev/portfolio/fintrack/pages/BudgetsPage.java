package dev.portfolio.fintrack.pages;

import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public final class BudgetsPage extends BasePage {

    private static final String PATH = "/budgets";

    private static final By LOADING_MESSAGE =
            By.xpath("//*[normalize-space(.)='Loading budgets...']");

    private static final By CATEGORY_SELECT = By.xpath(
            "//form//label[span[normalize-space(.)='Category']]/select");

    private static final By MONTHLY_LIMIT_INPUT = By.xpath(
            "//form//label[span[normalize-space(.)='Monthly Limit']]/input");

    private static final By FORM_MONTH_SELECT = By.xpath(
            "//form//label[span[normalize-space(.)='Month']]/select");

    private static final By FORM_YEAR_INPUT = By.xpath(
            "//form//label[span[normalize-space(.)='Year']]/input");

    private static final By PERIOD_FILTER =
            By.cssSelector("[data-testid='budget-period-filter']");

    private static final By FILTER_MONTH_SELECT = By.xpath(
            "//*[@data-testid='budget-period-filter']"
                    + "//label[span[normalize-space(.)='Month']]/select");

    private static final By FILTER_YEAR_SELECT = By.xpath(
            "//*[@data-testid='budget-period-filter']"
                    + "//label[span[normalize-space(.)='Year']]/select");

    private static final By CREATE_BUTTON = By.xpath(
            "//form//button[@type='submit'"
                    + " and normalize-space(.)='Create Budget']");

    private static final By UPDATE_BUTTON = By.xpath(
            "//form//button[@type='submit'"
                    + " and normalize-space(.)='Update Budget']");

    private static final By CANCEL_EDIT_BUTTON = By.xpath(
            "//form//button[normalize-space(.)='Cancel Edit']");

    private static final By SUBMISSION_ERROR = By.xpath(
            "//section[form]/p[@role='alert']");

    private static final By CATEGORY_ERROR = By.xpath(
            "//*[normalize-space(.)='Please select a category.']");

    public BudgetsPage(WebDriver driver) {
        super(driver);
    }

    public BudgetsPage open() {
        openPath(PATH);
        return waitUntilLoaded();
    }

    public BudgetsPage waitUntilLoaded() {
        waitUntilPathIs(PATH);

        waitUntil(ignored ->
                driver.findElements(LOADING_MESSAGE).isEmpty());

        waitUntilVisible(CATEGORY_SELECT);
        waitUntilVisible(PERIOD_FILTER);
        waitUntilVisible(CREATE_BUTTON);

        return this;
    }

    public BudgetsPage selectPeriod(YearMonth period) {
        Select monthSelect = new Select(
                waitUntilVisible(FILTER_MONTH_SELECT));

        monthSelect.selectByValue(
                Integer.toString(period.getMonthValue()));

        Select yearSelect = new Select(
                waitUntilVisible(FILTER_YEAR_SELECT));

        yearSelect.selectByValue(
                Integer.toString(period.getYear()));

        waitUntil(ignored ->
                selectedValue(FILTER_MONTH_SELECT).equals(
                        Integer.toString(period.getMonthValue()))
                        && selectedValue(FILTER_YEAR_SELECT).equals(
                        Integer.toString(period.getYear())));

        return this;
    }

    public BudgetsPage fillBudgetForm(
            String category,
            String monthlyLimit,
            YearMonth period) {

        Select categorySelect = new Select(
                waitUntilVisible(CATEGORY_SELECT));

        categorySelect.selectByVisibleText(category);

        enterText(MONTHLY_LIMIT_INPUT, monthlyLimit);

        Select monthSelect = new Select(
                waitUntilVisible(FORM_MONTH_SELECT));

        monthSelect.selectByValue(
                Integer.toString(period.getMonthValue()));

        enterText(
                FORM_YEAR_INPUT,
                Integer.toString(period.getYear()));

        return this;
    }

    public BudgetsPage fillBudgetFormWithoutCategory(
            String monthlyLimit,
            YearMonth period) {

        Select categorySelect = new Select(
                waitUntilVisible(CATEGORY_SELECT));

        categorySelect.selectByValue("");

        enterText(MONTHLY_LIMIT_INPUT, monthlyLimit);

        Select monthSelect = new Select(
                waitUntilVisible(FORM_MONTH_SELECT));

        monthSelect.selectByValue(
                Integer.toString(period.getMonthValue()));

        enterText(
                FORM_YEAR_INPUT,
                Integer.toString(period.getYear()));

        return this;
    }

    public BudgetsPage submitCreate() {
        click(CREATE_BUTTON);
        return this;
    }

    public BudgetsPage createBudget(
            String category,
            String monthlyLimit,
            YearMonth period) {

        fillBudgetForm(category, monthlyLimit, period);
        submitCreate();

        waitUntil(ignored ->
                selectedValue(FILTER_MONTH_SELECT).equals(
                        Integer.toString(period.getMonthValue()))
                        && selectedValue(FILTER_YEAR_SELECT).equals(
                        Integer.toString(period.getYear())));

        waitUntilVisible(
                cardByCategoryAndPeriod(category, period));

        return this;
    }

    public BudgetCard budget(
            String category,
            YearMonth period) {

        WebElement card = waitUntilVisible(
                cardByCategoryAndPeriod(category, period));

        return new BudgetCard(
                card.findElement(By.tagName("h3"))
                        .getText()
                        .trim(),
                readCardValue(card, "Monthly Limit:"));
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

        WebElement card = waitUntilVisible(
                cardByCategoryAndPeriod(category, period));

        card.findElement(By.xpath(
                        ".//button[normalize-space(.)='Edit']"))
                .click();

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

    public BudgetsPage updateBudget(
            String category,
            String monthlyLimit,
            YearMonth period) {

        fillBudgetForm(category, monthlyLimit, period);
        click(UPDATE_BUTTON);

        waitUntilClickable(CREATE_BUTTON);

        waitUntil(ignored ->
                selectedValue(FILTER_MONTH_SELECT).equals(
                        Integer.toString(period.getMonthValue()))
                        && selectedValue(FILTER_YEAR_SELECT).equals(
                        Integer.toString(period.getYear())));

        waitUntilVisible(
                cardByCategoryAndPeriod(category, period));

        return this;
    }

    public String deleteBudget(
            String category,
            YearMonth period) {

        selectPeriod(period);

        WebElement card = waitUntilVisible(
                cardByCategoryAndPeriod(category, period));

        card.findElement(By.xpath(
                        ".//button[normalize-space(.)='Delete']"))
                .click();

        Alert alert = waitUntil(
                ExpectedConditions.alertIsPresent());

        String confirmationText = alert.getText();
        alert.accept();

        waitUntil(ignored ->
                driver.findElements(
                                cardByCategoryAndPeriod(
                                        category,
                                        period))
                        .isEmpty());

        return confirmationText;
    }

    public void deleteIfPresent(
            String category,
            YearMonth period) {

        selectPeriod(period);

        if (isBudgetPresent(category, period)) {
            deleteBudget(category, period);
        }
    }

    public String submissionErrorMessage() {
        return waitUntilVisible(SUBMISSION_ERROR)
                .getText()
                .trim();
    }

    public String categoryErrorMessage() {
        return waitUntilVisible(CATEGORY_ERROR)
                .getText()
                .trim();
    }

    public String monthlyLimitValidationMessage() {
        return waitUntilVisible(MONTHLY_LIMIT_INPUT)
                .getDomProperty("validationMessage");
    }

    private String selectedValue(By locator) {
        return new Select(waitUntilVisible(locator))
                .getFirstSelectedOption()
                .getDomAttribute("value");
    }

    private String readCardValue(
            WebElement card,
            String label) {

        return card.findElement(By.xpath(
                        ".//div[span[normalize-space(.)="
                                + xpathLiteral(label)
                                + "]]/strong"))
                .getText()
                .trim();
    }

    private static By cardByCategoryAndPeriod(
            String category,
            YearMonth period) {

        String periodText = Month.of(period.getMonthValue())
                .getDisplayName(
                        TextStyle.FULL,
                        Locale.US)
                + " "
                + period.getYear();

        return By.xpath(
                "//article[starts-with("
                        + "@data-testid,'budget-card-')]"
                        + "[.//h3[normalize-space(.)="
                        + xpathLiteral(category)
                        + "]]"
                        + "[.//span[normalize-space(.)="
                        + xpathLiteral(periodText)
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

    public record BudgetCard(
            String category,
            String monthlyLimit) {
    }
}
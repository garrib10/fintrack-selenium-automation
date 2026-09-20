package dev.portfolio.fintrack.pages;

import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class DashboardPage extends BasePage {

    private static final String PATH = "/";

    private static final By PRIMARY_NAVIGATION =
            By.cssSelector(
                    "nav[aria-label='Primary navigation']");

    private static final By LOADING_MESSAGE =
            By.xpath(
                    "//*[normalize-space(.)='Loading dashboard...']");

    private static final By CURRENT_BALANCE =
            By.cssSelector(
                    "[data-testid='summary-current-balance']");

    private static final By TOTAL_INCOME =
            By.cssSelector(
                    "[data-testid='summary-total-income']");

    private static final By TOTAL_EXPENSES =
            By.cssSelector(
                    "[data-testid='summary-total-expenses']");

    private static final By MONTHLY_INCOME =
            By.cssSelector(
                    "[data-testid='summary-monthly-income']");

    private static final By MONTHLY_EXPENSES =
            By.cssSelector(
                    "[data-testid='summary-monthly-expenses']");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public DashboardPage open() {
        openPath(PATH);
        return waitUntilLoaded();
    }

    public DashboardPage waitUntilLoaded() {
        waitUntilPathIs(PATH);

        waitUntil(ignored ->
                driver.findElements(LOADING_MESSAGE).isEmpty());

        waitUntilVisible(PRIMARY_NAVIGATION);
        waitUntilVisible(CURRENT_BALANCE);
        waitUntilVisible(TOTAL_INCOME);
        waitUntilVisible(TOTAL_EXPENSES);
        waitUntilVisible(MONTHLY_INCOME);
        waitUntilVisible(MONTHLY_EXPENSES);

        return this;
    }

    public DashboardPage refresh() {
        driver.navigate().refresh();
        return waitUntilLoaded();
    }

    public boolean isPrimaryNavigationDisplayed() {
        return waitUntilVisible(PRIMARY_NAVIGATION)
                .isDisplayed();
    }

    public boolean isCurrentBalanceDisplayed() {
        return waitUntilVisible(CURRENT_BALANCE)
                .isDisplayed();
    }

    public boolean areAllSummaryValuesDisplayed() {
        return waitUntilVisible(CURRENT_BALANCE).isDisplayed()
                && waitUntilVisible(TOTAL_INCOME).isDisplayed()
                && waitUntilVisible(TOTAL_EXPENSES).isDisplayed()
                && waitUntilVisible(MONTHLY_INCOME).isDisplayed()
                && waitUntilVisible(MONTHLY_EXPENSES).isDisplayed();
    }

    public DashboardSummary summary() {
        return new DashboardSummary(
                readCurrency(CURRENT_BALANCE),
                readCurrency(TOTAL_INCOME),
                readCurrency(TOTAL_EXPENSES),
                readCurrency(MONTHLY_INCOME),
                readCurrency(MONTHLY_EXPENSES));
    }

    public DashboardBudget budget(String category) {
        WebElement card = waitUntilVisible(
                budgetCardByCategory(category));

        WebElement progressBar = card.findElement(
                By.cssSelector("[role='progressbar']"));

        return new DashboardBudget(
                card.findElement(By.tagName("h3"))
                        .getText()
                        .trim(),
                readCurrencyFromCard(
                        card,
                        "Monthly Limit:"),
                readCurrencyFromCard(
                        card,
                        "Spent:"),
                readCurrencyFromCard(
                        card,
                        "Remaining:"),
                readPercentageFromCard(
                        card,
                        "Used:"),
                progressBar.getDomAttribute(
                        "aria-valuenow"),
                progressBar.getDomAttribute(
                        "aria-valuetext"));
    }

    public boolean isBudgetDisplayed(String category) {
        return !driver.findElements(
                budgetCardByCategory(category))
                .isEmpty();
    }

    private BigDecimal readCurrency(By locator) {
        return parseCurrency(
                waitUntilVisible(locator)
                        .getText());
    }

    private BigDecimal readCurrencyFromCard(
            WebElement card,
            String label) {

        String value = card.findElement(By.xpath(
                ".//dl/div[dt[normalize-space(.)="
                        + xpathLiteral(label)
                        + "]]/dd"))
                .getText();

        return parseCurrency(value);
    }

    private BigDecimal readPercentageFromCard(
            WebElement card,
            String label) {

        String value = card.findElement(By.xpath(
                ".//dl/div[dt[normalize-space(.)="
                        + xpathLiteral(label)
                        + "]]/dd"))
                .getText()
                .replace("%", "")
                .trim();

        return new BigDecimal(value);
    }

    private static BigDecimal parseCurrency(String value) {
        String normalized = value
                .replace("$", "")
                .replace(",", "")
                .trim();

        if (normalized.startsWith("(")
                && normalized.endsWith(")")) {

            normalized = "-"
                    + normalized.substring(
                            1,
                            normalized.length() - 1);
        }

        return new BigDecimal(normalized);
    }

    private static By budgetCardByCategory(
            String category) {

        return By.xpath(
                "//section[div/div/h2[normalize-space(.)="
                        + "'Monthly Budgets']]"
                        + "//article[.//h3[normalize-space(.)="
                        + xpathLiteral(category)
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
                + value.replace(
                        "'",
                        "', \"'\", '")
                + "')";
    }

    public record DashboardSummary(
            BigDecimal currentBalance,
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses) {
    }

    public record DashboardBudget(
            String category,
            BigDecimal monthlyLimit,
            BigDecimal spent,
            BigDecimal remaining,
            BigDecimal usedPercentage,
            String progressValueNow,
            String progressValueText) {
    }
}
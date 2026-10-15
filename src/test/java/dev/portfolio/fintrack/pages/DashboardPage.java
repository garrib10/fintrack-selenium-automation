package dev.portfolio.fintrack.pages;

import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import dev.portfolio.fintrack.components.AppShell;

public final class DashboardPage extends BasePage {

    private static final String PATH = "/";

    private static final String HEADING = "Dashboard";

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

    /** The nearest section around the "Monthly Budgets" heading. */
    private static final String MONTHLY_BUDGETS_SECTION =
            "//h2[normalize-space(.)='Monthly Budgets']"
                    + "/ancestor::section[1]";

    private final AppShell appShell;

    public DashboardPage(WebDriver driver) {
        super(driver);
        this.appShell = new AppShell(driver);
    }

    public DashboardPage open() {
        openPath(PATH);
        return waitUntilLoaded();
    }

    /** Waits through lazy page loading and "Loading dashboard..." to the summary. */
    public DashboardPage waitUntilLoaded() {
        waitUntilPathIs(PATH);
        waitForPage(HEADING);

        appShell.waitUntilDisplayed();
        waitUntilVisible(CURRENT_BALANCE);
        waitUntilVisible(TOTAL_INCOME);
        waitUntilVisible(TOTAL_EXPENSES);
        waitUntilVisible(MONTHLY_INCOME);
        waitUntilVisible(MONTHLY_EXPENSES);

        return this;
    }

    /**
     * Opens the dashboard and reports whether the session is still signed in.
     * Returns false, leaving the browser on Login, when the session has ended.
     */
    public boolean openIfSignedIn() {
        openPath(PATH);

        if ("Login".equals(waitForAnyPage(HEADING, "Login"))) {
            return false;
        }

        waitUntilLoaded();
        return true;
    }

    public DashboardPage refresh() {
        driver.navigate().refresh();
        return waitUntilLoaded();
    }

    public AppShell appShell() {
        return appShell;
    }

    public boolean isPrimaryNavigationDisplayed() {
        return appShell.isDisplayed();
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
                normalizedText(card.findElement(By.tagName("h3"))),
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
                accessibleName(progressBar),
                progressBar.getDomAttribute(
                        "aria-valuemin"),
                progressBar.getDomAttribute(
                        "aria-valuemax"),
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

        return parseCurrency(cardValue(card, label));
    }

    private BigDecimal readPercentageFromCard(
            WebElement card,
            String label) {

        return new BigDecimal(
                cardValue(card, label)
                        .replace("%", "")
                        .trim());
    }

    private static String cardValue(
            WebElement card,
            String label) {

        return card.findElement(By.xpath(
                ".//dl/div[dt["
                        + xpathTextEqualsIgnoringCase(".", label)
                        + "]]/dd"))
                .getText();
    }

    /** The progress bar's computed accessible name, falling back to aria-label. */
    private static String accessibleName(WebElement element) {
        String name = element.getAccessibleName();

        if (name != null && !name.isBlank()) {
            return name.trim();
        }

        return element.getDomAttribute("aria-label");
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
                MONTHLY_BUDGETS_SECTION
                        + "//article[.//h3[normalize-space(.)="
                        + xpathLiteral(category)
                        + "]]");
    }

    public record DashboardSummary(
            BigDecimal currentBalance,
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses) {
    }

    /**
     * One Monthly Budgets card. The progress values are the bar's ARIA state,
     * for example name "Travel budget utilization", value text "20.0% used".
     */
    public record DashboardBudget(
            String category,
            BigDecimal monthlyLimit,
            BigDecimal spent,
            BigDecimal remaining,
            BigDecimal usedPercentage,
            String progressAccessibleName,
            String progressValueMin,
            String progressValueMax,
            String progressValueNow,
            String progressValueText) {
    }
}

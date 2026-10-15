package dev.portfolio.fintrack.pages;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * The Categories page. Cards are found through the "Search categories" box,
 * which shows one flat list of matches; while browsing, the "Other
 * categories" section is collapsed and its cards are not rendered at all.
 * Built-in categories have no actions and are never modified here.
 */
public final class CategoriesPage extends BasePage {

    private static final String PATH = "/categories";

    private static final String HEADING = "Categories";

    private static final By SEARCH_INPUT = By.id("category-search");

    private static final By PERIOD_MONTH_SELECT =
            By.id("category-period-month");

    private static final By PERIOD_YEAR_SELECT =
            By.id("category-period-year");

    private static final By BACK_TO_CURRENT_MONTH_BUTTON = By.xpath(
            "//button[normalize-space(.)='Back to current month']");

    private static final By NO_SEARCH_RESULTS = By.xpath(
            "//p[normalize-space(.)="
                    + "'No categories match your search and filter.']");

    private static final By NO_CATEGORIES = By.xpath(
            "//p[starts-with(normalize-space(.), 'You don')"
                    + " and contains(normalize-space(.), 'have any categories yet.')]");

    private static final String CUSTOM_BADGE = "Custom";

    public CategoriesPage(WebDriver driver) {
        super(driver);
    }

    public CategoriesPage open() {
        openPath(PATH);
        return waitUntilLoaded();
    }

    /** Waits through lazy page loading and "Loading categories…" to the list. */
    public CategoriesPage waitUntilLoaded() {
        waitUntilPathIs(PATH);
        waitForPage(HEADING);
        waitForListToSettle();
        return this;
    }

    /** Whether a card with exactly this name exists, searching for it first. */
    public boolean isCategoryPresent(String name) {
        search(name);
        return !driver.findElements(card(name)).isEmpty();
    }

    public boolean isCustom(String name) {
        WebElement card = findCard(name);

        return CUSTOM_BADGE.equals(normalizedText(card.findElement(
                By.xpath(".//h3/following-sibling::span[1]"))));
    }

    /** Opens the card's "More actions for {name}" disclosure (custom categories only). */
    public CategoriesPage openMoreActions(String name) {
        requireCustom(name);

        By trigger = cardControl(name, "button", "More actions for " + name);

        if (!"true".equals(firstVisible(trigger).getDomAttribute("aria-expanded"))) {
            click(trigger);
        }

        firstVisible(cardControl(name, "button", "Delete " + name));

        return this;
    }

    /** Opens the card's inline edit form through "Edit {name}". */
    public CategoriesPage startEditing(String name) {
        openMoreActions(name);
        click(cardControl(name, "button", "Edit " + name));
        return this;
    }

    /**
     * Whether "Delete {name}" is unavailable. FinTrack keeps the button
     * focusable and marks it aria-disabled so the reason stays reachable.
     */
    public boolean isDeleteDisabled(String name) {
        openMoreActions(name);

        WebElement deleteButton =
                firstVisible(cardControl(name, "button", "Delete " + name));

        return "true".equals(deleteButton.getDomAttribute("aria-disabled"))
                || !deleteButton.isEnabled();
    }

    /** The visible reason deletion is unavailable, such as "Used by 1 transaction. …". */
    public String deleteBlockedReason(String name) {
        openMoreActions(name);

        WebElement deleteButton =
                firstVisible(cardControl(name, "button", "Delete " + name));

        String reasonId = deleteButton.getDomAttribute("aria-describedby");

        if (reasonId == null || reasonId.isBlank()) {
            throw new IllegalStateException(
                    "Delete " + name + " is available; no reason is shown");
        }

        return waitForVisibleText(By.id(reasonId));
    }

    /** Follows "View transactions for {name}" to the filtered Transactions page. */
    public TransactionsPage viewTransactions(String name) {
        findCard(name);
        click(cardControl(name, "a", "View transactions for " + name));
        return new TransactionsPage(driver).waitUntilLoaded();
    }

    /**
     * Deletes an unused custom category through the inline confirmation and
     * returns its text and the success banner text.
     */
    public DeletionResult deleteCategory(String name) {
        String confirmation = confirmDeletion(name);

        String status = captureStatusMessage(
                "“" + name + "” was deleted successfully.");

        waitUntilCardAbsent(name);

        return new DeletionResult(confirmation, status);
    }

    /**
     * Cleanup-safe deletion of a custom category. A missing category counts as
     * already cleaned up; a built-in one is refused. Returns whether one was
     * deleted.
     */
    public boolean deleteIfPresent(String name) {
        if (!isCategoryPresent(name)) {
            return false;
        }

        confirmDeletion(name);
        waitUntilCardAbsent(name);

        return true;
    }

    /** Shows an earlier reporting month (January 2000 to the current month). */
    public CategoriesPage selectReportingPeriod(YearMonth period) {
        selectByValue(PERIOD_YEAR_SELECT, Integer.toString(period.getYear()));
        selectByValue(PERIOD_MONTH_SELECT, Integer.toString(period.getMonthValue()));

        waitUntil(ignored -> Integer.toString(period.getYear())
                        .equals(selectedValue(PERIOD_YEAR_SELECT))
                && Integer.toString(period.getMonthValue())
                        .equals(selectedValue(PERIOD_MONTH_SELECT)));

        waitForListToSettle();

        return this;
    }

    public YearMonth selectedReportingPeriod() {
        return YearMonth.of(
                Integer.parseInt(selectedValue(PERIOD_YEAR_SELECT)),
                Integer.parseInt(selectedValue(PERIOD_MONTH_SELECT)));
    }

    public boolean isBackToCurrentMonthDisplayed() {
        return driver.findElements(BACK_TO_CURRENT_MONTH_BUTTON)
                .stream()
                .anyMatch(BasePage::isDisplayedSafely);
    }

    public CategoriesPage backToCurrentMonth() {
        click(BACK_TO_CURRENT_MONTH_BUTTON);
        waitUntilAbsent(BACK_TO_CURRENT_MONTH_BUTTON);
        waitForListToSettle();
        return this;
    }

    private String confirmDeletion(String name) {
        if (isDeleteDisabled(name)) {
            throw new IllegalStateException(
                    "Category cannot be deleted yet: "
                            + deleteBlockedReason(name));
        }

        click(cardControl(name, "button", "Delete " + name));

        String cardXPath = cardXPath(name);

        String confirmation = waitForVisibleText(By.xpath(
                cardXPath + "//*[@role='group']/p"));

        click(By.xpath(
                cardXPath
                        + "//*[@role='group']"
                        + "//button[normalize-space(.)='Delete category']"));

        return confirmation;
    }

    /** Searching shows one flat list, so the card renders whatever its section. */
    private void search(String name) {
        enterText(SEARCH_INPUT, name);

        By match = card(name);

        waitUntil(ignored -> !driver.findElements(match).isEmpty()
                || driver.findElements(NO_SEARCH_RESULTS)
                        .stream()
                        .anyMatch(BasePage::isDisplayedSafely)
                || onlyOtherNamesShown(name));
    }

    /**
     * True once at least one card is rendered, every rendered card matches the
     * query, and none is this exact name (a longer name containing it, say).
     */
    private boolean onlyOtherNamesShown(String name) {
        String needle = name.trim().toLowerCase(Locale.ROOT);

        List<String> names = driver.findElements(By.xpath(
                        "//article[starts-with(@aria-labelledby, 'category-')]//h3"))
                .stream()
                .map(BasePage::normalizedText)
                .toList();

        return !names.isEmpty()
                && names.stream().allMatch(text -> !text.equals(name)
                        && text.toLowerCase(Locale.ROOT).contains(needle));
    }

    private WebElement findCard(String name) {
        search(name);
        return firstVisible(card(name));
    }

    private void requireCustom(String name) {
        if (!isCustom(name)) {
            throw new IllegalStateException(
                    "Built-in categories cannot be changed: " + name);
        }
    }

    private void waitUntilCardAbsent(String name) {
        waitUntilAbsent(card(name));
        waitForListToSettle();
    }

    /** Waits until loading text is gone and the list, search box, or empty state shows. */
    private void waitForListToSettle() {
        waitUntil(ignored -> !isLoadingIndicatorDisplayed()
                && (driver.findElements(SEARCH_INPUT)
                                .stream()
                                .anyMatch(BasePage::isDisplayedSafely)
                        || !driver.findElements(NO_CATEGORIES).isEmpty()));
    }

    private static By card(String name) {
        return By.xpath(cardXPath(name));
    }

    /** A category card by its exact h3 name; cards are labelled by that heading. */
    private static String cardXPath(String name) {
        return "//article[starts-with(@aria-labelledby, 'category-')]"
                + "[.//h3[normalize-space(.)="
                + xpathLiteral(name)
                + "]]";
    }

    /** A control inside the card, matched by its exact aria-label. */
    private static By cardControl(
            String name,
            String tagName,
            String ariaLabel) {

        return By.xpath(
                cardXPath(name)
                        + "//"
                        + tagName
                        + "[@aria-label="
                        + xpathLiteral(ariaLabel)
                        + "]");
    }
}

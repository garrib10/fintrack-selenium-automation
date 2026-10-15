package dev.portfolio.fintrack.components;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import dev.portfolio.fintrack.pages.BasePage;
import dev.portfolio.fintrack.pages.BudgetsPage;
import dev.portfolio.fintrack.pages.CategoriesPage;
import dev.portfolio.fintrack.pages.DashboardPage;
import dev.portfolio.fintrack.pages.TransactionsPage;

/**
 * The signed-in shell's primary navigation. FinTrack renders the same links in
 * the desktop sidebar and in the mobile drawer dialog, so links are only ever
 * looked up inside whichever copy is visible.
 */
public final class AppShell extends BasePage {

    /**
     * The sidebar copy, or the drawer copy while its dialog is open. The
     * drawer is opened by mobile-navigation support, which is not part of
     * this component yet.
     */
    private static final By VISIBLE_NAVIGATION_CANDIDATES = By.cssSelector(
            "#app-sidebar nav[aria-label='Primary navigation'],"
                    + " dialog#mobile-navigation[open]"
                    + " nav[aria-label='Primary navigation']");

    private static final By NAVIGATION_LINKS = By.cssSelector("a");

    public AppShell(WebDriver driver) {
        super(driver);
    }

    public AppShell waitUntilDisplayed() {
        visibleNavigation();
        return this;
    }

    /** Waits for the signed-in navigation; use for positive checks. */
    public boolean isDisplayed() {
        return visibleNavigation().isDisplayed();
    }

    /** Checks without waiting, for asserting the signed-in shell is absent. */
    public boolean isCurrentlyDisplayed() {
        return driver.findElements(VISIBLE_NAVIGATION_CANDIDATES)
                .stream()
                .anyMatch(BasePage::isDisplayedSafely);
    }

    /**
     * Follows a primary link by its accessible name ("Dashboard",
     * "Transactions", "Budgets" or "Categories"). Each link also contains a
     * hidden tooltip repeating its label, so DOM text is not used to match.
     */
    public void navigateTo(String linkName) {
        newWait()
                .ignoring(ElementClickInterceptedException.class)
                .withMessage(() -> "No visible navigation link named '"
                        + linkName
                        + "'")
                .until(ignored -> {
                    WebElement link = visibleNavigation()
                            .findElements(NAVIGATION_LINKS)
                            .stream()
                            .filter(candidate -> isDisplayedSafely(candidate)
                                    && hasName(candidate, linkName))
                            .findFirst()
                            .orElse(null);

                    if (link == null) {
                        return false;
                    }

                    link.click();
                    return true;
                });
    }

    public DashboardPage openDashboard() {
        navigateTo("Dashboard");
        return new DashboardPage(driver).waitUntilLoaded();
    }

    public TransactionsPage openTransactions() {
        navigateTo("Transactions");
        return new TransactionsPage(driver).waitUntilLoaded();
    }

    public BudgetsPage openBudgets() {
        navigateTo("Budgets");
        return new BudgetsPage(driver).waitUntilLoaded();
    }

    public CategoriesPage openCategories() {
        navigateTo("Categories");
        return new CategoriesPage(driver).waitUntilLoaded();
    }

    public AccountMenu accountMenu() {
        return new AccountMenu(driver);
    }

    private WebElement visibleNavigation() {
        return firstVisible(VISIBLE_NAVIGATION_CANDIDATES);
    }

    private static boolean hasName(WebElement link, String expectedName) {
        String accessibleName = link.getAccessibleName();

        if (accessibleName != null && !accessibleName.isBlank()) {
            return expectedName.equals(accessibleName.trim());
        }

        return expectedName.equals(normalizedText(link));
    }
}

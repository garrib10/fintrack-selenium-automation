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
 *
 * <p>Above 1100px the sidebar is shown. At 1100px and below the sidebar is
 * hidden and a top-bar "Open navigation menu" button opens
 * {@code dialog#mobile-navigation}, which also holds the account menu.
 */
public final class AppShell extends BasePage {

    private static final By SIDEBAR_NAVIGATION = By.cssSelector(
            "#app-sidebar nav[aria-label='Primary navigation']");

    /** The sidebar copy, or the drawer copy while its dialog is open. */
    private static final By VISIBLE_NAVIGATION_CANDIDATES = By.cssSelector(
            "#app-sidebar nav[aria-label='Primary navigation'],"
                    + " dialog#mobile-navigation[open]"
                    + " nav[aria-label='Primary navigation']");

    private static final By OPEN_MOBILE_NAVIGATION = By.cssSelector(
            "button[aria-label='Open navigation menu']");

    private static final By CLOSE_MOBILE_NAVIGATION = By.cssSelector(
            "dialog#mobile-navigation[open] button[aria-label='Close navigation menu']");

    private static final By OPEN_MOBILE_DIALOG =
            By.cssSelector("dialog#mobile-navigation[open]");

    /** Signed-in in either layout: the sidebar, or the mobile menu button. */
    private static final By SHELL_CANDIDATES = By.cssSelector(
            "#app-sidebar nav[aria-label='Primary navigation'],"
                    + " button[aria-label='Open navigation menu']");

    private static final By MOBILE_ACCOUNT_MENU_TRIGGER = By.cssSelector(
            "dialog#mobile-navigation[open] #account-menu-trigger");

    private static final By NAVIGATION_LINKS = By.cssSelector("a");

    public AppShell(WebDriver driver) {
        super(driver);
    }

    public AppShell waitUntilDisplayed() {
        firstVisible(SHELL_CANDIDATES);
        return this;
    }

    /** Waits for the signed-in shell in either layout; use for positive checks. */
    public boolean isDisplayed() {
        return firstVisible(SHELL_CANDIDATES).isDisplayed();
    }

    /** Checks without waiting, for asserting the signed-in shell is absent. */
    public boolean isCurrentlyDisplayed() {
        return driver.findElements(SHELL_CANDIDATES)
                .stream()
                .anyMatch(BasePage::isDisplayedSafely);
    }

    /** Whether the narrow layout is active: menu button shown, sidebar hidden. */
    public boolean isMobileLayout() {
        return anyVisible(OPEN_MOBILE_NAVIGATION) && !anyVisible(SIDEBAR_NAVIGATION);
    }

    public boolean isMobileNavigationOpen() {
        return anyVisible(OPEN_MOBILE_DIALOG);
    }

    public AppShell openMobileNavigation() {
        if (!isMobileNavigationOpen()) {
            click(OPEN_MOBILE_NAVIGATION);
            firstVisible(OPEN_MOBILE_DIALOG);
        }

        return this;
    }

    public AppShell closeMobileNavigation() {
        if (isMobileNavigationOpen()) {
            click(CLOSE_MOBILE_NAVIGATION);
            waitUntilAbsent(OPEN_MOBILE_DIALOG);
        }

        return this;
    }

    /** Whether the visible account-menu trigger sits inside the open mobile drawer. */
    public boolean isAccountMenuInMobileNavigation() {
        return anyVisible(MOBILE_ACCOUNT_MENU_TRIGGER);
    }

    /**
     * Follows a primary link by its accessible name ("Dashboard",
     * "Transactions", "Budgets" or "Categories"), opening the mobile drawer
     * first in the narrow layout. Each link also contains a hidden tooltip
     * repeating its label, so DOM text is not used to match.
     */
    public void navigateTo(String linkName) {
        if (isMobileLayout()) {
            openMobileNavigation();
        }

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

    /** In the narrow layout, open the mobile drawer first; the menu lives inside it. */
    public AccountMenu accountMenu() {
        return new AccountMenu(driver);
    }

    private WebElement visibleNavigation() {
        return firstVisible(VISIBLE_NAVIGATION_CANDIDATES);
    }

    private boolean anyVisible(By locator) {
        return driver.findElements(locator)
                .stream()
                .anyMatch(BasePage::isDisplayedSafely);
    }

    private static boolean hasName(WebElement link, String expectedName) {
        String accessibleName = link.getAccessibleName();

        if (accessibleName != null && !accessibleName.isBlank()) {
            return expectedName.equals(accessibleName.trim());
        }

        return expectedName.equals(normalizedText(link));
    }
}

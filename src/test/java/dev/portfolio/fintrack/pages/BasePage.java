package dev.portfolio.fintrack.pages;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import dev.portfolio.fintrack.config.TestConfig;

public abstract class BasePage {

    private static final Duration STATUS_POLL_INTERVAL =
            Duration.ofMillis(100);

    /** Success banners; the stack always holds empty live regions too. */
    private static final By STATUS_MESSAGES =
            By.cssSelector("#status-banner-stack [role='status']");

    private static final By PAGE_HEADINGS = By.tagName("h1");

    /**
     * Elements whose own text starts with "Loading", such as "Loading page…",
     * "Loading dashboard..." or "Loading budgets for October 2026…".
     */
    private static final By LOADING_CANDIDATES = By.xpath(
            "//*[text()[starts-with(normalize-space(.), 'Loading')]]");

    private static final String UPPERCASE_LETTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String LOWERCASE_LETTERS =
            "abcdefghijklmnopqrstuvwxyz";

    protected final WebDriver driver;
    private final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = Objects.requireNonNull(
                driver,
                "WebDriver must not be null");

        this.wait = newWait();
    }

    protected void openPath(String path) {
        driver.get(TestConfig.baseUrl() + path);
    }

    /**
     * Waits until lazy loading has finished and a visible h1 reads exactly
     * {@code expectedHeading}. A URL change alone does not mean the page has
     * rendered.
     */
    protected void waitForPage(String expectedHeading) {
        newWait()
                .withMessage(() -> "Expected the '"
                        + expectedHeading
                        + "' page; visible h1 headings were "
                        + visibleHeadingTextsOrUnavailable()
                        + (loadingIndicatorStateOrUnavailable()
                                ? " and loading text was still shown"
                                : ""))
                .until(ignored -> !isLoadingIndicatorDisplayed()
                        && visibleHeadingTexts().contains(expectedHeading));
    }

    /**
     * Like {@link #waitForPage(String)} for a navigation with more than one
     * possible outcome, such as a protected route that may redirect to Login.
     * Returns the heading that appeared.
     */
    protected String waitForAnyPage(String... expectedHeadings) {
        List<String> expected = List.of(expectedHeadings);

        return newWait()
                .withMessage(() -> "Expected one of the pages "
                        + expected
                        + "; visible h1 headings were "
                        + visibleHeadingTextsOrUnavailable())
                .until(ignored -> isLoadingIndicatorDisplayed()
                        ? null
                        : visibleHeadingTexts()
                                .stream()
                                .filter(expected::contains)
                                .findFirst()
                                .orElse(null));
    }

    /** Whether transient loading text ending in "..." or "…" is visible. */
    protected boolean isLoadingIndicatorDisplayed() {
        return driver.findElements(LOADING_CANDIDATES)
                .stream()
                .filter(BasePage::isDisplayedSafely)
                .map(BasePage::normalizedText)
                .anyMatch(text -> text.endsWith("...")
                        || text.endsWith("…"));
    }

    protected WebElement waitUntilVisible(By locator) {
        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitUntilClickable(By locator) {
        return wait.until(
                ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * The first displayed element among all matches. Responsive layouts can
     * keep hidden copies in the DOM, so the first DOM match is not assumed to
     * be the visible one.
     */
    protected WebElement firstVisible(By locator) {
        return firstVisible(driver, locator);
    }

    protected WebElement firstVisible(
            SearchContext context,
            By locator) {

        return newWait()
                .withMessage(() -> "No visible element matched " + locator)
                .until(ignored -> context.findElements(locator)
                        .stream()
                        .filter(BasePage::isDisplayedSafely)
                        .findFirst()
                        .orElse(null));
    }

    /** The trimmed text of the first visible match once it is not blank. */
    protected String waitForVisibleText(By locator) {
        return newWait()
                .withMessage(() -> "No visible text appeared for " + locator)
                .until(ignored -> driver.findElements(locator)
                        .stream()
                        .filter(BasePage::isDisplayedSafely)
                        .map(BasePage::normalizedText)
                        .filter(text -> !text.isEmpty())
                        .findFirst()
                        .orElse(null));
    }

    protected void waitUntilAbsent(By locator) {
        newWait()
                .withMessage(() -> "Expected no element to match " + locator)
                .until(ignored -> driver.findElements(locator).isEmpty());
    }

    protected void enterText(By locator, String value) {
        WebElement element = waitUntilVisible(locator);
        element.clear();
        element.sendKeys(value);
    }

    /**
     * Clicks the first visible, enabled match. A click blocked by an overlay,
     * such as the floating status banner, is attempted again until the wait
     * times out; a blocked click never reaches the page.
     */
    protected void click(By locator) {
        newWait()
                .ignoring(ElementClickInterceptedException.class)
                .withMessage(() -> "No visible, enabled element to click for "
                        + locator)
                .until(ignored -> {
                    WebElement element = driver.findElements(locator)
                            .stream()
                            .filter(candidate -> isDisplayedSafely(candidate)
                                    && candidate.isEnabled())
                            .findFirst()
                            .orElse(null);

                    if (element == null) {
                        return false;
                    }

                    element.click();
                    return true;
                });
    }

    protected String readText(By locator) {
        return waitUntilVisible(locator).getText();
    }

    /** Selects an option once it exists; options such as categories load asynchronously. */
    protected void selectByVisibleText(By selectLocator, String visibleText) {
        newWait()
                .withMessage(() -> "Option '"
                        + visibleText
                        + "' was not available in "
                        + selectLocator)
                .until(ignored -> new Select(waitUntilVisible(selectLocator))
                        .getOptions()
                        .stream()
                        .anyMatch(option -> visibleText.equals(
                                normalizedText(option))));

        new Select(waitUntilVisible(selectLocator))
                .selectByVisibleText(visibleText);
    }

    protected void selectByValue(By selectLocator, String value) {
        new Select(waitUntilVisible(selectLocator)).selectByValue(value);
    }

    protected String selectedValue(By selectLocator) {
        return new Select(waitUntilVisible(selectLocator))
                .getFirstSelectedOption()
                .getDomAttribute("value");
    }

    protected String selectedText(By selectLocator) {
        return normalizedText(
                new Select(waitUntilVisible(selectLocator))
                        .getFirstSelectedOption());
    }

    /**
     * Returns the text of a visible success banner containing
     * {@code expectedText}. Banners close after about three seconds, so this
     * polls quickly, reads the text once, and never waits for the banner to
     * close.
     */
    protected String captureStatusMessage(String expectedText) {
        WebDriverWait statusWait = newWait();
        statusWait.pollingEvery(STATUS_POLL_INTERVAL);

        return statusWait
                .withMessage(() -> "No status banner contained '"
                        + expectedText
                        + "'")
                .until(ignored -> driver.findElements(STATUS_MESSAGES)
                        .stream()
                        .filter(BasePage::isDisplayedSafely)
                        .map(BasePage::normalizedText)
                        .filter(text -> text.contains(expectedText))
                        .findFirst()
                        .orElse(null));
    }

    /** Accepts the native confirmation and returns its exact text. */
    protected String acceptConfirm() {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String confirmationText = alert.getText();
        alert.accept();
        return confirmationText;
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public String currentPath() {
        return URI.create(driver.getCurrentUrl()).getPath();
    }

    protected void waitUntilPathIs(String expectedPath) {
        wait.until(webDriver -> expectedPath.equals(
                URI.create(webDriver.getCurrentUrl()).getPath()));
    }

    protected String readDomProperty(By locator, String propertyName) {
        return waitUntilVisible(locator).getDomProperty(propertyName);
    }

    protected <T> T waitUntil(Function<WebDriver, T> condition) {
        return wait.until(condition);
    }

    /** A fresh wait, so a custom message or polling interval never leaks to other calls. */
    protected WebDriverWait newWait() {
        WebDriverWait newWait = new WebDriverWait(
                driver,
                TestConfig.defaultTimeout());

        newWait.ignoring(StaleElementReferenceException.class);

        return newWait;
    }

    /**
     * An XPath 1.0 string literal for any text, including text that contains
     * both apostrophes and quotation marks.
     */
    protected static String xpathLiteral(String value) {
        Objects.requireNonNull(value, "XPath literal value must not be null");

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

    /** An XPath expression that lower-cases ASCII letters, for case-insensitive labels. */
    protected static String xpathLowerCase(String expression) {
        return "translate("
                + expression
                + ", '"
                + UPPERCASE_LETTERS
                + "', '"
                + LOWERCASE_LETTERS
                + "')";
    }

    /** Matches label text such as "Monthly limit:" regardless of letter case. */
    protected static String xpathTextEqualsIgnoringCase(
            String expression,
            String expected) {

        return xpathLowerCase("normalize-space(" + expression + ")")
                + "="
                + xpathLiteral(expected.toLowerCase(Locale.ROOT));
    }

    protected static boolean isDisplayedSafely(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }

    protected static String normalizedText(WebElement element) {
        return element.getText().trim().replaceAll("\\s+", " ");
    }

    private List<String> visibleHeadingTexts() {
        return driver.findElements(PAGE_HEADINGS)
                .stream()
                .filter(BasePage::isDisplayedSafely)
                .map(BasePage::normalizedText)
                .toList();
    }

    private String visibleHeadingTextsOrUnavailable() {
        try {
            return visibleHeadingTexts().toString();
        } catch (RuntimeException exception) {
            return "unavailable";
        }
    }

    private boolean loadingIndicatorStateOrUnavailable() {
        try {
            return isLoadingIndicatorDisplayed();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}

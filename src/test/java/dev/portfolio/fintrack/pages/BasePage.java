package dev.portfolio.fintrack.pages;

import java.net.URI;
import java.util.Objects;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import dev.portfolio.fintrack.config.TestConfig;

public abstract class BasePage {

    protected final WebDriver driver;
    private final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = Objects.requireNonNull(
                driver,
                "WebDriver must not be null");

        this.wait = new WebDriverWait(
                driver,
                TestConfig.defaultTimeout());
    }

    protected void openPath(String path) {
        driver.get(TestConfig.baseUrl() + path);
    }

    protected WebElement waitUntilVisible(By locator) {
        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitUntilClickable(By locator) {
        return wait.until(
                ExpectedConditions.elementToBeClickable(locator));
    }

    protected void enterText(By locator, String value) {
        WebElement element = waitUntilVisible(locator);
        element.clear();
        element.sendKeys(value);
    }

    protected void click(By locator) {
        waitUntilClickable(locator).click();
    }

    protected String readText(By locator) {
        return waitUntilVisible(locator).getText();
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
}

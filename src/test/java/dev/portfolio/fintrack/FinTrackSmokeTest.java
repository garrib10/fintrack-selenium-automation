package dev.portfolio.fintrack;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class FinTrackSmokeTest {

    private static final String BASE_URL =
            "https://finance-operations-dashboard.vercel.app";

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void deployedApplicationLoadsLoginPage() {
        driver.get(BASE_URL + "/login");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement emailInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email"))
        );

        assertAll(
                () -> assertTrue(
                        driver.getCurrentUrl().endsWith("/login"),
                        "Expected the browser to remain on the login route"
                ),
                () -> assertTrue(
                        emailInput.isDisplayed(),
                        "Expected the login email field to be visible"
                )
        );
    }
}
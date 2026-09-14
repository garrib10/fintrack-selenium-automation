package dev.portfolio.fintrack.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

public abstract class BaseTest {

    protected WebDriver driver;

    @BeforeEach
    protected void setUpDriver() {
        driver = DriverFactory.createDriver();
    }

    @AfterEach
    protected void tearDownDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                driver = null;
            }
        }
    }
}
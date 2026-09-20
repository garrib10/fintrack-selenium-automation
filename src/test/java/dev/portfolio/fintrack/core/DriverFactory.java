package dev.portfolio.fintrack.core;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import dev.portfolio.fintrack.config.TestConfig;

public final class DriverFactory {

    private DriverFactory() {
        // Utility class; prevent object creation.
    }

    public static WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();

        options.setPageLoadStrategy(
                PageLoadStrategy.NORMAL);

        options.addArguments(
                "--disable-notifications",
                "--disable-popup-blocking");

        if (TestConfig.headless()) {
            options.addArguments(
                    "--headless=new",
                    "--window-size=1440,1000");
        } else {
            options.addArguments(
                    "--start-maximized");
        }

        return new ChromeDriver(options);
    }
}
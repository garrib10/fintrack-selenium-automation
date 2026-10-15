package dev.portfolio.fintrack.core;

import java.util.Map;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnexpectedAlertBehaviour;
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

        /*
         * Tests accept every confirmation explicitly. A dialog left open by a
         * failed step is dismissed (never accepted) by the next command, so it
         * cannot confirm a deletion or block cleanup.
         */
        options.setUnhandledPromptBehaviour(
                UnexpectedAlertBehaviour.DISMISS);

        options.addArguments(
                "--disable-notifications",
                "--disable-popup-blocking",
                // Keeps date inputs in month/day/year order on every machine.
                "--lang=en-US");

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

    /**
     * Emulates a phone-sized viewport (for example 390×844) through Chrome's
     * device metrics. A desktop window cannot be resized this narrow on every
     * platform, so the window itself is left alone.
     */
    public static void emulateMobileViewport(
            WebDriver driver,
            int width,
            int height) {

        if (!(driver instanceof ChromeDriver chromeDriver)) {
            throw new IllegalStateException(
                    "Mobile viewport emulation needs ChromeDriver");
        }

        chromeDriver.executeCdpCommand(
                "Emulation.setDeviceMetricsOverride",
                Map.of(
                        "width", width,
                        "height", height,
                        "deviceScaleFactor", 3,
                        "mobile", true));
    }
}

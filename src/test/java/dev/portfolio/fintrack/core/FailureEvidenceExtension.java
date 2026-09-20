package dev.portfolio.fintrack.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public final class FailureEvidenceExtension
        implements AfterTestExecutionCallback {

    private static final Path ARTIFACT_ROOT =
            Path.of("target", "test-artifacts");

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyyMMdd-HHmmss-SSS");

    private final Supplier<WebDriver> driverSupplier;

    public FailureEvidenceExtension(
            Supplier<WebDriver> driverSupplier) {
        this.driverSupplier = Objects.requireNonNull(
                driverSupplier,
                "Driver supplier must not be null");
    }

    @Override
    public void afterTestExecution(
            ExtensionContext context) {
        if (context.getExecutionException().isEmpty()) {
            return;
        }

        WebDriver driver = driverSupplier.get();

        if (driver == null) {
            return;
        }

        Throwable failure =
                context.getExecutionException().orElseThrow();

        String className = sanitize(
                context.getRequiredTestClass()
                        .getSimpleName());

        String methodName = sanitize(
                context.getRequiredTestMethod()
                        .getName());

        String timestamp = LocalDateTime.now()
                .format(TIMESTAMP_FORMAT);

        String fileStem =
                methodName + "-" + timestamp;

        Path artifactDirectory =
                ARTIFACT_ROOT.resolve(className);

        try {
            Files.createDirectories(
                    artifactDirectory);
        } catch (Exception exception) {
            reportCaptureProblem(
                    context,
                    "create the artifact directory",
                    exception);
            return;
        }

        captureScreenshot(
                driver,
                artifactDirectory.resolve(
                        fileStem + ".png"),
                context);

        capturePageSource(
                driver,
                artifactDirectory.resolve(
                        fileStem + ".html"),
                context);

        captureFailureDetails(
                driver,
                failure,
                context,
                artifactDirectory.resolve(
                        fileStem + ".txt"));
    }

    private void captureScreenshot(
            WebDriver driver,
            Path screenshotPath,
            ExtensionContext context) {
        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            return;
        }

        try {
            byte[] screenshot =
                    screenshotDriver.getScreenshotAs(
                            OutputType.BYTES);

            Files.write(
                    screenshotPath,
                    screenshot);
        } catch (Exception exception) {
            reportCaptureProblem(
                    context,
                    "capture the browser screenshot",
                    exception);
        }
    }

    private void capturePageSource(
            WebDriver driver,
            Path pageSourcePath,
            ExtensionContext context) {
        try {
            Files.writeString(
                    pageSourcePath,
                    driver.getPageSource(),
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            reportCaptureProblem(
                    context,
                    "capture the page source",
                    exception);
        }
    }

    private void captureFailureDetails(
            WebDriver driver,
            Throwable failure,
            ExtensionContext context,
            Path detailsPath) {
        try {
            StringWriter stackTrace =
                    new StringWriter();

            failure.printStackTrace(
                    new PrintWriter(stackTrace));

            String details =
                    "Test: "
                            + context.getDisplayName()
                            + System.lineSeparator()
                            + "Class: "
                            + context.getRequiredTestClass()
                                    .getName()
                            + System.lineSeparator()
                            + "URL: "
                            + currentUrl(driver)
                            + System.lineSeparator()
                            + System.lineSeparator()
                            + "Failure:"
                            + System.lineSeparator()
                            + stackTrace;

            Files.writeString(
                    detailsPath,
                    details,
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            reportCaptureProblem(
                    context,
                    "capture the failure details",
                    exception);
        }
    }

    private String currentUrl(WebDriver driver) {
        try {
            return driver.getCurrentUrl();
        } catch (Exception exception) {
            return "Unavailable: "
                    + exception.getMessage();
        }
    }

    private void reportCaptureProblem(
            ExtensionContext context,
            String action,
            Exception exception) {
        System.err.println(
                "Could not "
                        + action
                        + " for "
                        + context.getDisplayName()
                        + ": "
                        + exception.getMessage());
    }

    private static String sanitize(String value) {
        return value.replaceAll(
                "[^A-Za-z0-9._-]",
                "_");
    }
}
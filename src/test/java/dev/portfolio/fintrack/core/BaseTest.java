package dev.portfolio.fintrack.core;

import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import dev.portfolio.fintrack.config.StagingAccess;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.config.TestConfig.ExecutionTarget;

public abstract class BaseTest {

    protected WebDriver driver;

    @RegisterExtension
    protected final FailureEvidenceExtension
            failureEvidenceExtension =
            new FailureEvidenceExtension(
                    () -> driver);

    @BeforeEach
    protected void setUpDriver() {
        // Fails before a browser starts when the target is missing or contradictory.
        ExecutionTarget target = TestConfig.requireBrowserTarget();
        Optional<StagingAccess> stagingAccess = TestConfig.stagingAccess();

        driver = DriverFactory.createDriver();

        stagingAccess.ifPresent(access -> grantStagingAccess(target, access));
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

    /**
     * Passes Vercel Authentication once for this browser. The URL carrying the
     * token is left immediately, so later URLs, screenshots and page sources
     * never contain it, and failures here never repeat it.
     */
    private void grantStagingAccess(
            ExecutionTarget target,
            StagingAccess access) {

        String loginUrl = target.baseUrl() + "/login";

        try {
            driver.get(loginUrl + "?" + access.queryString());
        } catch (RuntimeException exception) {
            // Selenium messages can include the requested URL; do not chain it.
            throw new IllegalStateException(
                    "Could not open staging with the Vercel "
                            + access.kind()
                            + " (" + exception.getClass().getSimpleName() + ")");
        }

        driver.get(loginUrl);

        String expectedHost = URI.create(target.baseUrl()).getHost();
        String actualHost = URI.create(driver.getCurrentUrl()).getHost();

        if (!expectedHost.equalsIgnoreCase(actualHost)) {
            throw new IllegalStateException(
                    "Vercel did not accept the "
                            + access.kind()
                            + "; the browser was sent to "
                            + actualHost
                            + ". Create a new share link or check the bypass secret.");
        }
    }
}

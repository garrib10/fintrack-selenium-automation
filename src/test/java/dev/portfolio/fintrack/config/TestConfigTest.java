package dev.portfolio.fintrack.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import dev.portfolio.fintrack.config.TestConfig.ExecutionTarget;

@Tag("unit")
class TestConfigTest {

    @Test
    void missingTargetEnvironmentIsRejected() {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> resolve(Map.of()));

        assertTrue(
                failure.getMessage().contains("FINTRACK_TARGET_ENV is required"),
                "Expected an actionable message naming the variable");
    }

    @ParameterizedTest
    @ValueSource(strings = {"live", "qa-env", "https://example.test?token=secret-value"})
    void unsupportedTargetEnvironmentIsRejectedWithoutEchoingIt(String value) {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> resolve(Map.of("FINTRACK_TARGET_ENV", value)));

        assertAll(
                () -> assertTrue(failure.getMessage().contains("unsupported value")),
                () -> assertFalse(
                        failure.getMessage().contains(value),
                        "The configured value must not be echoed"));
    }

    @Test
    void targetEnvironmentIsCaseAndWhitespaceInsensitive() {
        assertEquals(
                TargetEnvironment.STAGING,
                TargetEnvironment.parse("  Staging "));
    }

    @Test
    void productionUsesTheKnownProductionUrlWhenNoBaseUrlIsSet() {
        ExecutionTarget target = resolve(Map.of("FINTRACK_TARGET_ENV", "production"));

        assertAll(
                () -> assertEquals(TargetEnvironment.PRODUCTION, target.environment()),
                () -> assertEquals(TestConfig.PRODUCTION_BASE_URL, target.baseUrl()),
                () -> assertEquals("finance-operations-dashboard.vercel.app", target.host()));
    }

    @Test
    void stagingRequiresAnExplicitBaseUrl() {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> resolve(Map.of("FINTRACK_TARGET_ENV", "staging")));

        assertTrue(failure.getMessage().contains("FINTRACK_BASE_URL is required"));
    }

    @Test
    void localRequiresAnExplicitBaseUrl() {
        assertThrows(
                IllegalStateException.class,
                () -> resolve(Map.of("FINTRACK_TARGET_ENV", "local")));
    }

    @Test
    void stagingBaseUrlIsNormalizedAndHostIsSanitized() {
        ExecutionTarget target = resolve(Map.of(
                "FINTRACK_TARGET_ENV", "staging",
                "FINTRACK_BASE_URL", " HTTPS://Staging.Example.test:8443/app/// "));

        assertAll(
                () -> assertEquals(TargetEnvironment.STAGING, target.environment()),
                () -> assertEquals("https://staging.example.test:8443/app", target.baseUrl()),
                () -> assertEquals("staging.example.test:8443", target.host()));
    }

    @Test
    void localAcceptsLoopbackHosts() {
        ExecutionTarget target = resolve(Map.of(
                "FINTRACK_TARGET_ENV", "local",
                "FINTRACK_BASE_URL", "http://localhost:5173/"));

        assertAll(
                () -> assertEquals(TargetEnvironment.LOCAL, target.environment()),
                () -> assertEquals("http://localhost:5173", target.baseUrl()));
    }

    @Test
    void localRejectsARemoteHost() {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> resolve(Map.of(
                        "FINTRACK_TARGET_ENV", "local",
                        "FINTRACK_BASE_URL", TestConfig.PRODUCTION_BASE_URL)));

        assertTrue(failure.getMessage().contains("disagree"));
    }

    @Test
    void stagingRejectsTheProductionHost() {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> resolve(Map.of(
                        "FINTRACK_TARGET_ENV", "staging",
                        "FINTRACK_BASE_URL", TestConfig.PRODUCTION_BASE_URL)));

        assertTrue(failure.getMessage().contains("production base URL"));
    }

    @Test
    void productionRejectsALocalhostUrl() {
        assertThrows(
                IllegalStateException.class,
                () -> resolve(Map.of(
                        "FINTRACK_TARGET_ENV", "production",
                        "FINTRACK_BASE_URL", "http://127.0.0.1:5173")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://automation:hunter2@staging.example.test",
            "https://staging.example.test/?access_token=hunter2",
            "https://staging.example.test/#hunter2",
            "ftp://staging.example.test/hunter2",
            "not a url hunter2"
    })
    void unsafeOrInvalidBaseUrlsAreRejectedWithoutExposingThem(String baseUrl) {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> resolve(Map.of(
                        "FINTRACK_TARGET_ENV", "staging",
                        "FINTRACK_BASE_URL", baseUrl)));

        assertAll(
                () -> assertTrue(failure.getMessage().startsWith("FINTRACK_BASE_URL")),
                () -> assertFalse(
                        failure.getMessage().contains("hunter2"),
                        "Configuration errors must not expose URL contents"));
    }

    @Test
    void eachEnvironmentReadsItsOwnAutomationAccount() {
        assertAll(
                () -> assertEquals("FINTRACK_STAGING_EMAIL",
                        TestConfig.credentialVariable(TargetEnvironment.STAGING, "EMAIL")),
                () -> assertEquals("FINTRACK_STAGING_PASSWORD",
                        TestConfig.credentialVariable(TargetEnvironment.STAGING, "PASSWORD")),
                () -> assertEquals("FINTRACK_TEST_EMAIL",
                        TestConfig.credentialVariable(TargetEnvironment.PRODUCTION, "EMAIL")),
                () -> assertEquals("FINTRACK_TEST_PASSWORD",
                        TestConfig.credentialVariable(TargetEnvironment.LOCAL, "PASSWORD")));
    }

    private static ExecutionTarget resolve(Map<String, String> variables) {
        Map<String, String> environment = new HashMap<>(variables);
        return TestConfig.resolveTarget(environment::get);
    }
}

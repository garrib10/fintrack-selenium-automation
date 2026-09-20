package dev.portfolio.fintrack.config;

import java.time.Duration;

public final class TestConfig {

    private static final String DEFAULT_BASE_URL = "https://finance-operations-dashboard.vercel.app";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private TestConfig() {
        // Utility class; prevent object creation.
    }

    public static String baseUrl() {
        String configuredUrl = readOptional(
                "FINTRACK_BASE_URL",
                DEFAULT_BASE_URL);

        return configuredUrl.replaceFirst("/+$", "");
    }

    public static String testEmail() {
        return readRequired("FINTRACK_TEST_EMAIL");
    }

    public static String testPassword() {
        return readRequired("FINTRACK_TEST_PASSWORD");
    }

    public static String testBudgetCategory() {
        return readRequired(
                "FINTRACK_TEST_BUDGET_CATEGORY");
    }

    public static boolean registrationAllowed() {
        return Boolean.parseBoolean(
                readOptional("FINTRACK_ALLOW_REGISTRATION", "false"));
    }

    public static Duration defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    private static String readRequired(String variableName) {
        String value = System.getenv(variableName);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required environment variable is missing: " + variableName);
        }

        return value;
    }

    private static String readOptional(
            String variableName,
            String defaultValue) {
        String value = System.getenv(variableName);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }
}
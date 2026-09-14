package dev.portfolio.fintrack.data;

import java.util.Locale;
import java.util.UUID;

public final class TestData {

    private static final String AUTOMATION_PREFIX = "selenium";

    private TestData() {
        // Utility class; prevent object creation.
    }

    public static String uniqueTransactionDescription(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException(
                    "Transaction scenario must not be blank"
            );
        }

        String normalizedScenario = scenario
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        return String.join(
                "-",
                AUTOMATION_PREFIX,
                normalizedScenario,
                UUID.randomUUID().toString()
        );
    }
}
package dev.portfolio.fintrack.config;

import java.util.Locale;

/**
 * The FinTrack deployment a browser run targets. It must always be chosen
 * explicitly through FINTRACK_TARGET_ENV so production is never selected by
 * accident.
 */
public enum TargetEnvironment {

    PRODUCTION,
    STAGING,
    LOCAL;

    static final String VARIABLE_NAME = "FINTRACK_TARGET_ENV";

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses a configured value. Error messages never echo the value, in case
     * something sensitive was pasted into the variable by mistake.
     */
    public static TargetEnvironment parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalStateException(
                    VARIABLE_NAME
                            + " is required. Set it to production,"
                            + " staging, or local.");
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);

        for (TargetEnvironment environment : values()) {
            if (environment.value().equals(normalized)) {
                return environment;
            }
        }

        throw new IllegalStateException(
                VARIABLE_NAME
                        + " has an unsupported value. Use production,"
                        + " staging, or local.");
    }
}

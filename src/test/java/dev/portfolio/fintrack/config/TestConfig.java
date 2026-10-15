package dev.portfolio.fintrack.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class TestConfig {

    static final String PRODUCTION_BASE_URL =
            "https://finance-operations-dashboard.vercel.app";

    private static final String PRODUCTION_HOST =
            URI.create(PRODUCTION_BASE_URL).getHost();

    private static final String BASE_URL_VARIABLE =
            "FINTRACK_BASE_URL";

    private static final Set<String> LOOPBACK_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "[::1]");

    private static final Duration DEFAULT_TIMEOUT =
            Duration.ofSeconds(30);

    private static final AtomicBoolean TARGET_LOGGED =
            new AtomicBoolean();

    private TestConfig() {
        // Utility class; prevent object creation.
    }

    /**
     * Resolves and validates the target before a browser starts, and logs the
     * environment and host once per JVM. Credentials are never logged.
     */
    public static ExecutionTarget requireBrowserTarget() {
        ExecutionTarget target = target();

        if (TARGET_LOGGED.compareAndSet(false, true)) {
            System.out.println(
                    "FinTrack Selenium target: "
                            + target.environment().value()
                            + " ("
                            + target.host()
                            + ")");
        }

        return target;
    }

    public static ExecutionTarget target() {
        return resolveTarget(System::getenv);
    }

    /** The Vercel Authentication token for a protected staging deployment, if set. */
    public static Optional<StagingAccess> stagingAccess() {
        return StagingAccess.resolve(
                System::getenv,
                target().environment());
    }

    public static boolean isProduction() {
        return target().environment() == TargetEnvironment.PRODUCTION;
    }

    public static String baseUrl() {
        return target().baseUrl();
    }

    public static String testEmail() {
        return readRequired(
                credentialVariable(target().environment(), "EMAIL"));
    }

    public static String testPassword() {
        return readRequired(
                credentialVariable(target().environment(), "PASSWORD"));
    }

    /**
     * Each environment has its own automation account: staging reads
     * FINTRACK_STAGING_EMAIL and FINTRACK_STAGING_PASSWORD; production and
     * local read FINTRACK_TEST_EMAIL and FINTRACK_TEST_PASSWORD. There is no
     * fallback, so one environment's login is never sent to another.
     */
    static String credentialVariable(
            TargetEnvironment environment,
            String field) {

        String prefix = environment == TargetEnvironment.STAGING
                ? "FINTRACK_STAGING_"
                : "FINTRACK_TEST_";

        return prefix + field;
    }

    public static String testBudgetCategory() {
        return readRequired(
                "FINTRACK_TEST_BUDGET_CATEGORY");
    }

    public static boolean registrationAllowed() {
        return Boolean.parseBoolean(
                readOptional(
                        "FINTRACK_ALLOW_REGISTRATION",
                        "false"));
    }

    /** Opt-in for the profile-photo round trip; staging is preferred. */
    public static boolean profilePhotoEnabled() {
        return Boolean.parseBoolean(
                readOptional("FINTRACK_ENABLE_PROFILE_PHOTO", "false"));
    }

    /** Opt-in for the maintenance sweep, on top of its excluded tag. */
    public static boolean maintenanceEnabled() {
        return Boolean.parseBoolean(
                readOptional("FINTRACK_RUN_MAINTENANCE", "false"));
    }

    public static boolean headless() {
        String systemProperty =
                System.getProperty("headless");

        if (systemProperty != null
                && !systemProperty.isBlank()) {
            return Boolean.parseBoolean(
                    systemProperty.trim());
        }

        return Boolean.parseBoolean(
                readOptional(
                        "FINTRACK_HEADLESS",
                        "false"));
    }

    public static Duration defaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    /**
     * Resolves the target from the given variable lookup. Production may use
     * the known production URL when FINTRACK_BASE_URL is unset; staging and
     * local must name their URL. Error messages name variables, never values.
     */
    static ExecutionTarget resolveTarget(
            Function<String, String> environment) {

        TargetEnvironment targetEnvironment =
                TargetEnvironment.parse(
                        environment.apply(
                                TargetEnvironment.VARIABLE_NAME));

        String configuredUrl =
                environment.apply(BASE_URL_VARIABLE);

        if (configuredUrl == null || configuredUrl.isBlank()) {
            if (targetEnvironment != TargetEnvironment.PRODUCTION) {
                throw new IllegalStateException(
                        BASE_URL_VARIABLE
                                + " is required when "
                                + TargetEnvironment.VARIABLE_NAME
                                + " is "
                                + targetEnvironment.value()
                                + ".");
            }

            configuredUrl = PRODUCTION_BASE_URL;
        }

        URI uri = parseBaseUrl(configuredUrl.trim());
        String host = uri.getHost().toLowerCase(Locale.ROOT);

        requireConsistentHost(targetEnvironment, host);

        String path = uri.getRawPath() == null
                ? ""
                : uri.getRawPath().replaceFirst("/+$", "");

        String hostAndPort = uri.getPort() == -1
                ? host
                : host + ":" + uri.getPort();

        return new ExecutionTarget(
                targetEnvironment,
                uri.getScheme().toLowerCase(Locale.ROOT)
                        + "://"
                        + hostAndPort
                        + path,
                hostAndPort);
    }

    private static URI parseBaseUrl(String value) {
        URI uri;

        try {
            uri = new URI(value);
        } catch (URISyntaxException exception) {
            // The exception message contains the value; do not propagate it.
            throw invalidBaseUrl("is not a valid URL");
        }

        String scheme = uri.getScheme();

        if (scheme == null
                || !(scheme.equalsIgnoreCase("http")
                        || scheme.equalsIgnoreCase("https"))) {
            throw invalidBaseUrl("must start with http:// or https://");
        }

        if (uri.getHost() == null) {
            throw invalidBaseUrl("must include a host name");
        }

        if (uri.getRawUserInfo() != null) {
            throw invalidBaseUrl("must not contain credentials");
        }

        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw invalidBaseUrl(
                    "must not contain a query string or fragment");
        }

        return uri;
    }

    private static void requireConsistentHost(
            TargetEnvironment environment,
            String host) {

        boolean loopback = LOOPBACK_HOSTS.contains(host)
                || host.endsWith(".localhost");

        switch (environment) {
            case LOCAL -> {
                if (!loopback) {
                    throw contradiction(
                            "local requires a localhost base URL");
                }
            }
            case STAGING -> {
                if (loopback) {
                    throw contradiction(
                            "staging cannot use a localhost base URL");
                }

                if (host.equals(PRODUCTION_HOST)) {
                    throw contradiction(
                            "staging cannot use the production base URL");
                }
            }
            case PRODUCTION -> {
                if (loopback) {
                    throw contradiction(
                            "production cannot use a localhost base URL");
                }
            }
        }
    }

    private static IllegalStateException invalidBaseUrl(String problem) {
        return new IllegalStateException(
                BASE_URL_VARIABLE + " " + problem + ".");
    }

    private static IllegalStateException contradiction(String problem) {
        return new IllegalStateException(
                TargetEnvironment.VARIABLE_NAME
                        + " and "
                        + BASE_URL_VARIABLE
                        + " disagree: "
                        + problem
                        + ".");
    }

    private static String readRequired(
            String variableName) {
        String value = System.getenv(variableName);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required environment variable is missing: "
                            + variableName);
        }

        return value.trim();
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

    /**
     * A validated target. The host is safe to log: it never includes
     * credentials, paths, or query strings.
     */
    public record ExecutionTarget(
            TargetEnvironment environment,
            String baseUrl,
            String host) {

        public ExecutionTarget {
            Objects.requireNonNull(environment, "environment");
            Objects.requireNonNull(baseUrl, "baseUrl");
            Objects.requireNonNull(host, "host");
        }
    }
}

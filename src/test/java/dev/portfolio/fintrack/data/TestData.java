package dev.portfolio.fintrack.data;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.random.RandomGenerator;

/**
 * Names for records created by this suite. Every name carries one run ID per
 * JVM, so cleanup can recognize records the current run created and leave
 * everything else alone.
 */
public final class TestData {

    private static final String TRANSACTION_PREFIX = "selenium";
    private static final String CATEGORY_PREFIX = "Sel";

    /** Keeps descriptions comfortably inside UI and database limits. */
    private static final int MAX_SCENARIO_LENGTH = 40;

    private static final DateTimeFormatter RUN_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm")
                    .withZone(ZoneOffset.UTC);

    private static final String SUFFIX_ALPHABET =
            "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final int SUFFIX_LENGTH = 4;

    private static final String RUN_ID =
            newRunId(Clock.systemUTC(), new SecureRandom());

    /** selenium-<runId>-<scenario>[ suffix], from any run. */
    private static final Pattern RUN_TRANSACTION = Pattern.compile(
            "^selenium-\\d{12}-[a-z0-9]{4}-[a-z0-9][a-z0-9-]*( .*)?$");

    /** selenium-<scenario>-<UUID>[ suffix], written by suite v1.0.0. */
    private static final Pattern LEGACY_TRANSACTION = Pattern.compile(
            "^selenium-[a-z0-9-]+-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}"
                    + "-[0-9a-f]{4}-[0-9a-f]{12}( .*)?$");

    /** Sel <runId>, from any run. */
    private static final Pattern RUN_CATEGORY = Pattern.compile(
            "^Sel \\d{12}-[a-z0-9]{4}$");

    private TestData() {
        // Utility class; prevent object creation.
    }

    /** The run ID shared by every record this JVM creates, e.g. 202610141530-k3x9. */
    public static String runId() {
        return RUN_ID;
    }

    /** A transaction description such as selenium-202610141530-k3x9-crud. */
    public static String transactionDescription(String scenario) {
        return transactionPrefix() + normalizeScenario(scenario);
    }

    /** The custom category name for this run, such as Sel 202610141530-k3x9. */
    public static String categoryName() {
        return CATEGORY_PREFIX + " " + RUN_ID;
    }

    /** Whether a transaction description or category name was created by this run. */
    public static boolean belongsToCurrentRun(String value) {
        return value != null
                && (value.startsWith(transactionPrefix())
                        || value.equals(categoryName()));
    }

    /**
     * Whether a transaction description was written by this suite in any run,
     * including suite v1.0.0. Used only by the maintenance sweep.
     */
    public static boolean isSeleniumTransaction(String description) {
        return description != null
                && (RUN_TRANSACTION.matcher(description).matches()
                        || LEGACY_TRANSACTION.matcher(description).matches());
    }

    /** Whether a category name was written by this suite in any run. */
    public static boolean isSeleniumCategory(String name) {
        return name != null && RUN_CATEGORY.matcher(name).matches();
    }

    static String newRunId(Clock clock, RandomGenerator random) {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);

        for (int index = 0; index < SUFFIX_LENGTH; index++) {
            suffix.append(SUFFIX_ALPHABET.charAt(
                    random.nextInt(SUFFIX_ALPHABET.length())));
        }

        return RUN_TIMESTAMP_FORMAT.format(clock.instant())
                + "-"
                + suffix;
    }

    static String normalizeScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException(
                    "Transaction scenario must not be blank");
        }

        String normalized = scenario
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (normalized.length() > MAX_SCENARIO_LENGTH) {
            normalized = normalized
                    .substring(0, MAX_SCENARIO_LENGTH)
                    .replaceAll("-$", "");
        }

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Transaction scenario must contain a letter or digit");
        }

        return normalized;
    }

    private static String transactionPrefix() {
        return TRANSACTION_PREFIX + "-" + RUN_ID + "-";
    }
}

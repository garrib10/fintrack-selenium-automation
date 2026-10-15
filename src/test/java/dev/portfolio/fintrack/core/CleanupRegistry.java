package dev.portfolio.fintrack.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Cleanup registered by a test as soon as it creates a record. Actions run by
 * bucket (transactions, then budgets, then categories, so nothing is still in
 * use when it is removed, then account settings) and newest first within a
 * bucket. Each action runs even when an earlier one fails; failures are
 * reported together afterwards.
 */
public final class CleanupRegistry {

    /** Declared in cleanup order. */
    public enum Bucket {
        TRANSACTIONS,
        BUDGETS,
        CATEGORIES,
        /** Account preferences and the profile photo, restored last. */
        ACCOUNT_SETTINGS
    }

    @FunctionalInterface
    public interface CleanupAction {
        void run() throws Exception;
    }

    /** Identifies one registered action so it can be cancelled later. */
    public static final class Registration {
        private Registration() {
        }
    }

    private record Entry(
            Bucket bucket,
            String description,
            CleanupAction action,
            long sequence,
            Registration registration) {
    }

    private final List<Entry> entries = new ArrayList<>();

    private long nextSequence;

    public Registration register(
            Bucket bucket,
            String description,
            CleanupAction action) {

        Registration registration = new Registration();

        entries.add(new Entry(
                Objects.requireNonNull(bucket, "bucket"),
                Objects.requireNonNull(description, "description"),
                Objects.requireNonNull(action, "action"),
                nextSequence++,
                registration));

        return registration;
    }

    /**
     * Cancels an action, for example after the test deleted the record itself.
     * Returns false when it already ran or was cancelled.
     */
    public boolean unregister(Registration registration) {
        Objects.requireNonNull(registration, "registration");
        return entries.removeIf(entry -> entry.registration() == registration);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }

    /**
     * Runs every registered action once, then empties the registry.
     *
     * @throws CleanupFailedException listing each failed action, with every
     *         failure attached as a suppressed exception
     */
    public void runAll() {
        List<Entry> ordered = entries.stream()
                .sorted(Comparator
                        .comparing(Entry::bucket)
                        .thenComparing(
                                Entry::sequence,
                                Comparator.reverseOrder()))
                .toList();

        entries.clear();

        List<String> failedDescriptions = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();

        for (Entry entry : ordered) {
            try {
                entry.action().run();
            } catch (Exception | AssertionError failure) {
                failedDescriptions.add(entry.description());
                failures.add(failure);
            }
        }

        if (!failures.isEmpty()) {
            CleanupFailedException exception =
                    new CleanupFailedException(
                            failures.size()
                                    + " of "
                                    + ordered.size()
                                    + " cleanup actions failed: "
                                    + String.join("; ", failedDescriptions));

            failures.forEach(exception::addSuppressed);

            throw exception;
        }
    }

    public static final class CleanupFailedException
            extends RuntimeException {

        private CleanupFailedException(String message) {
            super(message);
        }
    }
}

package dev.portfolio.fintrack.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Cleanup registered by a test as soon as it creates a record. Actions run by
 * bucket (transactions, then budgets, then categories, so nothing is still in
 * use when it is removed) and newest first within a bucket. Each action runs
 * even when an earlier one fails; failures are reported together afterwards.
 */
public final class CleanupRegistry {

    /** Declared in cleanup order. */
    public enum Bucket {
        TRANSACTIONS,
        BUDGETS,
        CATEGORIES
    }

    @FunctionalInterface
    public interface CleanupAction {
        void run() throws Exception;
    }

    private record Entry(
            Bucket bucket,
            String description,
            CleanupAction action,
            int sequence) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public void register(
            Bucket bucket,
            String description,
            CleanupAction action) {

        entries.add(new Entry(
                Objects.requireNonNull(bucket, "bucket"),
                Objects.requireNonNull(description, "description"),
                Objects.requireNonNull(action, "action"),
                entries.size()));
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

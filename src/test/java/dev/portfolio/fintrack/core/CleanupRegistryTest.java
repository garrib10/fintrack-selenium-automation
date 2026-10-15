package dev.portfolio.fintrack.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.CleanupRegistry.Bucket;
import dev.portfolio.fintrack.core.CleanupRegistry.CleanupFailedException;

@Tag("unit")
class CleanupRegistryTest {

    private final CleanupRegistry registry = new CleanupRegistry();

    private final List<String> calls = new ArrayList<>();

    @Test
    void runsTransactionsThenBudgetsThenCategoriesNewestFirst() {
        registry.register(Bucket.CATEGORIES, "category", record("category"));
        registry.register(Bucket.BUDGETS, "budget", record("budget"));
        registry.register(Bucket.TRANSACTIONS, "first transaction", record("first transaction"));
        registry.register(Bucket.TRANSACTIONS, "second transaction", record("second transaction"));

        registry.runAll();

        assertEquals(
                List.of("second transaction", "first transaction", "budget", "category"),
                calls);
    }

    @Test
    void aFailureDoesNotStopLaterActionsAndEveryFailureIsReported() {
        IllegalStateException transactionFailure = new IllegalStateException("row stuck");
        AssertionError budgetFailure = new AssertionError("card stuck");

        registry.register(Bucket.TRANSACTIONS, "transaction", () -> {
            calls.add("transaction");
            throw transactionFailure;
        });
        registry.register(Bucket.BUDGETS, "budget", () -> {
            calls.add("budget");
            throw budgetFailure;
        });
        registry.register(Bucket.CATEGORIES, "category", record("category"));

        CleanupFailedException failure = assertThrows(
                CleanupFailedException.class,
                registry::runAll);

        assertAll(
                () -> assertEquals(List.of("transaction", "budget", "category"), calls),
                () -> assertEquals(
                        "2 of 3 cleanup actions failed: transaction; budget",
                        failure.getMessage()),
                () -> assertEquals(2, failure.getSuppressed().length),
                () -> assertSame(transactionFailure, failure.getSuppressed()[0]),
                () -> assertSame(budgetFailure, failure.getSuppressed()[1]));
    }

    @Test
    void actionsRunOnceAndTheRegistryIsEmptiedEvenAfterFailures() {
        registry.register(Bucket.TRANSACTIONS, "transaction", () -> {
            calls.add("transaction");
            throw new IllegalStateException("failed");
        });

        assertThrows(CleanupFailedException.class, registry::runAll);
        registry.runAll();

        assertAll(
                () -> assertEquals(List.of("transaction"), calls),
                () -> assertTrue(registry.isEmpty()));
    }

    @Test
    void anEmptyRegistryDoesNothing() {
        registry.runAll();

        assertTrue(calls.isEmpty());
    }

    @Test
    void incompleteRegistrationsAreRejected() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> registry.register(null, "transaction", record("x"))),
                () -> assertThrows(NullPointerException.class,
                        () -> registry.register(Bucket.TRANSACTIONS, null, record("x"))),
                () -> assertThrows(NullPointerException.class,
                        () -> registry.register(Bucket.TRANSACTIONS, "transaction", null)));
    }

    private CleanupRegistry.CleanupAction record(String name) {
        return () -> calls.add(name);
    }
}

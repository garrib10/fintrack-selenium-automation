package dev.portfolio.fintrack.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Random;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class TestDataTest {

    private static final String RUN_ID_PATTERN = "\\d{12}-[a-z0-9]{4}";

    @Test
    void runIdIsStableForTheJvmAndReadable() {
        assertAll(
                () -> assertEquals(TestData.runId(), TestData.runId()),
                () -> assertTrue(
                        TestData.runId().matches(RUN_ID_PATTERN),
                        "Expected yyyyMMddHHmm-xxxx but was " + TestData.runId()));
    }

    @Test
    void runIdUsesUtcMinutesAndARandomSuffix() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-14T23:59:30Z"),
                ZoneId.of("America/New_York"));

        String first = TestData.newRunId(clock, new Random(1));
        String second = TestData.newRunId(clock, new Random(2));

        assertAll(
                () -> assertTrue(first.startsWith("202610142359-")),
                () -> assertTrue(first.matches(RUN_ID_PATTERN)),
                () -> assertNotEquals(first, second,
                        "Runs started in the same minute should differ by suffix"));
    }

    @Test
    void transactionDescriptionCarriesRunIdAndNormalizedScenario() {
        assertEquals(
                "selenium-" + TestData.runId() + "-crud-special-chars",
                TestData.transactionDescription("  CRUD: Special  chars! "));
    }

    @Test
    void longScenariosAreShortenedWithoutATrailingSeparator() {
        String description = TestData.transactionDescription(
                "a".repeat(39) + " tail that is far too long for a description");

        String scenario = description.substring(
                ("selenium-" + TestData.runId() + "-").length());

        assertAll(
                () -> assertEquals("a".repeat(39), scenario),
                () -> assertTrue(description.length() <= 67));
    }

    @Test
    void blankOrSymbolOnlyScenariosAreRejected() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> TestData.transactionDescription(" ")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> TestData.transactionDescription("!!!")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> TestData.transactionDescription(null)));
    }

    @Test
    void theSweepRecognizesOnlySeleniumWrittenTransactions() {
        assertAll(
                () -> assertTrue(TestData.isSeleniumTransaction(
                        TestData.transactionDescription("crud") + " O'Brien \"QA\" & Co.")),
                () -> assertTrue(TestData.isSeleniumTransaction(
                        "selenium-202001010000-zz99-filter-income")),
                () -> assertTrue(TestData.isSeleniumTransaction(
                        "selenium-dashboard-expense-123e4567-e89b-12d3-a456-426614174000")),
                () -> assertTrue(TestData.isSeleniumTransaction(
                        "selenium-special-characters-123e4567-e89b-12d3-a456-426614174000"
                                + " O'Brien \"QA\" & Co.")),
                () -> assertFalse(TestData.isSeleniumTransaction("selenium-test")),
                () -> assertFalse(TestData.isSeleniumTransaction("selenium-notes for March")),
                () -> assertFalse(TestData.isSeleniumTransaction("Groceries selenium-202001010000-zz99-x")),
                () -> assertFalse(TestData.isSeleniumTransaction("Selenium-202001010000-zz99-x")),
                () -> assertFalse(TestData.isSeleniumTransaction(null)));
    }

    @Test
    void theSweepRecognizesOnlySeleniumWrittenCategories() {
        assertAll(
                () -> assertTrue(TestData.isSeleniumCategory(TestData.categoryName())),
                () -> assertTrue(TestData.isSeleniumCategory("Sel 202001010000-zz99")),
                () -> assertFalse(TestData.isSeleniumCategory("Sel 202001010000-zz99 copy")),
                () -> assertFalse(TestData.isSeleniumCategory("Selling")),
                () -> assertFalse(TestData.isSeleniumCategory("Sel")),
                () -> assertFalse(TestData.isSeleniumCategory("Travel")),
                () -> assertFalse(TestData.isSeleniumCategory(null)));
    }

    @Test
    void categoryNameCarriesRunId() {
        assertEquals("Sel " + TestData.runId(), TestData.categoryName());
    }

    @Test
    void onlyThisRunsRecordsAreRecognized() {
        String description = TestData.transactionDescription("filter")
                + " O'Brien \"QA\" & Co.";

        assertAll(
                () -> assertTrue(TestData.belongsToCurrentRun(description)),
                () -> assertTrue(TestData.belongsToCurrentRun(TestData.categoryName())),
                () -> assertFalse(TestData.belongsToCurrentRun(
                        "selenium-202001010000-zzzz-filter")),
                () -> assertFalse(TestData.belongsToCurrentRun("Sel 202001010000-zzzz")),
                () -> assertFalse(TestData.belongsToCurrentRun(
                        TestData.categoryName() + " extra")),
                () -> assertFalse(TestData.belongsToCurrentRun("Travel")),
                () -> assertFalse(TestData.belongsToCurrentRun(null)));
    }
}

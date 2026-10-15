package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.pages.CategoriesPage;

/**
 * Optional: Categories for an earlier reporting month and back. Changing the
 * month only changes the view, so nothing is saved.
 */
@Tag("categories")
@Tag("extended")
@Tag("read-only")
class CategoriesHistoryTest extends AuthenticatedTest {

    @Test
    void earlierReportingMonthCanBeShownAndReset() {
        CategoriesPage categoriesPage = new CategoriesPage(driver).open();

        // The page's current month comes from the server, never the browser.
        YearMonth current = categoriesPage.selectedReportingPeriod();
        YearMonth earlier = current.minusMonths(1);

        assertAll(
                () -> assertTrue(
                        categoriesPage.periodSummary().contains("is used in " + label(current)),
                        "Expected the current month, was: " + categoriesPage.periodSummary()),
                () -> assertFalse(categoriesPage.isBackToCurrentMonthDisplayed(),
                        "Expected no Back to current month on the current month"));

        categoriesPage.selectReportingPeriod(earlier);

        assertAll(
                () -> assertEquals(earlier, categoriesPage.selectedReportingPeriod()),
                () -> assertTrue(
                        categoriesPage.periodSummary().contains("was used in " + label(earlier)),
                        "Expected the earlier month, was: " + categoriesPage.periodSummary()),
                () -> assertTrue(categoriesPage.isBackToCurrentMonthDisplayed(),
                        "Expected Back to current month for an earlier month"));

        categoriesPage.backToCurrentMonth();

        assertAll(
                () -> assertEquals(current, categoriesPage.selectedReportingPeriod()),
                () -> assertTrue(
                        categoriesPage.periodSummary().contains("is used in " + label(current)),
                        "Expected the current month again, was: " + categoriesPage.periodSummary()),
                () -> assertFalse(categoriesPage.isBackToCurrentMonthDisplayed()));
    }

    /** FinTrack's Intl en-US "long" month and numeric year, e.g. "September 2026". */
    private static String label(YearMonth period) {
        return Month.of(period.getMonthValue()).getDisplayName(TextStyle.FULL, Locale.US)
                + " "
                + period.getYear();
    }
}

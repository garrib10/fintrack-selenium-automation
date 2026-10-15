package dev.portfolio.fintrack.pages;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class TransactionRowValuesTest {

    @Test
    void displayedAmountsCompareByValueNotFormatting() {
        assertAll(
                () -> assertEquals(0, new BigDecimal("125.50")
                        .compareTo(TransactionsPage.parseAmount("$125.50"))),
                () -> assertEquals(0, new BigDecimal("1234.5")
                        .compareTo(TransactionsPage.parseAmount("$1,234.50"))),
                () -> assertEquals(0, new BigDecimal("42.75")
                        .compareTo(TransactionsPage.parseAmount("-$42.75"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> TransactionsPage.parseAmount("—")));
    }

    @Test
    void datesMatchEitherFinTrackDatePreference() {
        LocalDate date = LocalDate.of(2026, 10, 4);

        assertAll(
                () -> assertTrue(TransactionsPage.matchesDisplayedDate("Oct 4, 2026", date)),
                () -> assertTrue(TransactionsPage.matchesDisplayedDate(" Oct  4, 2026 ", date)),
                () -> assertTrue(TransactionsPage.matchesDisplayedDate("2026-10-04", date)),
                () -> assertFalse(TransactionsPage.matchesDisplayedDate("Oct 5, 2026", date)),
                () -> assertFalse(TransactionsPage.matchesDisplayedDate("10/04/2026", date)));
    }
}

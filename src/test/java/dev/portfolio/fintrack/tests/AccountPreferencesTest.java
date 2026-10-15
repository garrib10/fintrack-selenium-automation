package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.pages.AccountSettingsPage;

/**
 * Optional: the "Transactions per page" preference persists across a reload.
 * The original value is restored by the cleanup registry, whether or not the
 * test passes.
 */
@Tag("account")
@Tag("extended")
@Tag("mutating")
class AccountPreferencesTest extends AuthenticatedTest {

    @Test
    void transactionPageSizePersistsAfterReload() {
        AccountSettingsPage settings = new AccountSettingsPage(driver).open();

        int original = settings.transactionPageSize();
        int changed = original == 25 ? 50 : 25;

        restorePageSizeAfterTest(original);

        String status = settings.saveTransactionPageSize(changed);

        settings.reload();

        int afterReload = settings.transactionPageSize();

        assertAll(
                () -> assertEquals(AccountSettingsPage.PREFERENCES_SAVED, status),
                () -> assertEquals(changed, afterReload,
                        "Expected the saved page size after reload"));
    }
}

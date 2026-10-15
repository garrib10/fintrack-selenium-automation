package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import dev.portfolio.fintrack.config.TestConfig;
import dev.portfolio.fintrack.core.AuthenticatedTest;
import dev.portfolio.fintrack.core.CleanupRegistry.Registration;
import dev.portfolio.fintrack.data.ImageFixtures;
import dev.portfolio.fintrack.pages.AccountSettingsPage;
import dev.portfolio.fintrack.pages.DeletionResult;

/**
 * Optional profile-photo round trip, run only with
 * FINTRACK_ENABLE_PROFILE_PHOTO=true (staging preferred). The image is
 * generated at runtime. Once enabled, a missing control or a server error
 * fails the test rather than skipping it.
 */
@Tag("account")
@Tag("profile-photo")
@Tag("mutating")
class ProfilePhotoTest extends AuthenticatedTest {

    @TempDir
    Path fixtureDirectory;

    @Test
    void photoUploadPersistsAndRemovalRestoresInitials() throws Exception {
        assumeTrue(
                TestConfig.profilePhotoEnabled(),
                "Set FINTRACK_ENABLE_PROFILE_PHOTO=true to run the profile-photo round trip");

        AccountSettingsPage settings = new AccountSettingsPage(driver).open();

        assertTrue(settings.isPhotoUploadAvailable(),
                "Profile photos are enabled for this run, but the upload control is unavailable");

        if (settings.hasPhoto()) {
            throw new IllegalStateException(
                    "The automation account already has a profile photo. This run"
                            + " did not upload it, so it was left in place; remove it and rerun.");
        }

        Registration photoCleanup = removeProfilePhotoAfterTest();

        String uploaded = settings.uploadPhoto(
                ImageFixtures.writeProfilePhoto(fixtureDirectory));

        settings.waitForPhotoAvatars();

        boolean menuPhotoAfterUpload = settings.menuAvatarShowsPhoto();

        settings.reload().waitForPhotoAvatars();

        boolean menuPhotoAfterReload = settings.menuAvatarShowsPhoto();

        assertAll(
                () -> assertEquals(AccountSettingsPage.PHOTO_UPLOADED, uploaded),
                () -> assertTrue(menuPhotoAfterUpload,
                        "Expected the account-menu avatar to show the photo"),
                () -> assertTrue(menuPhotoAfterReload,
                        "Expected the photo to persist after reload"));

        DeletionResult removal = settings.removePhoto();

        settings.waitForInitialsAvatars();
        settings.reload().waitForInitialsAvatars();

        assertAll(
                () -> assertEquals("Remove your profile photo?", removal.confirmation()),
                () -> assertEquals(AccountSettingsPage.PHOTO_REMOVED, removal.statusMessage()),
                () -> assertTrue(settings.settingsAvatarShowsInitials(),
                        "Expected the initials fallback"),
                () -> assertFalse(settings.menuAvatarShowsPhoto(),
                        "Expected the account-menu avatar to fall back to initials"));

        cleanupNoLongerNeeded(photoCleanup);
    }
}

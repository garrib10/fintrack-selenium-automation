package dev.portfolio.fintrack.pages;

import java.nio.file.Path;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Account Settings: the profile photo and account preferences. Each form has
 * its own status and alert paragraphs, always rendered and usually empty, so
 * messages are read only inside the matching form. The password form is not
 * automated.
 */
public final class AccountSettingsPage extends BasePage {

    private static final String PATH = "/settings";

    private static final String HEADING = "Account Settings";

    public static final String PREFERENCES_SAVED = "Preferences saved.";

    public static final String PHOTO_UPLOADED = "Profile photo uploaded.";

    public static final String PHOTO_REMOVED = "Profile photo removed.";

    private static final String PREFERENCES_FORM =
            "//form[@aria-labelledby='preferences-heading']";

    private static final String PHOTO_FORM =
            "//form[@aria-labelledby='photo-heading']";

    private static final By PAGE_SIZE_SELECT =
            By.id("preferences-transactionPageSize");

    private static final By SAVE_PREFERENCES_BUTTON = By.xpath(
            PREFERENCES_FORM + "//button[normalize-space(.)='Save preferences']");

    private static final By PREFERENCES_STATUS = By.xpath(
            PREFERENCES_FORM + "//p[@role='status']");

    private static final By PREFERENCES_ALERT = By.xpath(
            PREFERENCES_FORM + "//p[@role='alert']");

    private static final By PHOTO_INPUT = By.id("profile-photo-input");

    private static final By UPLOAD_BUTTON = By.xpath(
            PHOTO_FORM + "//button[@type='submit' or not(@type)]"
                    + "[normalize-space(.)='Upload photo'"
                    + " or normalize-space(.)='Replace photo']");

    private static final By REMOVE_BUTTON = By.xpath(
            PHOTO_FORM + "//button[normalize-space(.)='Remove photo']");

    private static final By PHOTO_STATUS = By.xpath(
            PHOTO_FORM + "//p[@role='status']");

    private static final By PHOTO_ALERT = By.id("profile-photo-error");

    /** The labelled "Current" avatar in the photo form. */
    private static final By SETTINGS_PHOTO = By.xpath(
            PHOTO_FORM + "//figure[1]//img[starts-with(@alt, 'Profile photo for ')]");

    private static final By SETTINGS_INITIALS = By.xpath(
            PHOTO_FORM + "//figure[1]//*[@role='img'"
                    + " and starts-with(@aria-label, 'Initials for ')]");

    /** The account-menu avatar is decorative, so it has no accessible name. */
    private static final By MENU_PHOTO =
            By.cssSelector("#account-menu-trigger img");

    public AccountSettingsPage(WebDriver driver) {
        super(driver);
    }

    public AccountSettingsPage open() {
        openPath(PATH);
        return waitUntilLoaded();
    }

    public AccountSettingsPage waitUntilLoaded() {
        waitUntilPathIs(PATH);
        waitForPage(HEADING);
        waitUntilVisible(PAGE_SIZE_SELECT);
        return this;
    }

    public AccountSettingsPage reload() {
        driver.navigate().refresh();
        return waitUntilLoaded();
    }

    /** The saved "Transactions per page" value: 10, 25 or 50. */
    public int transactionPageSize() {
        return Integer.parseInt(selectedValue(PAGE_SIZE_SELECT));
    }

    /** Saves a new page size and returns the form's confirmation text. */
    public String saveTransactionPageSize(int pageSize) {
        selectByValue(PAGE_SIZE_SELECT, Integer.toString(pageSize));
        click(SAVE_PREFERENCES_BUTTON);

        return waitForOutcome(PREFERENCES_STATUS, PREFERENCES_ALERT, PREFERENCES_SAVED);
    }

    /** Whether the photo picker and its Upload button are present and usable. */
    public boolean isPhotoUploadAvailable() {
        return driver.findElements(PHOTO_INPUT).stream().anyMatch(WebElement::isEnabled)
                && driver.findElements(UPLOAD_BUTTON).stream().anyMatch(BasePage::isDisplayedSafely);
    }

    public boolean hasPhoto() {
        return !driver.findElements(SETTINGS_PHOTO).isEmpty();
    }

    /**
     * Uploads a file through the picker and returns the confirmation text. A
     * server error (for example when photos are disabled) fails the step.
     */
    public String uploadPhoto(Path file) {
        driver.findElement(PHOTO_INPUT).sendKeys(file.toAbsolutePath().toString());
        click(UPLOAD_BUTTON);

        return waitForOutcome(PHOTO_STATUS, PHOTO_ALERT, PHOTO_UPLOADED);
    }

    /** Removes the photo through the native confirmation. */
    public DeletionResult removePhoto() {
        click(REMOVE_BUTTON);

        String confirmation = acceptConfirm();
        String status = waitForOutcome(PHOTO_STATUS, PHOTO_ALERT, PHOTO_REMOVED);

        return new DeletionResult(confirmation, status);
    }

    /** Waits until both the settings and account-menu avatars show a loaded photo. */
    public AccountSettingsPage waitForPhotoAvatars() {
        newWait()
                .withMessage(() -> "Expected the uploaded photo in both avatars")
                .until(ignored -> isLoaded(SETTINGS_PHOTO) && isLoaded(MENU_PHOTO));
        return this;
    }

    /** Waits until both avatars fall back to initials. */
    public AccountSettingsPage waitForInitialsAvatars() {
        newWait()
                .withMessage(() -> "Expected initials in both avatars")
                .until(ignored -> !driver.findElements(SETTINGS_INITIALS).isEmpty()
                        && driver.findElements(MENU_PHOTO).isEmpty());
        return this;
    }

    public boolean settingsAvatarShowsInitials() {
        return !driver.findElements(SETTINGS_INITIALS).isEmpty();
    }

    public boolean menuAvatarShowsPhoto() {
        return isLoaded(MENU_PHOTO);
    }

    private boolean isLoaded(By image) {
        return driver.findElements(image).stream().anyMatch(element -> {
            String width = element.getDomProperty("naturalWidth");
            return width != null && !"0".equals(width);
        });
    }

    /**
     * Waits for the form's final message: the expected confirmation, or an
     * error, which is raised as a failure. Progress text such as
     * "Uploading photo…" is ignored.
     */
    private String waitForOutcome(By status, By alert, String expected) {
        String outcome = newWait()
                .withMessage(() -> "Expected '" + expected + "' or an error")
                .until(ignored -> {
                    String error = textOf(alert);

                    if (!error.isEmpty()) {
                        return "ERROR:" + error;
                    }

                    String message = textOf(status);
                    return message.equals(expected) ? message : null;
                });

        if (outcome.startsWith("ERROR:")) {
            throw new IllegalStateException(
                    "Account Settings reported an error: "
                            + outcome.substring("ERROR:".length()));
        }

        return outcome;
    }

    private String textOf(By locator) {
        return driver.findElements(locator)
                .stream()
                .map(BasePage::normalizedText)
                .filter(text -> !text.isEmpty())
                .findFirst()
                .orElse("");
    }
}

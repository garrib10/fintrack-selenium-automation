package dev.portfolio.fintrack.pages;

/**
 * What the user saw when deleting a record: the native confirmation text and
 * the success banner that followed. Tests assert both exactly.
 */
public record DeletionResult(
        String confirmation,
        String statusMessage) {
}

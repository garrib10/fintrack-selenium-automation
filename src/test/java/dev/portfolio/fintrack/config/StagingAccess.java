package dev.portfolio.fintrack.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

/**
 * Access through Vercel Authentication for a protected staging deployment.
 * A fresh browser opens one URL carrying the token; Vercel answers with an
 * access cookie that covers every later page and same-origin /api call.
 *
 * <p>The token is a credential. It is never logged, never part of a message,
 * and {@link #toString()} redacts it.
 */
public final class StagingAccess {

    static final String BYPASS_SECRET_VARIABLE =
            "FINTRACK_VERCEL_BYPASS_SECRET";

    static final String SHARE_TOKEN_VARIABLE =
            "FINTRACK_VERCEL_SHARE_TOKEN";

    private static final String SHARE_PARAMETER = "_vercel_share";

    private static final String BYPASS_PARAMETER =
            "x-vercel-protection-bypass";

    private final String kind;
    private final String queryString;

    private StagingAccess(String kind, String queryString) {
        this.kind = kind;
        this.queryString = queryString;
    }

    /** "bypass secret" or "share link", safe to log. */
    public String kind() {
        return kind;
    }

    /** The query string that grants access. Contains the token; never log it. */
    public String queryString() {
        return queryString;
    }

    @Override
    public String toString() {
        return "StagingAccess[" + kind + ", token redacted]";
    }

    /**
     * Reads the optional access token. The automation bypass secret wins over
     * a share token when both are set. Either is refused for any target but
     * staging, so a token can never be sent to production or a local server.
     */
    static Optional<StagingAccess> resolve(
            Function<String, String> environment,
            TargetEnvironment target) {

        String bypassSecret = trimmed(environment.apply(BYPASS_SECRET_VARIABLE));
        String shareToken = trimmed(environment.apply(SHARE_TOKEN_VARIABLE));

        if (bypassSecret == null && shareToken == null) {
            return Optional.empty();
        }

        if (target != TargetEnvironment.STAGING) {
            throw new IllegalStateException(
                    BYPASS_SECRET_VARIABLE
                            + " and "
                            + SHARE_TOKEN_VARIABLE
                            + " are only used with "
                            + TargetEnvironment.VARIABLE_NAME
                            + "=staging. Unset them for "
                            + target.value()
                            + ".");
        }

        if (bypassSecret != null) {
            return Optional.of(new StagingAccess(
                    "bypass secret",
                    BYPASS_PARAMETER
                            + "="
                            + encode(bypassSecret)
                            + "&x-vercel-set-bypass-cookie=true"));
        }

        return Optional.of(new StagingAccess(
                "share link",
                SHARE_PARAMETER + "=" + encode(shareTokenFrom(shareToken))));
    }

    /** Accepts the bare token or the whole share link copied from Vercel. */
    private static String shareTokenFrom(String value) {
        if (!value.contains(SHARE_PARAMETER + "=")) {
            return value;
        }

        String query;

        try {
            query = new URI(value).getRawQuery();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(
                    SHARE_TOKEN_VARIABLE
                            + " looks like a link but could not be read."
                            + " Paste the whole share link or only its token.");
        }

        if (query != null) {
            for (String pair : query.split("&")) {
                if (pair.startsWith(SHARE_PARAMETER + "=")) {
                    String token = URLDecoder.decode(
                            pair.substring(SHARE_PARAMETER.length() + 1),
                            StandardCharsets.UTF_8);

                    if (!token.isBlank()) {
                        return token;
                    }
                }
            }
        }

        throw new IllegalStateException(
                SHARE_TOKEN_VARIABLE
                        + " contains a link without a "
                        + SHARE_PARAMETER
                        + " token.");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

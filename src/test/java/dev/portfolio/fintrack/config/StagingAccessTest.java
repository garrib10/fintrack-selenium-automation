package dev.portfolio.fintrack.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag("unit")
class StagingAccessTest {

    private static final String TOKEN = "s3cret-Token_value";

    @Test
    void noTokenMeansNoAccessStep() {
        assertTrue(resolve(Map.of(), TargetEnvironment.STAGING).isEmpty());
    }

    @Test
    void bareShareTokenBecomesTheShareQuery() {
        StagingAccess access = resolve(
                Map.of("FINTRACK_VERCEL_SHARE_TOKEN", " " + TOKEN + " "),
                TargetEnvironment.STAGING).orElseThrow();

        assertAll(
                () -> assertEquals("share link", access.kind()),
                () -> assertEquals("_vercel_share=" + TOKEN, access.queryString()));
    }

    @Test
    void wholeShareLinkIsAcceptedAndOnlyItsTokenIsUsed() {
        StagingAccess access = resolve(
                Map.of("FINTRACK_VERCEL_SHARE_TOKEN",
                        "https://staging.example.test/?_vercel_share=" + TOKEN + "&other=1"),
                TargetEnvironment.STAGING).orElseThrow();

        assertEquals("_vercel_share=" + TOKEN, access.queryString());
    }

    @Test
    void bypassSecretWinsAndAsksVercelForACookie() {
        StagingAccess access = resolve(
                Map.of(
                        "FINTRACK_VERCEL_BYPASS_SECRET", TOKEN,
                        "FINTRACK_VERCEL_SHARE_TOKEN", "ignored"),
                TargetEnvironment.STAGING).orElseThrow();

        assertAll(
                () -> assertEquals("bypass secret", access.kind()),
                () -> assertEquals(
                        "x-vercel-protection-bypass=" + TOKEN
                                + "&x-vercel-set-bypass-cookie=true",
                        access.queryString()));
    }

    @Test
    void tokenCharactersAreUrlEncoded() {
        StagingAccess access = resolve(
                Map.of("FINTRACK_VERCEL_SHARE_TOKEN", "a+b/c=d"),
                TargetEnvironment.STAGING).orElseThrow();

        assertEquals("_vercel_share=a%2Bb%2Fc%3Dd", access.queryString());
    }

    @ParameterizedTest
    @EnumSource(value = TargetEnvironment.class, names = {"PRODUCTION", "LOCAL"})
    void tokensAreRefusedOutsideStagingWithoutRevealingThem(TargetEnvironment target) {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> resolve(Map.of("FINTRACK_VERCEL_SHARE_TOKEN", TOKEN), target));

        assertAll(
                () -> assertTrue(failure.getMessage().contains("only used with")),
                () -> assertFalse(failure.getMessage().contains(TOKEN)));
    }

    @Test
    void aLinkWithoutATokenIsRejectedWithoutRevealingIt() {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> resolve(
                        Map.of("FINTRACK_VERCEL_SHARE_TOKEN",
                                "https://staging.example.test/?x=" + TOKEN + "&_vercel_share="),
                        TargetEnvironment.STAGING));

        assertFalse(failure.getMessage().contains(TOKEN));
    }

    @Test
    void toStringRedactsTheToken() {
        StagingAccess access = resolve(
                Map.of("FINTRACK_VERCEL_SHARE_TOKEN", TOKEN),
                TargetEnvironment.STAGING).orElseThrow();

        assertAll(
                () -> assertFalse(access.toString().contains(TOKEN)),
                () -> assertTrue(access.toString().contains("redacted")));
    }

    private static Optional<StagingAccess> resolve(
            Map<String, String> variables,
            TargetEnvironment target) {

        return StagingAccess.resolve(variables::get, target);
    }
}

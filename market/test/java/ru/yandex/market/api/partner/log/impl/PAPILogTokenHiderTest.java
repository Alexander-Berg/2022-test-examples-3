package ru.yandex.market.api.partner.log.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PAPILogTokenHiderTest {
    private static final String MUST_HIDE_OAUTH_TOKEN_STRING =
            "oauth_token:\"sOm3-_T0ken\",blabla\n" +
                    "{oauthToken:sOm3-_T0ken},blabla\n" +
                    "[oauth-token:\"sOm3-_T0ken\"],blabla\n" +
                    "OAUTH_TOKEN:'sOm3-_T0ken'\n" +
                    "?oauthtoken=sOm3-_T0ken&blabla=...\n" +
                    "?blabla=...&oauthtoken=sOm3-_T0ken\n" +
                    "oauthtoken=sOm3-_T0ken\n";

    private static final String MUST_HIDE_TVM_TIKET_STRING =
            "x-tvm2-userid:\"123456789\",blabla\n" +
                    "xTvm2UserId:123456789\n" +
                    "?x_tvm2_user_id=123456789&blabla=\n";

    private static final String MUST_HIDE_YA_TOKEN_STRING =
            "x-ya-service-ticket:\"sOm3-_T0ken\",blabla\n" +
                    "x-yandex-service-ticket:'sOm3-_T0ken'\n" +
                    "?xYaServiceTicket=sOm3-_T0ken&blabla=\n";

    @Test
    void mustHideOauthToken() {
        String text = PAPILogTokenHider.hideCompletely(MUST_HIDE_OAUTH_TOKEN_STRING);
        assertThat(text).isNotNull().doesNotContain("sOm3-_T0ken");
        assertThatIsNotDeleted(MUST_HIDE_OAUTH_TOKEN_STRING, text, "blabla");
        assertThatIsNotDeleted(MUST_HIDE_OAUTH_TOKEN_STRING, text, ",");
        assertPermanency(text);
    }

    @Test
    void mustHideTvmTicket() {
        String text = PAPILogTokenHider.hideCompletely(MUST_HIDE_TVM_TIKET_STRING);
        assertThat(text).isNotNull().doesNotContain("123456789");
        assertThatIsNotDeleted(MUST_HIDE_TVM_TIKET_STRING, text, "blabla");
        assertThatIsNotDeleted(MUST_HIDE_TVM_TIKET_STRING, text, ",");
        assertPermanency(text);
    }

    @Test
    void mustHideYaTokens() {
        String text = PAPILogTokenHider.hideCompletely(MUST_HIDE_YA_TOKEN_STRING);
        assertThat(text).isNotNull().doesNotContain("sOm3-_T0ken");
        assertThatIsNotDeleted(MUST_HIDE_YA_TOKEN_STRING, text, "blabla");
        assertThatIsNotDeleted(MUST_HIDE_YA_TOKEN_STRING, text, ",");
        assertPermanency(text);
    }

    private void assertPermanency(String text) {
        assertThat(text).isEqualTo(PAPILogTokenHider.hideCompletely(text));
    }

    private void assertThatIsNotDeleted(String before, String after, String word) {
        assertThat(countMatches(after, word)).isEqualTo(countMatches(before, word));
    }

    private int countMatches(String text, String blabla) {
        return text.split(blabla).length - 1;
    }

}

package ru.yandex.market.mcrm.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author wanderer25
 */
public class HttpTest {
    @Test
    public void getUrlForPathWithEmptyLastSegmentShouldNotRemoveTrailingSlash() {
        Http http = Http.get()
                .path("services/members", "");

        Assertions.assertEquals(
                "https://abc-back.yandex-team.ru/api/v3/services/members/",
                http.getUrl("https://abc-back.yandex-team.ru/api/v3")
        );
    }

    @Test
    public void getUrlForPathWithNonEmptyLastSegmentShouldRemoveTrailingSlash() {
        Http http = Http.get()
                .path("services/members", "q");

        Assertions.assertEquals(
                "https://abc-back.yandex-team.ru/api/v3/services/members/q",
                http.getUrl("https://abc-back.yandex-team.ru/api/v3")
        );
    }
}

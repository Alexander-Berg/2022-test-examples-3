package ru.yandex.market.checkout.helpers;

import java.util.List;
import java.util.Objects;

import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.jetbrains.annotations.Nullable;

public class BnplTestHelper {

    private BnplTestHelper() {

    }

    @Nullable
    public static HttpHeaders findHttpHeaderByFirstRelevantUrl(List<ServeEvent> events, String url) {
        return Objects.requireNonNull(findServeEventByFirstRelevantRequestUrl(events, url)).getRequest().getHeaders();
    }

    @Nullable
    public static LoggedRequest findRequestByFirstRelevantUrl(List<ServeEvent> events, String url) {
        return Objects.requireNonNull(findServeEventByFirstRelevantRequestUrl(events, url)).getRequest();
    }

    @Nullable
    private static ServeEvent findServeEventByFirstRelevantRequestUrl(List<ServeEvent> events, String url) {
        return events.stream()
                .filter(event ->
                        event.getRequest().getUrl().equals(url))
                .findFirst()
                .orElse(null);
    }
}

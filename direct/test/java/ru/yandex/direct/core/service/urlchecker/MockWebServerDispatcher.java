package ru.yandex.direct.core.service.urlchecker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.uri.Uri;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class MockWebServerDispatcher extends Dispatcher {

    private Map<String, MockResponse> responses = new HashMap<>();

    public void respond(String requestPath, MockResponse response) {
        responses.put(requestPath, response);
    }

    public void redirectChain(List<String> redirectUrlsChain, MockResponse response) {
        checkArgument(redirectUrlsChain.size() > 1, "There should be at least 2 urls in redirect chain");

        String contextUrl = null;
        for (int i = 0; i < redirectUrlsChain.size() - 1; i++) {
            String currUrl = redirectUrlsChain.get(i);
            String nextUrl = redirectUrlsChain.get(i + 1);
            String currAbsUrl = contextUrl = buildRedirectAbsoluteUrl(contextUrl, currUrl);
            responses.put(currAbsUrl, new MockResponse().setResponseCode(301).setHeader("Location", nextUrl));
        }

        String lastUrl = buildRedirectAbsoluteUrl(contextUrl, redirectUrlsChain.get(redirectUrlsChain.size() - 1));
        responses.put(lastUrl, response);
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        MockResponse response = responses.get(request.getPath());
        checkState(response != null, String.format("No response registered for [%s] path", request.getPath()));

        Thread.sleep(response.getBodyDelay(TimeUnit.MILLISECONDS));
        return response;
    }

    private String buildRedirectAbsoluteUrl(String contextUrl, String redirectUrl) {
        if (contextUrl == null) {
            return redirectUrl;
        }
        return Uri.create(Uri.create(contextUrl), redirectUrl).toUrl();
    }
}

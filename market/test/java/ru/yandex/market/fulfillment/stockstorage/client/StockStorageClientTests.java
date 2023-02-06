package ru.yandex.market.fulfillment.stockstorage.client;

import org.springframework.test.web.client.RequestMatcher;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;

public class StockStorageClientTests {

    private StockStorageClientTests() {
        throw new AssertionError();
    }

    public static String buildUrl(String host, Object... parts) {
        StringBuilder path = new StringBuilder(host);
        for (Object part : parts) {
            if (!part.toString().startsWith("/")) {
                path.append("/");
            }
            path.append(part);
        }
        return path.toString();
    }

    public static RequestMatcher checkBody(String expectedJson) {
        return content().string(new JsonMatcher(expectedJson));
    }
}

package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.ApiStrings;

/**
 * @see {@link ru.yandex.market.api.internal.ClckHttpClient}
 */
@Service
public class ClckTestClient extends AbstractFixedConfigurationTestClient {

    public ClckTestClient() {
        super("Clck");
    }

    public void clck(String url, String shortUrl) {
        configure(x -> x
            .get()
            .param("url", url))
            .ok()
            .body(ApiStrings.getBytes(shortUrl));
    }

    public void clck(String shortUrl) {
        configure(x -> x
            .get())
            .ok()
            .body(ApiStrings.getBytes(shortUrl));
    }

    public void simpleUrl(String url, String shortUrl) {
        configure(x -> x
                .get()
                .param("url", url)
                .param("type", "simple")
        ).ok()
        .body(ApiStrings.getBytes(shortUrl));
    }
}

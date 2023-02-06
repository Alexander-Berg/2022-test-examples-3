package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;

/**
 * Created by tesseract on 03.03.17.
 */
@Service
public class MapsSuggestTestClient extends AbstractFixedConfigurationTestClient {

    public MapsSuggestTestClient() {
        super("MapsSuggest");
    }

    public void getSuggest(String text, String resource) {
        configure(x -> x.get().param("part", text)).ok().body(resource);
    }

    public void getSuggest(String text, int regionId, String resource) {
        configure(
            x -> x.get()
                .param("part", text)
                .param("in", String.valueOf(regionId))
        ).body(resource);
    }
}

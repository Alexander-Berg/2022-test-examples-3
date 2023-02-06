package ru.yandex.market.api.util.httpclient.clients;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

import java.util.Map;

/**
 * Created by vivg on 17.03.17.
 */
@Service
public class GuruassTestClient extends AbstractFixedConfigurationTestClient {

    public static final int TYRES_CATEGORY = 90490;

    public GuruassTestClient() {
        super("Guruass");
    }

    public HttpResponseConfigurer getControlIds(int categoryId) {
        HttpResponseConfigurer configurer = configure(x ->
            x.get()
                .param("hid", String.valueOf(categoryId))
        );
        if (TYRES_CATEGORY == categoryId) {
            configurer = configurer
                .ok()
                .body("/ru/yandex/market/api/util/httpclient/clients/guruass.json");
        } else {
            configurer = configurer
                .status(HttpResponseStatus.NOT_FOUND)
                .body("unknown category id".getBytes(ApiStrings.UTF8));
        }
        return configurer;
    }

    public HttpResponseConfigurer getControls(long categoryId, Map<String, String> params, String resourcePath) {
        return configure(x -> {
            HttpRequestExpectationBuilder builder = x.get().param("hid", String.valueOf(categoryId));
            for (Map.Entry<String, String> e : params.entrySet()) {
                builder = builder.param("filter", e.getKey() + ":" + e.getValue());
            }
            return builder;
        }).ok().body(resourcePath);
    }
}

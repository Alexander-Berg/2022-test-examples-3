package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Component;

/**
 * Created by tesseract on 12.07.17.
 */
@Component
public class ModelParamsTestClient extends AbstractFixedConfigurationTestClient {

    public ModelParamsTestClient() {
        super("ModelParams");
    }

    public void params(long id, String body) {
        configure(x -> x
            .get()
            .serverMethod("params")
            .param("modelid", String.valueOf(id))
        )
            .body(body).ok();
    }
}

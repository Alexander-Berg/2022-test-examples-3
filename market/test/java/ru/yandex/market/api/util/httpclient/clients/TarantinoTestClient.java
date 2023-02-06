package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

import javax.inject.Inject;

/**
 * @author dimkarp93
 */
@Service
public class TarantinoTestClient extends AbstractFixedConfigurationTestClient {
    @Inject
    public TarantinoTestClient() {
        super("Tarantino");
    }

    public void navigationPage(String id, String resultFileName) {
        configure(x -> x
            .get()
            .param("nid", id)
            .serverMethod("tarantino/getContextPage"))
            .ok()
            .body(resultFileName);
    }

    public void navigationPage(String id, String domain, String resultFileName) {
        configure(x -> x
            .get()
            .param("nid", id)
            .param("domain", domain)
            .serverMethod("tarantino/getContextPage"))
            .ok()
            .body(resultFileName);
    }

    public HttpResponseConfigurer model(long modelId, String domain) {
        return configure(x -> x
            .get()
            .param("domain", domain)
            .param("type", "mp_product")
            .param("product_id", String.valueOf(modelId))
            .serverMethod("tarantino/getContextPage")
        );
    }

    public void model(long modelId, String domain, String resultFileName) {
        model(modelId, domain)
            .ok()
            .body(resultFileName);
    }
}

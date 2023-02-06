package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;

import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

/**
 * Created by fettsery on 09.06.18.
 */
@Service
public class PersComparisonTestClient extends AbstractFixedConfigurationTestClient {
    protected PersComparisonTestClient() {
        super("PersComparison");
    }

    public HttpResponseConfigurer getComparsionLists(long uid, String resultFileName) {
        return configure(b -> b.serverMethod("/api/comparison/UID/" + uid)
                .get()
        ).ok().body(resultFileName);
    }

    public void merge(String userFrom, long userTo) {
        configure(
                b -> b.patch()
                        .serverMethod("api/comparison/UUID/" + userFrom)
                        .param("userIdTo", String.valueOf(userTo))
        ).ok().emptyResponse();
    }
}

package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

/**
 * Created by fettsery on 18.06.18.
 */
@Service
public class PersBasketTestClient extends AbstractFixedConfigurationTestClient {
    protected PersBasketTestClient() {
        super("PersBasket");
    }

    public HttpResponseConfigurer getItems(long uid, String resultFileName) {
        return configure(b -> b.serverMethod("/users/UID/" + uid + "/wishlist")
            .get()
        ).ok().body(resultFileName);
    }

    public HttpResponseConfigurer getLabels(long uid, String resultFileName) {
        return configure(b -> b.serverMethod("/users/UID/" + uid + "/tags")
            .get()
        ).ok().body(resultFileName);
    }

    public void merge(String userFrom, long userTo) {
        configure(
            b -> b.patch()
                .serverMethod("/users/UUID/" + userFrom + "/wishlist")
                .param("idTo", String.valueOf(userTo))
        ).ok().emptyResponse();
    }
}

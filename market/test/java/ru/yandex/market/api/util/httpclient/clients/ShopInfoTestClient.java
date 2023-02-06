package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;

@Service
public class ShopInfoTestClient extends AbstractFixedConfigurationTestClient {
    public ShopInfoTestClient() {
        super("ShopInfo");
    }

    public void supplier(long supplierId, String filename) {
        configure(x -> x.serverMethod("/partnerInfo")
                        .param("partner-id", String.valueOf(supplierId)))
            .body(filename);
    }
}

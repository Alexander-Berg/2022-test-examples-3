package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;
import ru.yandex.market.ir.http.UltraController;

import java.util.Arrays;

/**
 * Created by fettsery on 11.04.18.
 */
@Service
public class UltraControllerTestClient extends AbstractFixedConfigurationTestClient {
    public UltraControllerTestClient() {
        super("UltraController");
    }

    public HttpResponseConfigurer enrichOffer(UltraController.Offer offer, String filename) {
        return configure(r -> r.serverMethod("/enrichSingleOffer")
            .body(x -> Arrays.equals(offer.toByteArray(), x), "ultra controller post request protobuf")
            .post())
            .ok()
            .body(filename);
    }
}

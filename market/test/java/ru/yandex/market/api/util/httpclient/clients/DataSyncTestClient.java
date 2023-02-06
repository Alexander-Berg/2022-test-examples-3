package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;

/**
 * Created by tesseract on 04.04.17.
 */
@Service
public class DataSyncTestClient extends AbstractFixedConfigurationTestClient {

    public DataSyncTestClient() {
        super("DataSyncAddress");
    }

    public void getDeliveryAddresses(long uid, String resource) {
        configure(x -> x
            .get()
            .header("Authorization", "ClientToken token=3bddd1a316ea44d89a1e4db6b2843c3b;uid=" + uid)
            .serverMethod("/v1/personality/profile/market/delivery_addresses")
        )
            .ok()
            .body(resource);
    }

    public void getUserAddresses(long uid, String resource) {
        configure(x -> x
            .get()
            .serverMethod("/v2/" + uid + "/personality/profile/addresses")
        )
            .ok()
            .body(resource);
    }

    public void removeUserAddresses(long uid, String addressId) {
        configure(x -> x
            .delete()
            .header("Authorization", "ClientToken token=3bddd1a316ea44d89a1e4db6b2843c3b;uid=" + uid)
            .serverMethod("/v1/personality/profile/market/delivery_addresses/" + addressId)

        )
            .ok();
    }
}

package ru.yandex.market.api.tests.feature.clients;

import ru.yandex.market.api.listener.expectations.HttpExpectations;
import ru.yandex.market.api.listener.expectations.HttpRequestExpectationBuilder;
import ru.yandex.market.api.listener.expectations.HttpResponseConfigurer;

import java.util.function.Function;

/**
 * @author dimkarp93
 */
public class VendorApiTestClient {
    private HttpExpectations httpExpectations;

    public VendorApiTestClient(HttpExpectations httpExpectations) {
        this.httpExpectations = httpExpectations;
    }

    private HttpResponseConfigurer configure(
        Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> fn
    ) {
        return httpExpectations.configure(
            fn.apply(new HttpRequestExpectationBuilder())
        );
    }

    public void vendorPermissions(long vendorId, String response) {
        configure(x -> x.serverMethod("permissionsByVendor/" + vendorId)
                        .param("uid", String.valueOf(vendorId)))
            .ok()
            .body(response);
    }

    public void agencyComments(long agencyId,
                               long requestId,
                               long uid,
                               String response) {
        configure(x -> x.serverMethod("agencies/" + agencyId + "/modelEdit/requests/" + requestId + "/comments")
                        .param("uid", String.valueOf(uid)))
            .ok()
            .body(response);
    }

    public void agencyRequest(long agencyId,
                              long requestId,
                              long uid,
                              String response) {
        configure(x -> x.serverMethod("agencies/" + agencyId + "/modelEdit/requests/" + requestId)
                        .param("uid", String.valueOf(uid)))
            .ok()
            .body(response);
    }

}

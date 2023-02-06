package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

/**
 * @author dimkarp93
 */
@Service
public class VendorApiTestClient extends AbstractFixedConfigurationTestClient {
    public VendorApiTestClient() {
        super("VendorApi");
    }

    public HttpResponseConfigurer agencyCategoryParamsByModel(long vendorId,
                                                              long categoryId,
                                                              String response) {
        return configure(r -> r.serverMethod("agencies/" + vendorId + "/modelEdit/params/byCategory")
            .param("categoryId", String.valueOf(categoryId))
            .get())
            .ok()
            .body(response);
    }

    public HttpResponseConfigurer vendorCategoryParamsByModel(long vendorId,
                                                              long categoryId,
                                                              String response) {
        return configure(r -> r.serverMethod("vendors/" + vendorId + "/modelEdit/params/byCategory")
            .param("categoryId", String.valueOf(categoryId))
            .get())
            .ok()
            .body(response);
    }

    public HttpResponseConfigurer vendorModelParamsByModel(long vendorId,
                                                           long modelId,
                                                           String response) {
        return configure(r -> r.serverMethod("vendors/" + vendorId + "/modelEdit/params/byModel")
                                .param("modelId", String.valueOf(modelId))
                                .get())
            .ok()
            .body(response);
    }

    public HttpResponseConfigurer vendorByBrandId(long brandId,
                                                  String response) {
        return configure(r -> r.serverMethod("vendors")
                                .param("text", String.valueOf(brandId)))
            .ok()
            .body(response);
    }
}

package ru.yandex.market.vendor;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.vendor.products.model.VendorProduct;
import ru.yandex.vendor.vendors.VendorService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/VendorServiceTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/VendorServiceTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
public class VendorServiceTest extends AbstractVendorPartnerFunctionalTest {

    @Autowired
    private WireMockServer csBillingApiMock;

    @Autowired
    private VendorService vendorService;

    @Test
    void canNotCreateProductCampaignAndVendorDatasourceWithMoreThen1ParamInResponse() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames" +
                        "=IS_DEFAULT_ANALYTICS_FOR_OFFER")
                .willReturn(aResponse().withBody(
                        getStringResource("/canNotCreateProductCampaignAndVendorDatasource" +
                                "/retrofit2_2_params_response.json"))));

        assertThrows(
                IllegalArgumentException.class,
                () -> vendorService.createProductCampaignAndVendorDatasource(
                        2L,
                        1000002,
                        null,
                        100500,
                        VendorProduct.MARKET_ANALYTICS,
                        true,
                        1
                )
        );
    }

    @Test
    void canNotCreateProductCampaignAndVendorDatasourceWith0ParamsInResponse() {
        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames" +
                        "=IS_DEFAULT_ANALYTICS_FOR_OFFER")
                .willReturn(aResponse().withBody(
                        getStringResource("/canNotCreateProductCampaignAndVendorDatasource" +
                                "/retrofit2_0_params_response.json"))));

        assertThrows(
                IllegalArgumentException.class,
                () -> vendorService.createProductCampaignAndVendorDatasource(
                        2L,
                        1000002,
                        null,
                        100500,
                        VendorProduct.MARKET_ANALYTICS,
                        true,
                        1
                )
        );
    }
}

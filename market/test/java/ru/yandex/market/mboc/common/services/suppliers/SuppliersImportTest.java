package ru.yandex.market.mboc.common.services.suppliers;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.fulfillment.SupplierInfo;
import ru.yandex.market.mboc.common.config.RestApiClientsConfig;

/**
 * @author s-ermakov
 */
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:linelength"})
public class SuppliersImportTest {

    private MockRestServiceServer server;
    private MbiApiClient mbiApiClient;

    @Before
    public void setUp() throws Exception {
        RestApiClientsConfig restApiClientsConfig = new RestApiClientsConfig();
        ReflectionTestUtils.setField(restApiClientsConfig, "mbiApiUrl", "http://some-mbi-server.ru");
        ReflectionTestUtils.setField(restApiClientsConfig, "connectionTimeoutSeconds", 60);
        ReflectionTestUtils.setField(restApiClientsConfig, "socketTimeoutSeconds", 600);
        ReflectionTestUtils.setField(restApiClientsConfig, "maxConnections", 1);

        RestTemplate restTemplate = restApiClientsConfig.restTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        mbiApiClient = restApiClientsConfig.mbiApiClient();
        ReflectionTestUtils.setField(mbiApiClient, "restTemplate", restTemplate);
    }

    @Test
    public void testSuppliersResponse() throws IOException {
        server.expect(MockRestRequestMatchers.requestTo(
            "http://some-mbi-server.ru/fulfillment/suppliers?type=REAL_SUPPLIER&type=FIRST_PARTY&type=THIRD_PARTY"))
            .andRespond(MockRestResponseCreators.withSuccess(
                IOUtils.resourceToByteArray("supplier/mbi_response.xml", this.getClass().getClassLoader()),
                org.springframework.http.MediaType.APPLICATION_XML));

        List<SupplierInfo> supplierInfoList = mbiApiClient.getSupplierInfoList(
            SupplierType.REAL_SUPPLIER, SupplierType.FIRST_PARTY, SupplierType.THIRD_PARTY);

        server.verify();

        Assertions.assertThat(supplierInfoList)
            .usingRecursiveFieldByFieldElementComparator()
            .hasSize(370)
            .contains(
                new SupplierInfo.Builder()
                    .setId(10263700L)
                    .setName(" EReznikova синий магазин")
                    .setSupplierType(SupplierType.THIRD_PARTY)
                    .build(),
                new SupplierInfo.Builder()
                    .setId(10264169L)
                    .setName("Беру")
                    .setOrganisationName("ООО \"Яндекс.Маркет\"")
                    .setPrepayRequestId(23128L)
                    .setSupplierType(SupplierType.FIRST_PARTY)
                    .build(),
                new SupplierInfo.Builder()
                    .setId(9191919L)
                    .setRsId("09010901")
                    .setName("all fields")
                    .setOrganisationName("All Fields")
                    .setPrepayRequestId(919191L)
                    .setSupplierType(SupplierType.THIRD_PARTY)
                    .setDropship(true)
                    .setGoodContentAllowed(true)
                    .setNeedContentAllowed(true)
                    .build()
            );
    }
}

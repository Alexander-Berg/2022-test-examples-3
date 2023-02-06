package ru.yandex.market.wms.servicebus.api.wrap.client;

import java.util.Arrays;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.EqualToXmlPattern;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.WrapInforClient;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.dto.IdentifierMappingDto;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class WrapClientTest  extends IntegrationTest {

    @Autowired
    private WrapInforClient wrapInforClient;

    private static final String MSKU_1 = "123.456";
    private static final String SKU_1 = "ROV0000001";
    private static final long STORER_1 = 111111L;
    private static final String MSKU_2 = "abc.def";
    private static final String SKU_2 = "ROV0000002";
    private static final long STORER_2 = 222222L;

    private WireMockServer server;

    @BeforeEach
    protected void initInforServer() {
        server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(19000));
        server.start();
    }

    @AfterEach
    protected void shutdownInforServer() {
        server.stop();
    }

    @Test
    public void shouldConnect() {
        server.stubFor(WireMock.post(urlEqualTo("/identifierMapping"))
                .withRequestBody(new EqualToXmlPattern(
                        extractFileContent("api/wrap/client/map-reference-items/request.xml")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                        .withBody(extractFileContent("api/wrap/client/map-reference-items/response.xml"))));

        List<IdentifierMappingDto> result = wrapInforClient.mapReferenceItems(Arrays.asList(
                new UnitId(MSKU_1, STORER_1, MSKU_1),
                new UnitId(MSKU_2, STORER_2, MSKU_2)
        ));

        server.verify(postRequestedFor(urlEqualTo("/identifierMapping")));

        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get(0).getInforUnitId().getFormattedId(), SKU_1);
        Assert.assertEquals(result.get(1).getInforUnitId().getFormattedId(), SKU_2);
    }

}

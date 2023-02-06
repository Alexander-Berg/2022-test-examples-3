package ru.yandex.market.checkout.pushapi.web;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.providers.OrderShipmentStatusProvider;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * @author mmetlov
 */
public abstract class AbstractShopWebTestBase extends AbstractWebTestBase {
    protected static final long SHOP_ID = 774L;
    @Autowired
    protected WireMockServer shopadminStubMock;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PushApiTestSerializationService testSerializationService;
    protected DataType dataType;

    public AbstractShopWebTestBase(DataType dataType) {
        this.dataType = dataType;
    }

    @BeforeEach
    public void setUpClass() {
        var settings = new Settings(
                "http://localhost:" + shopadminStubMock.port() + "/svn-shop/" + SHOP_ID,
                "asdasd", dataType, AuthType.HEADER, false
        );
        mockPostSettings(SHOP_ID, settings);
        updateSettings(SHOP_ID, settings);
    }

    protected void updateSettings(long shopId, Settings settings) {
        mockPostSettings(shopId, settings, false);
        settingsService.updateSettings(shopId, settings, false);
    }

    protected ResultActions performOrderShipmentStatusOldFormat(long shopId) throws Exception {
        return performOrderShipmentStatus(
                shopId,
                // Старый формат нельзя записать в виде java-объекта.
                "<order id=\"123\"\n" +
                "       status=\"PROCESSING\"\n" +
                "       substatus=\"\"\n" +
                "       creation-date=\"07-10-2014 16:12:58\"\n" +
                "       currency=\"RUR\"\n" +
                "       items-total=\"2190\"\n" +
                "       total=\"2190\"\n" +
                "       payment-type=\"PREPAID\"\n" +
                "       payment-method=\"YANDEX\"\n" +
                "       fake=\"false\">\n" +
                "   <items>\n" +
                "       <item feed-id=\"200305173\" offer-id=\"4\" feed-category-id=\"{{feedcategory}}\" offer-name=\"{{offername}}\" count=\"1\" price=\"100\"/>\n" +
                "   </items>\n" +
                "   <delivery type=\"PICKUP\" price=\"0\" service-name=\"Почта России\" region-id=\"2\">\n" +
                "       <dates from-date='25-08-2015' to-date='25-09-2015' />\n" +
                "       <shipment weight=\"10\" width=\"10\" height=\"10\" depth=\"10\" status=\"NEW\"/>\n" +
                "       <outlet id=\"567633\"/>\n" +
                "   </delivery>\n" +
                "   <buyer id='32kj5hkhasdas231lkj' last-name='последнееимя' first-name='первоеимя' middle-name='среднееимя' phone='+77777777777' email='ymail@y.mail' uid=\"54321\"/>\n" +
                "</order>"
        );
    }

    protected ResultActions performOrderShipmentStatusNewFormat(long shopId) throws Exception {
        shopadminStubMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/svn-shop/" + shopId
                + "/order/shipment/status")).willReturn(aResponse().withStatus(200)));

        return performOrderShipmentStatus(
                shopId,
                testSerializationService.serialize(OrderShipmentStatusProvider.buildOrderShipmentStatus())

        );
    }

    private ResultActions performOrderShipmentStatus(long shopId, String body) throws Exception {
        RequestContextHolder.createNewContext();
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/shops/{shopId}/order/shipment/status", shopId)
                        .content(body)
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(result));
    }
}

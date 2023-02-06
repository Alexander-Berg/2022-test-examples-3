package ru.yandex.market.shopadminstub.stub;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.yandex.market.helpers.CartParameters;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.ItemDeliveryOptionProvider;
import ru.yandex.market.providers.ItemProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.util.TestSerializationService;
import ru.yandex.market.util.feeddispatcher.FeedDispatcherConfigurer;
import ru.yandex.market.util.report.ReportConfigurer;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.ONLY_CARD;

public class FeedDispatcherCallFailureTest extends AbstractTestBase {

    @Autowired
    private WireMockServer feedDispatcherMock;
    @Autowired
    private ReportConfigurer reportConfigurer;
    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private FeedDispatcherConfigurer feedDispatcherConfigurer;

    @Test
    public void shouldNotSetCountTo0IfFeedDispatcherResponseParseFailed() throws Exception {
        CartParameters cartParameters = configureCartParameters();

        feedDispatcherConfigurer.configureFeedDispatcher(cartParameters.getFeedDispatcherOffers());
        feedDispatcherMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/smart/offer"))
                                           .withQueryParam("feed_id", equalTo(String.valueOf(383183L)))
                                           .withQueryParam("offer_id", equalTo("2"))
                                           .willReturn(ResponseDefinitionBuilder.responseDefinition()
                                                               .withStatus(200)
                                                               .withBody("Some content that is not XML")));

        assertItemsCountNotZero(cartParameters);
    }

    @Test
    public void shouldNotSetCountTo0IfFeedDispatcherCallFailed() throws Exception {
        CartParameters cartParameters = configureCartParameters();

        feedDispatcherConfigurer.configureFeedDispatcher(cartParameters.getFeedDispatcherOffers());
        feedDispatcherMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/smart/offer"))
                                           .withQueryParam("feed_id", equalTo(String.valueOf(383183L)))
                                           .withQueryParam("offer_id", equalTo("2"))
                                           .willReturn(ResponseDefinitionBuilder.responseDefinition()
                                                               .withStatus(500)
                                                               .withBody("Server error")));

        assertItemsCountNotZero(cartParameters);
    }

    private void assertItemsCountNotZero(CartParameters cartParameters) throws Exception {
        mockMvc.perform(post("/{shopId}/cart", cartParameters.getShopId())
                                .content(testSerializationService.serializeXml(cartParameters.getCartRequest()))
                                .contentType(MediaType.APPLICATION_XML))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/cart/items/item[%d]/@count", 1).number(1d))
                .andExpect(xpath("/cart/items/item[%d]/@count", 2).number(1d));
    }

    private CartParameters configureCartParameters() throws IOException {
        CartParameters cartParameters = new CartParameters(
                CartRequestProvider.buildCartRequest(
                        ItemProvider.buildItem(383182L, "1",
                                               ItemDeliveryOptionProvider.buildFree(),
                                               ItemDeliveryOptionProvider.buildAverage(),
                                               ItemDeliveryOptionProvider.buildFastest()
                        ),
                        ItemProvider.buildItem(383183L, "2",
                                               ItemDeliveryOptionProvider.buildFree(),
                                               ItemDeliveryOptionProvider.buildAverage(ONLY_CARD),
                                               ItemDeliveryOptionProvider.buildFastest()
                        )
                )
        );

        reportConfigurer.mockReport(cartParameters.getReportParameters());
        reportConfigurer.mockGeo(cartParameters.getReportGeoParameters());
        return cartParameters;
    }
}

package ru.yandex.market.ff4shops.delivery.stocks.source;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.model.entity.PartnerFulfillmentEntity;
import ru.yandex.market.ff4shops.repository.PartnerRepositoryService;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DbUnitDataSet(before = "PushApiStockSourceImplTest.csv")
public class PushApiStockSourceImplTest extends FunctionalTest {

    @Autowired
    private StockSource pushApiStockSource;

    @Autowired
    private PartnerRepositoryService partnerRepositoryService;

    @Autowired
    @Qualifier("pushApiRestTemplate")
    private RestTemplate mbiApiRestTemplate;

    @Autowired
    @Value("${market.checkout.pushapi.url}")
    private String pushApiUrl;
    private MockRestServiceServer pushApiMockRestServiceServer;

    @BeforeEach
    public void initMock() {
        pushApiMockRestServiceServer = MockRestServiceServer.createServer(mbiApiRestTemplate);
    }

    @Test
    void shouldNotRequestArchivedOffers() {
        long warehouseId = 10L;

        Collection<PartnerFulfillmentEntity> fulfillments =
                partnerRepositoryService.findFulfillmentByServiceId(warehouseId);

        pushApiMockRestServiceServer.expect(ExpectedCount.once(),
                        requestTo(String.format("%s/shops/100/stocks?context=MARKET&apiSettings=PRODUCTION" +
                                        "&logResponse=false",
                                pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().xml(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<stocksRequest warehouse-id=\"10\">" +
                                "<skus><sku>AAA</sku><sku>VVV</sku></skus></stocksRequest>"
                ))
                .andRespond(withSuccess( //language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"AAA\" warehouseId=\"10\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "<stock sku=\"BBB\" warehouseId=\"10\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));

        List<UnitId> unitIds = List.of(
                new UnitId("AAA", 100L, "AAA"),
                new UnitId("BBB", 100L, "BBB", true),
                new UnitId("VVV", 100L, "VVV")
        );

        List<ItemStocks> stocks = pushApiStockSource.getStocks(warehouseId, fulfillments, unitIds);

        assertThat(stocks.size(), equalTo(3));
    }
}

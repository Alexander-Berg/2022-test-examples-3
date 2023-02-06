package ru.yandex.market.api.partner.controllers.stocks;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStockInfo;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.order.ResourceUtilitiesMixin;
import ru.yandex.market.api.partner.controllers.stocks.checkers.PartnerOfferStockUpdateEnabledChecker;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.logbroker.event.datacamp.DataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.core.ff4shops.FF4ShopsOpenApiClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.ff4shops.client.model.StocksWarehouseGroupDto;
import ru.yandex.market.mbi.ff4shops.client.model.WarehouseDto;
import ru.yandex.market.mbi.ff4shops.client.model.WrappedStocksWarehouseGroupDto;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.context.FunctionalTestHelper.makeRequest;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "OfferStockControllerTest.updateStocks.warehouses.before.csv")
class OfferStockControllerTest extends FunctionalTest implements ResourceUtilitiesMixin {
    private static final String PAPI_PUSH_STOCKS_ENABLE = "partner_api.push_stocks.enable";

    @Autowired
    @Qualifier("marketQuickStocksLogbrokerService")
    private LogbrokerService logbrokerService;

    @Autowired
    private PartnerOfferStockUpdateEnabledChecker offerStockUpdateEnabledChecker;

    @Autowired
    TestableClock clock;

    @Autowired
    EnvironmentService environmentService;

    @Autowired
    FF4ShopsOpenApiClient ff4ShopsOpenApiClient;

    @Captor
    private final ArgumentCaptor<SyncChangeOfferLogbrokerEvent> changeOfferEventCaptor =
            ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);

    @BeforeEach
    void setUp() {
        // set clock to "2017-12-04T01:15:01.000+03:00"
        clock.setFixed(DateTimes.toInstant(2017, 12, 4, 1, 15, 1), DateTimes.MOSCOW_TIME_ZONE);
        when(ff4ShopsOpenApiClient.getGroupByPartner(anyLong()))
                .thenReturn(CompletableFuture.completedFuture(new WrappedStocksWarehouseGroupDto()));
    }

    @Test
    void updateStocksValid() {
        setPushStocksEnabled();

        var response = assertDoesNotThrow(() -> makeRequest(
                stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                resourceAsString("update_stocks_valid.json")));

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), equalTo("{\"status\":\"OK\"}"));

        response = assertDoesNotThrow(() -> makeRequest(
                stocksUri(10774L), HttpMethod.PUT, Format.XML,
                resourceAsString("update_stocks_valid.xml")));

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), equalTo("<response><status>OK</status></response>"));
    }

    @Test
    void updateStocksNotValid() {
        setPushStocksEnabled();

        HttpClientErrorException error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_no_items.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_nulls.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_stock_type.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_negative_stock.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_float_stock.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void updateStocksSkuNotValid() {
        setPushStocksEnabled();

        var error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_sku_empty.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_sku_too_long.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_sku_leading_whitespace.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_sku_trailing_whitespace.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void updateStocksUpdatedAtNotValid() {
        setPushStocksEnabled();

        var error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_datetime.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_datetime_too_old.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_datetime_in_future.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void updateStocksNotPartnerWarehouse() {
        long partnerCampaignWithoutWarehouses = 1000571241L;

        setPushStocksEnabled();

        var error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_warehouse.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertThat(error.getResponseBodyAsString(), equalTo("{\"status\":\"ERROR\"," +
                "\"errors\":[{\"code\":\"INVALID_WAREHOUSE_ID\",\"message\":\"4123456 is not a warehouse of partner " +
                "774\"}]}"));

        error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(partnerCampaignWithoutWarehouses), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_valid.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertThat(error.getResponseBodyAsString(), equalTo("{\"status\":\"ERROR\"," +
                "\"errors\":[{\"code\":\"INVALID_WAREHOUSE_ID\",\"message\":\"2 is not a warehouse of partner 666\"}," +
                "{\"code\":\"INVALID_WAREHOUSE_ID\",\"message\":\"3 is not a warehouse of partner 666\"}]}"));
    }

    @Test
    void updateStocksDuplicateWarehouseStock() {
        setPushStocksEnabled();

        HttpClientErrorException error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_invalid_duplicate_warehouse_stock.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void updateStocksCampaignNotFound() {
        setPushStocksEnabled();

        HttpClientErrorException error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(11111111L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_valid.json")));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    @DbUnitDataSet(before = "OfferStockControllerTest.updateStocks.business.before.csv")
    void updateStocksPublishedToMarketQuickTopic() {
        setPushStocksEnabled();

        assertDoesNotThrow(() -> makeRequest(
                stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                resourceAsString("update_stocks_valid.json")));

        //Проверяем, что эвент был отправлен в Логброкер
        verify(logbrokerService, times(1)).publishEvent(changeOfferEventCaptor.capture());

        List<DataCampOffer.Offer> offers = changeOfferEventCaptor.getValue().getPayload()
                .stream()
                .map(DataCampEvent::convertToDataCampOffer)
                .collect(Collectors.toList());
        System.out.println("offers: " + offers);
        assertEquals(2, offers.size());

        ProtoTestUtil.assertThat(offers.get(0).getIdentifiers())
                .isEqualTo(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(100)
                        .setShopId(774)
                        .setWarehouseId(2)
                        .setOfferId("A200.190")
                        .build()
                );
        ProtoTestUtil.assertThat(offers.get(0).getStockInfo().getPartnerStocks())
                .isEqualTo(DataCampOfferStockInfo.OfferStocks.newBuilder()
                        .setCount(15)
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setSeconds(Instant.parse("2017-12-03T22:15:01Z").getEpochSecond())))
                        .build());


        ProtoTestUtil.assertThat(offers.get(1).getIdentifiers())
                .isEqualTo(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(100)
                        .setShopId(774)
                        .setWarehouseId(3)
                        .setOfferId("A200.190")
                        .build()
                );
        ProtoTestUtil.assertThat(offers.get(1).getStockInfo().getPartnerStocks())
                .isEqualTo(DataCampOfferStockInfo.OfferStocks.newBuilder()
                        .setCount(0)
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setSeconds(Instant.parse("2017-12-03T22:05:55Z").getEpochSecond())))
                        .build());
    }

    @Test
    void updateStocksPublishedToMarketQuickTopicWithoutBusiness() {
        setPushStocksEnabled();

        assertDoesNotThrow(() -> makeRequest(
                stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                resourceAsString("update_stocks_valid.json")));

        //Проверяем, что эвент был отправлен в Логброкер
        verify(logbrokerService, times(1)).publishEvent(changeOfferEventCaptor.capture());

        List<DataCampOffer.Offer> offers = changeOfferEventCaptor.getValue().getPayload()
                .stream()
                .map(DataCampEvent::convertToDataCampOffer)
                .collect(Collectors.toList());
        System.out.println("offers: " + offers);
        assertEquals(2, offers.size());

        ProtoTestUtil.assertThat(offers.get(0).getIdentifiers())
                .isEqualTo(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setShopId(774)
                        .setWarehouseId(2)
                        .setOfferId("A200.190")
                        .build()
                );

        ProtoTestUtil.assertThat(offers.get(1).getIdentifiers())
                .isEqualTo(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setShopId(774)
                        .setWarehouseId(3)
                        .setOfferId("A200.190")
                        .build()
                );
    }

    @Test
    void updateStocksDisabled() {
        environmentService.setValue(PAPI_PUSH_STOCKS_ENABLE, "false");

        HttpServerErrorException error = assertThrows(HttpServerErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_valid.json")));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }


    @Test
    void updateStocksDisabledByNotInEnabledPartnersList() {
        environmentService.setValue(PAPI_PUSH_STOCKS_ENABLE, "true");
        HttpClientErrorException error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_valid.json")));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, error.getStatusCode());
    }

    /**
     * Проверяет, что для склада в группе возвращаем 400.
     */
    @Test
    void updateStocksWarehouseInGroup() {
        setPushStocksEnabled();
        mockWarehouseGroup();

        HttpClientErrorException error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_stocks_valid.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertThat(error.getResponseBodyAsString(), equalTo("{\"status\":\"ERROR\"," +
                "\"errors\":[{\"code\":\"GROUPED_WAREHOUSE\",\"message\":\"774 partner in warehouse group 1\"}]}"));
    }

    /**
     * Проверяет, что для главного склада из разрешенного бизнеса обновляем стоки на всех складах группы.
     */
    @Test
    @DbUnitDataSet(before = "OfferStockControllerTest.updateStocks.business.before.csv")
    void updateMainWarehouseStocksInGroup() {
        setPushStocksEnabled();
        mockWarehouseGroup();

        ResponseEntity<String> response = makeRequest(stocksUri(10774L), HttpMethod.PUT, Format.JSON,
                resourceAsString("update_shared_stocks.json"));
        org.assertj.core.api.Assertions.assertThat(response)
                .returns(HttpStatus.OK, ResponseEntity::getStatusCode);

        //Проверяем события в логрокер
        verify(logbrokerService, times(2)).publishEvent(changeOfferEventCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(changeOfferEventCaptor.getAllValues()).hasSize(2);
        assertOffers(0, 774, 10);
        assertOffers(1, 666, 11);
    }

    /**
     * Проверяет, что для второстепенного склада из разрешенного бизнеса возвращаем 400.
     */
    @Test
    void updateSecondaryWarehouseStocksInGroupAllowed() {
        setPushStocksEnabled();
        mockWarehouseGroup();

        HttpClientErrorException error = assertThrows(HttpClientErrorException.class,
                () -> makeRequest(stocksUri(1000571241), HttpMethod.PUT, Format.JSON,
                        resourceAsString("update_shared_secondary_stocks.json")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertThat(error.getResponseBodyAsString(), equalTo("{\"status\":\"ERROR\"," +
                "\"errors\":[{\"code\":\"GROUPED_WAREHOUSE\",\"message\":\"666 partner in warehouse group 1\"}]}"));
    }

    private void mockWarehouseGroup() {
        when(ff4ShopsOpenApiClient.getGroupByPartner(anyLong()))
                .thenReturn(CompletableFuture.supplyAsync(() -> new WrappedStocksWarehouseGroupDto().result(
                        new StocksWarehouseGroupDto()
                                .id(1L)
                                .mainWarehouseId(10L)
                                .warehouses(List.of(new WarehouseDto().warehouseId(10L).partnerId(774L),
                                        new WarehouseDto().warehouseId(11L).partnerId(666L))))
                ));
    }

    private void assertOffers(int index, long shopId, long warehouseId) {
        List<DataCampOffer.Offer> offers1 = changeOfferEventCaptor.getAllValues().get(index).getPayload()
                .stream()
                .map(DataCampEvent::convertToDataCampOffer)
                .collect(Collectors.toList());

        org.assertj.core.api.Assertions.assertThat(offers1)
                .hasSize(2)
                .allMatch(o -> o.getIdentifiers().getShopId() == shopId)
                .allMatch(o -> o.getIdentifiers().getWarehouseId() == warehouseId);
    }

    private URI stocksUri(long campaignId) {
        return new UriTemplate("{base}/campaigns/{campaignId}/offers/stocks")
                .expand(ImmutableMap.of("base", urlBasePrefix, "campaignId", campaignId));
    }

    private void setPushStocksEnabled() {
        environmentService.setValue(PAPI_PUSH_STOCKS_ENABLE, "true");
        when(offerStockUpdateEnabledChecker.isEnabledForPartner(anyLong())).thenReturn(true);
    }
}

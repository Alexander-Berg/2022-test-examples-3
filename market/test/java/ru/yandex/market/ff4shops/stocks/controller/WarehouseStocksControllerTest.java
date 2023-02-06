package ru.yandex.market.ff4shops.stocks.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.openapi.AbstractStocksOpenApiTest;
import ru.yandex.market.ff4shops.lgw.LogisticApiRequestsClientService;
import ru.yandex.market.ff4shops.model.entity.PartnerFulfillmentId;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;

import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.validatedWith;

/**
 * Тесты для {@link WarehouseStocksController}.
 */
@DbUnitDataSet(before = "WarehouseStocksControllerTest.before.csv")
public class WarehouseStocksControllerTest extends AbstractStocksOpenApiTest {
    @Autowired
    private LogisticApiRequestsClientService logisticApiRequestsClientService;

    /**
     * Проверяет обнуление стока
     */
    @Test
    void testResetStocks() {
        apiClient.warehouseStocks().resetStocks()
                .warehouseIdPath(10L)
                .execute(validatedWith(shouldBeCode(SC_OK)));

        ArgumentCaptor<List<ItemStocks>> captor = ArgumentCaptor.forClass(List.class);
        verify(logisticApiRequestsClientService).pushStocks(eq(new PartnerFulfillmentId(110, 10)), captor.capture());

        assertThat(captor.getValue())
                .hasSize(2)
                .allSatisfy(is -> {
                    assertThat(is).returns(new ResourceId(null, "10"), ItemStocks::getWarehouseId);
                    assertThat(is.getStocks())
                            .hasSize(1)
                            .allSatisfy(s -> assertThat(s)
                                    .returns(0, Stock::getCount)
                                    .returns(StockType.FIT, Stock::getType));
                })
                .extracting(ItemStocks::getUnitId)
                .containsOnly(new UnitId("0", 110L, "0"), new UnitId("1", 110L, "1"));
    }
}

package ru.yandex.market.mbi.bpmn.client;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.ff4shops.client.model.StocksWarehouseGroupRequest;

public class Ff4shopsClientTest extends FunctionalTest {
    @Autowired
    private Ff4shopsClient ff4shopsClient;

    @Test
    void testCreateStocksWarehouseGroup() {
        // todo: дописать тест
        ff4shopsClient.createStocksWarehouseGroup(1, new StocksWarehouseGroupRequest().warehouseIds(List.of(1L, 2L)));
    }
}

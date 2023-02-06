package ru.yandex.market.replenishment.autoorder.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.WarehouseLogisticPointLoader;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
@ActiveProfiles("unittest")
public class WarehouseLogisticPointLoaderTest extends FunctionalTest {

    @Autowired
    WarehouseLogisticPointLoader warehouseLogisticPointLoader;

    @Autowired
    LMSClient lmsClient;

    @Before
    public void mockLMSClient() {
        when(lmsClient.getLogisticsPoints(notNull()))
                .thenReturn(List.of(
                        LogisticsPointResponse.newBuilder()
                                .active(true)
                                .id(1L)
                                .partnerId(145L)
                                .build(),
                        LogisticsPointResponse.newBuilder()
                                .active(true)
                                .id(2L)
                                .partnerId(147L)
                                .build()));
    }

    @Test
    @DbUnitDataSet(after = "WarehouseLogisticPointLoaderTest.after.csv")
    public void testLMSClient_updateWarehouseLogisticPoint() {
        warehouseLogisticPointLoader.load();
    }

}

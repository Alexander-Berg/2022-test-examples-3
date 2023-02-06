package ru.yandex.market.billing.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.ShipmentDateCalculationRuleUpdateExecutor;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class ShipmentDateCalculationRuleUpdateExecutorTest extends FunctionalTest {

    @Autowired
    private CheckouterAPI checkouterClient;

    private final CheckouterShopApi checkouterShopApi = Mockito.mock(CheckouterShopApi.class);

    @Autowired
    private ShipmentDateCalculationRuleUpdateExecutor shipmentDateCalculationRuleUpdateExecutor;

    @BeforeEach
    public void setUp() {
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        when(checkouterShopApi.getShopData(anyLong())).thenReturn(ShopMetaData.DEFAULT);
    }

    @DisplayName("Проверяем, что правила отсылаются только для партнеров через ПИ")
    @Test
    @DbUnitDataSet(before = "ShipmentDateCalculationRuleUpdateExecutorTest.before.csv")
    void testExecutor() {
        shipmentDateCalculationRuleUpdateExecutor.doJob(null);
        Mockito.verify(checkouterShopApi, times(1))
                .saveShipmentDateCalculationRules(eq(1L), any());
        Mockito.verify(checkouterShopApi, times(1))
                .saveShipmentDateCalculationRules(eq(2L), any());
    }
}

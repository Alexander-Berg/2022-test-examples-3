package ru.yandex.market.deliveryintegrationtests.delivery.tests.ondemand.lavka;

import dto.requests.checkouter.Address;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;

import java.util.Collections;
import java.util.List;

@DisplayName("Blue Lavka vs PVZ Order Test")
@Epic("Blue Lavka")
@Slf4j
public class LavkaPvzTest extends AbstractLavkaTest {

    @Test
    @TmsLink("logistic-16")
    @DisplayName("Синий заказ в Лавке: успешная доставка")
    public void deliveryLavkaOrderTest() {
        order = TRISTERO_ORDER_STEPS.createLavkaOrder(Collections.singletonList(OfferItems.FF_172_UNFAIR_STOCK.getItem()), Address.LAVKA_WITH_PVZ);
        ORDER_STEPS.verifySDTracksCreated(order);
        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        List<WaybillSegmentDto> waybillSegments = LOM_ORDER_STEPS.getWaybillSegments(lomOrderId);
        Assertions.assertEquals(3, waybillSegments.size(), "Заказ попал не в лавку " + order.getId());
    }
}

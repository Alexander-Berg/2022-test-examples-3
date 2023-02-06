package ru.yandex.market.pvz.internal.controller.pi.order.mapper;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.core.domain.order.model.OrderLabel;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.client.model.order.DeliveryServiceType.DBS;
import static ru.yandex.market.pvz.client.model.order.DeliveryServiceType.MARKET_COURIER;
import static ru.yandex.market.pvz.client.model.order.DeliveryServiceType.YANDEX_DELIVERY;

class OrderLabelsGetterTest {

    @Test
    void containPassportRequiredLabel() {
        var labelsGetter = new OrderLabelsGetter(true, MARKET_COURIER, false, false);
        verifyContains(OrderLabel.PASSPORT_REQUIRED, labelsGetter);
    }

    @Test
    void containDBSLabel() {
        var labelsGetter = new OrderLabelsGetter(false, DBS, false, false);
        verifyContains(OrderLabel.DBS, labelsGetter);
    }

    @Test
    void containYandexDeliveryLabel() {
        var labelsGetter = new OrderLabelsGetter(false, YANDEX_DELIVERY, false, false);
        verifyContains(OrderLabel.YANDEX_DELIVERY, labelsGetter);
    }

    @Test
    void containFittingAvailableLabel() {
        var labelsGetter = new OrderLabelsGetter(false, MARKET_COURIER, true, false);
        verifyContains(OrderLabel.FITTING_AVAILABLE, labelsGetter);
    }

    @Test
    void containC2cLabel() {
        var labelsGetter = new OrderLabelsGetter(false, MARKET_COURIER, false, true);
        verifyContains(OrderLabel.C2C, labelsGetter);
    }

    @Test
    void notContainAnyLabel() {
        var labelsGetter = new OrderLabelsGetter(false, MARKET_COURIER, false, false);
        verifyDoesNotContains(OrderLabel.PASSPORT_REQUIRED, labelsGetter);
        verifyDoesNotContains(OrderLabel.YANDEX_DELIVERY, labelsGetter);
        verifyDoesNotContains(OrderLabel.FITTING_AVAILABLE, labelsGetter);
        verifyDoesNotContains(OrderLabel.DBS, labelsGetter);
        verifyDoesNotContains(OrderLabel.C2C, labelsGetter);
    }

    private void verifyContains(OrderLabel label, OrderLabelsGetter labelsGetter) {
        assertThat(labelsGetter.getLabels()).contains(label);
    }

    private void verifyDoesNotContains(OrderLabel label, OrderLabelsGetter labelsGetter) {
        assertThat(labelsGetter.getLabels()).doesNotContain(label);
    }
}

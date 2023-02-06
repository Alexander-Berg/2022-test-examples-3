package ru.yandex.market.sc.core;

import org.springframework.context.event.EventListener;

import ru.yandex.market.sc.core.domain.scan_log.event.OrderScanLogEvent;
import ru.yandex.market.sc.core.domain.scan_log.model.OrderScanLogRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderScanLogSomeIdConstraintCheckListener {

    private static int hitCounter;

    public static int getHitCounter() {
        return hitCounter;
    }

    public static void resetHitCounter() {
        hitCounter = 0;
    }

    @EventListener
    public void onOrderScanLogEvent(OrderScanLogEvent event) {
        checkRequest(event.getRequest());
        OrderScanLogSomeIdConstraintCheckListener.hitCounter++;
    }

    private void checkRequest(OrderScanLogRequest request) {
        var someIdIsSet = request.getExternalOrderId() != null ||
                request.getLotId() != null ||
                request.getExternalInboundId() != null ||
                request.getSortableBarcode() != null ||
                request.getSortableId() != null;
        assertThat(someIdIsSet).isTrue();
    }

}

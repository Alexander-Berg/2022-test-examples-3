package ru.yandex.market.tpl.core.test;

import java.util.concurrent.atomic.AtomicLong;

import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.external.delivery.sc.dto.ScRoutingResult;

@UtilityClass
public class TplMonitoringTestFactory {

    private static final AtomicLong idGenerator = new AtomicLong();

    public OrderRepository.OrderUser createTplOrderUserItem(String externalOrderId, String courierId) {
        long id = idGenerator.incrementAndGet();
        return new OrderRepository.OrderUser() {
            @Override
            public Long getOrderId() {
                return id;
            }

            @Override
            public String getExternalOrderId() {
                return externalOrderId;
            }

            @Override
            public Long getUserId() {
                return null;
            }

            @Override
            public String getUserUid() {
                return courierId;
            }
        };
    }

    public ScRoutingResult.OrderCourier createScOrderCourierItem(String externalOrderId, String courierId) {
        return new ScRoutingResult.OrderCourier(externalOrderId, courierId);
    }
}

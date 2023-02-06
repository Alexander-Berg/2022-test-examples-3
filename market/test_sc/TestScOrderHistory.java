package ru.yandex.market.tpl.core.domain.test_sc;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.common.db.jpa.BaseJpaEntity;

/**
 * @author kukabara
 */
@Entity

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestScOrderHistory extends BaseJpaEntity.LongGenAud {

    protected String externalOrderId;
    private String yandexId;
    private String orderPartnerId;
    private long partnerId;

    @Enumerated(EnumType.STRING)
    private OrderStatusType status;

    private Instant deliveryDate;

    private String courier;

    public TestScOrderHistory(TestScOrder testScOrder) {
        this.partnerId = testScOrder.getPartnerId();
        this.orderPartnerId = testScOrder.getOrderPartnerId();
        this.externalOrderId = testScOrder.getExternalOrderId();
        this.yandexId = testScOrder.getYandexId();
        this.status = testScOrder.getStatus();
        this.deliveryDate = testScOrder.getDeliveryDate();
        this.courier = testScOrder.getCourier();
    }

    @JsonProperty("statusName")
    public String getStatusName() {
        return status.name();
    }

}

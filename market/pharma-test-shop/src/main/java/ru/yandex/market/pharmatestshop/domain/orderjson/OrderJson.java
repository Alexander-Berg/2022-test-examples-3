package ru.yandex.market.pharmatestshop.domain.orderjson;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus;
import ru.yandex.market.pharmatestshop.domain.order.status.OrderSubstatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_test_shop_order")
public class OrderJson {
    @Id
    private long id;
    private OrderStatus status;
    private OrderSubstatus substatus;
    private String deliveryType;
    private String jsonData;
    private long shopId;

    private String campaignId;

}

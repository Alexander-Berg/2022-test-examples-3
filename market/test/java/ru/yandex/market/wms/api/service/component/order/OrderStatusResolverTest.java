package ru.yandex.market.wms.api.service.component.order;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDetailDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderStatusResolverTest extends IntegrationTest {

    @Autowired
    private OrderStatusResolver resolver;

    @Test
    public void interStoreOrderWithEmptyDetails() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderdetails(new ArrayList<>());
        orderDTO.setType(OrderType.OUTBOUND_WH_2_WH.getCode());

        resolver.resolve(orderDTO);

        assertEquals(OrderStatus.WAITING_FOR_DETAILS.getValue(), orderDTO.getStatus(),
                "Order with empty details should get WAITING_FOR_DETAILS status");
    }

    @Test
    public void interStoreOrderDamageWithEmptyDetails() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderdetails(new ArrayList<>());
        orderDTO.setType(OrderType.OUTBOUND_WH_2_WH_DMG.getCode());

        resolver.resolve(orderDTO);

        assertEquals(OrderStatus.WAITING_FOR_DETAILS.getValue(), orderDTO.getStatus(),
            "Order with empty details should get WAITING_FOR_DETAILS status");
    }

    @Test
    public void interStoreOrderWithFilledDetails() {
        OrderDTO orderDTO = new OrderDTO();
        List<OrderDetailDTO> details = new ArrayList<>();
        details.add(new OrderDetailDTO());
        orderDTO.setOrderdetails(details);
        orderDTO.setType(OrderType.OUTBOUND_WH_2_WH.getCode());

        resolver.resolve(orderDTO);

        assertEquals(OrderStatus.CREATED_EXTERNALLY.getValue(), orderDTO.getStatus(),
                "Order with filled details should get CREATED_EXTERNALLY status");
    }

    @Test
    public void interStoreOrderDamageWithFilledDetails() {
        OrderDTO orderDTO = new OrderDTO();
        List<OrderDetailDTO> details = new ArrayList<>();
        details.add(new OrderDetailDTO());
        orderDTO.setOrderdetails(details);
        orderDTO.setType(OrderType.OUTBOUND_WH_2_WH_DMG.getCode());

        resolver.resolve(orderDTO);

        assertEquals(OrderStatus.CREATED_EXTERNALLY.getValue(), orderDTO.getStatus(),
            "Order with filled details should get CREATED_EXTERNALLY status");
    }

    @Test
    public void orderWithFilledDetails() {
        OrderDTO orderDTO = new OrderDTO();
        List<OrderDetailDTO> details = new ArrayList<>();
        orderDTO.setOrderdetails(details);
        orderDTO.setType(OrderType.STANDARD.getCode());
        orderDTO.setStatus(OrderStatus.SORTING_COMPLETE.getValue());

        resolver.resolve(orderDTO);

        assertEquals(OrderStatus.SORTING_COMPLETE.getValue(), orderDTO.getStatus(),
                "Order status not with type OUTBOUND_WH_2_WH and OUTBOUND_WH_2_WH_DMG should not change");
    }

    @Test
    public void orderStatusNullShouldBeSet() {
        OrderDTO orderDTO = new OrderDTO();
        List<OrderDetailDTO> details = new ArrayList<>();
        orderDTO.setOrderdetails(details);
        orderDTO.setType(OrderType.STANDARD.getCode());
        orderDTO.setStatus(null);

        resolver.resolve(orderDTO);

        assertEquals(OrderStatus.CREATED_EXTERNALLY.getValue(), orderDTO.getStatus(),
                "Order status NULL should be replaced with default value");
    }



}

package ru.yandex.market.deepdive.domain.orders;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.yandex.market.deepdive.domain.controller.dto.OrdersDto;

public class OrdersServiceTest extends TestCase {

    OrdersMapper mapper = new OrdersMapper();

    @Test
    public void testGetOrders_2Orders_2OrdersDto() {
        OrdersRepository repository = Mockito.mock(OrdersRepository.class);

        List<Orders> ordersList = create2OrdersList();
        List<OrdersDto> ordersDtoList = ordersList.stream()
                .map(mapper::map)
                .collect(Collectors.toList());
        Page<Orders> ordersPage = new PageImpl<>(ordersList);
        Page<OrdersDto> ordersDtoPage = new PageImpl<>(ordersDtoList);

        Mockito
                .when(
                        repository.findAll(
                                Mockito.any(),
                                Mockito.any()
                        )

                ).thenReturn(ordersPage);

        OrdersService service = new OrdersService(repository, mapper);

        Assert.assertEquals(ordersDtoPage, service.getOrders(1, Optional.empty(), Optional.empty(), null));
    }

    private List<Orders> create2OrdersList() {
        List<Orders> ordersList = new ArrayList<>();
        Orders orders;

        orders = new Orders();
        orders.setId(1L);
        orders.setPickupPointId(101L);
        orders.setDeliveryDate(new Date());
        orders.setPaymentType("PREPAID");
        orders.setStatus("CREATED");
        orders.setTotalPrice(228.0);
        ordersList.add(orders);

        orders = new Orders();
        orders.setId(2L);
        orders.setPickupPointId(101L);
        orders.setDeliveryDate(new Date());
        orders.setPaymentType("PREPAID");
        orders.setStatus("CREATED");
        orders.setTotalPrice(5151.0);
        ordersList.add(orders);

        return ordersList;
    }
}

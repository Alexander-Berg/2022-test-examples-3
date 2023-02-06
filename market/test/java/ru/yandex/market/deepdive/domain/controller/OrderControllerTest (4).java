package ru.yandex.market.deepdive.domain.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.AbstractTest;
import ru.yandex.market.deepdive.domain.order.Order;
import ru.yandex.market.deepdive.domain.order.OrderFilter;
import ru.yandex.market.deepdive.domain.order.OrderRepository;
import ru.yandex.market.deepdive.domain.order.OrderSpecificationFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.deepdive.utils.IntegrationTestUtils.extractFileContent;

public class OrderControllerTest extends AbstractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSpecificationFactory specificationFactory;

    @Test
    public void emptyOrderList() throws Exception {
        Mockito.when(orderRepository.findAll(specificationFactory
                .fromFilter(new OrderFilter())))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/pickup-points/0/orders")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(extractFileContent("domain/controller/order/empty_list.json")));
    }

    @Test
    public void nonEmptyOrderList() throws Exception {
        Order order1 = new Order()
                .setId(1L)
                .setPickupPointId(1L)
                .setPvzMarketId(123456L)
                .setDeliveryDate(LocalDate.of(2021, 7, 8))
                .setTotalPrice(100)
                .setPaymentType("PREPAID")
                .setStatus("CREATED");

        Mockito.when(orderRepository.findAll(Mockito.any(Specification.class)))
                .thenReturn(List.of(order1));

        mockMvc.perform(get("/api/pickup-points/123456/orders?status=CREATED&paymentType=PREPAID")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(extractFileContent("domain/controller/order/non_empty_list.json")));
    }
}

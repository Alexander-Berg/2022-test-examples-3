package ru.yandex.market.deepdive.domain;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.deepdive.AbstractTest;
import ru.yandex.market.deepdive.domain.order.Order;
import ru.yandex.market.deepdive.domain.order.OrderRepository;
import ru.yandex.market.deepdive.domain.order.OrderStatus;
import ru.yandex.market.deepdive.domain.order.PaymentType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerTest extends AbstractTest {
    @Autowired
    private OrderRepository repository;

    @Test
    @DisplayName("NoOrdersTest")
    public void testEmpty() throws Exception {
        findById(Collections.emptyList(), 0L)
                .andExpect(content().json(readFromFile("tests/order0.json")));
    }

    @Test
    @DisplayName("OneOrderTest")
    public void testOne() throws Exception {
        Order order1 = new Order(
                1L,
                1L,
                OrderStatus.CREATED,
                LocalDate.of(2021, 1, 1),
                PaymentType.CARD,
                1L
        );
        findById(List.of(order1), 1L)
                .andExpect(content().json(readFromFile("tests/order1.json")));
    }

    @Test
    @DisplayName("SeveralOrderTest")
    public void testSeveral() throws Exception {
        Order order1 = new Order(
                1L,
                1L,
                OrderStatus.CREATED,
                LocalDate.of(2021, 7, 8),
                PaymentType.CARD,
                100L
        );
        Order order2 = new Order(
                2L,
                1L,
                OrderStatus.READY_FOR_RETURN,
                LocalDate.of(2021, 2, 5),
                PaymentType.CASH,
                150L
        );
        findById(List.of(order1, order2), 2L)
                .andExpect(content().json(readFromFile("tests/order2.json")));
    }

    private ResultActions findById(
            final List<Order> toSubstitute,
            final Long id
    ) throws Exception {
        Mockito.when(repository.findAll(Mockito.any(Specification.class))).thenReturn(toSubstitute);
        return mockMvc.perform(get(String.format("/api/pickup-points/%d/orders", id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());
    }


}

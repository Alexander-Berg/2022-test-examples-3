package ru.yandex.market.deepdive.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.deepdive.AbstractIntegrationTest;
import ru.yandex.market.deepdive.entities.Order;
import ru.yandex.market.deepdive.repositories.OrderRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerTest extends AbstractIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Test of non-containing orders point")
    public void testOfOrderEmptyPoint() throws Exception {
        Mockito.when(orderRepository.findAllByPickupPointId(1L)).thenReturn(new LinkedList<>());

        mockMvc.perform(get("/api/orders/1")
               .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(status().is2xxSuccessful())
               .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Test of containing orders point")
    public void testOfOrderContainingPoint() throws Exception {
        Order firstOrderFromList = Order.builder()
                .id(322L)
                .pickupPointId(1L)
                .deliveryDate(LocalDate.now())
                .status("DELIVERED")
                .paymentType("CARD")
                .totalPrice(1488L)
                .build();


        Order secondOrderFromList = Order.builder()
                .id(228L)
                .pickupPointId(1L)
                .deliveryDate(LocalDate.now())
                .status("DELIVERED")
                .paymentType("CARD")
                .totalPrice(3228L)
                .build();

        List<Order> implOfExpectedValue = Arrays.asList(firstOrderFromList, secondOrderFromList);

        Mockito.when(orderRepository.findAllByPickupPointId(1L)).thenReturn(implOfExpectedValue);

        mockMvc.perform(get("/api/orders/1")
               .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(status().is2xxSuccessful())
               .andDo(print())
               .andExpect(content().json(
                 "[{\"id\":322,\"pickupPointId\":1,\"status\":\"DELIVERED\",\"deliveryDate\":[2021,7,13]," +
                         "\"paymentType\":\"CARD\",\"totalPrice\":1488},{\"id\":228,\"pickupPointId\":1," +
                         "\"status\":\"DELIVERED\",\"deliveryDate\":[2021,7,13],\"paymentType\":\"CARD\"," +
                         "\"totalPrice\":3228}]"));
    }

    @Test
    @DisplayName("Test of non-existing orders point")
    public void testOfNonExistingPoint() throws Exception {
        Mockito.when(orderRepository.findAllByPickupPointId(1L)).thenReturn(null);

        mockMvc.perform(get("/api/orders/1")
               .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(status().is4xxClientError());
    }

}

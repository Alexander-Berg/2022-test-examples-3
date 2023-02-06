package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.configuration.TestConfig;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point_order.PickupPointOrderRepository;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class PickupPointControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PickupPointOrderRepository orderRepository;

    @MockBean
    private PickupPointRepository repository;

    @Test
    void getPickupPoints() throws Exception {
        List<PickupPoint> pickupPoints = new ArrayList<>();
        pickupPoints.add(new PickupPoint(0L, "PVZ#1", true, 0L));
        pickupPoints.add(new PickupPoint(1L, "PVZ#2", false, 1L));
        when(repository.findAll()).thenReturn(pickupPoints);
        mockMvc.perform(get("/api/pickup-points")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[ { \"id\" : 0, \"name\": \"PVZ#1\"}," +
                                " { \"id\" : 1, \"name\": \"PVZ#2\" } ]"));
    }
}

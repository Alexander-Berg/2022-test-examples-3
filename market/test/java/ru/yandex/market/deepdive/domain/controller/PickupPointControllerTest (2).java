package ru.yandex.market.deepdive.domain.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.configuration.TestConfiguration;
import ru.yandex.market.deepdive.domain.orders.OrdersRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = TestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
public class PickupPointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PickupPointRepository repo;

    @MockBean
    private OrdersRepository ordersRepository; // Без этого spring не смог добавить
    // OrdersRepository в Application Context.

    @Test
    @DisplayName("Check")
    public void getPickupPoints() throws Exception {
        Assertions.assertNotNull(mockMvc);
        Assertions.assertNotNull(repo);
        mockMvc.perform(get("/api/pickup-points")).andExpect(status().isOk());

    }
}

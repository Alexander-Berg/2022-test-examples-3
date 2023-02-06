package ru.yandex.market.deepdive.domain;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.configuration.IntegrationTestConfiguration;
import ru.yandex.market.deepdive.domain.order.OrderRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.deepdive.utils.Utils.extractFileContent;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = IntegrationTestConfiguration.class)
@AutoConfigureMockMvc(secure = false)
@DisplayName("Тесты для ручек pickupPointOrdersController")
public class PickupPointControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PickupPointRepository pickupPointRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Пустой список ПВЗ")
    public void emptyListTest() throws Exception {
        Mockito.when(pickupPointRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/pickup-points")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Не пустой список ПВЗ")
    public void nonEmptyTest() throws Exception {
        Mockito.when(pickupPointRepository.findAll()).thenReturn(List.of(
                new PickupPoint(1L, "inch", true, 1L),
                new PickupPoint(21L, "two", true, 2L)
        ));

        mockMvc.perform(get("/api/pickup-points")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        extractFileContent("json/PickupPointControllerTest/nonEmptyList.json")
                ));
    }
}

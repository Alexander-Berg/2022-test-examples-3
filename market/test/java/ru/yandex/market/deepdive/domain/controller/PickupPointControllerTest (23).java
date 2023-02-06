package ru.yandex.market.deepdive.domain.controller;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.AbstractTest;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.deepdive.utils.IntegrationTestUtils.extractFileContent;

public class PickupPointControllerTest extends AbstractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PickupPointRepository pickupPointRepository;

    @Test
    public void nonEmptyPointListTest() throws Exception {
        PickupPoint point1 = new PickupPoint()
                .setId(1L)
                .setName("point1");
        PickupPoint point2 = new PickupPoint()
                .setId(2L)
                .setName("point2");
        Mockito.when(pickupPointRepository.findAll()).thenReturn(Arrays.asList(point1, point2));

        mockMvc.perform(get("/api/pickup-points")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(extractFileContent("domain/controller/pickup_point/non_empty_list.json")));
    }

    @Test
    public void emptyPointListTest() throws Exception {
        Mockito.when(pickupPointRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/pickup-points")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(extractFileContent("domain/controller/pickup_point/empty_list.json")));
    }
}

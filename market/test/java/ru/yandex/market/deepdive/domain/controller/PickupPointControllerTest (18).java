package ru.yandex.market.deepdive.domain.controller;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.TestConfiguration;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ContextConfiguration(classes = TestConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
public class PickupPointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PickupPointService pickupPointService;

    @Autowired
    private PickupPointController pickupPointController;

    @Test
    public void pickupPointControllerShouldLoadTest() {
        assertThat(pickupPointController, notNullValue());
    }

    @Test
    public void getPickupPointsShouldReturnEmptyListTest() throws Exception {
        when(pickupPointService.getPickupPoints()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/pickup-points").contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json("[]"));
    }

    @Test
    public void getPickupPointsShouldReturnNonEmptyListTest() throws Exception {
        when(pickupPointService.getPickupPoints()).thenReturn(List.of(
                PickupPointDto.builder().id(1).name("name1").build(),
                PickupPointDto.builder().id(2).name("name2").build()
        ));
        mockMvc.perform(get("/api/pickup-points").contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json("[{ \"id\": 1, \"name\": \"name1\" }, "
                        + "{ \"id\": 2, \"name\": \"name2\" }]"));
    }
}

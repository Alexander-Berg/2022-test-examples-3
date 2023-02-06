package ru.yandex.market.deepdive.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.deepdive.AbstractIntegrationTest;
import ru.yandex.market.deepdive.entities.PickupPoint;
import ru.yandex.market.deepdive.repositories.PickupPointRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PickupPointControllerTest extends AbstractIntegrationTest {
    @Autowired
    private PickupPointRepository repository;

    @Test
    @DisplayName("Test of empty collection")
    public void listOfPointsIsEmpty() throws Exception {
        Mockito.when(repository.findAll()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/pickup-points")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Test of non-empty collection")
    public void listOfPointsIsNotEmpty() throws Exception {
        Mockito.when(repository.findAll()).thenReturn(
                List.of(new PickupPoint(322L, "first", false),
                        new PickupPoint(228L, "second", true)));

        mockMvc.perform(get("/api/pickup-points")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "[{\"id\":322, \"name\":\"first\"}, {\"id\":228, \"name\":\"second\"}]"));
    }
}

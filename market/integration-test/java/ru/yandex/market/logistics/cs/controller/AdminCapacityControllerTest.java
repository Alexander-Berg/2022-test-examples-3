package ru.yandex.market.logistics.cs.controller;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.domain.dto.CapacityValueCounterDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DatabaseSetup("/service/external/before/before.xml")
@DisplayName("Админский контроллер капасити")
public class AdminCapacityControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    @SneakyThrows
    @DisplayName("Получение счетчиков по временному отрезку и capacityValueId")
    void getCapacityCountersByValueIdAndDaysInterval() {
        MockHttpServletRequestBuilder operation =
            MockMvcRequestBuilders.get("/admin/capacity/counters")
                .accept(MediaType.APPLICATION_JSON)
                .param("capacityValueId", "10")
                .param("from", LocalDate.of(2021, 5, 22).toString())
                .param("to", LocalDate.of(2021, 5, 23).toString());

        ResultActions resultActions = mockMvc.perform(operation);
        assertEquals(HttpStatus.OK.value(), resultActions.andReturn().getResponse().getStatus());

        List<CapacityValueCounterDto> counters = mapper.readValue(
            resultActions.andReturn().getResponse().getContentAsString(),
            new TypeReference<List<CapacityValueCounterDto>>() {
            }
        );
        assertEquals(2, counters.size());
        assertTrue(counters.stream().anyMatch(counter -> counter.getId().equals(11L)));
        assertTrue(counters.stream().anyMatch(counter -> counter.getId().equals(12L)));
    }
}

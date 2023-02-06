package ru.yandex.market.deepdive.domain;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.deepdive.AbstractTest;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class PickupPointControllerTest extends AbstractTest {

    @Autowired
    private PickupPointRepository repository;

    @Test
    @DisplayName("NoPointsTest")
    public void testEmpty() throws Exception {
        findAll(Collections.emptyList()).andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("OnePointTest")
    public void testOne() throws Exception {
        PickupPoint point = new PickupPoint(
                0L,
                "first",
                true,
                0L
        );
        findAll(List.of(point)).andExpect(content().json(readFromFile("tests/pickup-point1.json")));
    }

    @Test
    @DisplayName("SeveralPointsTest")
    public void testSeveral() throws Exception {
        PickupPoint point1 = new PickupPoint(
                0L,
                "first",
                true,
                0L
        );
        PickupPoint point2 = new PickupPoint(
                1L,
                "second",
                false,
                1L
        );
        findAll(List.of(point1, point2)).andExpect(content().json(readFromFile("tests/pickup-point2.json")));
    }

    private ResultActions findAll(final List<PickupPoint> toSubstitute) throws Exception {
        when(repository.findAll()).thenReturn(toSubstitute);
        return mockMvc.perform(get("/api/pickup-points")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}

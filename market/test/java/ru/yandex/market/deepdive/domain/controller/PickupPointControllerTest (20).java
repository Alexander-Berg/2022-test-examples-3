package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.DeepDive;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)

@WebMvcTest
public class PickupPointControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    PickupPointController pickupPointController;
    @Autowired
    PickupPointService pickupPointService;

    @Autowired
    private ObjectMapper objectMapper;

    private static Stream<Arguments> getPickupPoints() {
        return Stream.of(
                Arguments.of(emptyList()),
                Arguments.of(Collections.singletonList(new PickupPoint())),
                Arguments.of(Arrays.asList(new PickupPoint(), new PickupPoint()))
        );
    }


    //Пока проверяет только что лист не пустой
    @ParameterizedTest
    @MethodSource("getPickupPoints")
    public void testGetPickupPoints(List<PickupPointDto> expectedResult) throws Exception {
        String json =
                mockMvc.perform(get("/api/pickup-points")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<PickupPointDto> actualResult = objectMapper.readValue(json, new TypeReference<List<PickupPointDto>>(){});
        assertTrue(actualResult.size() != 0);
    }
}

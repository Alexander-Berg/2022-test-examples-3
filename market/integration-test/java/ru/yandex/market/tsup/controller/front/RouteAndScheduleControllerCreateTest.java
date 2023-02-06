package ru.yandex.market.tsup.controller.front;

import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteDto;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tsup.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public class RouteAndScheduleControllerCreateTest extends AbstractContextualTest {

    @Autowired
    private TransportManagerClient transportManagerClient;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any()))
                .thenReturn(List.of(
                        LogisticsPointResponse.newBuilder()
                                .id(1L)
                                .partnerId(10003395090L)
                                .build(),
                        LogisticsPointResponse.newBuilder()
                                .id(2L)
                                .partnerId(10001677852L)
                                .build()
                ));

        Mockito.when(transportManagerClient.createOrGetRoute(Mockito.any()))
                .thenReturn(RouteDto.builder().id(1L).build());
    }

    @SneakyThrows
    @Test
    void shouldCreateRoute() {
        mockMvc.perform(post("/routes")
                .content(IntegrationTestUtils.extractFileContent("fixture/route/route_create.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/route/route_id.json"));
    }
}

package ru.yandex.market.tsup.controller.front;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.model.EntityType;
import ru.yandex.mj.generated.client.carrier.api.RoutingTransportTypeApiClient;
import ru.yandex.mj.generated.client.carrier.model.RoutingTransportTypeDto;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoutingTransportTypeSuggestTest extends AbstractContextualTest {

    @Autowired
    private RoutingTransportTypeApiClient routingTransportTypeApiClient;

    @BeforeEach
    public void setUp() {
        ExecuteCall<List<RoutingTransportTypeDto>, RetryStrategy> executeCall = Mockito.mock(ExecuteCall.class);
        Mockito.when(executeCall.schedule()).thenReturn(
                CompletableFuture.completedFuture(
                        makeRoutingTransportTypeResult()
                )
        );

        Mockito.when(routingTransportTypeApiClient.getRoutingTransportTypes())
                .thenReturn(executeCall);
    }

    @NotNull
    private List<RoutingTransportTypeDto> makeRoutingTransportTypeResult() {
        RoutingTransportTypeDto firstType = new RoutingTransportTypeDto();
        firstType.setId(1L);
        firstType.setName("Name");
        firstType.setRoutingPriority(10);
        firstType.setCapacity(BigDecimal.ONE);

        return List.of(firstType);
    }

    @SneakyThrows
    @Test
    void routingTransportTypeGet() {
        mockMvc.perform(get("/filter/suggest")
                        .param("entityType", EntityType.ROUTING_TRANSPORT_TYPE.name())
                        .header("action", FrontAction.GET.getAction()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggest").isArray())
                .andExpect(jsonPath("$.suggest", hasSize(1)))
                .andExpect(jsonPath("$.suggest[0].externalId").value("1"))
                .andExpect(jsonPath("$.suggest[0].name").value("Name"));
    }
}

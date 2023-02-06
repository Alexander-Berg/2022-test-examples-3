package ru.yandex.market.tsup.controller.front;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.routing.api.RoutingApiClient;
import ru.yandex.mj.generated.client.routing.model.PageOfRoutingDto;
import ru.yandex.mj.generated.client.routing.model.RoutingInfoDto;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class RoutingControllerTest extends AbstractContextualTest {

    @Autowired
    private RoutingApiClient routingApiClient;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        ExecuteCall<PageOfRoutingDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule())
                .thenReturn(CompletableFuture.completedFuture(
                        new PageOfRoutingDto()
                                .size(10)
                                .number(0)
                                .totalElements(1L)
                                .totalPages(1)
                                .content(List.of(
                                        new RoutingInfoDto()
                                                .depotId(123L)
                                ))
                ));
        Mockito.when(routingApiClient.routingGet(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(call);

        Mockito.when(lmsClient.searchPartners(Mockito.any()))
                .thenReturn(List.of(PartnerResponse.newBuilder().build()));
    }

    @SneakyThrows
    @Test
    void shouldGetRouting() {
        mockMvc.perform(MockMvcRequestBuilders.get("/routing"))
                .andExpect(status().isOk());
    }
}

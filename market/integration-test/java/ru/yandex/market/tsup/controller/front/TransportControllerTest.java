package ru.yandex.market.tsup.controller.front;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.TransportApiClient;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfTransportDto;
import ru.yandex.mj.generated.client.carrier.model.TransportDto;
import ru.yandex.mj.generated.client.carrier.model.TransportSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TransportControllerTest extends AbstractContextualTest {
    @Autowired
    private TransportApiClient transportApiClient;

    @BeforeEach
    void beforeEach() {
        ExecuteCall<PageOfTransportDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(transport()));

        Mockito.when(
            transportApiClient.internalTransportGet(
                null,
                null,
                null,
                null,
                null,
                10,
                "id,DESC"
            )
        ).thenReturn(call);
    }

    @SneakyThrows
    @Test
    void getTransport() {
        mockMvc.perform(get("/transport")
                .param("pageSize", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").value(Matchers.hasSize(2)))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.data[0].id").value(1L))
            .andExpect(jsonPath("$.data[0].name").value("Машинка раз"))
            .andExpect(jsonPath("$.data[0].brand").value("КАМАЗ"))
            .andExpect(jsonPath("$.data[0].model").value("5490-S5"))
            .andExpect(jsonPath("$.data[0].number").value("BT320X"))
            .andExpect(jsonPath("$.data[0].trailerNumber").value("123"))
            .andExpect(jsonPath("$.data[0].source").value("CARRIER"))
            .andExpect(jsonPath("$.data[0].company.id").value(1))
            .andExpect(jsonPath("$.data[0].company.name").value("company"))
            .andExpect(jsonPath("$.data[1].id").value(2L))
            .andExpect(jsonPath("$.data[1].name").value("Машинка два"))
            .andExpect(jsonPath("$.data[1].number").value("BT321X"))
            .andExpect(jsonPath("$.data[1].source").value("LOGISTICS_COORDINATOR"));
    }

    @NotNull
    private PageOfTransportDto transport() {
        return new PageOfTransportDto()
            .totalPages(1)
            .totalElements(2L)
            .size(10)
            .number(0)
            .content(List.of(
                new TransportDto()
                    .id(1L)
                    .name("Машинка раз")
                    .brand("КАМАЗ")
                    .model("5490-S5")
                    .company(new CompanyDto().id(1L).name("company"))
                    .number("BT320X")
                    .trailerNumber("123")
                    .source(TransportSource.CARRIER),
                new TransportDto()
                    .id(2L)
                    .name("Машинка два")
                    .number("BT321X")
                    .source(TransportSource.LOGISTICS_COORDINATOR)
            ));
    }
}

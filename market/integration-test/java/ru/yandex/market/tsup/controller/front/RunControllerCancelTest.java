package ru.yandex.market.tsup.controller.front;

import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.service.data_provider.entity.run.dto.CancelRunDto;
import ru.yandex.market.tsup.service.data_provider.entity.run.dto.RunCancelType;
import ru.yandex.mj.generated.client.carrier.api.RunApiClient;
import ru.yandex.mj.generated.client.carrier.model.RunDetailDto;
import ru.yandex.mj.generated.client.carrier.model.RunStatusDto;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RunControllerCancelTest extends AbstractContextualTest {

    @Autowired
    private RunApiClient runApiClient;

    @SneakyThrows
    @Test
    void shouldGetCancelTypes() {
        mockMvc.perform(MockMvcRequestBuilders.get("/runs/cancel-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(RunCancelType.values().length)));
    }

    @SneakyThrows
    @Test
    void shouldCancelRun() {
        ExecuteCall<RunDetailDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(runApiClient.internalRunsIdCancelPost(Mockito.anyLong(), Mockito.any()))
                .thenReturn(call);
        Mockito.when(call.schedule())
                .thenReturn(CompletableFuture.completedFuture(new RunDetailDto().runId(123L)
                        .status(RunStatusDto.CANCELLED_INCORRECT)));

        CancelRunDto value = new CancelRunDto();
        value.setType(RunCancelType.CREATED_BY_MISTAKE);

        mockMvc.perform(MockMvcRequestBuilders.post("/runs/{id}/cancel", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(value))
                )
                .andExpect(status().isOk());
    }
}

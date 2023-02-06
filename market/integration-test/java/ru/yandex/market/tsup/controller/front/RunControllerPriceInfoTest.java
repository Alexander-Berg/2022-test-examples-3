package ru.yandex.market.tsup.controller.front;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tpl.common.data_provider.meta.FrontHttpRequestMeta;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.RunApiClient;
import ru.yandex.mj.generated.client.carrier.model.PriceControlDto;
import ru.yandex.mj.generated.client.carrier.model.PriceControlPenaltyStatusDto;
import ru.yandex.mj.generated.client.carrier.model.PriceControlPenaltyTypeDto;
import ru.yandex.mj.generated.client.carrier.model.RunPriceInfoDto;
import ru.yandex.mj.generated.client.carrier.model.RunPriceStatusDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup("/repository/permission/super_user.xml")
public class RunControllerPriceInfoTest extends AbstractContextualTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RunApiClient runClient;

    @Test
    void priceInfo() throws Exception {
        ExecuteCall<RunPriceInfoDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(
                new RunPriceInfoDto()
                        .runId(1L)
                        .priceCents(13_000_00L)
                        .totalCostCent(13_000_00L)
                        .totalPenalty(-3_000_00L)
                        .priceControls(List.of(
                                new PriceControlDto()
                                        .id(10L)
                                        .type(PriceControlPenaltyTypeDto.MANUAL_DELAY)
                                        .penaltyCent(-3_000_00L)
                                        .comment("Задержка на час")
                                        .author("ogonek")
                                        .penaltyStatus(PriceControlPenaltyStatusDto.CONFIRMED)
                        ))
                        .priceStatus(RunPriceStatusDto.CONFIRMED)
        ));

        Mockito.when(runClient.internalRunsIdPriceInfoGet(
                        Mockito.any()
                ))
                .thenReturn(call);

        mockMvc.perform(get("/runs/1/priceInfo")
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "super-user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/trip/price_info_response.json", true));
    }

    @Test
    void addPriceControl() throws Exception {
        ExecuteCall<RunPriceInfoDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(
                new RunPriceInfoDto()
                        .runId(1L)
                        .priceCents(13_000_00L)
                        .totalCostCent(13_000_00L)
                        .totalPenalty(-3_000_00L)
                        .priceControls(List.of(
                                new PriceControlDto()
                                        .id(10L)
                                        .type(PriceControlPenaltyTypeDto.MANUAL_DELAY)
                                        .penaltyCent(-3_000_00L)
                                        .comment("Задержка на час")
                                        .author("ogonek")
                                        .penaltyStatus(PriceControlPenaltyStatusDto.CONFIRMED)
                        ))
                        .priceStatus(RunPriceStatusDto.CONFIRMED)
        ));

        Mockito.when(runClient.internalRunsIdPriceControlPost(
                        Mockito.any(), Mockito.any()
                ))
                .thenReturn(call);

        mockMvc.perform(post("/runs/1/priceControl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "super-user")
                        .content(extractFileContent("fixture/pipeline/request/postPriceInfo.json")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/trip/price_info_response.json", true));
    }

    @Test
    void updatePriceControl() throws Exception {
        ExecuteCall<RunPriceInfoDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(
                new RunPriceInfoDto()
                        .runId(1L)
                        .priceCents(13_000_00L)
                        .totalCostCent(13_000_00L)
                        .totalPenalty(-3_000_00L)
                        .priceControls(List.of(
                                new PriceControlDto()
                                        .id(10L)
                                        .type(PriceControlPenaltyTypeDto.MANUAL_DELAY)
                                        .penaltyCent(-3_000_00L)
                                        .comment("Задержка на час")
                                        .author("ogonek")
                                        .penaltyStatus(PriceControlPenaltyStatusDto.CONFIRMED)
                        ))
                        .priceStatus(RunPriceStatusDto.CONFIRMED)
        ));

        Mockito.when(runClient.internalRunsIdUpdatePriceControlPut(
                        Mockito.any(), Mockito.any()
                ))
                .thenReturn(call);

        mockMvc.perform(put("/runs/1/updatePriceControl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "super-user")
                        .content(extractFileContent("fixture/pipeline/request/postPriceInfo.json")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/trip/price_info_response.json", true));
    }
}

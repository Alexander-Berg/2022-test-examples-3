package ru.yandex.market.tsup.controller.front;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.DutyApiClient;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.DutyDto;
import ru.yandex.mj.generated.client.carrier.model.DutyRunDto;
import ru.yandex.mj.generated.client.carrier.model.DutyStatusDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DutyControllerGetByIdTest extends AbstractContextualTest {

    private static final long ID = 1L;

    @Autowired
    private DutyApiClient dutyApiClient;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any()))
                .thenReturn(List.of(
                        LogisticsPointResponse.newBuilder()
                                .id(456L)
                                .partnerId(567L)
                                .name("Point name")
                                .build()
                ));

        ExecuteCall<DutyDto, RetryStrategy> mock = Mockito.mock(ExecuteCall.class);
        Mockito.when(mock.schedule())
                .thenReturn(CompletableFuture.completedFuture(
                        new DutyDto()
                                .id(1L)
                                .status(DutyStatusDto.CREATED)
                                .name("Name")
                                .dutyStartTime(OffsetDateTime.of(2022, 4, 1, 10, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID))
                                .dutyEndTime(OffsetDateTime.of(2022, 4, 2, 0, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID))
                                .deliveryServiceId(123L)
                                .dutyLogisticPointId(456L)
                                .pallets(33)
                                .runId(2223L)
                                .run(new DutyRunDto().id(2223L))
                            .company(new CompanyDto().id(123L).name("ВелесТорг").deliveryServiceId(22154342L))
                                .priceCents(3000_00L)
                ));
        Mockito.when(dutyApiClient.internalDutiesIdGet(ID))
                .thenReturn(mock);
    }

    @SneakyThrows
    @Test
    void shouldGetDutyById() {
        mockMvc.perform(get("/duties/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/duty/duty_by_id_response.json"));
    }
}

package ru.yandex.market.tsup.controller.front;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.data.dto.Interval;
import ru.yandex.market.tsup.service.data_provider.entity.duty.dto.DutyCreateDTO;
import ru.yandex.market.tsup.util.DateTimeUtil;
import ru.yandex.mj.generated.client.carrier.api.DutyApiClient;
import ru.yandex.mj.generated.client.carrier.model.DutyDto;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.management.entity.type.PartnerType.FULFILLMENT;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public class DutyControllerCreateTest extends AbstractContextualTest {

    public static final long LOGISTIC_POINT_PARTNER_ID = 234L;
    public static final long LOGISTIC_POINT_ID = 234L;
    @Autowired
    private DutyApiClient dutyApiClient;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Mockito.when(lmsClient.searchPartners(any()))
                .thenReturn(List.of(
                        partnerResponse(LOGISTIC_POINT_PARTNER_ID, "Софьино 1", "SOFINO", FULFILLMENT)
                ));
        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any()))
                .thenReturn(List.of(LogisticsPointResponse.newBuilder()
                                .id(LOGISTIC_POINT_ID)
                                .name("name")
                                .partnerId(LOGISTIC_POINT_PARTNER_ID)
                                .build()
                                )
                );

        mockCreateDuty();
    }

    private void mockCreateDuty() {
        ExecuteCall executeCall = Mockito.mock(ExecuteCall.class);
        Mockito.when(executeCall.schedule())
                        .thenReturn(CompletableFuture.completedFuture(
                                new DutyDto()
                                        .id(111L)
                                        .dutyLogisticPointId(LOGISTIC_POINT_ID)
                                        .dutyStartTime(DateTimeUtil.toMoscowZonedDateTime(
                                                LocalDateTime.of(
                                                        2022, 4, 8, 9, 0, 0, 0
                                                )
                                        ))
                                        .dutyEndTime(DateTimeUtil.toMoscowZonedDateTime(
                                                LocalDateTime.of(
                                                        2022, 4, 8, 18, 0, 0, 0
                                                )
                                        ))
                        ));

        Mockito.when(dutyApiClient.internalDutiesPost(Mockito.any()))
                .thenReturn(executeCall);
    }

    @SneakyThrows
    @Test
    void shouldCreateDuty() {
        mockMvc.perform(post("/duties")
                .content(objectMapper.writeValueAsString(
                        DutyCreateDTO.builder()
                                .name("Дежурство на тарном")
                                .movingPartnerId(123L)
                                .partnerId(LOGISTIC_POINT_PARTNER_ID)
                                .priceCents(3500_00L)
                                .interval(new Interval(
                                        LocalDateTime.of(2022, 4, 8, 9, 0, 0),
                                        LocalDateTime.of(2022, 4, 8, 18, 0, 0)
                                ))
                                .pallets(33)
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(111L));
    }

    private static PartnerResponse partnerResponse(long id, String name, String readableName, PartnerType partnerType) {
        return PartnerResponse.newBuilder()
                .partnerType(partnerType)
                .readableName(readableName)
                .name(name)
                .id(id)
                .build();
    }
}

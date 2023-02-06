package ru.yandex.market.tsup.controller.front;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.service.data_provider.entity.duty.dto.DutyStatus;
import ru.yandex.mj.generated.client.carrier.api.DutyApiClient;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.DutyDto;
import ru.yandex.mj.generated.client.carrier.model.DutyRunDto;
import ru.yandex.mj.generated.client.carrier.model.DutyRunUserDto;
import ru.yandex.mj.generated.client.carrier.model.DutyStatusDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfDutyDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class DutyControllerTest extends AbstractContextualTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DutyApiClient dutyClient;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void init() {
        ExecuteCall<PageOfDutyDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(
            CompletableFuture.completedFuture(page(List.of(
                new DutyDto()
                    .id(10L)
                    .runId(100L)
                    .run(new DutyRunDto()
                            .id(100L)
                            .user(new DutyRunUserDto()
                                    .id(1L)
                                    .name("Иванов Игорь Петрович")
                                    .firstName("Игорь")
                                    .lastName("Иванов")
                                    .patronymic("Петрович")
                                    .phone("+7927223462")))
                    .status(DutyStatusDto.DUTY_FINISHED)
                    .priceCents(10_000_000L)
                    .pallets(33)
                    .name("Патруль в Конохе")
                    .dutyLogisticPointId(2000L)
                    .deliveryServiceId(22154342L)
                    .company(new CompanyDto().name("ВелесТорг").deliveryServiceId(22154342L).id(123L))
                    .dutyStartTime(OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 8, 0), ZoneOffset.of("+3")))
                    .dutyEndTime(OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 20, 0), ZoneOffset.of("+3"))),
                new DutyDto()
                    .id(11L)
                    .runId(101L)
                    .status(DutyStatusDto.DUTY_FINISHED)
                    .priceCents(15_000_000L)
                    .pallets(15)
                    .dutyLogisticPointId(2000L)
                    .deliveryServiceId(22154342L)
                    .company(new CompanyDto().name("ВелесТорг").deliveryServiceId(22154342L).id(123L))
                    .dutyStartTime(OffsetDateTime.of(LocalDateTime.of(2021, 1, 2, 9, 0), ZoneOffset.of("+0")))
                    .dutyEndTime(OffsetDateTime.of(LocalDateTime.of(2021, 1, 2, 20, 0), ZoneOffset.of("+0")))
            ))));


        Mockito.when(dutyClient.internalDutiesGet(
                Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(),
                Mockito.any()
            ))
            .thenReturn(call);

        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any()))
                .thenReturn(List.of(
                        LogisticsPointResponse.newBuilder()
                                .id(2000L)
                                .partnerId(2000L)
                                .name("Шляпа Гриффиндора")
                                .build()));

        Mockito.when(lmsClient.searchPartners(Mockito.any()))
                .thenReturn(List.of(PartnerResponse.newBuilder()
                        .id(2000L)
                        .name("Шляпа Гриффиндора").build()));
    }

    @SneakyThrows
    @Test
    void duties() {
        mockMvc.perform(get("/duties"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value(10))
            .andExpect(jsonPath("$.data[0].runId").value(100))
            .andExpect(jsonPath("$.data[0].run.id").value(100))
            .andExpect(jsonPath("$.data[0].run.user.id").value(1L))
            .andExpect(jsonPath("$.data[0].run.user.name").value("Иванов Игорь Петрович"))
            .andExpect(jsonPath("$.data[0].run.user.firstName").value("Игорь"))
            .andExpect(jsonPath("$.data[0].run.user.lastName").value("Иванов"))
            .andExpect(jsonPath("$.data[0].run.user.patronymic").value("Петрович"))
            .andExpect(jsonPath("$.data[0].run.user.phone").value("+7927223462"))
            .andExpect(jsonPath("$.data[0].status").value(DutyStatus.DUTY_FINISHED.name()))
            .andExpect(jsonPath("$.data[0].priceCents").value(10_000_000L))
            .andExpect(jsonPath("$.data[0].pallets").value(33))
            .andExpect(jsonPath("$.data[0].interval.from").value("2021-01-01T08:00:00"))
            .andExpect(jsonPath("$.data[0].interval.to").value("2021-01-01T20:00:00"))
            .andExpect(jsonPath("$.data[0].movingPartner.id").value(22154342L))
            .andExpect(jsonPath("$.data[0].movingPartner.name").value("ВелесТорг"))
            .andExpect(jsonPath("$.data[0].company.id").value(123L))
            .andExpect(jsonPath("$.data[0].company.name").value("ВелесТорг"))
            .andExpect(jsonPath("$.data[0].company.deliveryServiceId").value(22154342L))
            .andExpect(jsonPath("$.data[0].partner.id").value(2000L))
            .andExpect(jsonPath("$.data[0].name").value("Патруль в Конохе"))
            .andExpect(jsonPath("$.data[0].partner.name").value("Шляпа Гриффиндора"));
    }

    @SneakyThrows
    @Test
    void dutiesOffsetToMoscowTimezone() {
        mockMvc.perform(get("/duties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[1].id").value(11))
                .andExpect(jsonPath("$.data[1].interval.from").value("2021-01-02T12:00:00"))
                .andExpect(jsonPath("$.data[1].interval.to").value("2021-01-02T23:00:00"));
    }

    @SneakyThrows
    @Test
    void emptyResponse() {
        ExecuteCall<PageOfDutyDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(
                CompletableFuture.completedFuture(page(Collections.emptyList())));

        Mockito.when(dutyClient.internalDutiesGet(
                        Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.any()
                ))
                .thenReturn(call);

        mockMvc.perform(get("/duties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private static PageOfDutyDto page(List<DutyDto> dtos) {
        PageOfDutyDto page = new PageOfDutyDto();
        page.setContent(dtos);
        page.setTotalElements((long) dtos.size());
        page.setTotalPages(0);
        page.setNumber(0);
        page.setSize(20);
        return page;
    }
}

package ru.yandex.market.tsup.controller.front;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.RunApiClient;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.DriverDto;
import ru.yandex.mj.generated.client.carrier.model.DriverLogDto;
import ru.yandex.mj.generated.client.carrier.model.FinancialStatusDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfRunDto;
import ru.yandex.mj.generated.client.carrier.model.RunDto;
import ru.yandex.mj.generated.client.carrier.model.RunMessageTypeDto;
import ru.yandex.mj.generated.client.carrier.model.RunPointDto;
import ru.yandex.mj.generated.client.carrier.model.RunPriceStatusDto;
import ru.yandex.mj.generated.client.carrier.model.RunProcessedByCoordinatorStatusDto;
import ru.yandex.mj.generated.client.carrier.model.RunStatusDto;
import ru.yandex.mj.generated.client.carrier.model.RunSubtypeDto;
import ru.yandex.mj.generated.client.carrier.model.RunTypeDto;
import ru.yandex.mj.generated.client.carrier.model.RunTypeSubtypeDto;
import ru.yandex.mj.generated.client.carrier.model.TimestampDto;
import ru.yandex.mj.generated.client.carrier.model.TransportDto;
import ru.yandex.mj.generated.client.carrier.model.UserLocationDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RunControllerTest extends AbstractContextualTest {

    @Autowired
    RunApiClient runApiClient;


    @BeforeEach
    void setUp() {
        ExecuteCall<List<RunTypeSubtypeDto>, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(List.of(
                new RunTypeSubtypeDto()
                        .type(RunTypeDto.LINEHAUL)
                            .addSubtypesItem(RunSubtypeDto.MAIN)
                            .addSubtypesItem(RunSubtypeDto.UNSCHEDULED)
                            .addSubtypesItem(RunSubtypeDto.DUTY)
                            .addSubtypesItem(RunSubtypeDto.SUPPLEMENTARY_1)
                            .addSubtypesItem(RunSubtypeDto.SUPPLEMENTARY_2)
                            .addSubtypesItem(RunSubtypeDto.SUPPLEMENTARY_3)
                            .addSubtypesItem(RunSubtypeDto.SUPPLEMENTARY_4)
                            .addSubtypesItem(RunSubtypeDto.SUPPLEMENTARY_5)
                            .addSubtypesItem(RunSubtypeDto.PALLETS_RETURN),
                new RunTypeSubtypeDto()
                        .type(RunTypeDto.INTAKE),
                new RunTypeSubtypeDto()
                        .type(RunTypeDto.XDOCK),
                new RunTypeSubtypeDto()
                        .type(RunTypeDto.INTERWAREHOUSE)
        )));

        Mockito.when(runApiClient.internalRunsTypeAndSubtypesGet()).thenReturn(call);
    }

    @Test
    void tripStatuses() throws Exception {
        mockMvc.perform(get("/runs/filterOptions/movementState")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent(
                        "fixture/trip/filter_options/movementState.json",
                        true
                ));
    }

    @Test
    void tripTypes() throws Exception {
        mockMvc.perform(get("/runs/filterOptions/transportationType")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent(
                        "fixture/trip/filter_options/transportationType.json",
                        JSONCompareMode.LENIENT
                ));
    }

    @Test
    @SneakyThrows
    void runTypeSubtype() {
        mockMvc.perform(get("/runs/filterOptions/typeSubtypes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(IntegrationTestUtils.jsonContent(
                        "fixture/trip/filter_options/runTypeSubtype.json",
                        JSONCompareMode.LENIENT
                ));
    }

    @Test
    @SneakyThrows
    void shouldReturnRun() {
        ExecuteCall<PageOfRunDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(new PageOfRunDto()
                .content(List.of(getResponse()))
                .size(1)
                .totalElements(1L)
                .totalPages(1)
                .number(1)));

        /*Mockito.when(runApiClient.internalRunsGet(
                        Mockito.anyLong(), Mockito.any(),
                        Mockito.anyLong(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.anyLong(), Mockito.any(),
                        Mockito.anyLong(), Mockito.anyInt(),
                        Mockito.anyInt(), Mockito.any()
        )).thenReturn(call);*/

        Mockito.when(runApiClient.internalRunsGet(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(call);

        mockMvc.perform(get("/runs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(IntegrationTestUtils.jsonContent(
                        "fixture/run/pageOfRunsResponse.json",
                        JSONCompareMode.LENIENT
                ));

    }

    private RunDto getResponse() {
        var driver = new DriverDto();
        driver.setId(777L);
        driver.setFirstName("Вин");
        driver.setLastName("Дизель");

        var transport = new TransportDto();
        transport.setId(999L);
        transport.setName("Dodge");
        transport.setNumber("A777AA777");

        var zoneName = "Europe/Moscow";
        var timestamp = new TimestampDto()
                .timestamp(ZonedDateTime.of(2021, 12, 12, 12, 12, 12, 0, ZoneId.of(zoneName)).toOffsetDateTime())
                .timezoneName(zoneName);

        var firstPoint = new RunPointDto()
                .yandexId(1001L)
                .name("точка")
                .address("Где-то в Москве")
                .latitude(new BigDecimal(12))
                .longitude(new BigDecimal(34));

        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDateTime.of(2021, 1, 2, 11, 0, 0), ZoneOffset.of("+3"));

        var firstLocation = new UserLocationDto()
                .latitude(BigDecimal.ONE)
                .longitude(BigDecimal.TEN)
                .timestamp(OffsetDateTime.of(LocalDateTime.of(2021, 1, 2, 12, 0, 0), ZoneOffset.of("+3")));

        var driverLog = new DriverLogDto()
                .estimatedTimeOfArrival(offsetDateTime)
                .plannedTimeOfArrival(offsetDateTime)
                .lastMessageTime(offsetDateTime)
                .lastMessageType(RunMessageTypeDto.OTHER);

        var response = new RunDto();
        var localDate = LocalDate.of(2022, 3, 20);

        response.setId(12L);
        response.setExternalId("TMT12");
        response.setDate(localDate);
        response.setStartDateTime(offsetDateTime);
        response.setLastPointArrivalTime(offsetDateTime);
        response.setPriceCents(10000L);
        response.setWarningDriverNotAssigned(Boolean.FALSE);
        response.setPriceStatus(RunPriceStatusDto.CONFIRMED);
        response.setFinancialStatus(FinancialStatusDto.NEW);
        response.setProcessedByCoordinatorStatusDto(RunProcessedByCoordinatorStatusDto.NOT_PROCESSED_BY_COORDINATOR);
        response.setDefaultStartDateTime(timestamp);
        response.setLocalStartDateTime(timestamp);
        response.setDefaultLastPointArrivalTime(timestamp);
        response.setLocalLastPointArrivalTime(timestamp);
        response.setExpectedDateTime(timestamp);
        response.setEndDateTime(offsetDateTime);
        response.setDefaultEndDateTime(timestamp);
        response.setLocalEndDateTime(timestamp);
        response.setStatus(RunStatusDto.CONFIRMED);
        response.setType(RunTypeDto.LINEHAUL);
        response.setTypes(List.of(RunTypeDto.LINEHAUL, RunTypeDto.INTERWAREHOUSE));
        response.setPoints(List.of(firstPoint));
        response.setLastLocation(firstLocation);
        response.setUser(driver);
        response.setTransport(transport);
        response.setCompany(new CompanyDto().id(1234L).name("test_company").deliveryServiceId(12345L));
        response.driverLog(driverLog);
        response.setSubtype(RunSubtypeDto.MAIN);
        response.setSubtypes(List.of(RunSubtypeDto.MAIN, RunSubtypeDto.SUPPLEMENTARY_1));

        return response;
    }
}

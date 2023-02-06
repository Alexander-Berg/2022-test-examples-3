package ru.yandex.market.tsup.controller.front;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.service.user_process.RunMessageProcessingService;
import ru.yandex.mj.generated.client.carrier.api.RunApiClient;
import ru.yandex.mj.generated.client.carrier.model.BarcodeDto;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.DriverDto;
import ru.yandex.mj.generated.client.carrier.model.FinancialStatusDto;
import ru.yandex.mj.generated.client.carrier.model.MessageDto;
import ru.yandex.mj.generated.client.carrier.model.PhotoDto;
import ru.yandex.mj.generated.client.carrier.model.PointDto;
import ru.yandex.mj.generated.client.carrier.model.PointStatusDto;
import ru.yandex.mj.generated.client.carrier.model.PointTypeDto;
import ru.yandex.mj.generated.client.carrier.model.RegistryDto;
import ru.yandex.mj.generated.client.carrier.model.RegistryStatusDto;
import ru.yandex.mj.generated.client.carrier.model.RunCommentDto;
import ru.yandex.mj.generated.client.carrier.model.RunCommentScopeDto;
import ru.yandex.mj.generated.client.carrier.model.RunCommentSeverityDto;
import ru.yandex.mj.generated.client.carrier.model.RunCommentTypeDto;
import ru.yandex.mj.generated.client.carrier.model.RunDetailDto;
import ru.yandex.mj.generated.client.carrier.model.RunProcessedByCoordinatorStatusDto;
import ru.yandex.mj.generated.client.carrier.model.RunStatusDto;
import ru.yandex.mj.generated.client.carrier.model.RunSubtypeDto;
import ru.yandex.mj.generated.client.carrier.model.RunTypeDto;
import ru.yandex.mj.generated.client.carrier.model.TimestampDto;
import ru.yandex.mj.generated.client.carrier.model.TransportDto;
import ru.yandex.mj.generated.client.carrier.model.UserLocationDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class RunDetailsControllerTest extends AbstractContextualTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RunApiClient runClient;

    @MockBean
    private RunMessageProcessingService runMessageProcessingService;

    @BeforeEach
    void setUp() {
        ExecuteCall<RunDetailDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(getResponse()));

        Mockito.when(runClient.internalRunsIdGet(Mockito.any())).thenReturn(call);
        Mockito.when(runClient.internalRunsByExternalIdExternalIdGet(Mockito.any())).thenReturn(call);
    }

    @Test
    void runDetails() throws Exception {
        mockMvc.perform(get("/runDetails/byRunExternalId/123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/run/runDetailsResponse.json"));

        //cache smoke check
        mockMvc.perform(get("/runDetails/byRunExternalId/123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/run/runDetailsResponse.json"));
    }

    @Test
    void runDetailsByTripId() throws Exception {
        mockMvc.perform(get("/runs/byTrip/12")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/run/runDetailsResponse.json"));
    }

    @Test
    void runDetailsById() throws Exception {
        mockMvc.perform(get("/runDetails/12")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/run/runDetailsResponse.json"));
    }

    @Test
    void ignoreExpiredRunDefaultHours() throws Exception {
        mockMvc.perform(
                        put("/runDetails/1/ignoreExpiredRun"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(runMessageProcessingService).temporaryIgnoreExpiredRun(1, 2);
    }

    @Test
    void ignoreExpiredRunExplicitHours() throws Exception {
        mockMvc.perform(
                        put("/runDetails/1/ignoreExpiredRun")
                                .param("hours", "4"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(runMessageProcessingService).temporaryIgnoreExpiredRun(1, 4);
    }

    private static RunDetailDto getResponse() {
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

        var firstPoint = new PointDto()
                .routePointId(1001L)
                .type(PointTypeDto.COLLECT_DROPSHIP)
                .latitude(new BigDecimal(12))
                .longitude(new BigDecimal(34))
                .address("Где-то в Москве")
                .status(PointStatusDto.IN_PROGRESS)
                .photos(List.of(new PhotoDto().url("https://blabla").id(12345L)))
                .registry(new RegistryDto()
                        .id(1L)
                        .movementId(2L)
                        .taskId(3L)
                        .status(RegistryStatusDto.FINALISED)
                        .sortables(List.of(new BarcodeDto().id(1L).barcode("abc"))))
                .expectedArrivalTimestamp(timestamp.getTimestamp())
                .localExpectedArrivalTimestamp(timestamp)
                .defaultExpectedArrivalTimestamp(timestamp)
                .arrivalTimestamp(timestamp.getTimestamp())
                .carrierArrivalTimestamp(timestamp.getTimestamp())
                .localArrivalTimestamp(timestamp)
                .defaultArrivalTimestamp(timestamp)
                .isArrivalTimestampEditable(false);

        var firstMassage = new MessageDto()
                .timestamp(timestamp.getTimestamp())
                .defaultTimestamp(timestamp)
                .localTimestamp(timestamp)
                .longitude(new BigDecimal(12))
                .latitude(new BigDecimal(34))
                .message("smth");

        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDateTime.of(2021, 1, 2, 11, 0, 0), ZoneOffset.of("+3"));
        var comment = new RunCommentDto()
                .createdAt(offsetDateTime)
                .defaultTimestamp(new TimestampDto().timestamp(offsetDateTime).timezoneName("Europe/Moscow"))
                .localTimestamp(new TimestampDto().timestamp(offsetDateTime).timezoneName("Europe/Moscow"))
                .scope(RunCommentScopeDto.DELAY_REPORT)
                .type(RunCommentTypeDto.DISRUPTION_TRANSPORT_COMPANY)
                .severity(RunCommentSeverityDto.CRITICAL)
                .text("Водила пьян!!!")
                .author("coordinator");

        var firstLocation = new UserLocationDto()
                .latitude(BigDecimal.ONE)
                .longitude(BigDecimal.TEN)
                .timestamp(OffsetDateTime.of(LocalDateTime.of(2021, 1, 2, 12, 0, 0), ZoneOffset.of("+3")));

        var response = new RunDetailDto();
        response.setRunId(12L);
        response.setName("Test run");
        response.setPallets(33);
        response.setType(RunTypeDto.LINEHAUL);
        response.setSubtype(RunSubtypeDto.MAIN);
        response.setCompany(new CompanyDto().id(1234L).name("test_company").deliveryServiceId(12345L));
        response.setStatus(RunStatusDto.CONFIRMED);
        response.setFinancialStatus(FinancialStatusDto.NEW);
        response.setDriver(driver);
        response.setTransport(transport);
        response.setPoints(List.of(firstPoint));
        response.setMessages(List.of(firstMassage));
        response.setComments(List.of(comment));
        response.setUserLocations(List.of(firstLocation));
        response.setExpectedDateTime(new TimestampDto()
                .timezoneName("Europe/Moscow")
                .timestamp(OffsetDateTime.of(2022, 3, 2, 11, 0, 0, 0, ZoneOffset.ofHours(3)))
        );
        response.setProcessedByCoordinatorStatusDto(RunProcessedByCoordinatorStatusDto.NOT_PROCESSED_BY_COORDINATOR);
        response.setTypes(List.of(RunTypeDto.LINEHAUL, RunTypeDto.INTERWAREHOUSE));
        response.setSubtypes(List.of(RunSubtypeDto.MAIN, RunSubtypeDto.SUPPLEMENTARY_1));
        return response;
    }
}

package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.PointDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RoutePointControllerTest extends BasePlannerWebTest {

    private final ObjectMapper objectMapper;
    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final RunRepository runRepository;
    private final UserShiftRepository userShiftRepository;
    private final RoutePointRepository routePointRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TestableClock clock;

    private User user;
    private Transport transport;
    private Run run;
    private Run expensiveRun;
    private LocalDate runDate;
    private Instant expectedFirst;
    private Instant expectedSecond;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.MILLIS_TO_ALLOW_ACTUAL_ARRIVAL_TIME_EDIT_AFTER_EXPECTED_TIME, 600000); //10 mins

        testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        user = testUserHelper.findOrCreateUser(1L);
        transport = testUserHelper.findOrCreateTransport();

        runDate = LocalDate.now(clock); // 1989-12-31T21:00:00Z
        expectedFirst = Instant.now(clock).minusSeconds(4 * 60 * 60); // 1989-12-31T17:00:00Z
        expectedSecond = Instant.now(clock).minusSeconds(2 * 60 * 60); // 1989-12-31T19:00:00Z
        run = runGenerator.generate(r -> r
                .runDate(runDate)
                .price(1_000_00L)
                .clearItems()
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .outboundArrivalTime(expectedFirst)
                                .inboundArrivalTime(expectedSecond)
                                .build())
                        .orderNumber(1)
                        .build()));
        expensiveRun = runGenerator.generate(r -> r
                .runDate(runDate)
                .price(100_000_00L)
                .clearItems()
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .outboundArrivalTime(expectedFirst)
                                .inboundArrivalTime(expectedSecond)
                                .build())
                        .orderNumber(1)
                        .build()));
    }

    @Test
    @SneakyThrows
    void shouldNotAllowToUpdateArrivalTimesWhenShiftNotAssignedYet() {
        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(List.of()))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Driver or transport have not been assigned yet! Unable to change arrival time"));
    }

    @Test
    @SneakyThrows
    void shouldNotAllowToUpdateArrivalTimesIfInvalidUpdateDataSize() {
        var userShift = runHelper.assignUserAndTransport(run, user, transport);

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(List.of()))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Expected and provided RoutePoints do not match"));
    }

    @Test
    @SneakyThrows
    void shouldNotAllowToUpdateArrivalTimesIfInvalidUpdateData() {
        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        var routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        Instant arrivalTimestamp2 = Instant.now(clock).plusSeconds(15 * 60);
        var payload = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(null),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp2, ZoneId.of("UTC")))
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("One of the routePoints have missing data"));
    }

    @Test
    @SneakyThrows
    void shouldNotAllowToUpdateArrivalTimesIfTimeThresholdNotYetPassed() {
        Instant arrivalTimestamp1 = Instant.now(clock);
        Instant arrivalTimestamp2 = Instant.now(clock).plusSeconds(15 * 60);
        run = runGenerator.generate(r -> r
                .runDate(runDate)
                .clearItems()
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .deliveryIntervalFrom(arrivalTimestamp1)
                                .inboundArrivalTime(expectedSecond)
                                .build())
                        .orderNumber(1)
                        .build()));

        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        var routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        var payload = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp2, ZoneId.of("UTC"))),
        new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp2, ZoneId.of("UTC")))
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Too little time after expected arrival time. Try again later"));
    }

    @Test
    @SneakyThrows
    void shouldUpdateDataIfShiftNotStartedYet() {
        var userShift = runHelper.assignUserAndTransport(run, user, transport);

        var routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        Assertions.assertEquals(UserShiftStatus.SHIFT_CREATED, userShift.getStatus());
        Assertions.assertEquals(RoutePointStatus.NOT_STARTED, routePointRepository.findById(routePoints.get(0).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(RoutePointStatus.NOT_STARTED, routePointRepository.findById(routePoints.get(1).getId()).orElseThrow().getStatus());

        Instant arrivalTimestamp1 = Instant.now(clock);
        Instant arrivalTimestamp2 = Instant.now(clock).plusSeconds(15 * 60);
        var payload = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp1, ZoneId.of("UTC"))),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(null)
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].arrivalTimestamp").value(arrivalTimestamp1.toString()));

        var updatedUserShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        Assertions.assertEquals(UserShiftStatus.SHIFT_CREATED, updatedUserShift.getStatus());
        Assertions.assertEquals(RoutePointStatus.FINISHED, routePointRepository.findById(routePoints.get(0).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(RoutePointStatus.NOT_STARTED, routePointRepository.findById(routePoints.get(1).getId()).orElseThrow().getStatus());

        var payload2 = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp1, ZoneId.of("UTC"))),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp2, ZoneId.of("UTC")))
                );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload2))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].arrivalTimestamp").value(arrivalTimestamp1.toString()))
                .andExpect(jsonPath("$[1].routePointId").value(routePoints.get(1).getId()))
                .andExpect(jsonPath("$[1].arrivalTimestamp").value(arrivalTimestamp2.toString()));

        Assertions.assertEquals(RoutePointStatus.FINISHED, routePointRepository.findById(routePoints.get(0).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(RoutePointStatus.FINISHED, routePointRepository.findById(routePoints.get(1).getId()).orElseThrow().getStatus());
        updatedUserShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        Assertions.assertEquals(UserShiftStatus.SHIFT_CREATED, updatedUserShift.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldEmulateDriverActionsIfShiftIsOnTask() {
        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(userShift.getUser(), userShift.getId());
        var updatedUserShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        var routePoints = userShift.streamRoutePoints().collect(Collectors.toList());

        Assertions.assertEquals(RoutePointStatus.IN_TRANSIT, routePointRepository.findById(routePoints.get(0).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(RoutePointStatus.NOT_STARTED, routePointRepository.findById(routePoints.get(1).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(UserShiftStatus.ON_TASK, updatedUserShift.getStatus());

        Instant arrivalTimestamp1 = Instant.now(clock);
        Instant arrivalTimestamp2 = Instant.now(clock).plusSeconds(15 * 60);
        var payload = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp1, ZoneId.of("UTC"))),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(null)
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].arrivalTimestamp").value(arrivalTimestamp1.toString()));


        Assertions.assertEquals(RoutePointStatus.FINISHED, routePointRepository.findById(routePoints.get(0).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(RoutePointStatus.IN_TRANSIT, routePointRepository.findById(routePoints.get(1).getId()).orElseThrow().getStatus());
        updatedUserShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        Assertions.assertEquals(UserShiftStatus.ON_TASK, updatedUserShift.getStatus());

        var payload2 = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp1, ZoneId.of("UTC"))),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp2, ZoneId.of("UTC")))
                );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload2))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].arrivalTimestamp").value(arrivalTimestamp1.toString()));

        run = runRepository.findById(run.getId()).orElseThrow();
        updatedUserShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        Assertions.assertEquals(RoutePointStatus.FINISHED, routePointRepository.findById(routePoints.get(0).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(RoutePointStatus.FINISHED, routePointRepository.findById(routePoints.get(1).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(UserShiftStatus.SHIFT_FINISHED, updatedUserShift.getStatus());
        Assertions.assertEquals(RunStatus.COMPLETED, run.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldNotUpdateDataIfNothingChanged() {
        var userShift = runHelper.assignUserAndTransport(run, user, transport);

        var routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        Instant arrivalTimestamp1 = Instant.now(clock);
        Instant arrivalTimestamp2 = Instant.now(clock).plusSeconds(15 * 60);
        var payload = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp1, ZoneId.of("UTC"))),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(null)
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].arrivalTimestamp").value(arrivalTimestamp1.toString()))
                .andExpect(jsonPath("$[1].routePointId").value(routePoints.get(1).getId()))
                .andExpect(jsonPath("$[1].arrivalTimestamp").doesNotExist());

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].arrivalTimestamp").value(arrivalTimestamp1.toString()))
                .andExpect(jsonPath("$[1].routePointId").value(routePoints.get(1).getId()))
                .andExpect(jsonPath("$[1].arrivalTimestamp").doesNotExist());

        var updatedUserShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        Assertions.assertEquals(RoutePointStatus.FINISHED, routePointRepository.findById(routePoints.get(0).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(RoutePointStatus.NOT_STARTED, routePointRepository.findById(routePoints.get(1).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(UserShiftStatus.SHIFT_CREATED, updatedUserShift.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldUpdateArrivalDataInClosedShift() {
        var userShift = runHelper.assignUserAndTransport(run, user, transport);

        var routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        Assertions.assertEquals(UserShiftStatus.SHIFT_CREATED, userShift.getStatus());
        Assertions.assertEquals(RoutePointStatus.NOT_STARTED, routePointRepository.findById(routePoints.get(0).getId()).orElseThrow().getStatus());
        Assertions.assertEquals(RoutePointStatus.NOT_STARTED, routePointRepository.findById(routePoints.get(1).getId()).orElseThrow().getStatus());

        Instant arrivalTimestamp1 = Instant.now(clock);
        Instant arrivalTimestamp2 = Instant.now(clock).plusSeconds(15 * 60);
        var payload = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp1, ZoneId.of("UTC"))),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp2, ZoneId.of("UTC")))
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].arrivalTimestamp").value(arrivalTimestamp1.toString()))
                .andExpect(jsonPath("$[1].routePointId").value(routePoints.get(1).getId()))
                .andExpect(jsonPath("$[1].arrivalTimestamp").value(arrivalTimestamp2.toString()));

        var updatedUserShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        Assertions.assertEquals(UserShiftStatus.SHIFT_CREATED, updatedUserShift.getStatus());

        Instant arrivalTimestamp3 = arrivalTimestamp1.plusSeconds(100000);
        Instant arrivalTimestamp4 = arrivalTimestamp3.plusSeconds(15 * 60);
        var payload2 = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp3, ZoneId.of("UTC"))),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp4, ZoneId.of("UTC")))
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload2))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].arrivalTimestamp").value(arrivalTimestamp3.toString()))
                .andExpect(jsonPath("$[1].routePointId").value(routePoints.get(1).getId()))
                .andExpect(jsonPath("$[1].arrivalTimestamp").value(arrivalTimestamp4.toString()));

    }

    @Test
    @SneakyThrows
    void shouldUpdateArrivalData() {
        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(userShift.getUser(), userShift.getId());
        testUserHelper.arriveAtRoutePoint(userShift.streamRoutePoints().findFirst().get());
        var routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        Instant arrivalTimestamp1 = Instant.now(clock);
        Instant arrivalTimestamp2 = Instant.now(clock).plusSeconds(15 * 60);
        var payload = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp1, ZoneId.of("UTC"))),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(OffsetDateTime.ofInstant(arrivalTimestamp2, ZoneId.of("UTC")))
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].arrivalTimestamp").value(arrivalTimestamp1.toString()))
                .andExpect(jsonPath("$[1].routePointId").value(routePoints.get(1).getId()))
                .andExpect(jsonPath("$[1].arrivalTimestamp").value(arrivalTimestamp2.toString()));
    }

    @Test
    @SneakyThrows
    void shouldReturnIsEditableFalseIfThresholdNotPassed() {
        Instant arrivalTimestamp1 = Instant.now(clock);
        run = runGenerator.generate(r -> r
                .runDate(runDate)
                .clearItems()
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .deliveryIntervalFrom(arrivalTimestamp1)
                                .inboundArrivalTime(expectedSecond)
                                .build())
                        .orderNumber(1)
                        .build()));

        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        var routePoints = userShift.streamRoutePoints().collect(Collectors.toList());
        var payload = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(null),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(null)
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].isArrivalTimestampEditable").value(false))
                .andExpect(jsonPath("$[1].routePointId").value(routePoints.get(1).getId()))
                .andExpect(jsonPath("$[1].isArrivalTimestampEditable").value(true));
    }

    @Test
    @SneakyThrows
    void shouldReturnIsEditableTrue() {
        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        var routePoints = userShift.streamRoutePoints().collect(Collectors.toList());

        var payload = List.of(
                new PointDto()
                        .routePointId(routePoints.get(0).getId())
                        .arrivalTimestamp(null),
                new PointDto()
                        .routePointId(routePoints.get(1).getId())
                        .arrivalTimestamp(null)
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                .content(objectMapper.writeValueAsString(payload))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].routePointId").value(routePoints.get(0).getId()))
                .andExpect(jsonPath("$[0].isArrivalTimestampEditable").value(true))
                .andExpect(jsonPath("$[1].routePointId").value(routePoints.get(1).getId()))
                .andExpect(jsonPath("$[1].isArrivalTimestampEditable").value(true));
    }

}

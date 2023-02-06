package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.TripInfo;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyRepository;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointType;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.mj.generated.server.model.PointTimesDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.INBOUND_DEFAULT_INTERVAL;
import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.OUTBOUND_DEFAULT_INTERVAL;
import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.prepareMovement;

@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PutMovementShouldStartDutyRunWithoutDriverTest extends BasePlannerWebTest {

    public static final long DELIVERY_SERVICE_ID = 100500L;
    private final DutyGenerator dutyGenerator;
    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;
    private final ObjectMapper objectMapper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final Clock clock;
    private final PutMovementHelper putMovementHelper;

    private final DutyRepository dutyRepository;
    private final UserShiftRepository userShiftRepository;

    private User user;

    private Run run;
    private Duty duty;
    private UserShift userShift;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.MILLIS_TO_ALLOW_ACTUAL_ARRIVAL_TIME_EDIT_AFTER_EXPECTED_TIME, 0
        );

        testUserHelper.deliveryService(DELIVERY_SERVICE_ID, Set.of(testUserHelper.findOrCreateCompany(
                TestUserHelper.CompanyGenerateParam.builder()
                        .deliveryServiceIds(Set.of(DELIVERY_SERVICE_ID))
                        .build()
        )));

        duty = dutyGenerator.generate(
                dutyGenerateParamsBuilder -> dutyGenerateParamsBuilder
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .dutyStartTime(clock.instant())
        );
        user = testUserHelper.findOrCreateUser(UID);
        Transport transport = testUserHelper.findOrCreateTransport();
        run = duty.getRun();
        userShift = runHelper.assignUserAndTransport(run, user, transport);
    }

    @SneakyThrows
    @Test
    void shouldStartDutyRun() {
        testUserHelper.openShift(user, userShift.getId());

        arriveManually(UserShiftStatus.ON_TASK);

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                        prepareMovement(
                                new ResourceId("TMM2", null),
                                PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                                BigDecimal.ONE,
                                new ResourceId("2", "1234"),
                                new ResourceId("3", "1234"),
                                INBOUND_DEFAULT_INTERVAL,
                                OUTBOUND_DEFAULT_INTERVAL,
                                mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                        .setTripId(new ResourceId("TMM123", String.valueOf(run.getId())))
                                        .setRouteName("RouteName")
                                        .setFromIndex(0)
                                        .setToIndex(1)
                                        .setTotalCount(2)
                                        .setPrice(10_000_000L)
                                        .build())
                        )
                ))
                .andExpect(status().isOk());

        duty = dutyRepository.findSingleByIdOrThrow(duty.getId());

        Assertions.assertThat(duty.getStatus()).isEqualTo(DutyStatus.TRIP_CREATED);
    }

    @SneakyThrows
    @Test
    void shouldStartDutyRunIfShiftWasNotOpened() {
        arriveManually(UserShiftStatus.SHIFT_CREATED);

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                        prepareMovement(
                                new ResourceId("TMM2", null),
                                PutMovementControllerTestUtil.DEFAULT_INTERVAL,
                                BigDecimal.ONE,
                                new ResourceId("2", "1234"),
                                new ResourceId("3", "1234"),
                                INBOUND_DEFAULT_INTERVAL,
                                OUTBOUND_DEFAULT_INTERVAL,
                                mb -> mb.setTrip(new TripInfo.TripInfoBuilder()
                                        .setTripId(new ResourceId("TMM123", String.valueOf(run.getId())))
                                        .setRouteName("RouteName")
                                        .setFromIndex(0)
                                        .setToIndex(1)
                                        .setTotalCount(2)
                                        .setPrice(10_000_000L)
                                        .build())
                        )
                ))
                .andExpect(status().isOk());

        duty = dutyRepository.findSingleByIdOrThrow(duty.getId());

        Assertions.assertThat(duty.getStatus()).isEqualTo(DutyStatus.TRIP_CREATED);
    }

    private void arriveManually(UserShiftStatus task) throws Exception {
        Long dutyRoutePointId = userShift.streamRoutePoints().findFirst().orElseThrow().getId();
        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                        .content(objectMapper.writeValueAsString(
                                List.of(
                                        new PointTimesDto()
                                                .routePointId(dutyRoutePointId)
                                                .arrivalTimestamp(clock.instant().atOffset(DateTimeUtil.DEFAULT_ZONE_ID))
                                )
                        ))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            Assertions.assertThat(userShift.getStatus()).isEqualTo(task);
            RoutePoint routePoint = userShift.findRoutePoint(dutyRoutePointId).orElseThrow();
            Assertions.assertThat(routePoint.getType()).isEqualTo(RoutePointType.DUTY);
            Assertions.assertThat(routePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
            return null;
        });
    }


}

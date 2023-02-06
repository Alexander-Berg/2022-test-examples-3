package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
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
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.mj.generated.server.model.PointDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties.MILLIS_TO_ALLOW_ACTUAL_ARRIVAL_TIME_EDIT_AFTER_EXPECTED_TIME;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RoutePointControllerDutyTest extends BasePlannerWebTest {
    private final DutyGenerator dutyGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final ObjectMapper objectMapper;

    private final DutyRepository dutyRepository;

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final Clock clock;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(MILLIS_TO_ALLOW_ACTUAL_ARRIVAL_TIME_EDIT_AFTER_EXPECTED_TIME, 0);
    }

    @SneakyThrows
    @Test
    void shouldFinishDutyIfArrivalTimeFilled() {
        testUserHelper.deliveryService(123L, Set.of(testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME)));
        User user = testUserHelper.findOrCreateUser(UID);
        Transport transport = testUserHelper.findOrCreateTransport();

        Duty duty = dutyGenerator.generate(dutyGenerateParamsBuilder ->
                dutyGenerateParamsBuilder.dutyStartTime(clock.instant()));
        Run run = duty.getRun();
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);

        RoutePoint dutyRoutePoint = userShift.streamRoutePoints().findFirst().orElseThrow();
        OffsetDateTime arrivalTimestamp = DateTimeUtil.toOffsetDateTime(
                LocalDateTime.of(
                        2022, 4, 13, 15, 35, 0
                )
        );
        var payload = List.of(
                new PointDto()
                        .routePointId(dutyRoutePoint.getId())
                        .arrivalTimestamp(arrivalTimestamp)
        );

        mockMvc.perform(post("/internal/runs/{runId}/route-points", run.getId())
                        .content(objectMapper.writeValueAsString(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        duty = dutyRepository.findSingleByIdOrThrow(duty.getId());
        Assertions.assertThat(duty.getStatus()).isEqualTo(DutyStatus.DUTY_STARTED);

    }
}

package ru.yandex.market.tpl.carrier.driver.controller.api;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.http.HttpHeaders;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointType;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_=@Autowired)
class CollectDropshipTaskControllerTest extends BaseDriverApiIntTest {

    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final UserShiftRepository userShiftRepository;

    private final TransactionTemplate transactionTemplate;

    private UserShift userShift;
    private User user;

    private RoutePoint collectDropshipRoutePoint;
    private CollectDropshipTask collectDropshipTask;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
        Transport transport = testUserHelper.findOrCreateTransport();

        Run run = runGenerator.generate();
        userShift = runHelper.assignUserAndTransport(run, user, transport);
        List<RoutePoint> routePoints = userShift.streamRoutePoints().toList();
        Assertions.assertThat(routePoints).hasSize(2);

        collectDropshipRoutePoint = routePoints.get(0);
        Assertions.assertThat(collectDropshipRoutePoint.getType()).isEqualTo(RoutePointType.COLLECT_DROPSHIP);

        collectDropshipTask = collectDropshipRoutePoint.streamCollectDropshipTasks().findFirst().orElseThrow();

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.arriveAtRoutePoint(collectDropshipRoutePoint);
        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
    }

    @SneakyThrows
    @Test
    void shouldFinishCollectTask() {
        mockMvc.perform(post("/api/tasks/collect-dropship/{task-id}/finish", collectDropshipTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            List<RoutePoint> routePoints = userShift.streamRoutePoints().toList();
            Assertions.assertThat(routePoints.get(0).getStatus()).isEqualTo(RoutePointStatus.FINISHED);

            CollectDropshipTask collectDropshipTaskResult =
                    routePoints.get(0).streamCollectDropshipTasks().findFirst().orElseThrow();
            Assertions.assertThat(collectDropshipTaskResult.getStatus()).isEqualTo(CollectDropshipTaskStatus.FINISHED);
            return null;
        });

    }

}

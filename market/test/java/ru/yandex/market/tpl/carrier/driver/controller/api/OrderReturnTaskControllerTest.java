package ru.yandex.market.tpl.carrier.driver.controller.api;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.http.HttpHeaders;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.OrderReturnTaskStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointType;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.api.model.task.OrderScanTaskRequestDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
class OrderReturnTaskControllerTest extends BaseDriverApiIntTest {
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final UserShiftRepository userShiftRepository;

    private final ObjectMapper tplObjectMapper;
    private final TransactionTemplate transactionTemplate;

    private UserShift userShift;
    private User user;

    private RoutePoint orderReturnRoutePoint;
    private OrderReturnTask orderReturnTask;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
        Transport transport = testUserHelper.findOrCreateTransport();

        Run run = runGenerator.generate();
        userShift = runHelper.assignUserAndTransport(run, user, transport);
        List<RoutePoint> routePoints = userShift.streamRoutePoints().toList();
        Assertions.assertThat(routePoints).hasSize(2);

        orderReturnRoutePoint = routePoints.get(1);
        Assertions.assertThat(orderReturnRoutePoint.getType()).isEqualTo(RoutePointType.ORDER_RETURN);

        orderReturnTask = orderReturnRoutePoint.streamReturnTasks().findFirst().orElseThrow();

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishCollectDropships(routePoints.get(0));
        testUserHelper.arriveAtRoutePoint(orderReturnRoutePoint);
        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
    }

    @SneakyThrows
    @Test
    void shouldStartOrderReturn() {
        mockMvc.perform(post("/api/tasks/order-return/{task-id}/start", orderReturnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            List<RoutePoint> routePoints = userShift.streamRoutePoints().toList();
            Assertions.assertThat(routePoints.get(1).getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

            OrderReturnTask orderTaskResult =
                    routePoints.get(1).streamReturnTasks().findFirst().orElseThrow();
            Assertions.assertThat(orderTaskResult.getStatus()).isEqualTo(OrderReturnTaskStatus.IN_PROGRESS);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldFinishReturnOrders() {
        mockMvc.perform(post("/api/tasks/order-return/{task-id}/start", orderReturnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/order-return/{task-id}/finish", orderReturnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(new OrderScanTaskRequestDto()))
        )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            List<RoutePoint> routePoints = userShift.streamRoutePoints().toList();
            Assertions.assertThat(routePoints.get(1).getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

            OrderReturnTask orderTaskResult =
                    routePoints.get(1).streamReturnTasks().findFirst().orElseThrow();
            Assertions.assertThat(orderTaskResult.getStatus()).isEqualTo(OrderReturnTaskStatus.READY_TO_FINISH);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldFinishTask() {
        mockMvc.perform(post("/api/tasks/order-return/{task-id}/start", orderReturnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/order-return/{task-id}/finish", orderReturnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(new OrderScanTaskRequestDto()))
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/order-return/{task-id}/task/finish", orderReturnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk());


        transactionTemplate.execute(tc -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            List<RoutePoint> routePoints = userShift.streamRoutePoints().toList();
            Assertions.assertThat(routePoints.get(1).getStatus()).isEqualTo(RoutePointStatus.FINISHED);

            OrderReturnTask orderTaskResult =
                    routePoints.get(1).streamReturnTasks().findFirst().orElseThrow();
            Assertions.assertThat(orderTaskResult.getStatus()).isEqualTo(OrderReturnTaskStatus.FINISHED);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldFinishTaskNewFlow() {
        mockMvc.perform(post("/api/tasks/order-return/{task-id}/task/finish-v2", orderReturnTask.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            List<RoutePoint> routePoints = userShift.streamRoutePoints().toList();
            Assertions.assertThat(routePoints.get(1).getStatus()).isEqualTo(RoutePointStatus.FINISHED);

            OrderReturnTask orderTaskResult =
                    routePoints.get(1).streamReturnTasks().findFirst().orElseThrow();
            Assertions.assertThat(orderTaskResult.getStatus()).isEqualTo(OrderReturnTaskStatus.FINISHED);
            return null;
        });
    }

}

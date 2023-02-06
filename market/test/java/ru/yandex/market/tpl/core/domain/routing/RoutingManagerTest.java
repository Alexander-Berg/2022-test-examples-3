package ru.yandex.market.tpl.core.domain.routing;

import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;
import ru.yandex.market.tpl.core.external.routing.vrp.model.TaskInfo;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class RoutingManagerTest extends TplAbstractTest {
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper usHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final TplRoutingManager routingManager;
    private final UserShiftRepository userShiftRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final VrpClient vrpClient;

    private UserShift userShift;

    @AfterEach
    void clean() {
        reset(vrpClient);
    }

    @BeforeEach
    void init() {
        var user = userHelper.findOrCreateUser(777L);
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var params = OrderGenerateService.OrderGenerateParam.builder().deliveryDate(LocalDate.now(clock));
        Order order = orderGenerateService.createOrder(params.build());
        Order order2 = orderGenerateService.createOrder(params.build());
        Order order3 = orderGenerateService.createOrder(params.build());


        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(usHelper.taskUnpaid("addr1", 12, order.getId()))
                .routePoint(usHelper.taskPrepaid("addr3", 14, order2.getId()))
                .routePoint(usHelper.taskPrepaid("addrPaid", 13, order3.getId()))
                .build();

        userShift = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));

    }

    @Test
    @SneakyThrows
    void shouldSkipErrorResponse() {
        TaskInfo taskInfo = ObjectMappers.TPL_DB_OBJECT_MAPPER.readValue(
                new String(Files.readAllBytes(
                        new ClassPathResource("vrp/vrp_svrp_error_response.json").getFile().toPath()
                )),
                TaskInfo.class);
        testReroute(taskInfo);
    }

    @Test
    @SneakyThrows
    void shouldSkipInternalErrorResponse() {
        TaskInfo taskInfo = ObjectMappers.TPL_DB_OBJECT_MAPPER.readValue(
                new String(Files.readAllBytes(
                        new ClassPathResource("vrp/vrp_svrp_internal_error_response.json").getFile().toPath()
                )),
                TaskInfo.class);
        testReroute(taskInfo);
    }


    void testReroute(TaskInfo taskInfo) {
        var requestTask = new TaskInfo();
        requestTask.setId(taskInfo.getId());
        when(vrpClient.addMVRPTask(any(), any())).thenReturn(requestTask);

        when(vrpClient.getTaskResult(any(), any())).thenReturn(taskInfo);
        routingManager.rerouteUserShift(userShift, RoutingMockType.REAL, null, false, true, false);

        List<String> rows = jdbcTemplate.query(
                "select processing_id\n" +
                        "from routing_log\n" +
                        "where calculate_status = 'ERROR'\n" +
                        "order by created_at\n" +
                        "limit 10",
                (rs, rowNum) -> rs.getString("processing_id")
        );
        assertThat(rows).hasSize(3);
        rows = jdbcTemplate.query(
                "select processing_id\n" +
                        "from routing_log\n" +
                        "where shift_date >  current_date - interval '7 day'\n" +
                        "and transit_distance is null and processing_id is not null\n" +
                        "and  EXTRACT(EPOCH FROM (now() - created_at)) > 10\n" +
                        "and  (calculate_status is null or calculate_status != 'ERROR')\n" +
                        "order by created_at\n" +
                        "limit 400",
                (rs, rowNum) -> rs.getString("processing_id")
        );
        assertThat(rows).hasSize(0);
    }
}

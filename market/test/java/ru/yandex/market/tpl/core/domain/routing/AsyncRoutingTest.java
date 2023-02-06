package ru.yandex.market.tpl.core.domain.routing;

import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.routing.api.RoutingRequestItemTestBuilder;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogRecord;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingRequestGroupRepository;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingResultWithShiftDate;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.TplRoutingShiftManager;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultStatus;
import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpRequest;
import ru.yandex.market.tpl.core.external.routing.vrp.model.RouteNodeNode;
import ru.yandex.market.tpl.core.external.routing.vrp.model.TaskInfo;
import ru.yandex.market.tpl.core.external.routing.vrp.model.TaskInfoStatus;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.PUBLISH_ROUTING_BY_USER_SHIFTS_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED;

@RequiredArgsConstructor
public class AsyncRoutingTest extends TplAbstractTest {

    private static final String PROCESSING_ID = "processingId";

    private static final long ANOTHER_SORTING_CENTER_ID = 47819L;

    private final TestUserHelper testUserHelper;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderGenerateService orderGenerateService;

    private final SortingCenterService sortingCenterService;

    private final ShiftManager shiftManager;
    private final TplRoutingShiftManager tplRoutingShiftManager;
    private final RoutingLogDao routingLogDao;
    private final NamedParameterJdbcOperations jdbcTemplate;
    private final Clock clock;

    private final VrpClient vrpClient;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;
    private final RoutingRequestGroupRepository routingRequestGroupRepository;

    private final LocalDate date = LocalDate.parse("2023-11-20");
    private Shift shift;
    private User courier;

    @AfterEach
    void cleanUp() {
        reset(vrpClient);
    }

    @BeforeEach
    public void prepareShift() {


        when(clock.instant()).thenAnswer(invocation -> Clock.systemDefaultZone().instant());
        when(clock.getZone()).thenReturn(DateTimeUtil.DEFAULT_ZONE_ID);
        long sortingCenterId = ANOTHER_SORTING_CENTER_ID;

        courier = testUserHelper.findOrCreateUserForSc(sortingCenterId + 123L, date, sortingCenterId);

        List<Shift> shifts = shiftManager.assignShiftsForDate(date, sortingCenterId);
        shift = shiftManager.findOrThrow(date, sortingCenterId);

        configurationServiceAdapter.deleteValue(PUBLISH_ROUTING_BY_USER_SHIFTS_ENABLED);
        configurationServiceAdapter.deleteValue(SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED);
        sortingCenterPropertyService.deletePropertyFromSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.PUBLISH_ROUTING_BY_USER_SHIFTS_ENABLED);

        Order order = generateOrder();

        reset(vrpClient);
    }

    private Order generateOrder() {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.CREATED)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .build())
                .deliveryServiceId(sortingCenterService.findDsForSortingCenter(
                        shift.getSortingCenter().getId()).get(0).getId())
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryDate(date)
                .build());
    }

    @Test
    public void shouldCreateTaskForPerformRoutingRequest() {
        String routingRequestId = tplRoutingShiftManager.routeShiftGroupAsync(shift, RoutingMockType.MANUAL,
                RoutingProfileType.GROUP, 0L);

        Optional<RoutingRequest> routingRequestByRequestId =
                routingLogDao.findRoutingRequestByRequestId(routingRequestId);

        assertThat(routingRequestByRequestId).isPresent();

        dbQueueTestUtil.assertQueueHasSingleEvent(QueueType.PERFORM_ROUTING_REQUEST, routingRequestId);
    }

    @Test
    public void shouldPerformRoutingRequestFromQueue() {
        String routingRequestId = tplRoutingShiftManager.routeShiftGroupAsync(shift, RoutingMockType.MANUAL,
                RoutingProfileType.GROUP, 0L);

        shouldPerformAddMVRPTaskRequestWithProcessingId(PROCESSING_ID);

        List<Boolean> resultByProcessingId = jdbcTemplate.query("" +
                        "SELECT true as result FROM routing_log WHERE processing_id = :processing_id",
                Map.of("processing_id", PROCESSING_ID),
                (rs, rowNum) -> rs.getBoolean("result"));

        assertThat(resultByProcessingId).hasSize(1);
    }

    @Test
    public void shouldCreateTaskForCollectRoutingResult() {
        String routingRequestId = tplRoutingShiftManager.routeShiftGroupAsync(shift, RoutingMockType.MANUAL,
                RoutingProfileType.GROUP, 0L);

        shouldPerformAddMVRPTaskRequestWithProcessingId(PROCESSING_ID);

        dbQueueTestUtil.assertQueueHasSingleEvent(QueueType.COLLECT_ROUTING_RESULT, PROCESSING_ID);
    }

    @Test
    public void shouldPerformCollectRoutingResultRequestFromQueue() {
        String routingRequestId = tplRoutingShiftManager.routeShiftGroupAsync(shift, RoutingMockType.MANUAL,
                RoutingProfileType.GROUP, 0L);

        shouldPerformAddMVRPTaskRequestWithProcessingId(PROCESSING_ID);

        TaskInfo mockRoutingTaskInfo = new TaskInfo();
        mockRoutingTaskInfo.setId(PROCESSING_ID);

        when(vrpClient.getTaskResult(any(VrpClient.ApiType.class), eq(PROCESSING_ID)))
                .thenReturn(mockRoutingTaskInfo);

        dbQueueTestUtil.executeSingleQueueItem(QueueType.COLLECT_ROUTING_RESULT);

        verify(vrpClient, times(2)).getTaskResult(VrpClient.ApiType.SVRP, PROCESSING_ID);
    }

    @SneakyThrows
    @Test
    public void shouldSaveRoutingResultOnSuccess() {
        String routingRequestId = tplRoutingShiftManager.routeShiftGroupAsync(shift, RoutingMockType.MANUAL,
                RoutingProfileType.GROUP, 0L);

        shouldPerformAddMVRPTaskRequestWithProcessingId(PROCESSING_ID);
        assertThatPublishStatusByRoutingRequestId(routingRequestId, RoutingResultStatus.COLLECT_ROUTING_RESULT);

        TaskInfo taskInfo = ObjectMappers.TPL_DB_OBJECT_MAPPER.readValue(
                new String(Files.readAllBytes(new ClassPathResource("vrp/vrp_good_response.json").getFile().toPath())),
                TaskInfo.class);

        prepareRoutingRequest(taskInfo);
        // т.к. предыдущая строчка меняет статус - возвращаем в исходное состояние
        routingLogDao.updatePublishingStatus(PROCESSING_ID, RoutingResultStatus.COLLECT_ROUTING_RESULT);

        when(vrpClient.getTaskResult(VrpClient.ApiType.SVRP, PROCESSING_ID))
                .thenReturn(taskInfo);

        dbQueueTestUtil.assertQueueHasSize(QueueType.COLLECT_ROUTING_RESULT, 1);

        dbQueueTestUtil.executeSingleQueueItem(QueueType.COLLECT_ROUTING_RESULT);

        assertThatPublishStatusByRoutingRequestId(routingRequestId, RoutingResultStatus.PUBLISH_ROUTING_RESULT);

        verify(vrpClient, times(2)).getTaskResult(VrpClient.ApiType.SVRP, PROCESSING_ID);
        reset(vrpClient);

        Optional<RoutingResultWithShiftDate> resultO = routingLogDao.findResultByProcessingId(PROCESSING_ID);

        assertThat(resultO).isPresent();
        assertThat(resultO.get().getRoutingResult()).isNotNull();
    }

    private void assertThatPublishStatusByRoutingRequestId(
            String routingRequestId,
            RoutingResultStatus expectedStatus
    ) {
        RoutingLogRecord routingLogRecord =
                routingLogDao.findRoutingRequestRecordByRequestId(routingRequestId).orElseThrow();

        assertThat(routingLogRecord.getPublishStatus()).isEqualTo(expectedStatus);
    }

    @SneakyThrows
    @Test
    public void shouldReenqueueWhenAnotherMultiRequestIsInProgress() {
        tplRoutingShiftManager.routeShiftGroupAsyncMulti(
                shift,
                RoutingMockType.REAL,
                Set.of(
                        RoutingProfileType.GROUP, RoutingProfileType.GROUP_FALLBACK_1,
                        RoutingProfileType.GROUP_FALLBACK_2
                ),
                LocalTime.now(clock)
        );

        dbQueueTestUtil.executeSingleQueueItem(QueueType.QUEUE_ROUTING_REQUEST_GROUP);

        jdbcTemplate.update("update queue_task set process_time = now()", Map.of());

        shouldPerformAddMVRPTaskRequestWithProcessingId(PROCESSING_ID);
        shouldPerformAddMVRPTaskRequestWithProcessingId(PROCESSING_ID + 2);
        shouldPerformAddMVRPTaskRequestWithProcessingId(PROCESSING_ID + 3);

        TaskInfo taskInfo = ObjectMappers.TPL_DB_OBJECT_MAPPER.readValue(
                new String(Files.readAllBytes(new ClassPathResource("vrp/vrp_good_response.json").getFile().toPath())),
                TaskInfo.class);
        TaskInfoStatus status = new TaskInfoStatus();
        status.setCompleted(1L);
        taskInfo.setStatus(status);

        prepareRoutingRequest(taskInfo);

        // т.к. предыдущая строчка меняет статус - возвращаем в исходное состояние
        routingLogDao.updatePublishingStatus(PROCESSING_ID, RoutingResultStatus.COLLECT_ROUTING_RESULT);
        routingLogDao.updatePublishingStatus(PROCESSING_ID + 2, RoutingResultStatus.COLLECT_ROUTING_RESULT);
        routingLogDao.updatePublishingStatus(PROCESSING_ID + 3, RoutingResultStatus.COLLECT_ROUTING_RESULT);

        when(vrpClient.getTaskResult(eq(VrpClient.ApiType.SVRP), anyString()))
                .thenReturn(taskInfo);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.COLLECT_ROUTING_RESULT);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.COLLECT_ROUTING_RESULT);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.COLLECT_ROUTING_RESULT);
        reset(vrpClient);

        Optional<RoutingResultWithShiftDate> resultO = routingLogDao.findResultByProcessingId(PROCESSING_ID);

        assertThat(resultO).isPresent();
        assertThat(resultO.get().getRoutingResult()).isNotNull();

        tplRoutingShiftManager.routeShiftGroupAsyncMulti(
                shift,
                RoutingMockType.REAL,
                Set.of(RoutingProfileType.GROUP),
                LocalTime.now(clock)
        );

        dbQueueTestUtil.executeSingleQueueItem(QueueType.QUEUE_ROUTING_REQUEST_GROUP);

        assertThat(dbQueueTestUtil.isEmpty(QueueType.QUEUE_ROUTING_REQUEST_GROUP)).isFalse();

        dbQueueTestUtil.executeSingleQueueItem(QueueType.PUBLISH_ROUTING_RESULT);

        jdbcTemplate.update("update queue_task set process_time = now()", Map.of());
        routingLogDao.updatePublishingStatus(PROCESSING_ID, RoutingResultStatus.SUCCESS);

        dbQueueTestUtil.executeSingleQueueItem(QueueType.QUEUE_ROUTING_REQUEST_GROUP);

        assertThat(dbQueueTestUtil.isEmpty(QueueType.QUEUE_ROUTING_REQUEST_GROUP)).isTrue();
    }

    @SneakyThrows
    @Test
    public void shouldCorrectObtainRoutingResponseWhenTheSameProcesingId() {
        configurationServiceAdapter.insertValue(PUBLISH_ROUTING_BY_USER_SHIFTS_ENABLED, true);
        configurationServiceAdapter.insertValue(SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED, true);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.PUBLISH_ROUTING_BY_USER_SHIFTS_ENABLED, true);


        tplRoutingShiftManager.routeShiftGroupAsyncMulti(
                shift,
                RoutingMockType.REAL,
                Set.of(
                        RoutingProfileType.GROUP, RoutingProfileType.GROUP_FALLBACK_1
                ),
                LocalTime.now(clock)
        );

        dbQueueTestUtil.executeSingleQueueItem(QueueType.QUEUE_ROUTING_REQUEST_GROUP);

        jdbcTemplate.update("update queue_task set process_time = now()", Map.of());

        shouldPerformAddMVRPTaskRequestWithProcessingId(PROCESSING_ID);
        shouldPerformAddMVRPTaskRequestWithProcessingId(PROCESSING_ID);


        TaskInfo taskInfo = ObjectMappers.TPL_DB_OBJECT_MAPPER.readValue(
                new String(Files.readAllBytes(new ClassPathResource("vrp/vrp_good_response.json").getFile().toPath())),
                TaskInfo.class);
        TaskInfoStatus status = new TaskInfoStatus();
        status.setCompleted(1L);
        taskInfo.setStatus(status);
        //создание сущностей
        taskInfo.getResult().getOptions().date(date.plusDays(1).toString());
        taskInfo.getResult().getRoutes().forEach(route -> {
            route.setVehicleId(courier.getId());
            route.getRoute().forEach(node -> {
                if (node.getNode().getType() == RouteNodeNode.TypeEnum.LOCATION) {
                    node.getNode().getLocation().setId(String.valueOf(generateOrder().getId()));
                }
            });
        });

        prepareRoutingRequest(taskInfo);

        // т.к. предыдущая строчка меняет статус для всех записей с PROCESSING_ID
        // - возвращаем в исходное состояние
        routingLogDao.updatePublishingStatus(PROCESSING_ID, RoutingResultStatus.COLLECT_ROUTING_RESULT);

        when(vrpClient.getTaskResult(eq(VrpClient.ApiType.SVRP), anyString()))
                .thenReturn(taskInfo);
        dbQueueTestUtil.executeAllQueueItems(QueueType.COLLECT_ROUTING_RESULT);

        reset(vrpClient);

        dbQueueTestUtil.executeSingleQueueItem(QueueType.PUBLISH_ROUTING_RESULT);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.PUBLISH_USER_SHIFT);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.COLLECT_PUBLISHED_USER_SHIFTS);


        //then
        //Проверка что есть победитель и он опубликован
        var routingLogs = routingLogDao.findRecordsByShiftDateAndSortingCenterId(shift.getShiftDate(),
                shift.getSortingCenter().getId());

        assertThat(routingLogs.stream().map(RoutingLogRecord::getPublishStatus).collect(Collectors.toSet()))
                .containsOnly(RoutingResultStatus.SUCCESS, RoutingResultStatus.PUBLISH_ROUTING_RESULT);

        assertThat(routingLogs.stream().filter(log -> log.getPublishStatus() == RoutingResultStatus.SUCCESS).count())
                .isEqualTo(1);

        var winnerLog = routingLogs.stream().filter(log -> log.getPublishStatus() == RoutingResultStatus.SUCCESS).findFirst().orElseThrow();

        var winnerResult = routingLogDao.findResultWithShiftDateByRequestId(winnerLog.getRequestId()).orElseThrow();

        //проверка состава опубликованной смены
        var userShifts = userShiftRepository.findAllByShiftId(shift.getId());
        assertThat(userShifts).hasSize(1);
        transactionTemplate.execute(s -> {
            UserShift userShift = userShifts.iterator().next();
            assertThat(userShift.getUser().getId()).isEqualTo(courier.getId());
            assertThat(userShiftRepository.findByIdOrThrow(userShift.getId())
                    .streamRoutePoints().count()).isEqualTo(
                    taskInfo.getResult().getRoutes().get(0).getRoute().size());
            assertThat(userShift.getShift().getProcessingId())
                    .isEqualTo(winnerResult.getRoutingResult().getProcessingId());
            return null;
        });
    }

    private void shouldPerformAddMVRPTaskRequestWithProcessingId(String processingId) {
        TaskInfo mockRoutingTaskInfo = new TaskInfo();
        mockRoutingTaskInfo.setId(processingId);

        when(vrpClient.addMVRPTask(eq(VrpClient.ApiType.SVRP), any(MvrpRequest.class)))
                .thenReturn(mockRoutingTaskInfo);

        dbQueueTestUtil.executeSingleQueueItem(QueueType.PERFORM_ROUTING_REQUEST);
        verify(vrpClient).addMVRPTask(eq(VrpClient.ApiType.SVRP), any(MvrpRequest.class));
        reset(vrpClient);
    }

    private void prepareRoutingRequest(TaskInfo taskInfo) {
        List<String> taskIds = taskInfo.getResult().getRoutes().stream()
                .flatMap(it -> it.getRoute().stream())
                .filter(it -> it.getNode().getType() == RouteNodeNode.TypeEnum.LOCATION)
                .map(it -> it.getNode().getLocation().getId())
                .collect(Collectors.toList());

        var routingLogs = jdbcTemplate.query(
                "select rl.id, rl.request_id from routing_log rl", (rs, rn) -> Map.of(
                        "id", rs.getLong("id"),
                        "requestId", rs.getString("request_id")
                )
        );

        routingLogs.forEach(routingLog -> {
            var routingRequestOpt = routingLogDao.findRoutingRequestByRequestId(
                    (String) routingLog.get("requestId")
            );

            if (routingRequestOpt.isPresent()) {
                RoutingRequest routingRequest = routingRequestOpt.get();
                routingRequest.getItems().addAll(
                        taskIds.stream()
                                .map(it -> RoutingRequestItemTestBuilder.builder()
                                        .taskId(it)
                                        .type(RoutingRequestItemType.CLIENT)
                                        .build().get()
                                ).collect(Collectors.toList())
                );

                routingLogDao.fillRequestAndOrderCountData((Long) routingLog.get("id"), routingRequest);
            }
        });
    }

}

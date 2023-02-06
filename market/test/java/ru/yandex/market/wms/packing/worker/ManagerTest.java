package ru.yandex.market.wms.packing.worker;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.spring.dao.implementation.LotLocIdDao;
import ru.yandex.market.wms.common.spring.service.ClickHouseTaskActionLogConsumer;
import ru.yandex.market.wms.common.spring.service.EmptyToteService;
import ru.yandex.market.wms.common.spring.service.HostnameService;
import ru.yandex.market.wms.common.spring.service.SerialInventoryService;
import ru.yandex.market.wms.common.spring.service.balance.BalanceService;
import ru.yandex.market.wms.common.spring.service.time.WarehouseDateTimeService;
import ru.yandex.market.wms.packing.MockTaskConsumer;
import ru.yandex.market.wms.packing.dao.PackingTaskDao;
import ru.yandex.market.wms.packing.dto.PackingTaskDto;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.exception.TableOccupiedException;
import ru.yandex.market.wms.packing.logging.PackingAlgoLogger;
import ru.yandex.market.wms.packing.pojo.PackingTable;
import ru.yandex.market.wms.packing.pojo.PackingTask;
import ru.yandex.market.wms.packing.pojo.SortingCell;
import ru.yandex.market.wms.packing.pojo.Ticket;
import ru.yandex.market.wms.packing.service.CartonRecommendationService;
import ru.yandex.market.wms.packing.service.MetricService;
import ru.yandex.market.wms.packing.service.OrderMaxParcelDimensionsService;
import ru.yandex.market.wms.packing.service.PackingNotificationService;
import ru.yandex.market.wms.packing.service.PackingTableService;
import ru.yandex.market.wms.packing.service.PackingTaskService;
import ru.yandex.market.wms.packing.service.ParcelIdService;
import ru.yandex.market.wms.packing.service.PickByLightService;
import ru.yandex.market.wms.packing.service.PromoTaskService;
import ru.yandex.market.wms.packing.service.SettingsService;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.wms.common.utils.CollectionUtils.asSet;

class ManagerTest {
    private static final String SOURCE_LOC = "SRC";
    private static final int TASK_CONSUMERS_COUNT = 10;
    private static final int TASK_REQUESTS_COUNT_PER_CONSUMER = 1000;
    private static final int TOTAL_IDLE_TASK_REQUESTS_COUNT = 100;

    private MockPackingTaskService taskService;
    private Manager manager;
    private SettingsService settingsService;

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(TASK_CONSUMERS_COUNT);

    @BeforeEach
    void init() {
        taskService = new MockPackingTaskService();
        settingsService = mock(SettingsService.class);
        manager = new Manager(
                taskService,
                mock(PackingTableService.class),
                settingsService,
                mock(WarehouseDateTimeService.class),
                mock(ClickHouseTaskActionLogConsumer.class),
                mock(MetricService.class),
                mock(BalanceService.class),
                mock(EmptyToteService.class),
                mock(PromoTaskService.class),
                mock(LotLocIdDao.class),
                mock(PickByLightService.class),
                mock(SecurityDataProvider.class),
                mock(PackingAlgoLogger.class)
        );
        manager.loadTickets();
    }

    @AfterAll
    static void cleanup() {
        EXECUTOR_SERVICE.shutdown();
    }

    /**
     * Проверка асинхронного получения задач
     */
    @Test
    void requestNewTask() throws Exception {
        List<Ticket> initialTickets = taskService.getTickets();
        ConcurrentLinkedQueue<PackingTaskDto> receivedTasks = new ConcurrentLinkedQueue<>();
        AtomicInteger idleCount = new AtomicInteger();

        Mockito.when(settingsService.getTaskRouterTablesRegexp())
                .thenReturn(".+");

        List<Callable<Integer>> callables = Stream.iterate(1, i -> i + 1)
                .map(id -> createTaskConsumerCallable(id, receivedTasks, idleCount))
                .limit(TASK_CONSUMERS_COUNT)
                .collect(Collectors.toList());

        EXECUTOR_SERVICE.invokeAll(callables, 30, TimeUnit.SECONDS);

        Comparator<Ticket> comparator = Comparator.comparing(Ticket::getOrderKey);

        initialTickets.sort(comparator);
        List<Ticket> receivedTickets = receivedTasks.stream()
                .map(PackingTaskDto::getTicket)
                .sorted(comparator)
                .toList();

        assertThat(receivedTickets).hasSameSizeAs(initialTickets);

        for (int i = 0; i < initialTickets.size(); i++) {
            assertThat(receivedTickets.get(i)).usingRecursiveComparison()
                    .ignoringFields("ticketId")
                    .isEqualTo(initialTickets.get(i));
        }

        assertThat(idleCount.get()).isEqualTo(TOTAL_IDLE_TASK_REQUESTS_COUNT);
    }

    private Callable<Integer> createTaskConsumerCallable(int id,
                                                         Queue<PackingTaskDto> receivedTasks,
                                                         AtomicInteger idleCount) {
        return () -> {
            MockTaskConsumer taskConsumer = createConsumer(id);
            manager.register(taskConsumer);
            for (int i = 0; i < TASK_REQUESTS_COUNT_PER_CONSUMER; i++) {
                manager.requestTask(taskConsumer.getUser(), false);
                if (taskConsumer.getTask() != null) {
                    receivedTasks.add(taskConsumer.getTask());
                    Thread.sleep(1 + ThreadLocalRandom.current().nextInt(5));
                    manager.finishTask(taskConsumer.getUser());
                } else if (Boolean.TRUE.equals(taskConsumer.isIdle())) {
                    idleCount.incrementAndGet();
                } else {
                    throw new AssertionError("Unexpected state for consumer: " + taskConsumer);
                }
            }
            return id;
        };
    }

    private MockTaskConsumer createConsumer(int id) {
        return new MockTaskConsumer(
                PackingTable.builder().loc("pack-" + id).sourceLocs(asSet(SOURCE_LOC)).build(),
                "user-" + id
        );
    }

    private static class MockPackingTaskService extends PackingTaskService {
        ConcurrentLinkedQueue<Ticket> tickets = createTickets();

        MockPackingTaskService() {
            super(mock(PackingTaskDao.class),
                    mock(PromoTaskService.class),
                    mock(SettingsService.class), null, new HostnameService(),
                    mock(PackingNotificationService.class),
                    mock(OrderMaxParcelDimensionsService.class),
                    mock(SerialInventoryService.class),
                    mock(SecurityDataProvider.class),
                    mock(ParcelIdService.class),
                    mock(PackingTaskDao.class),
                    mock(CartonRecommendationService.class)
            );
        }

        @Override
        public List<Ticket> getTickets() {
            return new ArrayList<>(tickets);
        }

        @Override
        public PackingTask getPackingTask(Ticket ticket) {
            return new PackingTask(ticket, List.of(), List.of(), false, List.of());
        }

        private ConcurrentLinkedQueue<Ticket> createTickets() {
            ConcurrentLinkedQueue<Ticket> ticketConcurrentLinkedQueue = new ConcurrentLinkedQueue<>();
            int ticketsCount = TASK_CONSUMERS_COUNT * TASK_REQUESTS_COUNT_PER_CONSUMER - TOTAL_IDLE_TASK_REQUESTS_COUNT;
            for (int i = 0; i < ticketsCount; i++) {
                Ticket ticket = Ticket.builder()
                        .sourceLoc(SOURCE_LOC)
                        .type(TicketType.SORTABLE)
                        .sortingCells(asSet(SortingCell.builder()
                                .loc(SOURCE_LOC)
                                .cell("cell-" + i)
                                .id("id-" + i)
                                .build()))
                        .orderKey("order-" + i)
                        .shippingDeadline(LocalDateTime.now())
                        .editDate(Instant.now())
                        .maxOrderStatus(OrderStatus.PICKED_COMPLETE)
                        .build();
                ticketConcurrentLinkedQueue.add(ticket);
            }
            return ticketConcurrentLinkedQueue;
        }

        @Override
        public void scheduleUserTasksForRemoval(String user) {
        }

        @Override
        public void setIdle(String assignee, String table) throws TableOccupiedException {
        }

        @Override
        public Optional<Ticket> restoreAssignedTicket(String assignee, String table) {
            return Optional.empty();
        }
    }

}

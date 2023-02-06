package ru.yandex.market.sc.tms.domain.sqs;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.sc.ScSegmentStatusesEvent;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.jdbc.OrderFFStatusJdbcRepository;
import ru.yandex.market.sc.core.domain.order.model.CreateReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderIdResponse;
import ru.yandex.market.sc.core.domain.order.model.OrderReturnType;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderFFStatusHistoryItemRepository;
import ru.yandex.market.sc.core.domain.place.jdbc.PlaceJdbcRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sqs.SendCargoUpdateStatusHistoryToSqsService;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.LocationDto;
import ru.yandex.market.sc.internal.model.WarehouseDto;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

@Deprecated
@EmbeddedDbTmsTest
public class SendCargoUpdateStatusHistoryToSqsServiceTest {

    private static final String QUEUE_NAME = "sc_out";
    private static final String SOURCE = "sc";

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ScOrderFFStatusHistoryItemRepository repository;

    @Autowired
    TestFactory testFactory;

    @MockBean
    JmsTemplate jmsTemplate;

    @SpyBean
    OrderFFStatusJdbcRepository orderFFStatusJdbcRepository;

    @Autowired
    SqsQueueProperties sqsQueueProperties;

    @Autowired
    AcceptService acceptService;

    SendCargoUpdateStatusHistoryToSqsService service;

    @Autowired
    PlaceJdbcRepository placeJdbcRepository;

    @Autowired
    OrderCommandService orderCommandService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Clock clock;

    @AfterEach
    void tearDown() {
        Mockito.clearInvocations(jmsTemplate);
    }

    @BeforeEach
    void setUp() {
        configurationService.mergeValue(ConfigurationProperties.SEND_STATUS_CARGO_EVENTS_TO_SQS_ENABLED, true);
        configurationService.mergeValue(ConfigurationProperties.SEGMENT_FF_STATUS_HISTORY_BATCH_SIZE, 2);
        configurationService.mergeValue(ConfigurationProperties.LAST_SENT_SEGMENT_FF_STATUS_HISTORY_ITEM_ID, 0);

        Mockito.when(sqsQueueProperties.getOutQueue()).thenReturn(QUEUE_NAME);
        Mockito.when(sqsQueueProperties.getSource()).thenReturn(SOURCE);

        service = new SendCargoUpdateStatusHistoryToSqsService(
                orderFFStatusJdbcRepository,
                configurationService,
                configurationService,
                jmsTemplate,
                sqsQueueProperties,
                placeJdbcRepository
        );

        var sortingCenter = testFactory.storedSortingCenter(3, "parent3_3");
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
        testFactory.storedFakeReturnDeliveryService();
        // пока тестируем только на клиентских возвратах тк пока через них сохраняется segmentUid
        testFactory.accept(testFactory.getOrder(createClientReturn(sortingCenter, "ex-id-0", "segmentUid-0").getId()));
        testFactory.accept(testFactory.getOrder(createClientReturn(sortingCenter, "ex-id-2", "1234todo_segment_uid").getId()));

        var orderIdResponse = createClientReturn(testFactory.storedSortingCenter(), "ex-id-1", "segmentUid-1");
        var scOrder = testFactory.getOrder(orderIdResponse.getId());
        testFactory.accept(scOrder);
        scOrder = testFactory.getOrder(orderIdResponse.getId());
        testFactory.sortOrder(scOrder);
        scOrder = testFactory.getOrder(orderIdResponse.getId());

        testFactory.shipOrderRoute(scOrder);
        Mockito.verify(orderFFStatusJdbcRepository).createOrderFFStatusHistoryItems(any(), any());

        Mockito.clearInvocations(jmsTemplate);
    }

    @Test
    void sendOnlyNewItemsInBatches() {
        configurationService.mergeValue(ConfigurationProperties.SEGMENT_FF_STATUS_HISTORY_BATCH_SIZE, 2);
        configurationService.mergeValue(ConfigurationProperties.LAST_SENT_SEGMENT_FF_STATUS_HISTORY_ITEM_ID, 1);
        service.sendSegmentFfStatusHistoryToSqs();

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);

        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(eq(QUEUE_NAME), argumentCaptor.capture());
        Mockito.verify(orderFFStatusJdbcRepository).findLastOrderFfStatusHistoryItemId();
        Mockito.verify(orderFFStatusJdbcRepository).findUniqueSortedSegmentUids(1L, 8L, 0);

        Mockito.verify(orderFFStatusJdbcRepository).findSegmentFfStatusHistoryBetweenItemIds(argThat(arg -> {
            assertThat(arg).hasSize(2);
            return true;
        }), eq(8L), eq(0));


        argumentCaptor.getAllValues().forEach(event -> {
                    assertThat(event.getSource()).isEqualTo(SOURCE);
                    assertThat(event.getPayload()).isInstanceOf(ScSegmentStatusesEvent.class);
                }
        );
    }

    @Test
    void doNotSendIfFlagIsOff() {
        configurationService.mergeValue(ConfigurationProperties.SEND_STATUS_CARGO_EVENTS_TO_SQS_ENABLED, false);
        service.sendSegmentFfStatusHistoryToSqs();

        Mockito.verifyNoMoreInteractions(jmsTemplate);
    }

    @Test
    void sendAllInOneBatch() {
        configurationService.mergeValue(ConfigurationProperties.SEGMENT_FF_STATUS_HISTORY_BATCH_SIZE, 5);
        service.sendSegmentFfStatusHistoryToSqs();

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);

        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(eq(QUEUE_NAME), argumentCaptor.capture());
        Mockito.verify(orderFFStatusJdbcRepository).findLastOrderFfStatusHistoryItemId();
        Mockito.verify(orderFFStatusJdbcRepository).findUniqueSortedSegmentUids(0L, 8L, 0);
        Mockito.verify(orderFFStatusJdbcRepository).findSegmentFfStatusHistoryBetweenItemIds(
                argThat(arg -> {
                    assertThat(arg).hasSize(2);
                    return true;
                }),
                eq(8L), eq(0));

        Mockito.verifyNoMoreInteractions(orderFFStatusJdbcRepository);

        argumentCaptor.getAllValues().forEach(event -> {
                    assertThat(event.getSource()).isEqualTo(SOURCE);
                    assertThat(event.getPayload()).isInstanceOf(ScSegmentStatusesEvent.class);
                }
        );
    }

    @Test
    void doNotSendYoungItems() {
        configurationService.mergeValue(ConfigurationProperties.SEGMENT_FF_STATUS_HISTORY_BATCH_SIZE, 5);
        configurationService.mergeValue(ConfigurationProperties.ORDER_FF_STATUS_HISTORY_MIN_ITEM_AGE_SEC, 300);
        service.sendSegmentFfStatusHistoryToSqs();

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);

        Mockito.verify(jmsTemplate, Mockito.times(0)).convertAndSend(eq(QUEUE_NAME), argumentCaptor.capture());
        Mockito.verify(orderFFStatusJdbcRepository).findLastOrderFfStatusHistoryItemId();
        Mockito.verify(orderFFStatusJdbcRepository).findUniqueSortedSegmentUids(0L, 8L, 300);

        Mockito.verifyNoMoreInteractions(orderFFStatusJdbcRepository);
    }


    @Test
    void sendAllSmallSelectBatchSize() {
        configurationService.mergeValue(ConfigurationProperties.SEGMENT_FF_STATUS_HISTORY_BATCH_SIZE, 1);
        configurationService.mergeValue(ConfigurationProperties.SEGMENT_FF_STATUS_SELECT_BATCH_SIZE, 3);

        service.sendSegmentFfStatusHistoryToSqs();

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);

        Mockito.verify(jmsTemplate, Mockito.times(2)).convertAndSend(eq(QUEUE_NAME), argumentCaptor.capture());
        Mockito.verify(orderFFStatusJdbcRepository).findLastOrderFfStatusHistoryItemId();
        Mockito.verify(orderFFStatusJdbcRepository).findUniqueSortedSegmentUids(0L, 8L, 0);
        Mockito.verify(orderFFStatusJdbcRepository, Mockito.times(2)).findSegmentFfStatusHistoryBetweenItemIds(argThat(arg -> {
                    assertThat(arg).hasSize(1);
                    return true;
                }), eq(8L), eq(0));
        Mockito.verify(orderFFStatusJdbcRepository, Mockito.times(2)).findSegmentFfStatusHistoryBetweenItemIds(argThat(arg -> {
                    assertThat(arg).hasSize(1);
                    return true;
                }),
                 eq(8L), eq(0));
        Mockito.verifyNoMoreInteractions(orderFFStatusJdbcRepository);

        argumentCaptor.getAllValues().forEach(event -> {
                    assertThat(event.getSource()).isEqualTo(SOURCE);
                    assertThat(event.getPayload()).isInstanceOf(ScSegmentStatusesEvent.class);
                }
        );
    }

    @Test
    void doNotSendIfNothingNewAppeared() {
        configurationService.mergeValue(ConfigurationProperties.LAST_SENT_SEGMENT_FF_STATUS_HISTORY_ITEM_ID, 8L);
        service.sendSegmentFfStatusHistoryToSqs();

        Mockito.verifyNoMoreInteractions(jmsTemplate);
    }


    private final LocationDto MOCK_WAREHOUSE_LOCATION = LocationDto.builder()
            .country("Россия")
            .region("Москва и Московская область")
            .locality("Котельники")
            .build();

    private OrderIdResponse createClientReturn(SortingCenter sortingCenter, String barcode, String segmentUid) {
        var cargoUnitId = "1aaaa123";
        var fromWarehouse = WarehouseDto.builder()
                .type(WarehouseType.SHOP.name())
                .yandexId("123123")
                .logisticPointId("123123")
                .incorporation("ООО фром мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .shopId("123")
                .build();
        var returnWarehouse = WarehouseDto.builder()
                .type(WarehouseType.SHOP.name())
                .yandexId("222222")
                .logisticPointId("222222")
                .incorporation("ООО ретурн мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .shopId("333")
                .build();
        return orderCommandService.createReturn(CreateReturnRequest.builder()
                .sortingCenter(sortingCenter)
                .orderBarcode(cargoUnitId + "_" + segmentUid)
                .returnDate(LocalDate.now())
                .returnWarehouse(returnWarehouse)
                .fromWarehouse(fromWarehouse)
                .segmentUuid(segmentUid)
                .cargoUnitId(cargoUnitId)
                .timeIn(Instant.now(clock))
                .timeOut(Instant.now(clock))
                .orderReturnType(OrderReturnType.CLIENT_RETURN)
                .assessedCost(new BigDecimal(10_000))
                .build()
        , testFactory.getOrCreateAnyUser(sortingCenter));
    }
}

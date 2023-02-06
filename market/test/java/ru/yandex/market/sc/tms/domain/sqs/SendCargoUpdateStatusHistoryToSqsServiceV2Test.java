package ru.yandex.market.sc.tms.domain.sqs;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.sc.ScCargoUnit;
import ru.yandex.market.logistics.les.sc.ScCargoUnitStatus;
import ru.yandex.market.logistics.les.sc.ScSegmentStatusesEvent;
import ru.yandex.market.logistics.les.sc.StatusHistory;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.jdbc.OrderFFStatusJdbcRepository;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderFFStatusHistoryItemRepository;
import ru.yandex.market.sc.core.domain.place.jdbc.PlaceJdbcRepository;
import ru.yandex.market.sc.core.domain.place.model.CargoSegment;
import ru.yandex.market.sc.core.domain.place.model.SendStatusesSegmentDto;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sqs.SendCargoUpdateStatusHistoryToSqsService;
import ru.yandex.market.sc.core.domain.user.UserCommandService;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTmsTest
public class SendCargoUpdateStatusHistoryToSqsServiceV2Test {

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
    OrderCommandService orderCommandService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PlaceJdbcRepository placeJdbcRepository;

    @Autowired
    UserCommandService userCommandService;
    @Autowired
    Clock clock;

    @Autowired
    PlaceRepository placeRepository;

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

        Mockito.clearInvocations(jmsTemplate);
    }

    @Test
    @DisplayName("success нашли историю статусу после изменения статусов грузомест")
    void successFoundStatusHistoryForSend() {
        var sc1 = testFactory.storedSortingCenter(10);
        var whRet1 = testFactory.storedWarehouse("sc1-wh-return1", WarehouseType.SORTING_CENTER);
        var user1 = userCommandService.findOrCreateRobotUser(sc1);

        var o1Sc1 = testFactory.createForToday(order(sc1).places("o1Sc1-p1").sortingCenter(sc1).build()).cancel();
        var o1p1SegmentUid = "segment-uuid-o1Sc1p1";
        testFactory.updateSegment(o1Sc1.get().getId(), whRet1,
                o1p1SegmentUid, "cargo-unit-id-o1Sc1p1", user1);
        var p1Sc1 = o1Sc1.getPlace();
        var p1Accepted = testFactory.acceptPlace(p1Sc1).getPlace(p1Sc1.getId());
        testFactory.sortPlace(p1Accepted);

        var sc2 = testFactory.storedSortingCenter(12);
        var whRet2 = testFactory.storedWarehouse("sc2-wh-return2", WarehouseType.SORTING_CENTER);
        var user2 = userCommandService.findOrCreateRobotUser(sc2);

        var o1Sc2 = testFactory.createForToday(order(sc2).places("o1Sc2-p1").sortingCenter(sc2).build()).cancel();
        var o1Sc2p1SegmentUid = "segment-uuid-o1Sc2p1";
        testFactory.updateSegment(o1Sc2.get().getId(), whRet2,
                o1Sc2p1SegmentUid, "cargo-unit-id-o1Sc2p1", user2);
        var p1Sc2 = o1Sc2.getPlace();
        testFactory.acceptPlace(p1Sc2).getPlace(p1Sc2.getId());


        var toPlaceHistoryItemId = placeJdbcRepository.findLastPlaceHistoryItemId();
        var result = service.findPlaceStatusHistories(0L, toPlaceHistoryItemId);
        var statusHistories0 = filterHistoryBySegment(result, o1p1SegmentUid);
        assertThat(statusHistories0.getStatusHistory().stream().map(StatusHistory::getStatus).toList())
                .hasSameElementsAs(List.of(ScCargoUnitStatus.SORTED_RETURN, ScCargoUnitStatus.AWAITING_RETURN,
                        ScCargoUnitStatus.ACCEPTED_RETURN));

        var statusHistories1 = filterHistoryBySegment(result, o1Sc2p1SegmentUid);
        assertThat(statusHistories1.getStatusHistory().
                stream().map(StatusHistory::getStatus).toList())
                .hasSameElementsAs(List.of(ScCargoUnitStatus.AWAITING_RETURN,
                        ScCargoUnitStatus.ACCEPTED_RETURN));
    }

    @Test
    @DisplayName("success поиск и отправка сегментов, обновление офсета истории")
    void successSendEvents() {
        configurationService.mergeValue(ConfigurationProperties.LAST_SENT_SEGMENT_PLACE_STATUS_HISTORY_ITEM_ID, 0);
        configurationService.mergeValue(ConfigurationProperties.SEND_STATUS_CARGO_EVENTS_TO_SQS_ENABLED_V2, "true");
        var sc1 = testFactory.storedSortingCenter(10);
        var whRet1 = testFactory.storedWarehouse("sc1-wh-return1", WarehouseType.SORTING_CENTER);
        var user1 = userCommandService.findOrCreateRobotUser(sc1);

        var o1Sc1 = testFactory.createForToday(order(sc1).places("o1Sc1-p1").sortingCenter(sc1).build()).cancel();
        var o1p1SegmentUid = "segment-uuid-o1Sc1p1";
        testFactory.updateSegment(o1Sc1.get().getId(), whRet1,
                o1p1SegmentUid, "cargo-unit-id-o1Sc1p1", user1);
        var p1Sc1 = o1Sc1.getPlace();
        var p1Accepted = testFactory.acceptPlace(p1Sc1).getPlace(p1Sc1.getId());
        testFactory.sortPlace(p1Accepted);

        var beforeHistoryItemId =
                configurationService.getValueAsLong(ConfigurationProperties.LAST_SENT_SEGMENT_PLACE_STATUS_HISTORY_ITEM_ID).get();
        service.findAndSendLastStatuses();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(anyString(), any(Event.class));

        var afterHistoryItemId =
                configurationService.getValueAsLong(ConfigurationProperties.LAST_SENT_SEGMENT_PLACE_STATUS_HISTORY_ITEM_ID).get();

        assertThat(afterHistoryItemId).isGreaterThan(beforeHistoryItemId);
        assertThat(afterHistoryItemId).isEqualTo(placeJdbcRepository.findLastPlaceHistoryItemId());
    }

    @Test
    @DisplayName("success не нашли статусы для отправки, но все равно обновили офсет")
    void successUpdateOffsetWithoutSendEvents() {
        configurationService.mergeValue(ConfigurationProperties.SEND_STATUS_CARGO_EVENTS_TO_SQS_ENABLED_V2, "true");
        configurationService.mergeValue(ConfigurationProperties.LAST_SENT_SEGMENT_PLACE_STATUS_HISTORY_ITEM_ID, 0);
        var sc1 = testFactory.storedSortingCenter(10);
        var o1Sc1 = testFactory.createForToday(order(sc1).places("o1Sc1-p1").sortingCenter(sc1).build()).cancel();
        var p1Sc1 = o1Sc1.getPlace();
        var p1Accepted = testFactory.acceptPlace(p1Sc1).getPlace(p1Sc1.getId());
        testFactory.sortPlace(p1Accepted);
        var beforeHistoryItemId =
                configurationService.getValueAsLong(ConfigurationProperties.LAST_SENT_SEGMENT_PLACE_STATUS_HISTORY_ITEM_ID).get();

        service.findAndSendLastStatuses();

        var afterHistoryItemId =
                configurationService.getValueAsLong(ConfigurationProperties.LAST_SENT_SEGMENT_PLACE_STATUS_HISTORY_ITEM_ID).get();

        Mockito.verify(jmsTemplate, Mockito.times(0)).convertAndSend(anyString(), any(Event.class));
        assertThat(afterHistoryItemId).isGreaterThan(beforeHistoryItemId);
        assertThat(afterHistoryItemId).isEqualTo(placeJdbcRepository.findLastPlaceHistoryItemId());
    }

    @Test
    @DisplayName("success отправка истории статусов в LES")
    void successFindAndSendPlaceStatusHistory() {
        var sc1 = testFactory.storedSortingCenter(10);
        var whRet1 = testFactory.storedWarehouse("sc1-wh-return1", WarehouseType.SORTING_CENTER);
        var user1 = userCommandService.findOrCreateRobotUser(sc1);

        var o1Sc1 = testFactory.createForToday(order(sc1).places("o1Sc1-p1").sortingCenter(sc1).build()).cancel();
        var o1p1SegmentUid = "segment-uuid-o1Sc1p1";
        var o1p1CargoUnitId = "cargo-unit-id-o1Sc1p1";
        testFactory.updateSegment(o1Sc1.get().getId(), whRet1,
                o1p1SegmentUid, o1p1CargoUnitId, user1);
        var p1Sc1 = o1Sc1.getPlace();
        var p1Accepted = testFactory.acceptPlace(p1Sc1).getPlace(p1Sc1.getId());
        testFactory.sortPlace(p1Accepted);

        var sc2 = testFactory.storedSortingCenter(12);
        var whRet2 = testFactory.storedWarehouse("sc2-wh-return2", WarehouseType.SORTING_CENTER);
        var user2 = userCommandService.findOrCreateRobotUser(sc2);

        var o1Sc2 = testFactory.createForToday(order(sc2).places("o1Sc2-p1").sortingCenter(sc2).build()).cancel();
        var o1Sc2p1SegmentUid = "segment-uuid-o1Sc2p1";
        testFactory.updateSegment(o1Sc2.get().getId(), whRet2,
                o1Sc2p1SegmentUid, "cargo-unit-id-o1Sc2p1", user2);
        var p1Sc2 = o1Sc2.getPlace();
        testFactory.acceptPlace(p1Sc2).getPlace(p1Sc2.getId());

        var sendStatusesSegmentDto = new SendStatusesSegmentDto(List.of(
                new CargoSegment(o1p1SegmentUid, o1p1CargoUnitId),
                new CargoSegment(o1Sc2p1SegmentUid, null)
        ));
        service.findAndSendPlaceStatusHistory(sendStatusesSegmentDto);
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.eq("sc_out"), (Event) Mockito.argThat(arg -> {
            var event = (Event) arg;
            var statuses = ((ScSegmentStatusesEvent) event.getPayload()).getCargoUnitStatuses();
            var map = statuses.stream()
                    .collect(Collectors.groupingBy(ScCargoUnit::getSegmentUniqueId,
                            Collectors.mapping(unit -> convert(unit.getStatusHistory()), Collectors.toList())));
            if (map.size() != 2) {
                return false;
            }

            return map.get("segment-uuid-o1Sc1p1").get(0).equals(
                    List.of(
                    ScCargoUnitStatus.AWAITING_RETURN,
                    ScCargoUnitStatus.ACCEPTED_RETURN,
                    ScCargoUnitStatus.SORTED_RETURN
            )) && map.get("segment-uuid-o1Sc2p1").get(0).equals(
                    List.of(
                    ScCargoUnitStatus.AWAITING_RETURN,
                    ScCargoUnitStatus.ACCEPTED_RETURN
            ));
        }));
    }

    private List<ScCargoUnitStatus> convert(List<StatusHistory> statusHistories) {
        return statusHistories.stream()
                .sorted(Comparator.comparing(StatusHistory::getTimestamp))
                .map(StatusHistory::getStatus)
                .collect(Collectors.toList());
    }

    private ScCargoUnit filterHistoryBySegment(List<ScCargoUnit> histories, String segmentUid) {
        var result = histories.stream()
                .filter(h -> Objects.equals(h.getSegmentUniqueId(), segmentUid))
                .toList();
        assertThat(result).hasSize(1);
        return result.get(0);
    }

}

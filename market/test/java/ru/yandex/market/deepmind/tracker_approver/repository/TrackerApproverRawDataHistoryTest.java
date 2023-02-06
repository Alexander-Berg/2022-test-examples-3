package ru.yandex.market.deepmind.tracker_approver.repository;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.deepmind.tracker_approver.BaseTrackerApproverTest;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyMeta;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketChangeType;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawData;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawDataHistory;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.utils.JsonWrapper;

public class TrackerApproverRawDataHistoryTest extends BaseTrackerApproverTest {

    protected static final String TICKET = "TEST-1";
    protected static final String TYPE = "type";
    protected static final MyKey KEY_1 = new MyKey(1, "a1");
    protected static final MyKey KEY_2 = new MyKey(2, "a2");
    protected static final MyKey KEY_3 = new MyKey(3, "a3");
    protected static final MyKey KEY_4 = new MyKey(4, "a4");
    protected static final MyKey KEY_5 = new MyKey(5, "a5");

    protected static final String META_1 = "{\"statusAfter\": null, \"statusBefore\": \"PENDING\"}";
    protected static final String META_2 = "{\"statusAfter\": \"INACTIVE\", \"statusBefore\": \"PENDING\"}";


    @Autowired
    protected TrackerApproverDataHistoryRepository dataHistoryRepository;

    @Test
    public void insertData() {
        ticketRepository.save(rawStatus(TYPE, TICKET, TicketState.NEW, new MyMeta("my meta value")));

        TrackerApproverRawData rawData = rawData(KEY_1, TYPE, TICKET)
            .setMeta(JsonWrapper.fromJson(objectMapper, META_1));
        dataRepository.save(rawData);

        var savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).hasSize(1);

        var actualHistory = dataHistoryRepository.findAllByTicketOrderedByModifiedTs(TICKET);
        Assertions.assertThat(actualHistory).hasSize(1);
        Assertions.assertThat(actualHistory)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_1, META_1)
            );
    }

    @Test
    public void updateData() {
        ticketRepository.save(rawStatus(TYPE, TICKET, TicketState.NEW, new MyMeta("my meta value")));

        TrackerApproverRawData rawData = rawData(KEY_1, TYPE, TICKET)
            .setMeta(JsonWrapper.fromJson(objectMapper, META_1));
        dataRepository.save(rawData);

        var savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).hasSize(1);
        savedData.get(0).setMeta(JsonWrapper.fromJson(objectMapper, META_2));
        dataRepository.save(savedData);

        savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).hasSize(1);

        var actualHistory = dataHistoryRepository.findAllByTicketOrderedByModifiedTs(TICKET);
        Assertions.assertThat(actualHistory).hasSize(2);
        var historyItem = actualHistory.get(1);
        Assertions.assertThat(historyItem).isNotNull();
        Assertions.assertThat(actualHistory)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_1, META_1),
                createData(TicketChangeType.UPDATE, TicketState.NEW, KEY_1, META_2)
            );
    }

    @Test
    public void deleteData() {
        ticketRepository.save(rawStatus(TYPE, TICKET, TicketState.NEW, new MyMeta("my meta value")));

        TrackerApproverRawData rawData = rawData(KEY_1, TYPE, TICKET)
            .setMeta(JsonWrapper.fromJson(objectMapper, META_1));
        dataRepository.save(rawData);

        var savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).hasSize(1);
        savedData.get(0).setMeta(JsonWrapper.fromJson(objectMapper, META_2));
        dataRepository.save(savedData);
        dataRepository.deleteByTicket(TICKET);

        savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).isEmpty();

        var actualHistory = dataHistoryRepository.findAllByTicketOrderedByModifiedTs(TICKET);
        Assertions.assertThat(actualHistory).hasSize(3);
        Assertions.assertThat(actualHistory)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_1, META_1),
                createData(TicketChangeType.UPDATE, TicketState.NEW, KEY_1, META_2),
                createData(TicketChangeType.DELETE, TicketState.NEW, KEY_1, META_2)
            );
    }

    @Test
    public void insertMultipleData() {
        ticketRepository.save(rawStatus(TYPE, TICKET, TicketState.NEW, new MyMeta("my meta value")));
        List<TrackerApproverRawData> rawDataList = List.of(
            rawData(KEY_1, TYPE, TICKET),
            rawData(KEY_2, TYPE, TICKET),
            rawData(KEY_3, TYPE, TICKET),
            rawData(KEY_4, TYPE, TICKET),
            rawData(KEY_5, TYPE, TICKET)
        );
        rawDataList.stream().forEach(data -> data.setMeta(JsonWrapper.fromJson(objectMapper, META_1)));
        dataRepository.save(rawDataList);

        var savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).hasSize(rawDataList.size());

        var actualHistory = dataHistoryRepository.findAllByTicketOrderedByModifiedTs(TICKET);
        Assertions.assertThat(actualHistory).hasSize(rawDataList.size());
        Assertions.assertThat(actualHistory)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_1, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_2, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_3, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_4, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_5, META_1)
            );
    }

    @Test
    public void updateMultipleData() {
        ticketRepository.save(rawStatus(TYPE, TICKET, TicketState.NEW, new MyMeta("my meta value")));
        List<TrackerApproverRawData> rawDataList = List.of(
            rawData(KEY_1, TYPE, TICKET),
            rawData(KEY_2, TYPE, TICKET),
            rawData(KEY_3, TYPE, TICKET),
            rawData(KEY_4, TYPE, TICKET),
            rawData(KEY_5, TYPE, TICKET)
        );
        rawDataList.stream().forEach(data -> data.setMeta(JsonWrapper.fromJson(objectMapper, META_1)));
        dataRepository.save(rawDataList);

        var savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).hasSize(rawDataList.size());

        var ticket = ticketRepository.findByTicket(TICKET);
        ticket.setState(TicketState.ENRICHED);
        ticketRepository.save(ticket);

        savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).hasSize(rawDataList.size());

        savedData.stream().forEach(data -> data.setMeta(JsonWrapper.fromJson(objectMapper, META_2)));
        dataRepository.save(savedData);

        var actualHistory = dataHistoryRepository.findAllByTicketOrderedByModifiedTs(TICKET);
        Assertions.assertThat(actualHistory).hasSize(rawDataList.size() * 2);
        Assertions.assertThat(actualHistory)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_1, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_2, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_3, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_4, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_5, META_1),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_1, META_2),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_2, META_2),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_3, META_2),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_4, META_2),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_5, META_2)
            );
    }

    @Test
    public void deleteMultipleData() {
        ticketRepository.save(rawStatus(TYPE, TICKET, TicketState.NEW, new MyMeta("my meta value")));
        List<TrackerApproverRawData> rawDataList = List.of(
            rawData(KEY_1, TYPE, TICKET),
            rawData(KEY_2, TYPE, TICKET),
            rawData(KEY_3, TYPE, TICKET),
            rawData(KEY_4, TYPE, TICKET),
            rawData(KEY_5, TYPE, TICKET)
        );
        rawDataList.stream().forEach(data -> data.setMeta(JsonWrapper.fromJson(objectMapper, META_1)));
        dataRepository.save(rawDataList);

        var savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).hasSize(rawDataList.size());

        var ticket = ticketRepository.findByTicket(TICKET);
        ticket.setState(TicketState.ENRICHED);
        ticketRepository.save(ticket);

        savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).hasSize(rawDataList.size());

        savedData.stream().forEach(data -> data.setMeta(JsonWrapper.fromJson(objectMapper, META_2)));
        dataRepository.save(savedData);

        dataRepository.deleteByTicket(TICKET);

        savedData = dataRepository.findByTicket(TICKET);
        Assertions.assertThat(savedData).isEmpty();

        var actualHistory = dataHistoryRepository.findAllByTicketOrderedByModifiedTs(TICKET);
        Assertions.assertThat(actualHistory).hasSize(rawDataList.size() * 3);
        Assertions.assertThat(actualHistory)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_1, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_2, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_3, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_4, META_1),
                createData(TicketChangeType.INSERT, TicketState.NEW, KEY_5, META_1),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_1, META_2),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_2, META_2),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_3, META_2),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_4, META_2),
                createData(TicketChangeType.UPDATE, TicketState.ENRICHED, KEY_5, META_2),
                createData(TicketChangeType.DELETE, TicketState.ENRICHED, KEY_1, META_2),
                createData(TicketChangeType.DELETE, TicketState.ENRICHED, KEY_2, META_2),
                createData(TicketChangeType.DELETE, TicketState.ENRICHED, KEY_3, META_2),
                createData(TicketChangeType.DELETE, TicketState.ENRICHED, KEY_4, META_2),
                createData(TicketChangeType.DELETE, TicketState.ENRICHED, KEY_5, META_2)
            );
    }

    protected static TrackerApproverTicketRawStatus rawStatus(String type,
                                                              String ticket,
                                                              TicketState state,
                                                              MyMeta myMeta) {
        return new TrackerApproverTicketRawStatus()
            .setType(type)
            .setTicket(ticket)
            .setState(state)
            .setMeta(JsonWrapper.fromObject(myMeta));
    }

    protected static TrackerApproverRawData rawData(MyKey key, String type, String ticket) {
        return new TrackerApproverRawData()
            .setKey(JsonWrapper.fromObject(key))
            .setType(type)
            .setTicket(ticket);
    }

    protected TrackerApproverRawDataHistory createData(TicketChangeType changeType, TicketState state,

                                                       MyKey key, String keyMeta) {
        String keyJson;
        try {
            keyJson = objectMapper.writeValueAsString(key);
        } catch (JsonProcessingException e) {
            keyJson = null;
        }
        return new TrackerApproverRawDataHistory()
            .setKey(JsonWrapper.fromJson(objectMapper, keyJson))
            .setChangeType(changeType)
            .setState(state)
            .setKeyMeta(JsonWrapper.fromJson(objectMapper, keyMeta))
            .setType(TYPE)
            .setTicket(TICKET)
            .setModificationLogin("test_login")
            .setContext("test_context");

    }

    protected RecursiveComparisonConfiguration recursiveComparison() {
        var configuration = new RecursiveComparisonConfiguration();
        configuration.ignoreFields("modifiedTs");
        configuration.registerEqualsForType((jsonWrapper, jsonWrapper2) -> {
            var jsonNode = jsonWrapper.toJsonNode(objectMapper);
            var jsonNode2 = jsonWrapper2.toJsonNode(objectMapper);
            return jsonNode.equals(jsonNode2);
        }, JsonWrapper.class);
        return configuration;
    }
}

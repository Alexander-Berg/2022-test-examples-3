package ru.yandex.market.deepmind.tracker_approver.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Test;

import ru.yandex.market.deepmind.tracker_approver.BaseTrackerApproverTest;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawData;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.utils.JsonWrapper;

public class TrackerApproverDataRepositoryTest extends BaseTrackerApproverTest {
    private static final String TYPE = "type";

    @Test
    public void insert() {
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-1", TYPE, TicketState.NEW));

        var trackerApproverData = rawData(new MyKey(1, "a"), "test", "TEST-1");
        dataRepository.save(trackerApproverData);

        var data = dataRepository.findByKey("test", new MyKey(1, "a"));
        Assertions.assertThat(data).usingRecursiveComparison(recursiveComparison())
            .isEqualTo(trackerApproverData);
    }

    @Test
    public void update() {
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-1", TYPE, TicketState.NEW));

        dataRepository.save(rawData(new MyKey(1, "a"), "test", "TEST-1"));

        var data = dataRepository.findByKey("test", new MyKey(1, "a"));
        data.setMeta(JsonWrapper.fromObject("my meta"));
        dataRepository.save(data);

        var data2 = dataRepository.findByKey("test", new MyKey(1, "a"));
        Assertions.assertThat(data2).usingRecursiveComparison(recursiveComparison())
            .isEqualTo(data);
    }

    @Test
    public void delete() {
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-1", TYPE, TicketState.NEW));

        dataRepository.save(rawData(new MyKey(1, "a"), "test", "TEST-1"));

        var data = dataRepository.findByKey("test", new MyKey(1, "a"));
        Assertions.assertThat(data).isNotNull();

        dataRepository.deleteByKey("test", new MyKey(1, "a"));

        data = dataRepository.findByKey("test", new MyKey(1, "a"));
        Assertions.assertThat(data).isNull();
    }

    @Test
    public void batchInsert() {
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-1", TYPE, TicketState.NEW));

        var trackerApproverData1 = rawData(new MyKey(1, "a"), "test", "TEST-1");
        var trackerApproverData2 = rawData(new MyKey(1, "b"), "test", "TEST-1");
        var trackerApproverData3 = rawData(new MyKey(2, "a"), "test", "TEST-1");
        dataRepository.save(trackerApproverData1, trackerApproverData2, trackerApproverData3);

        var data = dataRepository.findByKeys("test", List.of(
            new MyKey(1, "a"),
            new MyKey(1, "b")
        ));
        Assertions.assertThat(data)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(trackerApproverData1, trackerApproverData2);
    }

    @Test
    public void batchUpdate() {
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-1", TYPE, TicketState.NEW));

        var trackerApproverData1 = rawData(new MyKey(1, "a"), "test", "TEST-1");
        var trackerApproverData2 = rawData(new MyKey(1, "b"), "test", "TEST-1");
        dataRepository.save(trackerApproverData1, trackerApproverData2);

        // update data
        var updatedData = dataRepository.findAll().stream()
            .map(v -> v.setMeta(JsonWrapper.fromObject(new BigDecimal("1.0"))))
            .collect(Collectors.toList());
        dataRepository.save(updatedData);

        var data = dataRepository.findAll();
        Assertions.assertThat(data)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsAll(updatedData);
    }

    @Test
    public void batchDelete() {
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-1", TYPE, TicketState.NEW));

        var trackerApproverData1 = rawData(new MyKey(1, "a"), "test", "TEST-1");
        var trackerApproverData2 = rawData(new MyKey(1, "b"), "test", "TEST-1");
        var trackerApproverData3 = rawData(new MyKey(2, "a"), "test", "TEST-1");
        dataRepository.save(trackerApproverData1, trackerApproverData2, trackerApproverData3);

        dataRepository.deleteByKeys("test", List.of(
            new MyKey(1, "a"),
            new MyKey(1, "b")
        ));
        var data = dataRepository.findAll();
        Assertions.assertThat(data)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(trackerApproverData3);
    }

    @Test
    public void deleteByTicket() {
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-1", TYPE, TicketState.NEW));
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-2", TYPE, TicketState.NEW));

        var test1a = rawData(new MyKey(1, "a"), "test", "TEST-1");
        var test2a = rawData(new MyKey(2, "a"), "test", "TEST-1");
        var anotherTest1a = rawData(new MyKey(1, "a"), "anotherTest", "TEST-2");
        var anotherTest1b = rawData(new MyKey(1, "b"), "anotherTest", "TEST-2");
        dataRepository.save(test1a, test2a, anotherTest1a, anotherTest1b);

        dataRepository.deleteByTicket("TEST-2");

        var all = dataRepository.findAll();
        Assertions.assertThat(all)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(test1a, test2a);
    }

    @Test
    public void findByData() {
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-1", TYPE, TicketState.NEW));
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-2", TYPE, TicketState.NEW));

        var test1a = rawData(new MyKey(1, "a"), "test", "TEST-1");
        var test2a = rawData(new MyKey(2, "a"), "test", "TEST-1");
        var anotherTest1a = rawData(new MyKey(1, "a"), "anotherTest", "TEST-2");
        var anotherTest1b = rawData(new MyKey(1, "b"), "anotherTest", "TEST-2");
        dataRepository.save(test1a, test2a, anotherTest1a, anotherTest1b);

        var result1 = dataRepository.findByKeys(List.of(new MyKey(1, "a"), new MyKey(1, "b")));
        Assertions.assertThat(result1)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(test1a, anotherTest1a, anotherTest1b);

        var result2 = dataRepository.findByKeys(List.of(new MyKey(2, "a")));
        Assertions.assertThat(result2)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(test2a);

        var result3 = dataRepository.findByKeys(List.of(new MyKey(1, "not-exists")));
        Assertions.assertThat(result3)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .isEmpty();
    }

    @Test
    public void findByTicket() {
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-1", TYPE, TicketState.NEW));
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-2", TYPE, TicketState.NEW));

        var test1a = rawData(new MyKey(1, "a"), "test", "TEST-1");
        var test2a = rawData(new MyKey(2, "a"), "test", "TEST-1");
        var anotherTest1a = rawData(new MyKey(1, "a"), "anotherTest", "TEST-2");
        var anotherTest1b = rawData(new MyKey(1, "b"), "anotherTest", "TEST-2");
        dataRepository.save(test1a, test2a, anotherTest1a, anotherTest1b);

        var result1 = dataRepository.findByTicket("TEST-1");
        Assertions.assertThat(result1)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(test1a, test2a);

        var result2 = dataRepository.findByTicket("TEST-2");
        Assertions.assertThat(result2)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(anotherTest1a, anotherTest1b);

        var result3 = dataRepository.findByTicket("TEST-100500");
        Assertions.assertThat(result3)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .isEmpty();
    }

    @Test
    public void trackerStatusesAreBeingFilteredByTypes() {
        //arrange
        var statusToFind = new TrackerApproverTicketRawStatus("TEST-1", "find", TicketState.NEW);
        ticketRepository.save(statusToFind);
        ticketRepository.save(new TrackerApproverTicketRawStatus("TEST-2", "skip", TicketState.NEW));
        var filter = new TrackerApproverTicketRepository.Filter()
            .setTypes(List.of("find", "confuse"));

        //act
        var rawStatuses = ticketRepository.findByFilter(filter);

        //assert
        Assertions.assertThat(rawStatuses)
            .usingElementComparatorOnFields("ticket")
            .containsExactly(statusToFind);
    }

    private static TrackerApproverRawData rawData(MyKey key, String type, String ticket) {
        return new TrackerApproverRawData()
            .setKey(JsonWrapper.fromObject(key))
            .setType(type)
            .setTicket(ticket);
    }

    public RecursiveComparisonConfiguration recursiveComparison() {
        var configuration = new RecursiveComparisonConfiguration();
        configuration.ignoreFields("id", "modifiedTs");
        configuration.registerEqualsForType((jsonWrapper, jsonWrapper2) -> {
            var jsonNode = jsonWrapper.toJsonNode(objectMapper);
            var jsonNode2 = jsonWrapper2.toJsonNode(objectMapper);
            return jsonNode.equals(jsonNode2);
        }, JsonWrapper.class);
        return configuration;
    }
}

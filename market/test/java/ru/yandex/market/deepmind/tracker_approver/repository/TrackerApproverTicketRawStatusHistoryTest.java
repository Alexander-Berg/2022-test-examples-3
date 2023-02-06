package ru.yandex.market.deepmind.tracker_approver.repository;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.tracker_approver.BaseTrackerApproverTest;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketChangeType;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.utils.JsonWrapper;

public class TrackerApproverTicketRawStatusHistoryTest extends BaseTrackerApproverTest {

    protected static final String TYPE = "type";
    protected static final MyKey BASE_KEY = new MyKey(1, "a");
    protected static final String TICKET = "TEST-1";

    @Autowired
    private TrackerApproverTicketStatusHistoryRepository ticketStatusHistoryRepository;


    @Test
    public void insertTicketStatus() {
        ticketRepository.save(rawStatus(TYPE, TICKET, TicketState.NEW,
            JsonWrapper.fromJson(objectMapper, "{\"author\": null, \"warnings\": null, \"toPending\": null, " +
                "\"toInactive\": null, \"description\": null, \"parsingErrors\": null, \"approveVerdict\": null, " +
                "\"nothingChanged\": false}"))
        );

        var actualHistory = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(TICKET);
        Assertions.assertThat(actualHistory).hasSize(1);

        var historyItem = actualHistory.get(0);
        Assertions.assertThat(historyItem).isNotNull();
        Assertions.assertThat(historyItem.getChangeType()).isEqualTo(TicketChangeType.INSERT);
        Assertions.assertThat(historyItem.getType()).isEqualTo(TYPE);
        Assertions.assertThat(historyItem.getState()).isEqualTo(TicketState.NEW);
        Assertions.assertThat(historyItem.getTicket()).isEqualTo(TICKET);
        Assertions.assertThat(historyItem.getTicketMeta())
            .usingRecursiveComparison(recursiveComparison())
            .isEqualTo(JsonWrapper.fromJson(objectMapper, "{\"author\": null, \"warnings\": null, " +
                "\"toPending\": null, \"toInactive\": null, \"description\": null, \"parsingErrors\": null, " +
                "\"approveVerdict\": null, \"nothingChanged\": false}"));
        Assertions.assertThat(historyItem.getModificationLogin()).isEqualTo("test_login");
        Assertions.assertThat(historyItem.getContext()).isEqualTo("test_context");
    }

    @Test
    public void updateTicketStatus() {
        TrackerApproverTicketRawStatus ticketRawStatus = rawStatus(TYPE, TICKET, TicketState.NEW,
            JsonWrapper.fromJson(objectMapper, "{\"author\": null, \"warnings\": null, \"toPending\": null, " +
                "\"toInactive\": null, \"description\": null, \"parsingErrors\": null, \"approveVerdict\": null, " +
                "\"nothingChanged\": false}"));
        ticketRepository.save(ticketRawStatus);

        var savedData = ticketRepository.findByTicket(TICKET);
        savedData.setState(TicketState.ENRICHED);
        ticketRepository.save(savedData);

        var actualHistory = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(TICKET);
        Assertions.assertThat(actualHistory).hasSize(2);

        var historyItem = actualHistory.get(0);
        Assertions.assertThat(historyItem).isNotNull();
        Assertions.assertThat(historyItem.getChangeType()).isEqualTo(TicketChangeType.INSERT);
        Assertions.assertThat(historyItem.getType()).isEqualTo(TYPE);
        Assertions.assertThat(historyItem.getState()).isEqualTo(TicketState.NEW);
        Assertions.assertThat(historyItem.getTicket()).isEqualTo(TICKET);
        Assertions.assertThat(historyItem.getTicketMeta())
            .usingRecursiveComparison(recursiveComparison())
            .isEqualTo(JsonWrapper.fromJson(objectMapper, "{\"author\": null, \"warnings\": null, " +
                "\"toPending\": null, \"toInactive\": null, \"description\": null, \"parsingErrors\": null, " +
                "\"approveVerdict\": null, \"nothingChanged\": false}"));
        Assertions.assertThat(historyItem.getModificationLogin()).isEqualTo("test_login");
        Assertions.assertThat(historyItem.getContext()).isEqualTo("test_context");

        historyItem = actualHistory.get(1);
        Assertions.assertThat(historyItem).isNotNull();
        Assertions.assertThat(historyItem.getChangeType()).isEqualTo(TicketChangeType.UPDATE);
        Assertions.assertThat(historyItem.getType()).isEqualTo(TYPE);
        Assertions.assertThat(historyItem.getState()).isEqualTo(TicketState.ENRICHED);
        Assertions.assertThat(historyItem.getTicket()).isEqualTo(TICKET);
        Assertions.assertThat(historyItem.getTicketMeta())
            .usingRecursiveComparison(recursiveComparison())
            .isEqualTo(JsonWrapper.fromJson(objectMapper, "{\"author\": null, \"warnings\": null, " +
                "\"toPending\": null, \"toInactive\": null, \"description\": null, \"parsingErrors\": null, " +
                "\"approveVerdict\": null, \"nothingChanged\": false}"));
        Assertions.assertThat(historyItem.getModificationLogin()).isEqualTo("test_login");
        Assertions.assertThat(historyItem.getContext()).isEqualTo("test_context");
    }

    @Test
    public void deleteTicketStatus() {
        TrackerApproverTicketRawStatus ticketRawStatus = rawStatus(TYPE, TICKET, TicketState.NEW,
            JsonWrapper.fromJson(objectMapper, "{\"author\": null, \"warnings\": null, \"toPending\": null, " +
                "\"toInactive\": null, \"description\": null, \"parsingErrors\": null, \"approveVerdict\": null, " +
                "\"nothingChanged\": false}"));
        ticketRepository.save(ticketRawStatus);

        var savedData = ticketRepository.findByTicket(TICKET);
        savedData.setState(TicketState.ENRICHED);
        ticketRepository.save(savedData);

        ticketRepository.delete(List.of(TICKET));

        var actualHistory = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(TICKET);
        Assertions.assertThat(actualHistory).hasSize(3);

        var historyItem = actualHistory.get(0);
        Assertions.assertThat(historyItem).isNotNull();
        Assertions.assertThat(historyItem.getChangeType()).isEqualTo(TicketChangeType.INSERT);
        Assertions.assertThat(historyItem.getType()).isEqualTo(TYPE);
        Assertions.assertThat(historyItem.getState()).isEqualTo(TicketState.NEW);
        Assertions.assertThat(historyItem.getTicket()).isEqualTo(TICKET);
        Assertions.assertThat(historyItem.getTicketMeta())
            .usingRecursiveComparison(recursiveComparison())
            .isEqualTo(JsonWrapper.fromJson(objectMapper, "{\"author\": null, \"warnings\": null, " +
                "\"toPending\": null, \"toInactive\": null, \"description\": null, \"parsingErrors\": null, " +
                "\"approveVerdict\": null, \"nothingChanged\": false}"));
        Assertions.assertThat(historyItem.getModificationLogin()).isEqualTo("test_login");
        Assertions.assertThat(historyItem.getContext()).isEqualTo("test_context");

        historyItem = actualHistory.get(1);
        Assertions.assertThat(historyItem).isNotNull();
        Assertions.assertThat(historyItem.getChangeType()).isEqualTo(TicketChangeType.UPDATE);
        Assertions.assertThat(historyItem.getType()).isEqualTo(TYPE);
        Assertions.assertThat(historyItem.getState()).isEqualTo(TicketState.ENRICHED);
        Assertions.assertThat(historyItem.getTicket()).isEqualTo(TICKET);
        Assertions.assertThat(historyItem.getTicketMeta())
            .usingRecursiveComparison(recursiveComparison())
            .isEqualTo(JsonWrapper.fromJson(objectMapper, "{\"author\": null, \"warnings\": null, " +
                "\"toPending\": null, \"toInactive\": null, \"description\": null, \"parsingErrors\": null, " +
                "\"approveVerdict\": null, \"nothingChanged\": false}"));
        Assertions.assertThat(historyItem.getModificationLogin()).isEqualTo("test_login");
        Assertions.assertThat(historyItem.getContext()).isEqualTo("test_context");

        historyItem = actualHistory.get(2);
        Assertions.assertThat(historyItem).isNotNull();
        Assertions.assertThat(historyItem.getChangeType()).isEqualTo(TicketChangeType.DELETE);
        Assertions.assertThat(historyItem.getType()).isEqualTo(TYPE);
        Assertions.assertThat(historyItem.getState()).isEqualTo(TicketState.ENRICHED);
        Assertions.assertThat(historyItem.getTicket()).isEqualTo(TICKET);
        Assertions.assertThat(historyItem.getTicketMeta())
            .usingRecursiveComparison(recursiveComparison())
            .isEqualTo(JsonWrapper.fromJson(objectMapper, "{\"author\": null, \"warnings\": null, " +
                "\"toPending\": null, \"toInactive\": null, \"description\": null, \"parsingErrors\": null, " +
                "\"approveVerdict\": null, \"nothingChanged\": false}"));
        Assertions.assertThat(historyItem.getModificationLogin()).isEqualTo("test_login");
        Assertions.assertThat(historyItem.getContext()).isEqualTo("test_context");
    }

    protected static TrackerApproverTicketRawStatus rawStatus(String type,
                                                              String ticket,
                                                              TicketState state,
                                                              JsonWrapper myMeta) {
        return new TrackerApproverTicketRawStatus()
            .setType(type)
            .setTicket(ticket)
            .setState(state)
            .setMeta(myMeta);
    }

    protected RecursiveComparisonConfiguration recursiveComparison() {
        var configuration = new RecursiveComparisonConfiguration();
        configuration.ignoreFields("modifiedTs");
        configuration.registerEqualsForType((jsonWrapper, jsonWrapper2) -> {
            return JsonWrapper.equals(jsonWrapper, jsonWrapper2, objectMapper);
        }, JsonWrapper.class);
        return configuration;
    }
}

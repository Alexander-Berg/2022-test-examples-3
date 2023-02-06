package ru.yandex.market.deepmind.tracker_approver.service;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.tracker_approver.BaseTrackerApproverTest;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyMeta;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverData;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketStatus;
import ru.yandex.market.deepmind.tracker_approver.strategies.MyKeyWithCommitteeStrategy;
import ru.yandex.market.deepmind.tracker_approver.utils.SessionUtils;
import ru.yandex.startrek.client.Session;

public class TrackerApproverStrategyTest extends BaseTrackerApproverTest {

    private TrackerApproverFactory trackerApproverFactory;

    private TrackerApproverFacade<MyKey, MyMeta, MyMeta> committeeService;
    private Session session;

    @Before
    public void setUp() {
        session = SessionUtils.create();

        var strategy = new MyKeyWithCommitteeStrategy(session);
        trackerApproverFactory = new TrackerApproverFactory(dataRepository, ticketRepository, transactionTemplate,
                objectMapper);
        trackerApproverFactory.registerStrategy(strategy);

        committeeService = trackerApproverFactory.getFacade(MyKeyWithCommitteeStrategy.TYPE);
    }

    @Test
    public void saveData() {
        var key1a = new MyKey(1, "a");
        var key1b = new MyKey(1, "b");

        var ticket = committeeService.start(List.of(key1a, key1b));
        Assertions.assertThat(ticket).isNotNull();

        var keys = committeeService.findKeysByTicket(ticket);
        Assertions.assertThat(keys)
            .containsExactlyInAnyOrder(key1a, key1b);

        var byTicket = ticketRepository.findByTicket(ticket);
        Assertions.assertThat(byTicket.getState()).isEqualTo(TicketState.NEW);
    }

    @Test
    public void saveDataWithMeta() {
        var key1a = new MyKey(1, "a");
        var key1b = new MyKey(1, "b");

        // act
        var request = StartRequest.of(List.of(key1a, key1b),
            new MyMeta("Hello world"), Map.of(key1b, new MyMeta("1b")));
        var ticket = committeeService.start(request);

        // assert ticket
        Assertions.assertThat(ticket).isNotNull();

        // assert data
        var rows = committeeService.findByTicket(ticket);
        Assertions.assertThat(rows).containsExactlyInAnyOrder(
            new TrackerApproverData<MyKey, MyMeta>()
                .setKey(key1a)
                .setType(committeeService.getType())
                .setTicket(ticket)
                .setMeta(null),
            new TrackerApproverData<MyKey, MyMeta>()
                .setKey(key1b)
                .setType(committeeService.getType())
                .setTicket(ticket)
                .setMeta(new MyMeta("1b"))
        );

        var ticketStatus = committeeService.findTicketStatus(ticket);
        Assertions.assertThat(ticketStatus)
            .usingRecursiveComparison().ignoringFields("modifiedTs")
            .isEqualTo(
                new TrackerApproverTicketStatus<MyMeta>()
                    .setTicket(ticket)
                    .setType(committeeService.getType())
                    .setState(TicketState.NEW)
                    .setMeta(new MyMeta("Hello world"))
                    .setStrategyVersion(1)
            );
    }

    @Test
    public void testToStartAlreadyStartedData() {
        var key1a = new MyKey(1, "a");
        var key1b = new MyKey(1, "b");
        var key1c = new MyKey(1, "c");

        var request1 = StartRequest.of(List.of(key1a, key1b),
            new MyMeta("Hello world"), Map.of(key1b, new MyMeta("1b")));
        var ticket1 = committeeService.start(request1);
        Assertions.assertThat(ticket1).isNotNull();

        var request2 = StartRequest.of(List.of(key1a, key1c),
            new MyMeta("Hello world"), Map.of(key1b, new MyMeta("1c")));
        Assertions.assertThatCode(() -> committeeService.start(request2))
            .hasMessageContaining("Can't start process 'committee', because keys already in another process: " +
                "[Key{supplierId=1, shopSku='a'}]");

        var rows = committeeService.findByKeys(List.of(key1a, key1b, key1c));
        Assertions.assertThat(rows).containsExactlyInAnyOrder(
            new TrackerApproverData<MyKey, MyMeta>()
                .setKey(key1a)
                .setType(committeeService.getType())
                .setTicket(ticket1)
                .setMeta(null),
            new TrackerApproverData<MyKey, MyMeta>()
                .setKey(key1b)
                .setType(committeeService.getType())
                .setTicket(ticket1)
                .setMeta(new MyMeta("1b"))
        );
    }
}

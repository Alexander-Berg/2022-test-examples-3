package ru.yandex.market.crm.platform.reader.export.startrek.parsers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.StartrekTicket;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.reader.checkouter.OrdersService;
import ru.yandex.market.mcrm.startrek.support.StartrekResult;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueTypeRef;
import ru.yandex.startrek.client.model.QueueRef;
import ru.yandex.startrek.client.model.StatusRef;
import ru.yandex.startrek.client.model.UserRef;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StartrekComplaintOrderParserTest {
    @Test
    public void test() {
        OrdersService ordersService = mock(OrdersService.class);
        when(ordersService.getOrderUserIds(123455))
            .thenReturn(Collections.singleton(Uids.create(UidType.PUID, 123455)));

        when(ordersService.getOrderUserIds(123456))
            .thenReturn(Collections.singleton(Uids.create(UidType.PUID, 123456)));

        when(ordersService.getOrderUserIds(5656))
            .thenReturn(Collections.singleton(Uids.create(UidType.MUID, 5656)));

        StartrekComplaintOrderParser parser = new StartrekComplaintOrderParser(ordersService);

        IssueTypeRef typeRef = mock(IssueTypeRef.class);
        when(typeRef.getDisplay()).thenReturn("Задача");

        UserRef authorRef = mock(UserRef.class);
        when(authorRef.getLogin()).thenReturn("wanderer25");

        StatusRef statusRef = mock(StatusRef.class);
        when(statusRef.getDisplay()).thenReturn("В работе");

        QueueRef queueRef = mock(QueueRef.class);
        when(queueRef.getKey()).thenReturn("BLUEMARKETORDER");

        Issue issue1 = mock(Issue.class);
        when(issue1.getId()).thenReturn("1");
        when(issue1.getKey()).thenReturn("QUEUE-1");
        when(issue1.getO("customerOrderNumber")).thenReturn(Option.of(Option.of("123455")));
        when(issue1.getSummary()).thenReturn("Some problem happened 1");
        when(issue1.getDescription()).thenReturn(Option.empty());
        when(issue1.getType()).thenReturn(typeRef);
        when(issue1.getCreatedAt()).thenReturn(Instant.parse("2018-12-21T10:15:30.00Z"));
        when(issue1.getUpdatedAt()).thenReturn(Instant.parse("2018-12-21T10:30:30.00Z"));
        when(issue1.getStatus()).thenReturn(statusRef);
        when(issue1.getCreatedBy()).thenReturn(authorRef);
        when(issue1.getAssignee()).thenReturn(Option.empty());
        when(issue1.getQueue()).thenReturn(queueRef);

        UserRef assigneeRef = mock(UserRef.class);
        when(assigneeRef.getLogin()).thenReturn("test");

        Issue issue2 = mock(Issue.class);
        when(issue2.getId()).thenReturn("2");
        when(issue2.getKey()).thenReturn("QUEUE-2");
        when(issue2.getO("customerOrderNumber")).thenReturn(Option.of(Option.of("123455")));
        when(issue2.getSummary()).thenReturn("Some problem happened 2");
        when(issue2.getDescription()).thenReturn(Option.empty());
        when(issue2.getType()).thenReturn(typeRef);
        when(issue2.getCreatedAt()).thenReturn(Instant.parse("2018-12-21T10:15:30.00Z"));
        when(issue2.getUpdatedAt()).thenReturn(Instant.parse("2018-12-21T10:30:30.00Z"));
        when(issue2.getStatus()).thenReturn(statusRef);
        when(issue2.getCreatedBy()).thenReturn(authorRef);
        when(issue2.getAssignee()).thenReturn(Option.of(assigneeRef));
        when(issue2.getQueue()).thenReturn(queueRef);

        Issue issue3 = mock(Issue.class);
        when(issue3.getId()).thenReturn("3");
        when(issue3.getKey()).thenReturn("QUEUE-3");
        when(issue3.getO("customerOrderNumber")).thenReturn(Option.of(Option.of("     123456, 5656")));
        when(issue3.getSummary()).thenReturn("Some problem happened");
        when(issue3.getDescription()).thenReturn(Option.empty());
        when(issue3.getType()).thenReturn(typeRef);
        when(issue3.getCreatedAt()).thenReturn(Instant.parse("2018-12-21T10:45:30.00Z"));
        when(issue3.getUpdatedAt()).thenReturn(Instant.parse("2018-12-21T10:50:30.00Z"));
        when(issue3.getStatus()).thenReturn(statusRef);
        when(issue3.getCreatedBy()).thenReturn(authorRef);
        when(issue3.getAssignee()).thenReturn(Option.empty());
        when(issue3.getQueue()).thenReturn(queueRef);

        StartrekResult export = new StartrekResult(
                Arrays.asList(issue1, issue2, issue3),
                new HashMap<>(),
                new HashMap<>()
        );

        List<Order> actual = parser.apply(export);
        List<Order> expected = Arrays.asList(
            Order.newBuilder()
            .setId(123455L)
                .setSource(Order.Source.STARTREK)
                .setKeyUid(Uids.create(UidType.PUID, 123455))
            .setCreationDate(Instant.parse("2018-12-21T10:30:30.00Z").getMillis())
                .addComplaints(0,
                    StartrekTicket.newBuilder()
                        .setId("1")
                        .setKey("QUEUE-1")
                        .setSubject("Some problem happened 1")
                        .setType("Задача")
                        .setStatus("В работе")
                        .setCreatedAt(Instant.parse("2018-12-21T10:15:30.00Z").getMillis())
                        .setUpdateAt(Instant.parse("2018-12-21T10:30:30.00Z").getMillis())
                        .setAuthorId("wanderer25")
                        .setQueue("BLUEMARKETORDER")
                        .build())
            .build(),
            Order.newBuilder()
                .setId(123455L)
                .setSource(Order.Source.STARTREK)
                .setKeyUid(Uids.create(UidType.PUID, 123455))
                .setCreationDate(Instant.parse("2018-12-21T10:30:30.00Z").getMillis())
                .addComplaints(0,
                    StartrekTicket.newBuilder()
                        .setId("2")
                        .setKey("QUEUE-2")
                        .setSubject("Some problem happened 2")
                        .setType("Задача")
                        .setStatus("В работе")
                        .setCreatedAt(Instant.parse("2018-12-21T10:15:30.00Z").getMillis())
                        .setUpdateAt(Instant.parse("2018-12-21T10:30:30.00Z").getMillis())
                        .setAuthorId("wanderer25")
                        .setAssigneeId("test")
                        .setQueue("BLUEMARKETORDER")
                        .build())
                .build(),
            Order.newBuilder()
                .setId(123456L)
                .setSource(Order.Source.STARTREK)
                .setKeyUid(Uids.create(UidType.PUID, 123456))
                .setCreationDate(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                .addComplaints(0,
                    StartrekTicket.newBuilder()
                        .setId("3")
                        .setKey("QUEUE-3")
                        .setSubject("Some problem happened")
                        .setType("Задача")
                        .setStatus("В работе")
                        .setCreatedAt(Instant.parse("2018-12-21T10:45:30.00Z").getMillis())
                        .setUpdateAt(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                        .setAuthorId("wanderer25")
                        .setQueue("BLUEMARKETORDER")
                        .build())
                .build(),
            Order.newBuilder()
                .setId(5656L)
                .setSource(Order.Source.STARTREK)
                .setKeyUid(Uids.create(UidType.MUID, 5656))
                .setCreationDate(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                .addComplaints(0,
                    StartrekTicket.newBuilder()
                        .setId("3")
                        .setKey("QUEUE-3")
                        .setSubject("Some problem happened")
                        .setType("Задача")
                        .setStatus("В работе")
                        .setCreatedAt(Instant.parse("2018-12-21T10:45:30.00Z").getMillis())
                        .setUpdateAt(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                        .setAuthorId("wanderer25")
                        .setQueue("BLUEMARKETORDER")
                        .build())
                .build()
        );

        Assert.assertEquals(expected, actual);
    }
}

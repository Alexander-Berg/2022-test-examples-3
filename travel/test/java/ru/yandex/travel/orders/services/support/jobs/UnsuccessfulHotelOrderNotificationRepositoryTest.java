package ru.yandex.travel.orders.services.support.jobs;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.AuthorizedUser;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.support.UnsuccessfulHotelOrderNotification;
import ru.yandex.travel.orders.repository.AuthorizedUserRepository;
import ru.yandex.travel.orders.repository.OrderRepository;
import ru.yandex.travel.orders.repository.support.UnsuccessfulHotelOrderNotificationRepository;
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState.OS_CANCELLED;
import static ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState.OS_CONFIRMED;
import static ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState.OS_REFUNDED;
import static ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState.OS_WAITING_CONFIRMATION;
import static ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState.OS_WAITING_PAYMENT;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class UnsuccessfulHotelOrderNotificationRepositoryTest {
    @Autowired
    private OrderRepository orders;
    @Autowired
    private WorkflowRepository workflows;
    @Autowired
    private AuthorizedUserRepository authorizedUsers;
    @Autowired
    private UnsuccessfulHotelOrderNotificationRepository notifications;

    @Test
    public void testCopyUnsuccessfulStreakBaseCases() {
        createTestOrder("2007-12-03T10:15:31.00Z", OS_CANCELLED, "p1", "");
        createTestOrder("2007-12-03T12:15:32.00Z", OS_REFUNDED, "p1", "");
        // the streak starts here
        HotelOrder o3 = createTestOrder("2007-12-03T13:15:33.00Z", OS_CANCELLED, "p1", "");
        HotelOrder o4 = createTestOrder("2007-12-03T14:15:34.00Z", OS_WAITING_CONFIRMATION, "p1", "");

        // notification grouping delay hasn't passed yet
        assertThat(notifications.copyNewUnconfirmedOrders(ts("2007-12-02T11:15:30.00Z"), ts("2007-12-03T13:14:00.00Z")))
                .isEqualTo(0);

        // it's time to schedule the streak now
        assertThat(notifications.copyNewUnconfirmedOrders(ts("2007-12-02T11:15:30.00Z"), ts("2007-12-03T13:16:00.00Z")))
                .isEqualTo(2);
        assertThat(getPendingNotifications()).isEqualTo(Set.of(o3.getId(), o4.getId()));

        // no re-scheduling for already scheduled
        assertThat(notifications.copyNewUnconfirmedOrders(ts("2007-12-02T11:15:30.00Z"), ts("2007-12-03T13:16:00.00Z"))).isEqualTo(0);
    }

    @Test
    public void testCopySuccessfullyFinishedStreak() {
        createTestOrder("2007-12-03T10:15:31.00Z", OS_CANCELLED, "p1", "");
        createTestOrder("2007-12-03T10:20:32.00Z", OS_CANCELLED, "p1", "");
        createTestOrder("2007-12-03T10:25:32.00Z", OS_CONFIRMED, "p1", "");

        // confirmed/refunded orders discard the streak
        assertThat(notifications.copyNewUnconfirmedOrders(ts("2007-12-02T11:15:30.00Z"), ts("2007-12-03T11:00:00.00Z")))
                .isEqualTo(0);
    }

    @Test
    public void testCopyUnsuccessfullyStreakTooOldOrders() {
        createTestOrder("2007-12-01T10:15:31.00Z", OS_CANCELLED, "p1", "");
        createTestOrder("2007-12-02T10:20:32.00Z", OS_CANCELLED, "p1", "");

        // the orders are too old
        assertThat(notifications.copyNewUnconfirmedOrders(ts("2007-12-02T11:00:00.00Z"), ts("2007-12-03T11:00:00.00Z")))
                .isEqualTo(0);

        HotelOrder o3 = createTestOrder("2007-12-03T10:25:32.00Z", OS_CANCELLED, "p1", "");
        assertThat(notifications.copyNewUnconfirmedOrders(ts("2007-12-02T11:00:00.00Z"), ts("2007-12-03T11:00:00.00Z")))
                .isEqualTo(1);
        assertThat(getPendingNotifications()).isEqualTo(Set.of(o3.getId()));
    }

    @Test
    public void testCopyUnsuccessfulStreakAuthGrouping() {
        HotelOrder o1 = createTestOrder("2007-12-03T13:15:33.00Z", OS_CANCELLED, "p1", "");
        createTestOrder("2007-12-03T14:15:31.00Z", OS_CONFIRMED, "p2", "");
        createTestOrder("2007-12-03T14:15:32.00Z", OS_CONFIRMED, "", "s2");

        // other users' orders don't affect each other's streak
        assertThat(notifications.copyNewUnconfirmedOrders(ts("2007-12-02T11:15:30.00Z"), ts("2007-12-03T14:00:00.00Z")))
                .isEqualTo(1);
        assertThat(getPendingNotifications()).isEqualTo(Set.of(o1.getId()));
    }

    @Test
    public void testCopyCrashedOrders() {
        HotelOrder o1 = createTestOrder("2007-12-03T13:15:31.00Z", OS_CANCELLED, "p1", "");
        HotelOrder o2 = createTestOrder("2007-12-03T13:15:32.00Z", OS_WAITING_PAYMENT, "p1", "");
        HotelOrder o3 = createTestOrder("2007-12-03T13:15:33.00Z", OS_CANCELLED, "p1", "");

        o2.getWorkflow().setState(EWorkflowState.WS_CRASHED);

        // crashed orders shouldn't be reported
        assertThat(notifications.copyNewUnconfirmedOrders(ts("2007-12-02T11:15:30.00Z"), ts("2007-12-03T14:00:00.00Z")))
                .isEqualTo(2);
        assertThat(getPendingNotifications()).isEqualTo(Set.of(o1.getId(), o3.getId()));
    }

    private Set<UUID> getPendingNotifications() {
        return notifications.findAll().stream().map(UnsuccessfulHotelOrderNotification::getOrderId).collect(toSet());
    }

    private HotelOrder createTestOrder(String date, EHotelOrderState state, String passportId, String sessionKey) {
        HotelOrder order = new HotelOrder();
        order.setId(UUID.randomUUID());
        order.setState(state);
        order = orders.saveAndFlush(order);
        // overriding the default createdAt timestamp
        order.setCreatedAt(Instant.parse(date));
        order = orders.saveAndFlush(order);
        workflows.save(Workflow.createWorkflowForEntity(order));
        AuthorizedUser auth = Strings.isEmpty(sessionKey) ?
                AuthorizedUser.createLogged(order.getId(), "", passportId, "", AuthorizedUser.OrderUserRole.OWNER) :
                AuthorizedUser.createGuest(order.getId(), sessionKey, "", AuthorizedUser.OrderUserRole.OWNER);
        authorizedUsers.saveAndFlush(auth);
        return order;
    }

    private Instant ts(String instant) {
        return Instant.parse(instant);
    }
}

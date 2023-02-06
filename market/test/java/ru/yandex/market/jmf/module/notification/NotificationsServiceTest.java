package ru.yandex.market.jmf.module.notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.db.hibernate.MetadataSessionFactory;
import ru.yandex.market.jmf.entity.impl.adapter.EntityAdapter;
import ru.yandex.market.jmf.module.notification.impl.SendNotificationService;
import ru.yandex.market.jmf.module.notification.impl.polling.NotificationsResult;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.xiva.XivaPersonalClient;
import ru.yandex.market.jmf.module.xiva.request.BatchSendRequest;
import ru.yandex.market.jmf.module.xiva.request.SendRequestParams;
import ru.yandex.market.jmf.module.xiva.request.XivaRequestUtils;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.html.SafeUrlService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.jmf.module.notification.operations.SendNotificationOperationHandler.XIVA_NOTIFICATIONS_ENABLED;

@SpringJUnitConfig({InternalModuleNotificationTestConfiguration.class, NotificationsServiceTest.Config.class})
public class NotificationsServiceTest {
    @Inject
    private NotificationsService notificationsService;
    @Inject
    private OuTestUtils ouTestUtils;
    @Inject
    private EmployeeTestUtils employeeTestUtils;
    @Inject
    private TxService txService;
    @Inject
    private DbService dbService;
    @Inject
    private MetadataSessionFactory metadataSessionFactory;

    @Inject
    private XivaPersonalClient xivaPersonalClient;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private SafeUrlService safeUrlService;

    private Ou ou;
    private Ou subOu;
    private Employee employee0, employee1, employee2;

    @BeforeEach
    public void setUp() {
        configurationService.setValue(XIVA_NOTIFICATIONS_ENABLED.key(), true);
        txService.runInNewTx(() -> {
            ou = ouTestUtils.createOu();
            subOu = ouTestUtils.createOu(ou);

            employee0 = employeeTestUtils.createEmployee(ou);
            employee1 = employeeTestUtils.createEmployee(subOu);
            employee2 = employeeTestUtils.createEmployee(subOu);
        });

        txService.runInTx(() -> {
            metadataSessionFactory.getCurrentSession(ou).refresh(((EntityAdapter) ou).adaptee());
            metadataSessionFactory.getCurrentSession(subOu).refresh(((EntityAdapter) subOu).adaptee());
        });
        resetXivaClient();
        when(safeUrlService.toSafeUrl(anyString())).then(inv -> "https://SBA_HOST/" + inv.getArgument(0));
    }

    @Test
    public void testNotifyEmployee() {
        notificationsService.notify(employee0, "test");
        final var unreadCount = notificationsService.countUnreadNotifications(employee0);
        Assertions.assertEquals(1, unreadCount);
        Assertions.assertEquals("test", getNotifications(employee0).get(0).getMessage());
        checkNotifications(Map.of(employee0, 1L));
        checkNotificationsCount(1);
    }

    @Test
    public void testSanitizeHtmlOnNotify() {
        notificationsService.notify(employee0, "<div><a href='http://link'>test</a><div>");
        Assertions.assertEquals(
                "<a href=\"https://SBA_HOST/http://link\" target=\"_blank\" rel=\"noopener noreferrer\">test</a>",
                getNotifications(employee0).get(0).getMessage()
        );
    }

    private List<Notification> getNotifications(Employee employee) {
        final var unreadNotifications = notificationsService.getUnreadNotifications(employee, 0, 100);
        final var readNotifications = notificationsService.getReadNotifications(employee, 0, 100);
        return Stream.of(unreadNotifications, readNotifications)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    @Test
    public void testMarkRead() {
        notificationsService.notify(employee0, "test");
        final var notification = getNotifications(employee0).get(0);
        Assertions.assertEquals(Notification.Statuses.UNREAD, notification.getStatus());
        resetXivaClient();
        notificationsService.markRead(notification);
        Assertions.assertEquals(Notification.Statuses.READ, get(notification.getGid()).getStatus());
        checkNotifications(Map.of(employee0, 0L));
        checkNotificationsCount(1);
    }

    @Test
    public void testMarkUnread() {
        notificationsService.notify(employee0, "test");
        final var notification = getNotifications(employee0).get(0);
        Assertions.assertEquals(Notification.Statuses.UNREAD, notification.getStatus());
        notificationsService.markRead(notification);
        resetXivaClient();
        notificationsService.markUnread(notification);
        Assertions.assertEquals(Notification.Statuses.UNREAD, get(notification.getGid()).getStatus());
        checkNotifications(Map.of(employee0, 1L));
        checkNotificationsCount(1);
    }

    @Test
    public void testMarkAllRead() {
        notificationsService.notify(employee0, "test");
        notificationsService.notify(employee0, "test");
        notificationsService.notify(employee0, "test");
        getNotifications(employee0)
                .forEach(notification -> Assertions.assertEquals(Notification.Statuses.UNREAD,
                        notification.getStatus()));
        resetXivaClient();
        notificationsService.markAllRead(employee0, Now.offsetDateTime());
        getNotifications(employee0)
                .forEach(notification -> Assertions.assertEquals(Notification.Statuses.READ,
                        get(notification.getGid()).getStatus()));
        checkNotifications(Map.of(employee0, 0L));
        checkNotificationsCount(1);
    }

    @Test
    public void testMarkAllReadFromDateTime() throws InterruptedException {
        notificationsService.notify(employee0, "test");
        notificationsService.notify(employee0, "test");
        final var from = Now.offsetDateTime();
        Thread.sleep(10);
        notificationsService.notify(employee0, "test");
        getNotifications(employee0)
                .forEach(notification -> Assertions.assertEquals(Notification.Statuses.UNREAD,
                        notification.getStatus()));
        resetXivaClient();
        notificationsService.markAllRead(employee0, from);
        getNotifications(employee0).forEach(notification ->
                Assertions.assertEquals(
                        notification.getCreationTime().isAfter(from)
                                ? Notification.Statuses.UNREAD
                                : Notification.Statuses.READ,
                        get(notification.getGid()).getStatus()
                )
        );
        checkNotifications(Map.of(employee0, 1L));
        checkNotificationsCount(1);
    }

    @Test
    @Transactional
    public void testGetNotificationsOrdering() throws InterruptedException {
        notificationsService.notify(employee0, "test");
        Thread.sleep(50);
        notificationsService.notify(employee0, "test");
        Thread.sleep(50);
        notificationsService.notify(employee0, "test");
        final var notifications = new ArrayList<>(getNotifications(employee0));
        notifications.sort(Comparator.comparing(Notification::getArchived).thenComparing(Notification::getCreationTime).reversed());
        notificationsService.markRead(notifications.get(1));

        final var expected = List.of(notifications.get(0), notifications.get(2), notifications.get(1));
        Assertions.assertEquals(expected, getNotifications(employee0));
    }

    @Test
    public void testNotifyOu() {
        notificationsService.notify(ou, "test");
        final var unreadCountEmployee0 = notificationsService.countUnreadNotifications(employee0);
        Assertions.assertEquals(1, unreadCountEmployee0);
        final var unreadCountEmployee1 = notificationsService.countUnreadNotifications(employee1);
        Assertions.assertEquals(0, unreadCountEmployee1);
        Assertions.assertEquals("test",
                getNotifications(employee0).get(0).getMessage());
        checkNotifications(Map.of(employee0, 1L));
        checkNotificationsCount(1);
    }

    @Test
    public void testNotifyAllOu() {
        notificationsService.notifyAll(ou, "test");
        final var unreadCountEmployee0 = notificationsService.countUnreadNotifications(employee0);
        Assertions.assertEquals(1, unreadCountEmployee0);
        final var unreadCountEmployee1 = notificationsService.countUnreadNotifications(employee1);
        Assertions.assertEquals(1, unreadCountEmployee1);
        Assertions.assertEquals("test",
                getNotifications(employee0).get(0).getMessage());
        Assertions.assertEquals("test",
                getNotifications(employee1).get(0).getMessage());
        checkNotifications(Map.of(employee0, 1L));
        checkNotifications(Map.of(
                employee1, 1L,
                employee2, 1L
        ));
        checkNotificationsCount(2);
    }

    private Notification get(String gid) {
        return txService.doInTx(() -> dbService.get(gid));
    }

    private void checkNotifications(Map<Employee, Long> notifications) {
        verify(xivaPersonalClient, times(1)).sendBatch(argThat(new SendRequestMatcher(notifications)));
    }

    private void checkNotificationsCount(int expected) {
        verify(xivaPersonalClient, times(expected)).sendBatch(any());
    }

    private void resetXivaClient() {
        reset(xivaPersonalClient);
    }

    private static class SendRequestMatcher implements ArgumentMatcher<BatchSendRequest> {

        private final Map<Employee, Long> notifications;

        public SendRequestMatcher(Map<Employee, Long> notifications) {
            this.notifications = notifications;
        }

        @Override
        public boolean matches(BatchSendRequest argument) {
            if (null == argument) {
                return false;
            }
            SendRequestParams params = argument.getParams();
            Object payload = params.getPayload();
            if (null == payload || !(payload instanceof XivaRequestUtils.Payload)) {
                return false;
            }
            XivaRequestUtils.Payload payloadWithType = (XivaRequestUtils.Payload) payload;
            if (!SendNotificationService.EVENT_NAME.equals(payloadWithType.getType())) {
                return false;
            }
            if (null == payloadWithType.getPayload() || !(payloadWithType.getPayload() instanceof Map)) {
                return false;
            }
            Map<String, NotificationsResult> actual = (Map<String, NotificationsResult>) payloadWithType.getPayload();
            return notifications.size() == actual.size()
                    && notifications.keySet().stream()
                    .allMatch(employee -> null != actual.get(employee.getGid()) && Objects.equals(
                            notifications.get(employee),
                            actual.get(employee.getGid()).getUnreadNotificationsCount()
                    ));
        }
    }

    @Configuration
    public static class Config {
        @Bean
        @Primary
        public SafeUrlService mockSafeUrlService() {
            return Mockito.mock(SafeUrlService.class);
        }
    }
}

package ru.yandex.market.notifier.test.integration;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.util.LoadTestingUtils;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

public class ShootingOrderPushTest extends AbstractServicesTestBase {
    public static final long SHOOTING_USER_ID = 2308324861409815965L;

    @Value("${market.notifier.streams}")
    int streamCount;

    @BeforeEach
    public void init() throws Exception {
        mockFactory.mockPersNotifyClientToThrowForPush(persNotifyClient);
    }

    @Test
    public void shouldAggregatedSmsForStatus() {
        Buyer buyer = new Buyer();
        buyer.setUid(SHOOTING_USER_ID);
        buyer.setEmail("checkouter-shooting@yandex-team.ru");
        buyer.setNormalizedPhone("7777777777");
        final int eventsNumber = 5;
        List<OrderHistoryEvent> allEvents = eventTestUtils.getAggregatedOrderHistoryEvents(buyer, eventsNumber);
        eventTestUtils.mockEvents(allEvents);

        int totalNotifications = allEvents.size();
        eventTestUtils.assertHasNewNotifications(totalNotifications);
        //первый раз попытались отправить PUSH
        eventTestUtils.deliverNotifications();
        assertAllPushNotificationsAreFailed(eventsNumber);

        //симулируем, что прошло 5 минут, иначе ретрай посыла нотификаций будет запрещен
        setFixedTime(getClock().instant().plus(eventsNumber, ChronoUnit.MINUTES), ZoneId.systemDefault());

        //второй раз пропустили отправку PUSH
        eventTestUtils.deliverNotifications();
        assertAllPushNotificationsAreFailed(eventsNumber);
    }

    @Test
    public void shouldUseLastStreamForLoadNotifications() {
        var loadTestingUser = new Buyer();
        loadTestingUser.setUid(SHOOTING_USER_ID);
        loadTestingUser.setEmail("checkouter-shooting@yandex-team.ru");
        List<OrderHistoryEvent> loadTestingEvents = eventTestUtils.getAggregatedOrderHistoryEvents(loadTestingUser, 3);

        var normalUser = new Buyer();
        normalUser.setUid(12345L);
        normalUser.setEmail("hello@yandex.ru");
        List<OrderHistoryEvent> normalEvents = eventTestUtils.getAggregatedOrderHistoryEvents(normalUser, 3);
        var allEvents = Stream.of(loadTestingEvents, normalEvents).flatMap(Collection::stream).collect(toList());

        eventTestUtils.mockEvents(allEvents);
        int totalNotifications = allEvents.size();
        eventTestUtils.assertHasNewNotifications(totalNotifications);
        var notifications = eventTestUtils.getAllNotifications();
        Assertions.assertThat(notifications.stream()
                        .flatMap(n -> n.getDeliveryChannels().stream())
                        .filter(LoadTestingUtils::isLoadTestingNotification)
                        .collect(toList()))
                .hasSize(6)
                .allMatch(dc -> dc.getStream() == (streamCount - 1));
        Assertions.assertThat(notifications.stream()
                        .flatMap(n -> n.getDeliveryChannels().stream())
                        .filter(not(LoadTestingUtils::isLoadTestingNotification))
                        .collect(toList()))
                .hasSize(6)
                .allMatch(dc -> dc.getStream() != streamCount);
    }

    private void assertAllPushNotificationsAreFailed(int size) {
        List<DeliveryChannel> deliveries = eventTestUtils.getNotifications(ChannelType.MOBILE_PUSH)
                .stream().map(Notification::getDeliveryChannels).flatMap(Collection::stream).collect(toList());
        assertThat(deliveries.size(), Matchers.equalTo(size));
        assertThat(deliveries, everyItem(hasProperty("status", is(NotificationStatus.FAILED))));
    }
}

package ru.yandex.market.notifier.test.integration;

import java.time.Clock;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.notifier.application.WireMockAbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.providers.OrderProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.notifier.util.providers.EventsProvider.orderStatusUpdated;

public class OrderStatusBadRequestRetryTest extends WireMockAbstractServicesTestBase {

    @Autowired
    protected WireMockServer pushApiMock;

    @Autowired
    @Qualifier("clock")
    protected TestableClock testableClock;

    @Value("${market.notifier.pushapi.order.status.pending_to_cancelled.retry.count:3}")
    private int pendingToCancelledRetryCount;

    @Autowired
    protected EventTestUtils eventTestUtils;

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                new Object[]{PENDING},
                new Object[]{UNPAID}
        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldRetryOrderStatusFromPendingToCancelledLimitedTimes(OrderStatus from) {
        pushApiMock.stubFor(WireMock.post(urlPathEqualTo("/shops/" + OrderProvider.SHOP_ID + "/order/status"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
                        .withBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<error>\n" +
                                "    <code>HTTP</code>\n" +
                                "    <message>400 Bad Request</message>\n" +
                                "    <shop-admin>true</shop-admin>\n" +
                                "</error>")));

        OrderHistoryEvent event = orderStatusUpdated(
                from,
                OrderStatus.CANCELLED,
                OrderSubstatus.PENDING_CANCELLED,
                ClientInfo.SYSTEM,
                order -> {
                    order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
                    order.setFulfilment(false);
                }
        );
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(1);

        for (int retry = 1; retry < pendingToCancelledRetryCount; retry++) {
            deliverNotificationAndCheckRetryCountAndStatus(retry, NotificationStatus.FAILED);
        }
        // достижение порога ретраев
        deliverNotificationAndCheckRetryCountAndStatus(pendingToCancelledRetryCount, NotificationStatus.DELETED);
    }

    private void deliverNotificationAndCheckRetryCountAndStatus(int retryCount, NotificationStatus status) {
        //симулируем, что прошло 10 минут, иначе ретрай посыла нотификаций будет запрещен
        setFixedTime(Clock.systemDefaultZone().instant()
                .plus(retryCount * 10, ChronoUnit.MINUTES), ZoneId.systemDefault());

        eventTestUtils.deliverNotifications();

        DeliveryChannel deliveryChannel = eventTestUtils.getSingleNotification(ChannelType.PUSH).getDeliveryChannels()
                .stream().findFirst().orElseThrow();
        assertEquals(retryCount, deliveryChannel.getRetryCount());
        assertEquals(status, deliveryChannel.getStatus());
    }
}

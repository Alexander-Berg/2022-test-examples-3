package ru.yandex.market.notifier.test.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.PersNotifyVerifier;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.notifier.util.providers.EventsProvider.orderItemsUpdated;

@Disabled
@ExtendWith(SpringExtension.class)
public class RedOrderItemsUpdatedTest extends AbstractServicesTestBase {
    private static final EnumSet<OrderAcceptMethod> METHODS_TO_SEND_PUSH_NOTIFICATION = EnumSet.of(
            OrderAcceptMethod.PUSH_API,
            OrderAcceptMethod.PUSHAPI_SANDBOX
    );


    public String caseName;
    public ClientRole role;
    public OrderAcceptMethod acceptMethod;
    public boolean shouldSendPushApiNotification;
    @Autowired
    private PersNotifyVerifier persNotifyVerifier;


    @Autowired
    private EventTestUtils eventTestUtils;
    @Autowired
    private PushApi pushClient;

    @BeforeEach
    public void setUp() {
        Mockito.reset(pushClient);
    }

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.stream(ClientRole.values())
                .flatMap(role -> Arrays.stream(OrderAcceptMethod.values())
                        .map(am -> new Object[]{
                                role.name() + " - " + am.name(),
                                role,
                                am,
                                METHODS_TO_SEND_PUSH_NOTIFICATION.contains(am) &&
                                        role == ClientRole.SHOP_USER
                        })
                )
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @Test
    public void checkEmailAndPushSent() {
        OrderHistoryEvent event = orderItemsUpdated(
                new ClientInfo(role, 123L),
                order -> {
                    order.setAcceptMethod(acceptMethod);
                    order.setRgb(Color.RED);
                }
        );
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(
                shouldSendPushApiNotification ? 2 : acceptMethod == OrderAcceptMethod.UNKNOWN ? 0 : 1
        );
        eventTestUtils.deliverNotifications();
        if (shouldSendPushApiNotification) {
            Order order = event.getOrderAfter();
            verify(pushClient)
                    .itemsChange(
                            eq(event.getOrderAfter().getShopId()),
                            any(),
                            eq(order.isFake()),
                            eq(order.getContext()),
                            any(),
                            Mockito.isNull(String.class)
                    );
        }
    }
}

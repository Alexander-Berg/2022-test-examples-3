package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.CUSTOM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.LATE_CONTACT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PENDING_CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.REPLACING_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.UNKNOWN;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_BOUGHT_CHEAPER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_FORGOT_TO_USE_BONUS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_PLACED_OTHER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTED_ANOTHER_PAYMENT_METHOD;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTS_TO_CHANGE_DELIVERY_DATE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WRONG_ITEM_DELIVERED;

public class UserCancellationRequestNegativeSubstatusTest
        extends AbstractCancellationRequestNegativeSubstatusTestBase {

    private static final ClientInfo USER_CLIENT_INFO = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

    @Test
    void createCancellationRequestWithoutSubstatusesMap() throws Exception {
        EnumSet<OrderSubstatus> negativeUser = EnumSet.allOf(OrderSubstatus.class);
        negativeUser.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeUser.removeAll(EnumSet.of(USER_CHANGED_MIND, USER_REFUSED_DELIVERY,
                USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER, CUSTOM, REPLACING_ORDER,
                USER_WANTED_ANOTHER_PAYMENT_METHOD, USER_WANTS_TO_CHANGE_ADDRESS,
                USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                USER_FORGOT_TO_USE_BONUS
        ));
        negativeUser.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeUserPickup = EnumSet.allOf(OrderSubstatus.class);
        negativeUserPickup.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeUserPickup.removeAll(EnumSet.of(USER_CHANGED_MIND, USER_REFUSED_DELIVERY,
                USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER, CUSTOM, REPLACING_ORDER,
                USER_WANTED_ANOTHER_PAYMENT_METHOD, USER_WANTS_TO_CHANGE_ADDRESS,
                USER_WANTS_TO_CHANGE_DELIVERY_DATE, WRONG_ITEM_DELIVERED,
                USER_FORGOT_TO_USE_BONUS
        ));
        negativeUserPickup.remove(UNKNOWN);

        EnumSet<OrderSubstatus> negativeUserPending = EnumSet.allOf(OrderSubstatus.class);
        negativeUserPending.removeIf(s -> s.getStatus() != OrderStatus.CANCELLED);
        negativeUserPending.removeAll(EnumSet.of(LATE_CONTACT, USER_CHANGED_MIND, PENDING_CANCELLED,
                USER_REFUSED_DELIVERY, USER_PLACED_OTHER_ORDER, USER_BOUGHT_CHEAPER, CUSTOM, REPLACING_ORDER,
                USER_WANTED_ANOTHER_PAYMENT_METHOD, USER_WANTS_TO_CHANGE_ADDRESS,
                USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                USER_FORGOT_TO_USE_BONUS
        ));
        negativeUserPending.remove(UNKNOWN);

        List<Object[]> result = new ArrayList<>();
        for (Boolean isCreateByOrderEditApi : new Boolean[]{Boolean.FALSE, Boolean.TRUE}) {
            Stream.of(
                            negativeUser.stream().map(ss -> new Object[]{USER_CLIENT_INFO, OrderStatus.UNPAID, ss,
                                    isCreateByOrderEditApi}),
                            negativeUserPending.stream().map(ss -> new Object[]{USER_CLIENT_INFO, OrderStatus.PENDING,
                                    ss, isCreateByOrderEditApi}),
                            negativeUser.stream().map(ss -> new Object[]{USER_CLIENT_INFO, OrderStatus.PROCESSING, ss,
                                    isCreateByOrderEditApi}),
                            negativeUserPickup.stream().map(ss -> new Object[]{USER_CLIENT_INFO, OrderStatus.PICKUP, ss,
                                    isCreateByOrderEditApi}),
                            negativeUser.stream().map(ss -> new Object[]{USER_CLIENT_INFO, OrderStatus.DELIVERY, ss,
                                    isCreateByOrderEditApi})
                    ).flatMap(Function.identity())
                    .forEach(result::add);
        }

        for (Object[] r : result) {
            super.createCancellationRequest((ClientInfo) r[0], (OrderStatus) r[1], (OrderSubstatus) r[2],
                    (Boolean) r[3]);
        }
    }
}

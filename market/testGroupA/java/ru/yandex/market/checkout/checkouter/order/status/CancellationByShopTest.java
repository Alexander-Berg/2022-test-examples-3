package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.checkout.backbone.validation.order.status.rule.CancellationByShopRule;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class CancellationByShopTest {

    protected CheckouterFeatureReader checkouterFeatureReader;
    protected CancellationByShopRule cancellationByShopRule;

    private static final Map<OrderStatus, Set<OrderSubstatus>> ALLOWED_SUBSTATUSES_FOR_SHOP = Map.of(
            OrderStatus.PROCESSING, Set.of(OrderSubstatus.SHOP_FAILED, OrderSubstatus.PRESCRIPTION_MISMATCH)
    );

    @BeforeEach
    public void setUp() {
        cancellationByShopRule = new CancellationByShopRule();
    }

    public static Stream<Arguments> testParameters() {
        Set<OrderStatus> orderStatuses = Set.of(DELIVERY, PENDING, PICKUP, PROCESSING, UNPAID);

        Set<OrderSubstatus> cancelSubsstatuses = Arrays.stream(OrderSubstatus.values())
                .filter(ss -> ss.getStatus() == CANCELLED)
                .collect(Collectors.toSet());


        return Sets.cartesianProduct(orderStatuses, cancelSubsstatuses)
                .stream()
                .map(e -> Arguments.of(e.get(0), e.get(1)));
    }

    protected Order createOrderWithStatus(OrderStatus status) {
        var order = OrderProvider.getBluePostPaidOrder();
        order.setStatus(status);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        order.setFulfilment(true);

        return order;
    }

    protected boolean cancellationAllowed(OrderStatus fromStatus, OrderSubstatus toSubstatus) {
        return ALLOWED_SUBSTATUSES_FOR_SHOP.containsKey(fromStatus)
                && ALLOWED_SUBSTATUSES_FOR_SHOP.get(fromStatus).contains(toSubstatus);
    }

    @ParameterizedTest(name = " from {0} to {1}")
    @MethodSource("testParameters")
    public void cancellationOrderByDBSShopTest(OrderStatus orderStatus, OrderSubstatus toSubstatus) {
        ClientRole.SHOP_ROLES.forEach(clientRole -> {
            var order = createOrderWithStatus(orderStatus);
            var clientInfo = ClientInfo.builder(clientRole).withShopId(order.getShopId()).build();
            try {
                cancellationByShopRule.validate(order, clientInfo, OrderStatus.CANCELLED, toSubstatus);
                if (!cancellationAllowed(orderStatus, toSubstatus)) {
                    Assertions.fail("cancellation not allowed!");
                }
            } catch (Exception e) {
                if (cancellationAllowed(orderStatus, toSubstatus)) {
                    Assertions.fail("cancellation allowed!");
                }
            }
        });
    }
}

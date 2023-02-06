package ru.yandex.market.checkout.checkouter.order.status;

import java.util.stream.Stream;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

/**
 * Убеждаемся, что после присвоения статуса pickup для ПВЗ маркета магазин не может изменять статусы товара
 *
 * @see <a href="https://st.yandex-team.ru/MARKETPROJECT-7625">Проект</a>
 */
public class DbsToMarketBrandedOutletRuleTest extends AbstractWebTestBase {

    private static final long MANAGER_UID = 2234562L;
    private Order order;


    public static Stream<Arguments> shopRoles() {
        Parameters parameters = WhiteParametersProvider.dbsPickupOrderWithCombinatorParameters(po ->
                po.setMarketBranded(true));
        return Stream.of(
                new Object[]{"shop",
                        parameters,
                        new ClientInfo(ClientRole.SHOP, parameters.getOrder().getShopId()),
                        OrderStatus.PICKUP,
                        OrderStatus.DELIVERED},
                new Object[]{"shop user",
                        parameters,
                        new ClientInfo(ClientRole.SHOP_USER, MANAGER_UID, parameters.getOrder().getShopId()),
                        OrderStatus.PICKUP,
                        OrderStatus.DELIVERED},
                new Object[]{"shop",
                        parameters,
                        new ClientInfo(ClientRole.SHOP, parameters.getOrder().getShopId()),
                        OrderStatus.PICKUP,
                        OrderStatus.CANCELLED},
                new Object[]{"shop user",
                        parameters,
                        new ClientInfo(ClientRole.SHOP_USER, MANAGER_UID, parameters.getOrder().getShopId()),
                        OrderStatus.PICKUP,
                        OrderStatus.CANCELLED},
                new Object[]{"shop",
                        parameters,
                        new ClientInfo(ClientRole.SHOP, parameters.getOrder().getShopId()),
                        OrderStatus.DELIVERY,
                        OrderStatus.PICKUP},
                new Object[]{"shop user",
                        parameters,
                        new ClientInfo(ClientRole.SHOP_USER, MANAGER_UID, parameters.getOrder().getShopId()),
                        OrderStatus.DELIVERY,
                        OrderStatus.PICKUP}
        ).map(Arguments::of);
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Чекаутер не должен разрешать переводить заказ из {3} в {4}")
    @ParameterizedTest(name = TEST_DISPLAY_NAME)
    @MethodSource("shopRoles")
    public void shouldNotAllowStatusUpdateForDbsToMarketBrandedOutlet(
            String testCaseName,
            Parameters parameters,
            ClientInfo clientInfo,
            OrderStatus oldStatus,
            OrderStatus newStatus
    ) {
        this.order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, oldStatus);

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () ->
                orderUpdateService.updateOrderStatus(order.getId(), StatusAndSubstatus.of(newStatus, null),
                        clientInfo));
    }
}

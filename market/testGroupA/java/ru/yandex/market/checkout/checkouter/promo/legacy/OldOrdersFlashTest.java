package ru.yandex.market.checkout.checkouter.promo.legacy;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.MARKET_BLUE;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.MARKET_DEAL;

/**
 * @author sergeykoles
 * Created on: 24.09.18
 */
public class OldOrdersFlashTest extends AbstractWebTestBase {

    private Order createdBluePromoOrder;

    @BeforeEach
    public void setUp() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueParametersWithDelivery(
                DeliveryProvider.MOCK_DELIVERY_SERVICE_ID
        );
        OrderItem bluePromoItem = parameters.getOrder().getItems().iterator().next();
        FeedOfferId feedOfferId = bluePromoItem.getFeedOfferId();
        ItemInfo bluePromoItemOverride = parameters.getReportParameters().overrideItemInfo(feedOfferId);
        bluePromoItemOverride.setSupplierType(SupplierType.THIRD_PARTY);
        bluePromoItemOverride.getPrices().discountOldMin = bluePromoItem.getPrice().add(BigDecimal.TEN);
        createdBluePromoOrder = orderCreateHelper.createOrder(parameters);
        assertThat(
                createdBluePromoOrder.getPromos().stream()
                        .map(OrderPromo::getType)
                        .collect(Collectors.toList()),
                hasItem(MARKET_BLUE)
        );
        hackOrderPromoType(createdBluePromoOrder.getId(), MARKET_BLUE, MARKET_DEAL);
    }

    private void hackOrderPromoType(Long id, PromoType oldPromo, PromoType newPromo) {
        int updateCount = orderService.transaction(id,
                i -> {
                    masterJdbcTemplate.update("UPDATE ORDER_PROMO_HISTORY SET TYPE=? WHERE ORDER_ID=? AND TYPE=?",
                            newPromo.getCode(), id, oldPromo.getCode()
                    );
                    return masterJdbcTemplate.update("UPDATE ORDER_PROMO SET TYPE=? WHERE ORDER_ID=? AND TYPE=?",
                            newPromo.getCode(), id, oldPromo.getCode()
                    );
                }
        );

        assertThat(updateCount, equalTo(1));
    }

    @Test
    @DisplayName("Проверяем, что можно читать заказы с флэш-промо")
    public void testOrderRead() {
        Order order = orderService.getOrder(createdBluePromoOrder.getId());
        verifyPromos(order);
    }

    @Test
    @DisplayName("Проверяем, что работает поиск флэш-заказов")
    public void testOrderSearch() {
        // создадим просто заказик без скидок
        orderCreateHelper.createOrder(new Parameters());
        OrderSearchRequest flashSearchRequest = new OrderSearchRequest();
        flashSearchRequest.isFlash = true;
        PagedOrders flashOrders = orderService.getOrders(flashSearchRequest, ClientInfo.SYSTEM);
        assertThat(flashOrders.getItems(), not(empty()));
        flashOrders.getItems().forEach(
                this::verifyPromos
        );
    }

    @Test
    @DisplayName("Проверяем, что флэш-акция старых заказов нормально выгружается в ивентах")
    public void testOrderEvents() {
        Collection<OrderHistoryEvent> flashOrderEvents = eventService.getPagedOrderHistoryEvents(
                createdBluePromoOrder.getId(),
                Pager.atPage(1, 10000),
                null,
                null,
                Collections.singleton(HistoryEventType.NEW_ORDER),
                false,
                ClientInfo.SYSTEM,
                null).getItems();
        assertThat(flashOrderEvents, not(empty()));
        flashOrderEvents.forEach(
                oe -> verifyPromos(oe.getOrderAfter())
        );
    }

    private void verifyPromos(Order order) {
        assertThat(
                order.getPromos().stream()
                        .map(OrderPromo::getType)
                        .collect(Collectors.toList()),
                hasItem(MARKET_DEAL)
        );
        List<PromoType> itemPromoTypes = order.getItems().stream()
                .flatMap(oi -> oi.getPromos().stream())
                .map(ItemPromo::getType)
                .collect(Collectors.toList());
        assertThat(
                itemPromoTypes, hasItem(MARKET_DEAL)
        );
    }
}

package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.service.business.PromosService;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ITEMS_UPDATED;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class PromosConfigurerServiceTest extends AbstractWebTestBase {

    @Autowired
    private PromosService promosService;
    @Autowired
    private OrderHistoryDao orderHistoryDao;

    @Test
    public void shouldDeleteItemsPromos() {
        final Parameters parameters = defaultBlueOrderParameters();
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("REAL_PROMO_CODE"));
        final OrderItem rawItem = parameters.getOrder().getItems().iterator().next();
        parameters.getLoyaltyParameters()
                .addLoyaltyDiscount(rawItem, new LoyaltyDiscount(BigDecimal.ONE, PromoType.MARKET_COUPON));
        parameters.setMockLoyalty(true);
        parameters.getReportParameters().setShopSupportsSubsidies(false);
        final Long orderId = orderCreateHelper.createOrder(parameters).getId();
        final Order order = orderService.getOrder(orderId);
        final Long itemId = order.getItems().iterator().next().getId();
        checkRowsCount("item_promo", 1L, itemId);
        checkRowsCount("item_promo_history", 1L, itemId);

        transactionTemplate.execute(ts -> {
            final long historyId = orderHistoryDao.insertOrderHistory(order, ITEMS_UPDATED, ClientInfo.SYSTEM);
            promosService.deleteItemsPromos(Collections.singleton(itemId), historyId);
            return null;
        });

        checkRowsCount("item_promo", 0L, itemId);
        // Старые записи в истории должны остаться. Новых появиться не должно.
        checkRowsCount("item_promo_history", 1L, itemId);
    }

    @Test
    void shouldNotCreateRowsWithNullIdInHistory() {
        final Parameters parameters = defaultBlueOrderParameters();
        final Long orderId = orderCreateHelper.createOrder(parameters).getId();
        final Order order = orderService.getOrder(orderId);
        final Long itemId = order.getItems().iterator().next().getId();
        checkRowsCount("item_promo_history", 0L, itemId);

        final Long historyId = transactionTemplate.execute(ts ->
                orderHistoryDao.insertOrderHistory(order, ITEMS_UPDATED, ClientInfo.SYSTEM));

        final DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> transactionTemplate.execute(ts ->
                        masterJdbcTemplate.update("insert into item_promo_history " +
                                "(id, history_id, item_id, promo_id, buyer_subsidy, buyer_discount, subsidy) " +
                                "values (null, ?, ?, 1, 0, 0, 0)", historyId, itemId))
        );
        assertNotNull(exception.getCause());
        assertThat(exception.getCause(), instanceOf(PSQLException.class));
        final PSQLException cause = (PSQLException) exception.getCause();
        assertEquals("23502", cause.getSQLState()); // violates not-null constraint
    }

    private void checkRowsCount(final String tableName, final long expectedCount, final Long itemId) {
        Long count = masterJdbcTemplate.queryForObject(
                "select count(*) as cnt from " + tableName + " where item_id = ?",
                (rs, rowNum) -> rs.getLong("cnt"), itemId);
        assertNotNull(count);
        assertEquals(expectedCount, count.longValue());
    }
}

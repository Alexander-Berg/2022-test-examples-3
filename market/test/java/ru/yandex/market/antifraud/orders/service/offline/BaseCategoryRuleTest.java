package ru.yandex.market.antifraud.orders.service.offline;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.logbroker.entities.CancelOrderRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class BaseCategoryRuleTest {

    private final OfflineAntifraudRule rule = new BaseCategoryRule();

    @Test
    public void check() {
        assertThat(rule.processRequest(CancelOrderRequest.builder().name("cancel_order").build()).getActions())
                .contains(AntifraudOfflineAction.CANCEL_ORDER);
        assertThat(rule.processRequest(CancelOrderRequest.builder().name("blacklist").build()).getActions())
                .contains(AntifraudOfflineAction.BAN_USER);
        assertThat(rule.processRequest(CancelOrderRequest.builder().name("cancel_cashback").build()).getActions())
                .contains(AntifraudOfflineAction.NULLIFY_CASHBACK_EMIT);
        assertThat(rule.processRequest(CancelOrderRequest.builder().build()).getActions())
                .isEmpty();
    }

}

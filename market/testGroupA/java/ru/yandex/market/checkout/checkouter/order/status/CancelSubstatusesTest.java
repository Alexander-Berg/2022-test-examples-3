package ru.yandex.market.checkout.checkouter.order.status;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.CancellationRules;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;

/**
 * @author mmetlov
 */

public class CancelSubstatusesTest extends AbstractWebTestBase {

    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;

    @Test
    public void testCancellationRulesForUserWithoutSubstatusesMap() throws Exception {
        CancellationRules cancellationRules = cancellationRequestHelper.getCancellationRules(ClientRole.USER);
        cancellationRules.getContent().forEach(rule -> {
            Assertions.assertNotNull(rule.getStatus());
            Assertions.assertNotNull(rule.getSubstatuses());
            Assertions.assertFalse(rule.getSubstatuses().isEmpty());
        });
        assertThat(cancellationRules.getContent(), containsInAnyOrder(
                hasProperty("status", is(UNPAID)),
                hasProperty("status", is(PENDING)),
                hasProperty("status", is(PROCESSING)),
                hasProperty("status", is(DELIVERY)),
                hasProperty("status", is(PICKUP))
        ));
        cancellationRules.getContent()
                .stream()
                .filter(
                        cr -> cr.getSubstatuses().stream().anyMatch(ds ->
                                ds.getOrderSubstatus() == OrderSubstatus.CUSTOM)
                ).forEach(
                        cr -> assertThat("last substatus for " + cr.getStatus() + " should be CUSTOM",
                                Iterables.getLast(cr.getSubstatuses()).getOrderSubstatus(), is(OrderSubstatus.CUSTOM))
                );
    }

    @Test
    public void testCancellationRulesForCallCenterOperatorWithoutSubstatusesMap() throws Exception {
        CancellationRules cancellationRules =
                cancellationRequestHelper.getCancellationRules(ClientRole.CALL_CENTER_OPERATOR);
        cancellationRules.getContent().forEach(rule -> {
            Assertions.assertNotNull(rule.getStatus());
            Assertions.assertNotNull(rule.getSubstatuses());
            Assertions.assertFalse(rule.getSubstatuses().isEmpty());
        });
        assertThat(cancellationRules.getContent(), containsInAnyOrder(
                hasProperty("status", is(UNPAID)),
                hasProperty("status", is(PENDING)),
                hasProperty("status", is(PROCESSING)),
                hasProperty("status", is(DELIVERY)),
                hasProperty("status", is(PICKUP))
        ));
        cancellationRules.getContent()
                .stream()
                .filter(
                        cr -> cr.getSubstatuses().stream().anyMatch(ds ->
                                ds.getOrderSubstatus() == OrderSubstatus.CUSTOM)
                ).forEach(
                        cr -> assertThat("last substatus for " + cr.getStatus() + " should be CUSTOM",
                                Iterables.getLast(cr.getSubstatuses()).getOrderSubstatus(), is(OrderSubstatus.CUSTOM))
                );
    }
}

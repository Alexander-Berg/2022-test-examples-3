package ru.yandex.market.loyalty.core.model.event;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.core.model.trigger.event.CoreOrderStatus;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class CoreOrderStatusTest {
    @Test
    public void shouldAllExternalValuesMapped() {
        assertThat(
                Arrays.stream(CoreOrderStatus.values())
                        .map(CoreOrderStatus::getOrderStatus)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList()),
                containsInAnyOrder(
                        Arrays.stream(OrderStatus.values())
                                .map(Matchers::equalTo)
                                .collect(Collectors.toList())
                )
        );
    }

    @Test
    public void shouldAllInternalValuesStartsWithLoyalty() {
        Arrays.stream(CoreOrderStatus.values())
                .filter(coreOrderStatus -> coreOrderStatus.getOrderStatus().isEmpty())
                .map(CoreOrderStatus::getCode)
                .forEach(code -> assertThat(code, startsWith("LOYALTY_")));
    }

    @Test
    public void shouldAllExternalValuesHasSameCodeAsExternalEnumName() {
        assertThat(
                Arrays.stream(CoreOrderStatus.values())
                        .filter(coreOrderStatus -> coreOrderStatus.getOrderStatus().isPresent())
                        .map(CoreOrderStatus::getCode)
                        .collect(Collectors.toList()),
                containsInAnyOrder(
                        Arrays.stream(OrderStatus.values())
                                .map(OrderStatus::name)
                                .map(Matchers::equalTo)
                                .collect(Collectors.toList())
                )
        );
    }
}

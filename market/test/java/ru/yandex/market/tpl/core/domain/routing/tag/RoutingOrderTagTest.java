package ru.yandex.market.tpl.core.domain.routing.tag;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.routing.tag.RoutingOrderTagType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RoutingOrderTagTest {

    @Test
    void getCostForMultiorder() {
        RoutingOrderTag tag = new RoutingOrderTag(
                "pvz",
                "pvz",
                BigDecimal.TEN,
                BigDecimal.ONE,
                RoutingOrderTagType.ORDER_TYPE,
                Set.of()
        );
        int costForMultiorder = tag.getCostForMultiorder(5);
        assertThat(costForMultiorder).isEqualTo(15);
    }
}

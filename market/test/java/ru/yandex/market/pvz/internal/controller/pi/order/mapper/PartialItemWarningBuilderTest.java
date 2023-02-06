package ru.yandex.market.pvz.internal.controller.pi.order.mapper;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.core.domain.order.OrderItemParams;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.partial_delivery.warning.ItemFittingForbiddenWarning;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.partial_delivery.warning.ItemWarning;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistic.api.model.delivery.CargoType.FASHION;
import static ru.yandex.market.logistic.api.model.delivery.CargoType.NO_TRYING;

class PartialItemWarningBuilderTest {

    @Test
    void addFittingForbiddenWarning() {
        OrderItemParams item = OrderItemParams.builder()
                .cargoTypeCodes(List.of(FASHION.getCode(), NO_TRYING.getCode()))
                .build();
        var builder = new PartialItemWarningBuilder(item);

        Set<ItemWarning> actual = builder.build();

        Set<ItemWarning> expected = Set.of(new ItemFittingForbiddenWarning());

        assertThat(actual).containsAll(expected);
    }

    @Test
    void emptyItemWarnings() {
        OrderItemParams item = OrderItemParams.builder()
                .cargoTypeCodes(List.of(FASHION.getCode()))
                .build();
        var builder = new PartialItemWarningBuilder(item);

        Set<ItemWarning> actual = builder.build();

        Set<ItemWarning> expected = Set.of();

        assertThat(actual).containsAll(expected);
    }
}

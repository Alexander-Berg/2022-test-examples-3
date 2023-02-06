package ru.yandex.market.checkout.checkouter.controllers.oms.archive;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.viewmodel.OrderChangesViewModel;

import static org.assertj.core.api.Assertions.assertThat;

class EndPointTest {

    @Test
    void mergeOrderChangesWithDuplicate() {
        List<OrderChangesViewModel> models1 = List.of(initOrderChanges(1), initOrderChanges(2));
        List<OrderChangesViewModel> models2 = List.of(initOrderChanges(1), initOrderChanges(3));
        List<OrderChangesViewModel> models3 = List.of();

        List<OrderChangesViewModel> result = EndPoint.mergeOrderChanges(List.of(models1, models2, models3));

        assertThat(result)
                .extracting(OrderChangesViewModel::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void mergeOrderChangesWithEmpty() {
        List<OrderChangesViewModel> models1 = List.of();
        List<OrderChangesViewModel> models2 = List.of();

        List<OrderChangesViewModel> result = EndPoint.mergeOrderChanges(List.of(models1, models2));

        assertThat(result).isEmpty();
    }

    private OrderChangesViewModel initOrderChanges(long modelId) {
        OrderChangesViewModel orderChangesViewModel = new OrderChangesViewModel();
        orderChangesViewModel.setId(modelId);
        return orderChangesViewModel;
    }
}

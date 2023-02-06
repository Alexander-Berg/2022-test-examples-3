package ru.yandex.market.checkout.checkouter.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.checkouter.json.RemovingCisRequirementRequest;
import ru.yandex.market.checkout.checkouter.json.RemovingCisRequirementResponse;
import ru.yandex.market.checkout.checkouter.order.OrderIdWithItemId;
import ru.yandex.market.checkout.checkouter.storage.item.OrderItemDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupportServiceImplTest {

    @InjectMocks
    private SupportServiceImpl supportService;
    @Mock
    private OrderItemDao orderItemDao;
    @Mock
    private TransactionTemplate transactionTemplate;

    SupportServiceImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldWorkWhenDoNotFoundAnyItem() {
        RemovingCisRequirementResponse response =
                supportService.removeCisRequirement(new RemovingCisRequirementRequest(false, List.of(1L)));

        assertThat(response.getEditedOrderIds()).isEmpty();
        assertThat(response.getOrderIdsWithError()).isEmpty();
    }

    @Test
    void shouldWorkWhenFindItems() {
        when(orderItemDao.findItemIdsForRemovingCisRequirement(anySet()))
                .thenReturn(List.of(
                        new OrderIdWithItemId(1L, 100L), new OrderIdWithItemId(2L, 101L)));

        RemovingCisRequirementResponse response =
                supportService.removeCisRequirement(new RemovingCisRequirementRequest(false, List.of(1L)));

        assertThat(response.getEditedOrderIds()).containsOnly(1L, 2L);
        assertThat(response.getOrderIdsWithError()).isEmpty();
    }

    @Test
    void shouldWorkWhenThrowExceptionOnUpdate() {
        when(orderItemDao.findItemIdsForRemovingCisRequirement(anySet()))
                .thenReturn(List.of(new OrderIdWithItemId(1L, 100L)));
        when(transactionTemplate.execute(any()))
                .thenThrow(RuntimeException.class);

        RemovingCisRequirementResponse response =
                supportService.removeCisRequirement(new RemovingCisRequirementRequest(false, List.of(1L)));

        assertThat(response.getEditedOrderIds()).isEmpty();
        assertThat(response.getOrderIdsWithError()).containsOnly(1L);
    }

    @Test
    void checkOnlyCase() {
        when(orderItemDao.findItemIdsForRemovingCisRequirement(anySet()))
                .thenReturn(List.of(
                        new OrderIdWithItemId(1L, 100L), new OrderIdWithItemId(2L, 101L)));

        RemovingCisRequirementResponse response =
                supportService.removeCisRequirement(new RemovingCisRequirementRequest(true, List.of(1L)));

        verify(transactionTemplate, never()).execute(any());
        assertThat(response.getEditedOrderIds()).containsOnly(1L, 2L);
        assertThat(response.getOrderIdsWithError()).isEmpty();
    }
}

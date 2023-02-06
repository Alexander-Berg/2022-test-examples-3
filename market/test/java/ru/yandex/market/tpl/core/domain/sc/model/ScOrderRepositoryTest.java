package ru.yandex.market.tpl.core.domain.sc.model;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterUtil;
import ru.yandex.market.tpl.core.domain.sc.ScOrderFactory;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ScOrderRepositoryTest {

    private final ScOrderRepository scOrderRepository;
    private final ScOrderFactory scOrderFactory;
    private final TestDataFactory testDataFactory;

    @Test
    void findOrdersToRefreshForSc() {
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.CREATED)
                .deliveryDate(LocalDate.now())
                .build());
        SortingCenter sortingCenter = SortingCenterUtil.sortingCenter();
        scOrderFactory.createScOrder(order, SortingCenter.DEFAULT_SC_ID);
        testDataFactory.flushAndClear();

        List<String> orders = scOrderRepository.findOrdersToRefreshForSc(
                20, sortingCenter.getId(), LocalDate.now()
        );

        assertThat(orders).containsExactly(order.getExternalOrderId());
    }

}

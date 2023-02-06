package ru.yandex.market.sc.core.domain.order;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CopyReturnOrderServiceTest {

    private final TestFactory testFactory;
    private final TransactionTemplate transactionTemplate;
    private final CopyReturnOrderService copyReturnOrderService;
    private final ScOrderRepository scOrderRepository;

    SortingCenter sortingCenter1;
    SortingCenter sortingCenter2;
    User user;

    @BeforeEach
    void setUp() {
        sortingCenter1 = testFactory.storedSortingCenter(100L);
        sortingCenter2 = testFactory.storedSortingCenter(200L);
        user = testFactory.storedUser(sortingCenter1, 123L);
    }

    @Test
    void copyReturnOrderTest() {
        String externalId = "o1";
        var order1 = transactionTemplate.execute(
                tx -> testFactory.createForToday(order(sortingCenter1).externalId(externalId).build())
                        .cancel().accept().get());
        var order2 = transactionTemplate.execute(
                tx -> {
                    copyReturnOrderService.copyReturnOrder(order1, sortingCenter2, null);
                    return scOrderRepository.findBySortingCenterAndExternalId(sortingCenter2, externalId).get();
                });
        assertThat(order2.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

}

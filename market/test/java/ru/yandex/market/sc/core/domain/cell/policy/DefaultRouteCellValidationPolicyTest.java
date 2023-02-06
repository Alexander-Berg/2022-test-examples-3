package ru.yandex.market.sc.core.domain.cell.policy;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.OrderNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.route.RouteNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

@EmbeddedDbTest
class DefaultRouteCellValidationPolicyTest {

    @Autowired
    OrderNonBlockingQueryService orderNonBlockingQueryService;
    @Autowired
    RouteNonBlockingQueryService routeNonBlockingQueryService;
    @Autowired
    OrderCommandService orderCommandService;
    @Autowired
    TestFactory testFactory;
    @Autowired
    DefaultRouteCellValidationPolicy cellValidationPolicy;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    ScOrderRepository scOrderRepository;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setupMockClock(clock);
        user = testFactory.storedUser(sortingCenter, 123L);
    }

    //todo нужен ли здесь вообще тест?

}

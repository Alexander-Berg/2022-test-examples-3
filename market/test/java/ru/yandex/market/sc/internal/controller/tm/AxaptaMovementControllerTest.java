package ru.yandex.market.sc.internal.controller.tm;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

@ScIntControllerTest
class AxaptaMovementControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    XDocFlow flow;

    @Autowired
    ScIntControllerCaller caller;

    @Autowired
    TestFactory testFactory;

    @BeforeEach
    void init() {
        Warehouse warehouse = testFactory.storedWarehouse();
        SortingCenter sortingCenter = testFactory.storedSortingCenter();
        User user = testFactory.storedUser(sortingCenter, TestFactory.USER_UID_LONG);
    }

    @Test
    void setAxaptaId() {
        flow.createInbound("in-1")
                .linkBoxes("XDOC-1")
                .fixInbound();

        caller.setAxaptaMovementRequestId(String.valueOf(flow.getSortingCenter().getId()), "in-1", "Зпер123");
        Inbound inbound = flow.getInbound("in-1");

        Assertions.assertThat(inbound.getInboundInfo().getAxaptaMovementRequest()).isEqualTo("Зпер123");
    }
}
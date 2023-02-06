package ru.yandex.market.logistics.logistics4go.controller.order;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.client.CombinatorGrpcClient;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.personal.client.api.DefaultPersonalMultiTypesStoreApi;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
public abstract class AbstractOrderTest extends AbstractIntegrationTest {

    protected static final Set<OptionalOrderPart> OPTIONAL_ORDER_PARTS = EnumSet.of(
        OptionalOrderPart.CHANGE_REQUESTS,
        OptionalOrderPart.CANCELLATION_REQUESTS,
        OptionalOrderPart.GLOBAL_STATUSES_HISTORY
    );

    @Autowired
    protected LomClient lomClient;

    @Autowired
    protected CombinatorGrpcClient combinatorGrpcClient;

    @Autowired
    protected DefaultPersonalMultiTypesStoreApi personalDataStoreApi;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lomClient, combinatorGrpcClient, personalDataStoreApi);
    }

    @Nonnull
    protected OrderSearchFilter getOrderFilter(long orderId) {
        return OrderSearchFilter.builder()
            .orderIds(Set.of(orderId))
            .platformClientIds(Set.of(PlatformClient.YANDEX_GO.getId()))
            .build();
    }

    protected void mockSearchLomOrder(long orderId, OrderDto lomOrder) {
        mockSearchLomOrder(orderId, List.of(lomOrder));
    }

    protected void mockSearchLomOrder(long orderId, List<OrderDto> lomOrders) {
        doReturn(PageResult.of(lomOrders, lomOrders.size(), 1, 1))
            .when(lomClient)
            .searchOrders(
                getOrderFilter(orderId),
                OPTIONAL_ORDER_PARTS,
                Pageable.unpaged()
            );
    }

    protected void verifySearchLomOrder(long orderId) {
        verify(lomClient).searchOrders(
            getOrderFilter(orderId),
            OPTIONAL_ORDER_PARTS,
            Pageable.unpaged()
        );
    }
}

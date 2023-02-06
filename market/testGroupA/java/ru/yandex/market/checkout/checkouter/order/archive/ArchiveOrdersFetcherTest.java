package ru.yandex.market.checkout.checkouter.order.archive;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.db.SortingInfo;
import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.CheckpointRequest;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.ControllerUtils;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderSortingField;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.storage.OrderSequences;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.checkout.checkouter.order.ControllerUtils.buildCheckpointRequest;
import static ru.yandex.market.checkout.checkouter.order.OrderUpdater.entityGroup;

public class ArchiveOrdersFetcherTest extends AbstractWebTestBase {

    @Autowired
    private ArchiveOrdersFetcher archiveOrdersFetcher;
    @Autowired
    private StorageOrderArchiveService orderArchiveService;
    @Autowired
    private OrderWritingDao writingDao;
    @Autowired
    private OrderSequences orderSequences;
    @Autowired
    private Storage storage;

    @Test
    public void shouldReturnTwoOrders() {
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withArchived(true)
                .withMultiOrderId("bf14fedc-c255-4586-9d27-b6e93d00be31")
                .withSorting(List.of(new SortingInfo<>(OrderSortingField.CREATION_DATE, SortingOrder.ASC)))
                .withPageInfo(Pager.atPage(0, 10))
                .build();
        ClientInfo clientInfo = ControllerUtils.buildClientInfo(ClientRole.SYSTEM, 1L);
        CheckpointRequest checkpointRequest = buildCheckpointRequest(false, ClientRole.SYSTEM);
        Order order1 = OrderProvider.getBlueOrder();
        Order order2 = OrderProvider.getBlueOrder();
        order1.setFake(true);
        order2.setFake(true);
        order1.setProperty("multiOrderId", "bf14fedc-c255-4586-9d27-b6e93d00be31");
        order2.setProperty("multiOrderId", "bf14fedc-c255-4586-9d27-b6e93d00be31");
        Date now = new Date();
        saveOrder(order1, now, ClientInfo.SYSTEM);
        saveOrder(order2, now, ClientInfo.SYSTEM);
        Set<Long> orderIds = Set.of(order1.getId(), order2.getId());
        orderArchiveService.archiveOrders(orderIds, false);
        PagedOrders archiveOrders = archiveOrdersFetcher.getOrders(request, clientInfo, checkpointRequest);
        assertThat(archiveOrders.getItems(), hasSize(2));
    }

    public long saveOrder(final Order order, Date creationDate, final ClientInfo clientInfo) {
        final long orderId = orderSequences.getNextOrderId();
        order.setId(orderId);

        order.setStatus(OrderStatus.PLACING);
        order.setSubstatus(null);

        order.setUpdateDate(creationDate);
        order.setCreationDate(creationDate);
        order.setStatusUpdateDate(creationDate);
        order.setSubstatusUpdateDate(creationDate);

        if (order.isFake() == null) {
            order.setFake(false);
        }

        if (order.getContext() == null) {
            order.setContext(Context.MARKET);
        }

        if (order.getUserGroup() == null) {
            order.setUserGroup(UserGroup.DEFAULT);
        }
        storage.createEntityGroup(entityGroup(orderId), () -> writingDao.insertOrder(order, clientInfo));
        return orderId;
    }

}

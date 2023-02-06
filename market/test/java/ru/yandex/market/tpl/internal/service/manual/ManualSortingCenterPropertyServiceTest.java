package ru.yandex.market.tpl.internal.service.manual;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.util.CacheTestUtil;
import ru.yandex.market.tpl.internal.TplIntAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED;

@RequiredArgsConstructor
class ManualSortingCenterPropertyServiceTest extends TplIntAbstractTest {

    private final ManualSortingCenterPropertyService subject;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final TestUserHelper testUserHelper;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final CacheManager oneMinuteCacheManager;
    private SortingCenter sortingCenter;

    @BeforeEach
    void beforeEach() {
        sortingCenter = testUserHelper.sortingCenter(SortingCenter.DEFAULT_SC_ID);
    }

    @Test
    void disableEqueue() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, ELECTRONIC_QUEUE_ENABLED, true
        );

        subject.equeueEnabled(sortingCenter.getId(), false);
        CacheTestUtil.clear(oneMinuteCacheManager);
        Boolean result = sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                ELECTRONIC_QUEUE_ENABLED, sortingCenter.getId()
        );
        assertThat(result).isFalse();

        dbQueueTestUtil.assertQueueHasSize(QueueType.EQUEUE_PUSH_COURIERS_TO_SC, 1);
    }

    @Test
    void enableEqueue() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, ELECTRONIC_QUEUE_ENABLED, false
        );

        subject.equeueEnabled(sortingCenter.getId(), true);
        CacheTestUtil.clear(oneMinuteCacheManager);
        Boolean result = sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                ELECTRONIC_QUEUE_ENABLED, sortingCenter.getId()
        );
        assertThat(result).isTrue();

        dbQueueTestUtil.assertQueueHasSize(QueueType.EQUEUE_PUSH_COURIERS_TO_SC, 0);
    }

    @Test
    void disableTransferActForOrderPickup() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, true
        );

        subject.transferActForOrderPickupEnabled(sortingCenter.getId(), false);
        CacheTestUtil.clear(oneMinuteCacheManager);
        Boolean result = sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, sortingCenter.getId()
        );
        assertThat(result).isFalse();

        dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_ACT_PUSH_COURIERS, 1);
    }

    @Test
    void enableTransferActForOrderPickup() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, false
        );

        subject.transferActForOrderPickupEnabled(sortingCenter.getId(), true);
        CacheTestUtil.clear(oneMinuteCacheManager);
        Boolean result = sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, sortingCenter.getId()
        );
        assertThat(result).isTrue();

        dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_ACT_PUSH_COURIERS, 0);
    }

}

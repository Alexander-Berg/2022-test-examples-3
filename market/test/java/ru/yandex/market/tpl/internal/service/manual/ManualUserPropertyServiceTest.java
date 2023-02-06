package ru.yandex.market.tpl.internal.service.manual;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.util.CacheTestUtil;
import ru.yandex.market.tpl.internal.TplIntAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.user.UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED;

@RequiredArgsConstructor
class ManualUserPropertyServiceTest extends TplIntAbstractTest {

    private final ManualUserPropertyService manualUserPropertyService;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;
    private final TestUserHelper testUserHelper;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final List<CacheManager> cacheManagers;
    private User user;

    @BeforeEach
    void beforeEach() {
        user = testUserHelper.findOrCreateUser(123L);
    }

    @Test
    void disableTransferActForOrderPickup() {
        transactionTemplate.execute(s -> {
            userPropertyService.addPropertyToUser(
                    user, TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, true
            );
            return null;
        });

        manualUserPropertyService.transferActForOrderPickupEnabled(user.getId(), false);
        CacheTestUtil.clear(cacheManagers);
        Boolean result = transactionTemplate.execute(s -> userPropertyService.findPropertyForUser(
                TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, user
        ));
        assertThat(result).isFalse();

        dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_ACT_PUSH_COURIERS, 1);
    }

    @Test
    void enableTransferActForOrderPickup() {
        transactionTemplate.execute(s -> {
            userPropertyService.addPropertyToUser(
                    user, TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, false
            );
            return null;
        });

        manualUserPropertyService.transferActForOrderPickupEnabled(user.getId(), true);
        CacheTestUtil.clear(cacheManagers);
        Boolean result = transactionTemplate.execute(s -> userPropertyService.findPropertyForUser(
                TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, user
        ));
        assertThat(result).isTrue();

        dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_ACT_PUSH_COURIERS, 0);
    }

}

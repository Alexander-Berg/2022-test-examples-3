package ru.yandex.market.pers.notify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityDAO;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 18.12.17
 */
public class SubscriptionsCacherTest extends MockedDbTest {
    @Autowired
    private SubscriptionsCacher cacher;

    @Test
    public void saveSubscriptionWithoutEmail() {
        assertTrue(cacher.saveSubscriptions(null, Collections.singletonList(
            new EmailSubscription(null, null, NotificationType.ADVERTISING, EmailSubscriptionStatus.CONFIRMED)
        )));
    }

    @Test
    public void createOrGetUserIdentityReturnsExistingIfCannotCreate() {
        SubscriptionsCacher cacher = new SubscriptionsCacher();
        cacher.setMemCachingService(mock(MemCachingService.class));
        SubscriptionAndIdentityDAO dao = mock(SubscriptionAndIdentityDAO.class);
        cacher.setSubscriptionAndIdentityDAO(dao);
        when(dao.createIfNecessaryUserIdentity(any(Identity.class))).thenReturn(null);
        when(dao.getIdentityId(any(Identity.class))).thenReturn(1L);
        assertEquals(1L, cacher.createOrGetUserIdentity(new Uid(2L), null).longValue());
    }

    @Test
    public void createOrGetUserIdentityMultiThreaded() throws Exception {
        int threadsCount = 10;
        int testCount = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threadsCount);
        List<Pair<Future<Long>, Future<Long>>> results = new ArrayList<>();
        for (int i = 0; i < testCount; i++) {
            long id = i;
            results.add(Pair.of(
                pool.submit(() -> cacher.createOrGetUserIdentity(new Uid(id), null)),
                pool.submit(() -> cacher.createOrGetUserIdentity(new Uid(id), null))
            ));
        }

        for (Pair<Future<Long>, Future<Long>> result : results) {
            assertTrue(result.getKey().get().longValue() == result.getValue().get().longValue());
        }
    }
}

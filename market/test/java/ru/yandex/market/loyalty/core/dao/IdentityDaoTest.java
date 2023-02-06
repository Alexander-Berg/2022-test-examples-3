package ru.yandex.market.loyalty.core.dao;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.api.model.identity.Uuid;
import ru.yandex.market.loyalty.api.model.identity.YandexUid;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.lightweight.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author ukchuvrus
 */
public class IdentityDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private IdentityDao identityDao;

    private static final List<Identity<?>> BUNCH_OF_IDENTITIES =
            Stream.of(
                    LongStream.range(0, 20)
                            .mapToObj((LongFunction<Identity<?>>) Uid::new),
                    IntStream.range(0, 20)
                            .mapToObj(Integer::toString)
                            .map(YandexUid::new),
                    IntStream.range(0, 20)
                            .mapToObj(Integer::toString)
                            .map(Uuid::new)
            )
                    .flatMap(stream -> stream)
                    .collect(ImmutableList.toImmutableList());

    @Test
    public void duplicateIdentityTest() {
        List<Long> ids = BUNCH_OF_IDENTITIES.stream().sequential()
                .map(identity -> identityDao.createIfNecessaryUserIdentity(identity))
                .collect(Collectors.toList());

        List<Long> duplicateIds = BUNCH_OF_IDENTITIES.stream().sequential()
                .map(identity -> identityDao.createIfNecessaryUserIdentity(identity))
                .collect(Collectors.toList());

        assertEquals(ids, duplicateIds);
    }

    @Test
    @Repeat(5)
    public void duplicateShuffledIdentityTestInParallel() throws InterruptedException {

        Set<Long> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());
        testConcurrency(cpus ->
                Stream.generate(() -> {
                    List<Identity<?>> shuffledIdentities = new ArrayList<>(BUNCH_OF_IDENTITIES);
                    Collections.shuffle(shuffledIdentities, ThreadLocalRandom.current());
                    return shuffledIdentities;
                })
                        .limit(cpus)
                        .map(shuffledIdentities -> (ExceptionUtils.RunnableWithException<Exception>) () ->
                                shuffledIdentities.stream()
                                        .map(identity -> identityDao.createIfNecessaryUserIdentity(identity))
                                        .collect(Collectors.toCollection(() -> ids))
                        )
                        .collect(Collectors.toList())
        );
        assertEquals(BUNCH_OF_IDENTITIES.size(), ids.size());
    }

    @Test
    @Repeat(5)
    public void duplicateIdentityTestInParallel() throws InterruptedException {
        Set<Long> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());
        testConcurrency(() -> () -> BUNCH_OF_IDENTITIES.stream()
                .map(identity -> identityDao.createIfNecessaryUserIdentity(identity))
                .collect(Collectors.toCollection(() -> ids))
        );

        assertEquals(BUNCH_OF_IDENTITIES.size(), ids.size());
    }
}

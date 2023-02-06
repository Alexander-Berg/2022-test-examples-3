package ru.yandex.market.mbo.redis.categories;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters.Category;
import ru.yandex.market.mbo.export.MboParameters.Word;
import ru.yandex.market.mbo.redis.common.model.RedisObject;
import ru.yandex.market.mbo.redis.common.service.RedisCache;
import ru.yandex.market.mbo.redis.common.service.RedisObjectType;
import ru.yandex.market.mbo.redis.common.service.RedisReader;
import ru.yandex.market.mbo.redis.common.service.RedisWriter;
import ru.yandex.market.mbo.redis.common.service.repo.RedisRepository;
import ru.yandex.market.mbo.redis.common.service.repo.RedisRepositoryMock;
import ru.yandex.market.mbo.utils.RandomTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Данный тест, хоть и использует Category как объект для RedisCache, но тестирует вне зависимости от этого.
 *
 * @author n-mago
 * @date 07.02.2020
 */
public class CategoryRedisCacheReloadTest {

    private RedisCache<Category, Category> redisCache;
    private CategoryRedisWriter writer;
    private EnhancedRandom random;

    private static final long SEED = 1;
    private static final int DEFAULT_CACHE_SIZE = 100;
    private static final int DEFAULT_TTL = 1; // 1 millisecond
    private static final int LOW_TIMESTAMP = 50;
    private static final int HIGH_TIMESTAMP = 200;

    @Before
    public void setUp() {
        RedisRepository redisRepository = new RedisRepositoryMock();
        redisCache = createRedisCache(redisRepository);
        writer = new CategoryRedisWriter(
            new RedisWriter<>(redisRepository, RedisObjectType.CATEGORY_PROTO, new CategoryRedisDataConverter()));
        random = RandomTestUtils.createNewRandom(SEED);
    }

    @Test
    public void testNoValueAtAll() {
        Optional<RedisObject<Category>> noSuchValue = redisCache.loadObjectIfNeeded("no-value-key",
            Optional.empty());
        assertThat(noSuchValue).isEqualTo(Optional.empty());
    }

    @Test
    public void testInitialLoadingValue() {
        Category oldVal = generateCategory();
        final String key = String.valueOf(oldVal.getHid());
        writer.putCategory(HIGH_TIMESTAMP, oldVal);
        // case: no old value at all
        Optional<RedisObject<Category>> gotValue1 = redisCache.loadObjectIfNeeded(key, Optional.empty());
        assertThat(gotValue1).isEqualTo(wrap(HIGH_TIMESTAMP, oldVal));
    }

    @Test
    public void testReloadingValue() {
        Category oldVal = generateCategory();
        Category newVal = newCategoryWithSameHid(oldVal);
        final String key = String.valueOf(oldVal.getHid());
        writer.putCategory(HIGH_TIMESTAMP, newVal);
        // case: timestamp of new value is the same (hence, should not reload)
        Optional<RedisObject<Category>> gotValue2 = redisCache.loadObjectIfNeeded(key,
            wrap(HIGH_TIMESTAMP, oldVal));
        assertThat(gotValue2).isEqualTo(wrap(HIGH_TIMESTAMP, oldVal));
        // case: reload value when timestamp of old value is low
        Optional<RedisObject<Category>> loadedVal = redisCache.loadObjectIfNeeded(key, wrap(LOW_TIMESTAMP, oldVal));
        Optional<RedisObject<Category>> newValWrapped = wrap(HIGH_TIMESTAMP, newVal);
        assertThat(loadedVal).isEqualTo(newValWrapped);
    }

    @Test
    public void testNewValueIsEmpty() {
        Category oldVal = generateCategory();
        final String key = String.valueOf(oldVal.getHid());
        Optional<RedisObject<Category>> oldValWrapped = wrap(LOW_TIMESTAMP, oldVal);
        Optional<RedisObject<Category>> loadedVal = redisCache.loadObjectIfNeeded(key, oldValWrapped);
        assertThat(loadedVal).isEqualTo(Optional.empty());
    }

    private Category generateCategory() {
        return Category.newBuilder()
            .setHid(random.nextLong())
            .addName(Word.newBuilder()
                .setName(random.nextObject(String.class))
                .build())
            .build();
    }

    private Category newCategoryWithSameHid(Category category) {
        String oldName = category.getNameList().stream().findFirst().get().toBuilder().getName();
        return Category.newBuilder(category)
            .clearName()
            .addName(Word.newBuilder()
                .setName(oldName + "_after")
                .build())
            .build();
    }

    private RedisCache<Category, Category> createRedisCache(RedisRepository redisRepository) {
        // our transformer transforms the object as is (c -> c)
        RedisReader<Category, Category> redisReader = new RedisReader<>(redisRepository,
            RedisObjectType.CATEGORY_PROTO, new CategoryRedisDataConverter(), c -> c);
        return new RedisCache<>(
            "CategoryRedisCacheReloadTest::redisCache",
            redisReader,
            DEFAULT_CACHE_SIZE,
            DEFAULT_TTL
        );
    }

    private Optional<RedisObject<Category>> wrap(long timestamp, Category category) {
        return Optional.of(new RedisObject<>(timestamp, category));
    }

}

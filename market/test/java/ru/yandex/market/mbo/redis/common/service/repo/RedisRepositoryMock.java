package ru.yandex.market.mbo.redis.common.service.repo;

import ru.yandex.market.mbo.redis.common.exceptions.ObjectInCacheTooOldException;
import ru.yandex.market.mbo.redis.common.model.RedisObject;
import ru.yandex.market.mbo.redis.common.service.utils.RedisDataConverter;
import ru.yandex.market.mbo.redis.common.service.utils.RedisUtils;
import ru.yandex.market.mbo.utils.RetryHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("checkstyle:magicnumber")
public class RedisRepositoryMock implements RedisRepository {

    private Map<String, byte[]> redis;
    private int retryCount;
    private int sleepMs;

    public RedisRepositoryMock() {
        this.redis = new HashMap<>();
        this.retryCount = 3;
        this.sleepMs = 100;
    }

    @Override
    public <V> Optional<RedisObject<V>> get(String prefix, String key, RedisDataConverter<V> converter) {
        Long ts = getTsFromRedis(prefix, key);
        V data = getDataFromRedis(prefix, key, converter);
        return ts == null || data == null ? Optional.empty() : Optional.of(new RedisObject<>(ts, data));
    }

    @Override
    public Optional<Long> getTimestamp(String prefix, String key) {
        return Optional.ofNullable(getTsFromRedis(prefix, key));
    }

    @Override
    public <V> Optional<RedisObject<V>> getWithTimestamp(String prefix, String key,
                                                         long expectedTimestamp, RedisDataConverter<V> converter) {
        AtomicInteger attempt = new AtomicInteger();
        return RetryHelper.retry("getWithTimestamp(" + key + ", " + expectedTimestamp + ")", retryCount,
            e -> e instanceof ObjectInCacheTooOldException, sleepMs,
            () -> {
                Long currentTimestamp = getTsFromRedis(prefix, key);
                V data = getDataFromRedis(prefix, key, converter);
                if (data == null || currentTimestamp == null || currentTimestamp < expectedTimestamp) {
                    return checkAttemptAndReturnEmpty(attempt, key, expectedTimestamp, currentTimestamp);
                }
                return Optional.of(new RedisObject<>(currentTimestamp, data));
            });
    }

    @Override
    public Map<String, Long> getAllTimestamps(String prefix) {
        return redis.entrySet().stream()
            .filter(e -> RedisUtils.isTimestampKey(prefix, e.getKey()))
            .collect(Collectors.toMap(
                e -> RedisUtils.getOriginalKeyFromTimestampKey(prefix, e.getKey()),
                e -> RedisUtils.deserializeTimestamp(e.getValue())));
    }

    @Override
    public void del(String prefix, String key) {
        redis.remove(RedisUtils.getTimestampKey(prefix, key));
        redis.remove(RedisUtils.getDataKey(prefix, key));
    }

    private <V> Optional<RedisObject<V>> checkAttemptAndReturnEmpty(AtomicInteger attempt, String key,
                                                                    long expectedTimestamp, Long currentTimestamp) {
        if (attempt.get() < retryCount - 1) {
            attempt.incrementAndGet();
            throw new ObjectInCacheTooOldException(key, expectedTimestamp, currentTimestamp);
        }
        return Optional.empty();
    }

    @Override
    public <V> long set(String prefix, String key, RedisObject<V> redisObject, RedisDataConverter<V> converter) {
        Long currentTimestamp = getTsFromRedis(prefix, key);
        if (currentTimestamp != null && currentTimestamp >= redisObject.getTimestamp()) {
            return currentTimestamp;
        } else {
            redis.put(RedisUtils.getTimestampKey(prefix, key),
                RedisUtils.serializeTimestamp(redisObject.getTimestamp()));
            redis.put(RedisUtils.getDataKey(prefix, key),
                RedisUtils.serializeData(redisObject.getData(), converter));
            return redisObject.getTimestamp();
        }
    }

    private Long getTsFromRedis(String prefix, String key) {
        return RedisUtils.deserializeTimestamp(redis.get(RedisUtils.getTimestampKey(prefix, key)));
    }

    private <V> V getDataFromRedis(String prefix, String key, RedisDataConverter<V> converter) {
        return RedisUtils.deserializeData(redis.get(RedisUtils.getDataKey(prefix, key)), converter);
    }
}

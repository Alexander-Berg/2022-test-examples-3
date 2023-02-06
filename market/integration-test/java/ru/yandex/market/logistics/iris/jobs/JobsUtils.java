package ru.yandex.market.logistics.iris.jobs;

import java.lang.reflect.Field;
import java.util.Arrays;

import ru.yandex.market.logistics.iris.jobs.cache.SourceCache;
import ru.yandex.market.logistics.iris.jobs.cache.SourceRetrievalInfo;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.init.QueueExecutionPool;
import ru.yandex.money.common.dbqueue.settings.QueueId;
import ru.yandex.money.common.dbqueue.spring.SpringQueueInitializer;

/**
 * Класс для работы с внутренними механизмами такими как db-queue и SourceCache, к которым нет доступа по открытому API.
 */
public class JobsUtils {

    private JobsUtils() {

    }

    /**
     * Вызывает метод wakeup для очереди с указанным типом, заставяя ее обработать следующую задачу,
     * но только, если очередь была в режиме ожидания (последний раз задача не была найден и не было обработки).
     * @param type тип очереди, что нужно пробудить.
     * @param initializer это нужно заинжектить и передать сюда, это объект,
     *                   с которого начинаем рефлективный спуск к методу wakeup
     */
    public static void reflectiveQueueTrigger(QueueType type, SpringQueueInitializer initializer) {
        Field executionPool = Arrays.stream(SpringQueueInitializer.class.getDeclaredFields())
            .filter(f -> QueueExecutionPool.class.equals(f.getType()))
            .findFirst()
            .map(f -> {
                f.setAccessible(true);
                return f;
            })
            .orElseThrow(() -> new RuntimeException("Can not find QueueExecutionPool"));
        try {
            QueueExecutionPool pool = (QueueExecutionPool) executionPool.get(initializer);
            pool.wakeup(new QueueId(type.name()), new QueueShardId(type.name()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing executionPool in our initializer bean");
        }
    }

    /**
     * Очищает закэшированные Source-ы.
     * @param injected сервис у которого будем очищать кэш.
     */
    public static void reflectiveCacheEvict(SourceRetrievalService injected) {
        Field cacheField = getFieldOfClassFromClass(SourceCache.class, SourceRetrievalService.class);
        Field cachedInfoField =
            getFieldOfClassFromClass(SourceRetrievalInfo.class, SourceCache.class);
        try {
            cacheField.setAccessible(true);
            cachedInfoField.setAccessible(true);
            SourceCache cache = (SourceCache) cacheField.get(injected);
            cachedInfoField.set(cache, null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing cache field");
        }
    }

    private static Field getFieldOfClassFromClass(Class<?> fieldClass, Class<?> lookupClass) {
        return Arrays.stream(lookupClass.getDeclaredFields())
            .filter(f -> f.getType().equals(fieldClass))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Error finding cache field, maybe test is irrelevant?"));
    }
}

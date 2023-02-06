package ru.yandex.market.markup2.utils;

import ru.yandex.market.markup2.workflow.general.AbstractTaskDataItemPayload;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.FieldDefinitionBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

/**
 * @author Alexander Kramarev (pochemuto@gmail.com)
 * @date 19.07.2018
 */
public class TaskDataItemPayloadRandomizer<T> implements Randomizer<T> {

    private final Class<T> payloadClass;
    private final EnhancedRandom random;

    private TaskDataItemPayloadRandomizer(Class<T> payloadClass, long seed) {
        this.payloadClass = payloadClass;
        this.random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).build();
    }

    public static FieldDefinition<?, ?> field() {
        return FieldDefinitionBuilder.field()
            .named("dataIdentifier")
            .ofType(Object.class)
            .inClass(AbstractTaskDataItemPayload.class).get();
    }

    public static <T> TaskDataItemPayloadRandomizer<T> create(Class<T> payloadClass, long seed) {
        return new TaskDataItemPayloadRandomizer<>(payloadClass, seed);
    }

    @Override
    public T getRandomValue() {
        return random.nextObject(payloadClass);
    }
}

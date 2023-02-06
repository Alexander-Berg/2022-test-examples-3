package ru.yandex.market.mbo.db.modelstorage.health;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

/**
 * @author anmalysh
 */
public class OperationStatsTest {

    private EnhancedRandom enhancedRandom = new EnhancedRandomBuilder()
        .seed(29883)
        .stringLengthRange(0, 100)
        .build();

    @Test
    public void testToMapReturnsAllFields() throws IllegalAccessException {
        OperationStats stats = new OperationStats();
        Map<String, Object> initial = stats.toMap();
        updateFieldsAndAssertChanged(stats, initial, stats);
    }

    private <T> void updateFieldsAndAssertChanged(T object,
                                                  Map<String, Object> initial,
                                                  OperationStats stats) throws IllegalAccessException {
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                if (!Modifier.isFinal(field.getModifiers())) {
                    field.setAccessible(true);
                    Object oldValue = field.get(object);
                    field.set(object, enhancedRandom.nextObject(field.getType()));
                    assertNotEquals(initial, stats.toMap());
                    field.set(object, oldValue);
                }
            } else {
                updateFieldsAndAssertChanged(field.get(object), initial, stats);
            }
        }
    }
}

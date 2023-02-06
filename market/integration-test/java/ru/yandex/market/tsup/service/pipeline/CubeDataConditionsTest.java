package ru.yandex.market.tsup.service.pipeline;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.CubeData;

public class CubeDataConditionsTest extends AbstractContextualTest {

    private static Set<Class<? extends CubeData>> cubeDataImplementations;

    @BeforeAll
    static void init() {
        Reflections reflections = new Reflections("ru/yandex/market/tsup/core/pipeline/data");
        cubeDataImplementations = reflections.getSubTypesOf(CubeData.class);
    }

    @Test
    void hasDefaultConstructorRule() {
        cubeDataImplementations.forEach(this::checkDefaultConstructor);
    }

    private void checkDefaultConstructor(Class<? extends CubeData> dataClass) {
        try {
            dataClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(String.format("No default constructor for class %s", dataClass));
        }
    }

}

package ru.yandex.chemodan.test;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.function.Function0;
import ru.yandex.commune.random.RandomValueGenerator;
import ru.yandex.misc.reflection.ClassX;

/**
 * @author akirakozov
 */
public class CustomRandomValueGenerator extends RandomValueGenerator {
    private final MapF<ClassX<Object>, Function0<Object>> customGenerators;

    public CustomRandomValueGenerator(boolean allowAnyClassRandom,
            MapF<ClassX<Object>, Function0<Object>> customGenerators)
    {
        super(allowAnyClassRandom);
        this.customGenerators = customGenerators;
    }

    @Override
    protected Object randomValueImpl(ClassX<Object> type) {
        if (customGenerators.containsKeyTs(type)) {
            return customGenerators.getTs(type).apply();
        } else {
            return super.randomValueImpl(type);
        }
    }
}

package ru.yandex.market.jmf.attributes.test.bool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;

abstract public class AbstractBooleanAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.booleanValue();
    }

    /**
     * Проверяем, что в базе находим entity по значению атрибута с использованием фильтра IN
     */
    @Override
    @ParameterizedTest
    @MethodSource("countOfElements")
    public void filterIn(int countOfElements) {
        var values = new ArrayList<>();
        for (var i = 0; i < countOfElements; i++) {
            values.add(randomAttributeValue());
        }

        createPersistedEntities(values);

        List<Entity> resultPositive = filter(Filters.in(attributeCode, Collections.singletonList(Boolean.TRUE)));
        List<Entity> resultNegative = filter(Filters.in(attributeCode, Collections.singletonList(Boolean.FALSE)));
        List<Entity> resultFull = filter(Filters.in(attributeCode, Arrays.asList(Boolean.TRUE, Boolean.FALSE)));

        Assertions.assertEquals(countOfElements, resultPositive.size() + resultNegative.size(), "Сумма найденных " +
                "объектов для " +
                "true и false должна равняться количеству всех объектов");
        Assertions.assertEquals(countOfElements, resultFull.size(), "количество найденных элементов соответствующих " +
                "true или " +
                "false должны равняться количеству всех объектов");
    }
}

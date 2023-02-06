package ru.yandex.market.ff4shops.dbqueue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.commons.util.ReflectionUtils;

import ru.yandex.market.ff4shops.dbqueue.dto.QueueTaskPayload;

import static org.assertj.core.api.Assertions.assertThat;

public class DbQueuePayloadTest {

    @Test
    public void testEveryPayloadHasEmptyConstructor() {
        List<Class<?>> classes = ReflectionUtils.findAllClassesInPackage("ru.yandex.market.ff4shops.dbqueue.dto",
                ClassFilter.of(QueueTaskPayload.class::isAssignableFrom))
                .stream().filter(cl -> cl != QueueTaskPayload.class)
                .collect(Collectors.toList());

        //Каждый класс QueueTaskPayload должен иметь пустой конструктор для нормальной сериализации
        assertThat(classes).allMatch(this::hasEmptyConstructor);
    }

    private boolean hasEmptyConstructor(Class<?> cl) {
        return !ReflectionUtils.findConstructors(cl, cons -> cons.getParameterCount() == 0).isEmpty();
    }

}

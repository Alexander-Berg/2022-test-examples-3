package ru.yandex.market.tsup;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.reflections.Reflections;

import ru.yandex.market.tpl.common.db.queue.base.QueuePayloadDto;
import ru.yandex.market.tsup.dbqueue.base.QueueType;

public class QueueTypeTest {

    @Test
    public void testAllPayloadsClassToEnumNoExcept() {
        Reflections reflections = new Reflections("ru/yandex/market/tsup/dbqueue");
        Set<Class<? extends QueuePayloadDto>> queuePayloadDtoClasses =
            new HashSet<>(reflections.getSubTypesOf(QueuePayloadDto.class));
        queuePayloadDtoClasses.forEach(QueueType::ofClass);
    }
}

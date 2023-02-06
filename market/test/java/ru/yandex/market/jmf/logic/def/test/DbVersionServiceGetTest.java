package ru.yandex.market.jmf.logic.def.test;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.jmf.entity.Entity;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DbVersionServiceGetTest extends AbstractDbVersionServiceTest {

    private static Stream<Arguments> testGetParameters() {
        return Stream.of(
                arguments("atLastEditTime", LAST_EDIT_TIME, entityValues.lastEditValue, false),
                arguments("atFirstEditTime", FIRST_EDIT_TIME, entityValues.firstEditValue, false),
                arguments("atCreateTime", CREATE_TIME, entityValues.createValue, true)
        );
    }

    @Test
    void testGetDefault() {
        Entity entity = dbService.get(entityGid);
        Assertions.assertEquals(entityValues.lastEditValue, entity.getAttribute(VERSIONED_STRING_ATTRIBUTE),
                "Текущее значение атрибута не верно");
    }

    @Test
    void testGetBefore() {
        Entity entity = dbVersionService.get(entityGid, BEFORE_TIME);
        Assertions.assertNull(entity,
                "Сущность еще не была создана");
    }

    @Test
    void testGetAfter() {
        Entity entity = dbVersionService.get(entityGid, AFTER_TIME);
        Assertions.assertEquals(entityValues.lastEditValue, entity.getAttribute(VERSIONED_STRING_ATTRIBUTE),
                "Последнее значение не верно");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testGetParameters")
    void testGet(String testName, OffsetDateTime atTime, String expectedAttributeValue, Boolean expectedNullEntity) {
        Entity beforeTimeEntity = dbVersionService.get(entityGid, atTime.minusHours(1));
        if (expectedNullEntity) {
            Assertions.assertNull(beforeTimeEntity,
                    "Сущность еще не была создана");
        } else {
            Assertions.assertNotEquals(expectedAttributeValue,
                    beforeTimeEntity.getAttribute(VERSIONED_STRING_ATTRIBUTE),
                    "Значение перед редактированием не верно");
        }

        Entity atTimeEntity = dbVersionService.get(entityGid, atTime);
        Assertions.assertEquals(expectedAttributeValue, atTimeEntity.getAttribute(VERSIONED_STRING_ATTRIBUTE),
                "Значение в момент редактирования не верно");

        Entity afterTimeEntity = dbVersionService.get(entityGid, atTime.plusHours(1));
        Assertions.assertEquals(expectedAttributeValue, afterTimeEntity.getAttribute(VERSIONED_STRING_ATTRIBUTE),
                "Значение после редактирования не верно");
    }
}

package ru.yandex.direct.grid.processing.service.group.converter;

import java.lang.reflect.Field;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.reflections.ReflectionUtils;

import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class AdGroupDataConverterTest {

    /**
     * Проверяем что в модель GdAdGroupFilter не было добавленно новых полей
     * <p>
     * Если поля были добавленны и по ним происходит фильтрация групп в коде, то нужно не забыть их учесть в методе
     * {@link ru.yandex.direct.grid.processing.service.group.converter.AdGroupDataConverter#hasAnyCodeFilter}
     */
    @Test
    public void testForNewFilterFields() {
        String[] expectFieldsName = {"adGroupIdIn", "adGroupIdNotIn", "adGroupIdContainsAny", "campaignIdIn",
                "typeIn", "primaryStatusIn", "nameContains", "archived", "nameIn", "nameNotContains", "nameNotIn",
                "mwPackNameContains", "mwPackNameNotContains", "stats", "goalStats", "recommendations",
                "libraryMwIdIn", "showConditionTypeIn", "tagIdIn", "reasonsContainSome"};

        @SuppressWarnings("unchecked")
        Set<Field> fields = ReflectionUtils.getAllFields(GdAdGroupFilter.class);

        Set<String> actualFieldsName = StreamEx.of(fields)
                .map(Field::getName)
                .filter(fieldName -> !Character.isUpperCase(fieldName.charAt(0)))
                .toSet();

        assertThat(actualFieldsName)
                .as("Нет новых полей в GdAdGroupFilter")
                .containsExactlyInAnyOrder(expectFieldsName);
    }
}

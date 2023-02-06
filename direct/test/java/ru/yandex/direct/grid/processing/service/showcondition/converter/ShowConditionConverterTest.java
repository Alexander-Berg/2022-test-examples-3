package ru.yandex.direct.grid.processing.service.showcondition.converter;

import java.lang.reflect.Field;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.reflections.ReflectionUtils;

import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class ShowConditionConverterTest {

    /**
     * Проверяем что в модель GdShowConditionFilter не было добавленно новых полей
     * <p>
     * Если были измененны поля и по ним происходит фильтрация условий показа в коде, то нужно учесть их в методе
     * {@link ru.yandex.direct.grid.processing.service.showcondition.keywords.ShowConditionDataService#getShowConditions}
     */
    @Test
    public void testForNewFilterFields() {
        String[] expectFieldsName = {"showConditionIdIn", "showConditionIdNotIn", "campaignIdIn", "adGroupIdIn",
                "keywordContains", "keywordIn", "keywordNotContains", "keywordNotIn", "stats", "goalStats",
                "keywordWithoutMinusWordsContains", "minusWordsContains", "minPrice", "maxPrice", "minPriceContext",
                "maxPriceContext", "autobudgetPriorityIn", "statusIn", "showConditionStatusIn", "typeIn",
                "reasonsContainSome"};

        @SuppressWarnings("unchecked")
        Set<Field> fields = ReflectionUtils.getAllFields(GdShowConditionFilter.class);

        Set<String> actualFieldsName = StreamEx.of(fields)
                .map(Field::getName)
                .filter(fieldName -> !Character.isUpperCase(fieldName.charAt(0)))
                .toSet();

        assertThat(actualFieldsName)
                .as("Нет новых полей в GdShowConditionFilter")
                .containsExactlyInAnyOrder(expectFieldsName);
    }
}









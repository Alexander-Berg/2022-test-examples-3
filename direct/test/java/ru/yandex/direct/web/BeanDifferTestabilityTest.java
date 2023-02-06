package ru.yandex.direct.web;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import one.util.streamex.StreamEx;
import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reflections.ReflectionUtils;

import ru.yandex.direct.web.entity.keyword.model.AddAdGroupMinusKeywordsRequestItem;
import ru.yandex.direct.web.entity.keyword.model.AddAdGroupMinusKeywordsResultItem;
import ru.yandex.direct.web.entity.keyword.model.AddCampaignMinusKeywordsRequestItem;
import ru.yandex.direct.web.entity.keyword.model.AddCampaignMinusKeywordsResultItem;
import ru.yandex.direct.web.entity.keyword.model.AddMinusKeywordsRequest;
import ru.yandex.direct.web.entity.keyword.model.AddMinusKeywordsResponse;
import ru.yandex.direct.web.entity.keyword.model.AddMinusKeywordsResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;

/**
 * Тест проверяет, что указанные бины могут сравниваться с помощью бин-диффера.
 * <p>
 * Он страхует от того, что если кто-то уберет неиспользуемые в коде геттеры, некоторые
 * тесты, которые используют для сравнения бинов BeanDiffer, молча перестанут что-либо проверять.
 */
@RunWith(Parameterized.class)
public class BeanDifferTestabilityTest {

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {AddMinusKeywordsRequest.class},
                {AddCampaignMinusKeywordsRequestItem.class},
                {AddAdGroupMinusKeywordsRequestItem.class},
                {AddMinusKeywordsResponse.class},
                {AddMinusKeywordsResult.class},
                {AddCampaignMinusKeywordsResultItem.class},
                {AddAdGroupMinusKeywordsResultItem.class},
        });
    }

    @Parameterized.Parameter
    public Class<?> beanClass;

    @Test
    public void beanHasGetters() {
        Map<String, PropertyDescriptor> propertyDescriptorMap =
                StreamEx.of(PropertyUtils.getPropertyDescriptors(beanClass))
                        .mapToEntry(Function.identity())
                        .mapKeys(PropertyDescriptor::getName)
                        .toMap();

        @SuppressWarnings("unchecked")
        Set<Field> fields = ReflectionUtils.getAllFields(beanClass);

        Predicate<Field> fieldIsNotStatic = f -> !Modifier.isStatic(f.getModifiers());

        Predicate<String> fieldHasGetter = fieldName ->
                propertyDescriptorMap.get(fieldName) != null &&
                        propertyDescriptorMap.get(fieldName).getReadMethod() != null;

        List<String> fieldsWithoutGetters = StreamEx.of(fields)
                .filter(fieldIsNotStatic)
                .map(Field::getName)
                .remove(fieldHasGetter)
                .toList();

        String errorMessage = String.format("Бин %s имеет поля, не имеющие геттеров, что не позволяет "
                + "тестировать их с помощью BeanDifferMatcher", beanClass.getCanonicalName());

        assertThat(errorMessage, fieldsWithoutGetters, emptyIterable());
    }
}

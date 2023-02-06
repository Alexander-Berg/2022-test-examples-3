package ru.yandex.market.loyalty.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import ru.yandex.market.loyalty.api.model.EnumWithPermanentCode;
import ru.yandex.market.loyalty.api.model.WithDescription;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

public class EnumWithPermanentCodeTest {
    @Test
    public void enumWithPermanentCodeShouldExtendWithDescription() {
        List<? extends Class<?>> enumsWithoutDescription = getAllEnums()
            .stream()
            .filter(aClass -> !WithDescription.class.isAssignableFrom(aClass))
            .collect(Collectors.toList());

        assertThat(enumsWithoutDescription, is(empty()));
    }

    @Test
    public void enumWithPermanentCodeShouldReturnUnknownValue() {
        List<? extends Class<?>> enumsWithoutDefaultField = getAllEnums()
            .stream()
            .filter(aClass -> {
                EnumWithPermanentCode unknown = (EnumWithPermanentCode) makeExceptionsUnchecked(() ->
                    aClass.getMethod("findByCode", String.class).invoke(null, UUID.randomUUID().toString())
                );
                if (unknown == null) {
                    return true;
                } else {
                    Object unknownEnum = makeExceptionsUnchecked(() -> aClass.getField("UNKNOWN").get(null));
                    return unknown != unknownEnum;
                }
            })
            .collect(Collectors.toList());

        assertThat(enumsWithoutDefaultField, is(empty()));
    }


    @Test
    public void enumWithPermanentCodeShouldHaveJsonCreator() {
        List<? extends Class<?>> enumsWithoutJsonCreator = getAllEnums()
            .stream()
            .filter(aClass -> {
                    Method findByCode = makeExceptionsUnchecked(() -> aClass.getMethod("findByCode", String.class));
                    return findByCode.getAnnotation(JsonCreator.class) == null;
                }
            )
            .collect(Collectors.toList());

        assertThat(enumsWithoutJsonCreator, is(empty()));
    }

    @Test
    public void enumWithPermanentCodeShouldHaveJsonValue() {
        List<? extends Class<?>> enumsWithoutJsonValue = getAllEnums()
            .stream()
            .filter(aClass -> {
                    Method getCode = makeExceptionsUnchecked(() -> aClass.getMethod("getCode"));
                    return getCode.getAnnotation(JsonValue.class) == null;
                }
            )
            .collect(Collectors.toList());

        assertThat(enumsWithoutJsonValue, is(empty()));
    }

    private static Set<? extends Class<?>> getAllEnums() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(EnumWithPermanentCode.class));
        return provider.findCandidateComponents("ru.yandex.market.loyalty.api").stream()
            .map(BeanDefinition::getBeanClassName)
            .map(makeExceptionsUnchecked(Class::forName))
            .collect(Collectors.toSet());
    }
}

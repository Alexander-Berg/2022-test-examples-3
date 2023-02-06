package ru.yandex.market.delivery.transport_manager.service.core;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.test.util.AopTestUtils;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

class TmEventPublisherTest extends AbstractContextualTest {

    private static final Set<Class<?>> WHITE_LIST = Set.of(TmEventPublisher.class, JdbcAggregateTemplate.class);
    private static final String FAIL_MESSAGE = "Found classes %s with injected ApplicationEventPublisher bean. " +
        "Use TmEventPublisher instead.";

    @Autowired
    ApplicationContext applicationContext;

    @Test
    @DisplayName("Проверка на использование TmEventPublisher в коде вместо ApplicationEventPublisher")
    void checkOnlyOneInjection() {

        List<String> classNames = StreamEx.of(applicationContext.getBeanDefinitionNames())
            .map(applicationContext::getBean)
            .map(AopTestUtils::getUltimateTargetObject)
            .map(this::extractClassFromBean)
            .remove(WHITE_LIST::contains)
            .filter(type -> getFieldTypes(type).contains(ApplicationEventPublisher.class))
            .map(Class::getSimpleName)
            .toList();

        softly.assertThat(classNames)
            .withFailMessage(FAIL_MESSAGE, classNames)
            .isEmpty();
    }

    private Class<?> extractClassFromBean(Object bean) {
        return MockUtil.isMock(bean) ?
            MockUtil.getMockSettings(bean).getTypeToMock() :
            bean.getClass();
    }

    private Set<Class<?>> getFieldTypes(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
            .map(Field::getType)
            .collect(Collectors.toSet());
    }
}

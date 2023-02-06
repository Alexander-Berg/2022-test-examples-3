package ru.yandex.market.logistics.lrm.model.entity;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import ru.yandex.market.logistics.lrm.LrmTest;

@DisplayName("Проверка корректности настроек версионирования")
class ReturnAnnotationsVersioningTest extends LrmTest {
    private static final Set<String> EXCLUSIONS = Set.of(
        ReturnStatusHistoryEntity.class.getName(),
        ReturnEventEntity.class.getName()
    );

    @DisplayName("Все нужные классы реализуют интерфейс RootAware")
    @ParameterizedTest
    @MethodSource("returnEntityChildren")
    void returnEntityChildrenCheck(Class<?> clazz) {
        softly.assertThat(RootAware.class).isAssignableFrom(clazz);
    }

    @Nonnull
    private static Stream<Arguments> returnEntityChildren() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        Set<Class<?>> childEntityClasses = new HashSet<>();
        String currentPackage = ReturnAnnotationsVersioningTest.class.getPackageName();
        for (BeanDefinition entity : provider.findCandidateComponents(currentPackage)) {
            if (EXCLUSIONS.contains(entity.getBeanClassName())) {
                continue;
            }

            Class<?> clazz = Class.forName(entity.getBeanClassName());

            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType().equals(ReturnEntity.class)
                    && (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class))) {
                    childEntityClasses.add(clazz);
                    break;
                }
            }
        }

        return childEntityClasses.stream()
            .sorted(Comparator.comparing(Class::getName))
            .map(Arguments::of);
    }
}

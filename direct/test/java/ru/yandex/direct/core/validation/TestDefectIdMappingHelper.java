package ru.yandex.direct.core.validation;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import ru.yandex.direct.validation.result.DefectId;

@ParametersAreNonnullByDefault
public final class TestDefectIdMappingHelper {
    private TestDefectIdMappingHelper() {
    }

    public static <P> void assertAllDefectIdIsMappedToPresentation(Function<DefectId, P> defectToPresentation) {
        try {
            SoftAssertions assertions = new SoftAssertions();
            ClassPathScanningCandidateComponentProvider provider =
                    new ClassPathScanningCandidateComponentProvider(false);
            provider.addIncludeFilter(new AssignableTypeFilter(DefectId.class));
            provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*TestDefectIds")));
            Set<BeanDefinition> beans = provider.findCandidateComponents("ru.yandex.direct");
            for (BeanDefinition bean : beans) {
                Class beanClass = Class.forName(bean.getBeanClassName());
                if (beanClass.isEnum()) {
                    for (Object o : beanClass.getEnumConstants()) {
                        assertions.assertThat(defectToPresentation.apply((DefectId) o))
                                .as(bean.getBeanClassName() + "." + o)
                                .isNotNull();
                    }
                }
            }
            assertions.assertAll();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertAllDefectIdsAreMappedToPresentations(
            Predicate<DefectId<?>> defectRegistrationPredicate) {
        try {
            SoftAssertions assertions = new SoftAssertions();
            ClassPathScanningCandidateComponentProvider provider =
                    new ClassPathScanningCandidateComponentProvider(false);
            provider.addIncludeFilter(new AssignableTypeFilter(DefectId.class));
            provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*TestDefectIds")));
            Set<BeanDefinition> beans = provider.findCandidateComponents("ru.yandex.direct");
            for (BeanDefinition bean : beans) {
                Class beanClass = Class.forName(bean.getBeanClassName());
                if (beanClass.isEnum()) {
                    for (Object o : beanClass.getEnumConstants()) {
                        assertions.assertThat(defectRegistrationPredicate.test((DefectId) o))
                                .as(bean.getBeanClassName() + "." + o)
                                .isEqualTo(true);
                    }
                }
            }
            assertions.assertAll();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

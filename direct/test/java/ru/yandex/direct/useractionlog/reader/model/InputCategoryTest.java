package ru.yandex.direct.useractionlog.reader.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

@ParametersAreNonnullByDefault
public class InputCategoryTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    /**
     * Все значения {@link OutputCategory} должны быть достижимы из {@link InputCategory}.
     */
    @Test
    public void allOutputCategoriesReachableFromInputCategories() {
        Set<String> fromInputCategoriesNames = Arrays.stream(InputCategory.values())
                .map(InputCategory::toOutputCategories)
                .flatMap(Collection::stream)
                .map(Enum::name)
                .collect(ImmutableSet.toImmutableSet());
        Set<String> outputCategoriesNames = Arrays.stream(OutputCategory.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        softly.assertThat(outputCategoriesNames)
                .isEqualTo(fromInputCategoriesNames);
    }
}

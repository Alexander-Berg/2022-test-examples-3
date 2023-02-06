package ru.yandex.market.fintech.banksint.service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.yt.model.Category;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


class CategoryServiceTest extends FunctionalTest {
    @Autowired
    private CategoryService categoryService;

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedCategoryFilteringTestData")
    public void testCategoryFiltering(
            String name,
            Set<Long> selectedCategories,
            Set<Long> expectedResult
    ) {
        assertThat(categoryService.reduceSelectedCategories(selectedCategories)).isEqualTo(expectedResult);
    }

    /*
    - Все товары (90401)
      - Книги (90829)
        - Компьютеры и интернет (90867)
          - Базы данных (90869)
          - Языки программирования (90868)
          - Графика, дизайн, CAD (90871)
        - Словари, справочники, энциклопедии (90945)
          - Справочные издания (90947)
          - Словари и разговорники (90946)
          - Путеводители (90949)
          - Толковые словари (90948)
          - Разное (90951)
          - Энциклопедии (90950)
     */
    public static Stream<Arguments> parameterizedCategoryFilteringTestData() {
        return Stream.of(
                Arguments.of(
                        "Компьютеры и интернет (90867)",
                        Set.of(90867L, 90869L, 90868L, 90871L),
                        Set.of(90867L)
                ),
                Arguments.of(
                        "Компьютеры и интернет (90867) и Словари, справочники, энциклопедии (90945)",
                        Set.of(90867L, 90869L, 90868L, 90871L,
                                90945L, 90947L, 90946L, 90949L, 90948L, 90951L, 90950L),
                        Set.of(90867L, 90945L)
                ),
                Arguments.of(
                        "Компьютеры и интернет (90867L), кроме графики(90871)",
                        Set.of(90867L, 90869L, 90868L),
                        Set.of(90869L, 90868L)
                ),
                Arguments.of(
                        "Книги, кроме Разное (90951)",
                        Set.of(90829L,
                                90867L, 90869L, 90868L, 90871L,
                                90945L, 90947L, 90946L, 90949L, 90948L, 90950L),
                        Set.of(90867L, 90947L, 90946L, 90949L, 90948L, 90950L)
                ),
                Arguments.of(
                        "Null safe",
                        null,
                        Collections.emptySet()
                ),
                Arguments.of(
                        "unknown category",
                        Set.of(90867L, 90869L, 90868L, 90871L, 9999L),
                        Set.of(90867L)
                )
        );
    }

    @Test
    public void getCategoryTest() {
        assertThat(categoryService.getCategories(Set.of(90867L, 90947L)).stream()
                .map(Category::getCategoryId)
                .collect(Collectors.toSet()))
                .isEqualTo(Set.of(90867L, 90947L));
    }
}

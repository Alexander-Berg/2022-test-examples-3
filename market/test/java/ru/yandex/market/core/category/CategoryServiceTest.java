package ru.yandex.market.core.category;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.category.model.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.core.category.matcher.CategoryMatcher.hasCategory;
import static ru.yandex.market.core.category.matcher.CategoryMatcher.hasHyperId;
import static ru.yandex.market.core.category.matcher.CategoryMatcher.hasParentId;

/**
 * Тесты для {@link CategoryService}
 */
@DbUnitDataSet(before = "db/CategoryService.before.csv")
class CategoryServiceTest extends FunctionalTest {

    @Autowired
    private CategoryService categoryService;


    static Stream<Arguments> childrenByHyperIdArgs() {
        return Stream.of(
                Arguments.of(
                        "Все товары",
                        90401L,
                        Matchers.contains(Arrays.asList(
                                allOf(
                                        hasCategory("Одежда, обувь и аксессуары"),
                                        hasHyperId(1L),
                                        hasParentId(90401L)
                                ),
                                allOf(
                                        hasCategory("Мужская одежда"),
                                        hasHyperId(11L),
                                        hasParentId(1L)
                                ),
                                allOf(
                                        hasCategory("Обувь"),
                                        hasHyperId(12L),
                                        hasParentId(1L)
                                ),
                                allOf(
                                        hasCategory("Продукты"),
                                        hasHyperId(2L),
                                        hasParentId(90401L)
                                )
                        ))
                ),
                Arguments.of(
                        "Категории поддерева",
                        1L,
                        Matchers.contains(Arrays.asList(
                                allOf(
                                        hasCategory("Мужская одежда"),
                                        hasHyperId(11L),
                                        hasParentId(1L)
                                ),
                                allOf(
                                        hasCategory("Обувь"),
                                        hasHyperId(12L),
                                        hasParentId(1L)
                                )
                        ))
                ),
                Arguments.of(
                        "Категория без детей",
                        2L,
                        Matchers.empty()
                )
        );
    }

    @DisplayName("Проверяем поиск детей по идентификатору категории")
    @MethodSource("childrenByHyperIdArgs")
    @ParameterizedTest(name = "{0}")
    void testGetChildrenByHyperId(@SuppressWarnings("unused") String description,
                                  long hyperId,
                                  Matcher<Iterable<Category>> expected
    ) {
        List<Category> categories = categoryService.getChildrenByHyperId(hyperId);
        assertThat(categories, expected);
    }

    @DisplayName("Ошибка неизвестного идентификатора")
    @Test
    void testGetChildrenByInvalidHyperId() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.getChildrenByHyperId(123)
        );
        assertThat(ex.getMessage(), equalTo("Didn't find hyperId: '123'"));
    }
}

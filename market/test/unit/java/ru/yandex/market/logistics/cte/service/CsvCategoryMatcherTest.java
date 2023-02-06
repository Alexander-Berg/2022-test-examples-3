package ru.yandex.market.logistics.cte.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.Sets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistics.cte.entity.category.CategoryAncestors;
import ru.yandex.market.logistics.cte.entity.category.CategoryInfo;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CsvCategoryMatcherTest {
    public static final CategoryInfo CAT_1 = CategoryInfo.of(1, "Продукты");
    public static final CategoryInfo CAT_2 = CategoryInfo.of(16593082, "Безалкогольное пиво и вино");
    public static final CategoryInfo ROOT = CategoryInfo.of(0, "root");

    CategoryService categoryService;

    CsvCategoryMatcher cut;

    @BeforeEach
    public void setup() {
        categoryService = Mockito.mock(CategoryService.class);

        Map<String, Integer> headerMap = new HashMap<>();
        headerMap.put("Категория (уровень 1)", 1);
        headerMap.put("Категория (уровень 2)", 2);
        cut = new CsvCategoryMatcher(categoryService, headerMap);

        Mockito.when(categoryService.getRoot()).thenReturn(ROOT);
    }

    @Test
    public void happyPath() {
        Mockito.when(categoryService.getSubcategory(0, "Продукты")).thenReturn(Optional.of(CAT_1));
        Mockito.when(categoryService.getSubcategory(1, "Безалкогольное пиво и вино")).thenReturn(Optional.of(CAT_2));

        CSVRecord record = provideHappyPathFixture();

        CategoryAncestors categoryAncestors = cut.matchCategoryPath(record);

        assertThat(categoryAncestors).isNotNull();
        assertThat(categoryAncestors.getCategory()).isEqualTo(CategoryInfo.of(16593082, "Безалкогольное пиво и вино"));
        assertThat(categoryAncestors.getAncestors()).isEqualTo(Sets.newHashSet(ROOT.getId(), CAT_1.getId()));
    }

    private CSVRecord provideHappyPathFixture() {
        List<CSVRecord> records = provideFixture();
        return records.get(0);
    }

    @Test
    public void categoryNotFound() {
        Mockito.when(categoryService.getSubcategory(0, "Электроника")).thenReturn(Optional.empty());

        CSVRecord record = provideCategoryNotFoundFixture();
        Assertions.assertThatThrownBy(() -> cut.matchCategoryPath(record)).isInstanceOf(IllegalStateException.class);
    }

    private CSVRecord provideCategoryNotFoundFixture() {
        List<CSVRecord> records = provideFixture();
        return records.get(1);
    }

    @NotNull
    private List<CSVRecord> provideFixture() {
        try {
            URL url = this.getClass().getClassLoader().getResource("unit/test-resupply-quality-attributes.csv");
            File file = new File(Objects.requireNonNull(url).toURI());
            CSVParser csvParser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withHeader());
            return csvParser.getRecords();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

package ru.yandex.market.vendors.analytics.core.calculate.table.extractor.category;

import java.time.LocalDate;
import java.util.Set;

import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.calculate.category.request.CategorySalesRequest;
import ru.yandex.market.vendors.analytics.core.calculate.common.SelectionInfoDTO;
import ru.yandex.market.vendors.analytics.core.calculate.growth.request.model.TopModelsGrowthRequest;
import ru.yandex.market.vendors.analytics.core.calculate.table.extractor.category.CategorySalesParamsExtractor;
import ru.yandex.market.vendors.analytics.core.model.dto.common.StartEndDateDTO;
import ru.yandex.market.vendors.analytics.core.model.dto.common.geo.GeoFiltersDTO;
import ru.yandex.market.vendors.analytics.core.model.dto.common.price.CategoryPriceSegmentsFilterDTO;
import ru.yandex.market.vendors.analytics.core.model.dto.common.socdem.GenderAgePairDTO;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.core.service.strategies.TopSelectionStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для CategorySalesParamsExtractor.
 */
public class CategorySalesParamsExtractorTest extends FunctionalTest {

    @Autowired
    private CategorySalesParamsExtractor categorySalesParamsExtractor;

    /**
     * {@link CategorySalesParamsExtractor#extractParams(CategorySalesRequest)}.
     */
    @Test
    @DisplayName("Проверка, что ExtractParams извлекаются корректно")
    @DbUnitDataSet(before = "CategorySalesParamsExtractorTest.before.csv")
    void validateExtractParams() {
        var categoryPriceSegmentsFilterDTO = CategoryPriceSegmentsFilterDTO.build(1, Set.of(2));
        var interval = new StartEndDateDTO(
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 1, 1)
        );

        var request = CategorySalesRequest.builder()
                .interval(interval)
                .timeDetailing(TimeDetailing.MONTH)
                .categoryPriceSegmentsFilterDTO(categoryPriceSegmentsFilterDTO)
                .build();

        String expected = "Период: 2019-01-01..2019-01-01; Детализация: Месяц; Ценовой сегмент: 100 - 200 ₽";

        var actual = categorySalesParamsExtractor.extractParams(request).getParamsValue();
        assertEquals(expected, actual);
    }
}
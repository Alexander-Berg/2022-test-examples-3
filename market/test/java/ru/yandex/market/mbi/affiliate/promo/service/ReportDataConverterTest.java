package ru.yandex.market.mbi.affiliate.promo.service;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReportDataConverterTest {
    @Test
    public void testConvertCategoriesToPromoDescriptions() {
        var result1 = ReportDataConverter.convertCategoriesToPromoDescription(
                Map.of("Товары для дома", 5)
        );
        assertEquals("Товары для дома", result1);

        var result2 = ReportDataConverter.convertCategoriesToPromoDescription(
                Map.of("Товары для дома", 5, "Iphone", 2, "KIDS", 1)
        );
        assertEquals("Товары для дома, iphone, KIDS", result2);

        var result3 = ReportDataConverter.convertCategoriesToPromoDescription(
                Map.of("Товары для дома", 5, "Товары для детей", 3, "Товары для животных", 2, "Электроника", 1)
        );
        assertEquals("Товары для дома, товары для детей, товары для животных и еще 1 категория", result3);
    }

    @Test
    public void testCaseCategories() {
        assertEquals("категория", ReportDataConverter.getCaseCategories(1));
        assertEquals("категории", ReportDataConverter.getCaseCategories(2));
        assertEquals("категории", ReportDataConverter.getCaseCategories(3));
        assertEquals("категории", ReportDataConverter.getCaseCategories(4));
        assertEquals("категорий", ReportDataConverter.getCaseCategories(5));
        assertEquals("категорий", ReportDataConverter.getCaseCategories(6));
        assertEquals("категорий", ReportDataConverter.getCaseCategories(7));
        assertEquals("категорий", ReportDataConverter.getCaseCategories(8));
        assertEquals("категорий", ReportDataConverter.getCaseCategories(9));
        assertEquals("категорий", ReportDataConverter.getCaseCategories(10));
        assertEquals("категорий", ReportDataConverter.getCaseCategories(11));
        assertEquals("категория", ReportDataConverter.getCaseCategories(21));
        assertEquals("категории", ReportDataConverter.getCaseCategories(133));
        assertEquals("категорий", ReportDataConverter.getCaseCategories(415));
    }
}

package ru.yandex.market.partner.mvc.controller.datacamp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.category.model.CategoryType;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.mvc.controller.datacamp.dto.CategoryDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.partner.mvc.controller.datacamp.DataCampUtils.typeEquality;

public class DataCampUtilsTest {

    @DisplayName("Конвертация типов категорий из коры в datacamp")
    @Test
    void conversionFromCategoryType() {
        assertEquals(typeEquality(CategoryType.GURU.name()), CategoryDTO.CategoryType.GURU);
        assertEquals(typeEquality(CategoryType.VISUAL.name()), CategoryDTO.CategoryType.VISUAL);
        assertEquals(typeEquality(CategoryType.GURULIGHT.name()), CategoryDTO.CategoryType.GURULIGHT);
        assertEquals(typeEquality(CategoryType.SIMPLE.name()), CategoryDTO.CategoryType.SIMPLE);
        assertEquals(typeEquality(CategoryType.MIXED.name()), CategoryDTO.CategoryType.MIXED);
        assertEquals(typeEquality(CategoryType.UNDEFINED.name()), CategoryDTO.CategoryType.UNDEFINED);
        assertEquals(typeEquality(null), CategoryDTO.CategoryType.UNDEFINED);
    }

    @DisplayName("Конвертация типа категории из формата протобуф в дто")
    @Test
    void conversionFromOutputType() {
        assertEquals(typeEquality(MboParameters.OutputType.GURU.name()), CategoryDTO.CategoryType.GURU);
        assertEquals(typeEquality(MboParameters.OutputType.VISUAL.name()), CategoryDTO.CategoryType.VISUAL);
        assertEquals(typeEquality(MboParameters.OutputType.GURULIGHT.name()), CategoryDTO.CategoryType.GURULIGHT);
        assertEquals(typeEquality(MboParameters.OutputType.SIMPLE.name()), CategoryDTO.CategoryType.SIMPLE);
        assertEquals(typeEquality(MboParameters.OutputType.MIXED.name()), CategoryDTO.CategoryType.MIXED);
        assertEquals(typeEquality(MboParameters.OutputType.UNDEFINED.name()), CategoryDTO.CategoryType.UNDEFINED);
        assertEquals(typeEquality(null), CategoryDTO.CategoryType.UNDEFINED);
    }
}

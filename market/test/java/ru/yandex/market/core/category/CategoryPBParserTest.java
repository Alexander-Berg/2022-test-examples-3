package ru.yandex.market.core.category;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.category.model.CategoryType;
import ru.yandex.market.mbo.export.MboParameters;

public class CategoryPBParserTest {

    @DisplayName("Проверка конвертации типов категорий из МБО")
    @Test
    void checkTypeMapping() {
        CategoryPBParser parser = new CategoryPBParser(null);
        Assertions.assertEquals(CategoryType.GURU, parser.getCategoryType(MboParameters.OutputType.GURU));
        Assertions.assertEquals(CategoryType.VISUAL, parser.getCategoryType(MboParameters.OutputType.VISUAL));
        Assertions.assertEquals(CategoryType.GURULIGHT, parser.getCategoryType(MboParameters.OutputType.GURULIGHT));
        Assertions.assertEquals(CategoryType.SIMPLE, parser.getCategoryType(MboParameters.OutputType.SIMPLE));
        Assertions.assertEquals(CategoryType.MIXED, parser.getCategoryType(MboParameters.OutputType.MIXED));
        Assertions.assertEquals(CategoryType.UNDEFINED, parser.getCategoryType(MboParameters.OutputType.UNDEFINED));
        Assertions.assertEquals(CategoryType.UNDEFINED, parser.getCategoryType(null));
    }
}
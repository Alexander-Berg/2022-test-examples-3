package ru.yandex.market.mbo.cms.core.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Тест проверяющий корректность парсинга плейсхолдеров из шаблона.
 *
 * @author s-ermakov
 */
public class OldTemplatesUtilFieldTest {

    @Test
    public void testPlaceholdersParsing() {
        String template = "Hello, __WHAT__!. __THIS__IS__SPARTA____BITCH__";

        List<FieldType> expectedPlaceholders = Arrays.asList(
                PlaceholderUtils.parsePlaceholder("__WHAT__"),
                PlaceholderUtils.parsePlaceholder("__THIS__"),
                PlaceholderUtils.parsePlaceholder("__SPARTA__"),
                PlaceholderUtils.parsePlaceholder("__BITCH__")
        );

        List<FieldType> actualPlaceholders = new ArrayList<>(PlaceholderUtils.extractPlaceholders(template).values());

        Assert.assertEquals(expectedPlaceholders, actualPlaceholders);
    }
}

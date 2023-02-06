package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.converter;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author prediger
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ValidStringConverterTest {

    protected CategoryParam categoryParam;
    protected int testLength;
    private ValidStringConverter validStringConverter;

    @Before
    public void setUp() {
        testLength = 10;
        categoryParam = CategoryParamBuilder.newBuilder(1, "test")
            .setType(Param.Type.STRING)
            .build();
        validStringConverter = new ValidStringConverter(testLength);
    }

    @Test
    public void testNullValue() throws ValueConverter.ConvertException {
        ParameterValue paramValue = validStringConverter.toParameterValue(null, categoryParam);

        Assert.assertNotNull(paramValue);
        Assert.assertTrue(paramValue.isEmpty());
    }

    @Test
    public void testNullAndEmptyElementsArray() throws ValueConverter.ConvertException {
        ParameterValue paramValue = validStringConverter.toParameterValue(Arrays.asList(
            null,
            ""), categoryParam);

        MboAssertions.assertThat(ParameterValues.of(paramValue)).values(
            "",
            "");
    }

    @Test
    public void testLengthException() {
        Assertions.assertThatThrownBy(() -> validStringConverter
            .toParameterValue(Collections.singletonList("01234567890"), categoryParam))
            .isInstanceOf(ValueConverter.ConvertException.class)
            .hasMessage("Значение должно быть не длиннее " + testLength + " символов");
    }

    @Test
    public void testTrimSpaces() throws ValueConverter.ConvertException {
        ParameterValue paramValue = validStringConverter.toParameterValue(Arrays.asList(
            "1",
            " 2",
            "3 ",
            " 4 ",
            "5 5",
            "6  6",
            "  7   7  "), categoryParam);

        MboAssertions.assertThat(ParameterValues.of(paramValue)).values(
            "1",
            "2",
            "3",
            "4",
            "5 5",
            "6 6",
            "7 7");
    }

}

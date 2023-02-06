package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.converter;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author prediger
 */
public class UrlStringConverterTest extends ValidStringConverterTest {

    private UrlStringConverter urlStringConverter;

    @Before
    public void setUp() {
        super.setUp();
        urlStringConverter = new UrlStringConverter(testLength);
    }

    @Test
    public void testNullValue() throws ValueConverter.ConvertException {
        ParameterValue paramValue = urlStringConverter.toParameterValue(null, categoryParam);

        Assert.assertNotNull(paramValue);
        Assert.assertTrue(paramValue.isEmpty());
    }

    @Test
    public void testNullAndEmptyElementsArray() throws ValueConverter.ConvertException {
        ParameterValue paramValue = urlStringConverter.toParameterValue(Arrays.asList(
            null,
            ""), categoryParam);

        MboAssertions.assertThat(ParameterValues.of(paramValue)).values(
            "",
            "");
    }

    @Test
    public void testLengthException() {
        Assertions.assertThatThrownBy(() -> urlStringConverter
            .toParameterValue(Collections.singletonList("01234567890"), categoryParam))
            .isInstanceOf(ValueConverter.ConvertException.class)
            .hasMessage("Значение должно быть не длиннее " + testLength + " символов");
    }

    @Test
    public void testTrimSpaces() throws ValueConverter.ConvertException {
        ParameterValue paramValue = urlStringConverter.toParameterValue(Arrays.asList(
            "t1.com",
            " t2.com",
            "t3.com ",
            " t4.com "), categoryParam);

        MboAssertions.assertThat(ParameterValues.of(paramValue)).values(
            "t1.com",
            "t2.com",
            "t3.com",
            "t4.com");
    }

    @Test
    public void testIncorrectUrl() {
        Assertions.assertThatThrownBy(() -> urlStringConverter
            .toParameterValue(Collections.singletonList("incorrect"), categoryParam))
            .isInstanceOf(ValueConverter.ConvertException.class)
            .hasMessage("Неверный формат URL");
    }

}

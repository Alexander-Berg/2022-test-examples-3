package ru.yandex.market.api.parsers;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.common.url.UrlControllerHelper;
import ru.yandex.market.api.controller.v2.ParametersV2;
import ru.yandex.market.api.domain.v2.criterion.Criterion;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.MultimapComparsionTestUtil;

/**
 * @author dimkarp93
 */
public class PreferModelsParserTest extends UnitTestBase {
    private final ParametersV2.PreferModelsParser parser = new ParametersV2.PreferModelsParser();

    @Test
    public void testParsePreferModelsTrue() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("prefer_models", true)
                .build();

        Result<Boolean, ValidationError> result = parser.get(request);

        Assert.assertTrue(result.isOk());
        Assert.assertTrue(result.getValue());
    }

    @Test
    public void testParsePreferModelsFalse() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("prefer_models", false)
                .build();

        Result<Boolean, ValidationError> result = parser.get(request);

        Assert.assertTrue(result.isOk());
        Assert.assertFalse(result.getValue());
    }

    @Test
    public void testParsePreferModelsNull() {
        HttpServletRequest request = MockRequestBuilder.start()
                .build();

        Result<Boolean, ValidationError> result = parser.get(request);

        Assert.assertTrue(result.isOk());
        Assert.assertNull(result.getValue());
    }
}
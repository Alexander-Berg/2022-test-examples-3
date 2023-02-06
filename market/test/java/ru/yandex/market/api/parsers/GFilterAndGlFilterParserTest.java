package ru.yandex.market.api.parsers;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.url.UrlControllerHelper;
import ru.yandex.market.api.domain.v2.criterion.Criterion;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.MultimapComparsionTestUtil;

/**
 * @author dimkarp93
 */
public class GFilterAndGlFilterParserTest extends UnitTestBase {
    private static final String GFILTER = Criterion.CriterionType.GFILTER.getType();
    private static final String GLFILTER = Criterion.CriterionType.GLFILTER.getType();

    /**
     * Проверяем, что правильно принимаем и валидируем числовые фильтры
     */
    @Test
    public void testParseNumberFilterIsValid() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(GFILTER, "12:23~34")
            .param(GLFILTER, "45:~9")
            .param(GLFILTER, "45:~9.1")
            .param(GFILTER, "32:7~")
            .param(GLFILTER, "14871214:14899397_100244224641")
            .param(GLFILTER, "13211231:0.5~0.5_664556015")
            .build();

        ValidationErrors errors = new ValidationErrors();

        Multimap<String, String> filters = new UrlControllerHelper.GFilterAndGlFilterParser().get(request, errors);
        Assert.assertTrue(errors.isEmpty());

        MultimapComparsionTestUtil.assertMapEquals(
            ImmutableListMultimap.<String, String>builder()
                .put(GFILTER, "12:23~34")
                .put(GLFILTER, "45:~9")
                .put(GLFILTER, "45:~9.1")
                .put(GFILTER, "32:7~")
                .put(GLFILTER, "14871214:14899397_100244224641")
                .put(GLFILTER, "13211231:0.5~0.5_664556015")
                .build(),
            filters
        );


    }

    /**
     * Проверяем, что правильно принимаем и валидируем enum фильтры
     */
    @Test
    public void testParseEnumFiltersIsValid() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(GFILTER, "34:57,12")
            .param(GLFILTER, "56:29,38,49")
            .param(GLFILTER, "7:7")
            .build();

        ValidationErrors errors = new ValidationErrors();
        Multimap<String, String> filters = new UrlControllerHelper.GFilterAndGlFilterParser().get(request, errors);

        Assert.assertTrue(errors.isEmpty());

        MultimapComparsionTestUtil.assertMapEquals(
            ImmutableListMultimap.<String, String>builder()
                .put(GFILTER, "34:57,12")
                .put(GLFILTER, "56:29,38,49")
                .put(GLFILTER, "7:7")
                .build(),
            filters
        );
    }


    /**
     * Проверяем, что считаем числовой фильтр некорректным, когда в две ~
     */
    @Test
    public void testParseNumberFilterIsNoValidWhenTwoTilda() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(GLFILTER, "12:5~6~7")
            .build();

        ValidationErrors errors = new ValidationErrors();

        new UrlControllerHelper.GFilterAndGlFilterParser().get(request, errors);

        Assert.assertFalse(errors.isEmpty());
    }

    /**
     * Проверяем, что считаем числовой фильтр некорректным, когда он состоит только из ~
     */
    @Test
    public void testParseNumberFilterIsNoValidWhenOnlyTilda() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(GFILTER, "12:~")
            .build();

        ValidationErrors errors = new ValidationErrors();

        new UrlControllerHelper.GFilterAndGlFilterParser().get(request, errors);

        Assert.assertFalse(errors.isEmpty());
    }

    /**
     * Проверяем, что игнорим фильтры не являющиеся gfilter или glfilter
     */
    @Test
    public void testIgnoreNotGFilterAndNotFlGilter() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(GFILTER, "34:55")
            .param("pricefrom", "1000")
            .param("abra", "52")
            .param("ss", "y~y~y")
            .build();

        ValidationErrors errors = new ValidationErrors();
        Multimap<String, String> filters = new UrlControllerHelper.GFilterAndGlFilterParser().get(request, errors);

        Assert.assertTrue(errors.isEmpty());

        MultimapComparsionTestUtil.assertMapEquals(
            ImmutableListMultimap.<String, String>builder()
                .put(GFILTER, "34:55")
                .build(),
            filters
        );
    }
}

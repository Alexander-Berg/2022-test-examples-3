package ru.yandex.market.api.parsers;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.url.UrlControllerHelper;
import ru.yandex.market.api.domain.v2.criterion.Criterion;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.internal.filters.FiltersRegistry;
import ru.yandex.market.api.util.MultimapComparsionTestUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * @author dimkarp93
 */
public class FilterWithTextNamesParserTest extends UnitTestBase {

    /**
     * Проверяем, что корректно обрабатываем, числовые фильтры с десктопа
     */
    @Test
    public void testVerstkaNumberFiltersValid() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(FiltersRegistry.getById(Filters.PRICE_FILTER_CODE).getVerstkaParamName(), "100")
            .param(FiltersRegistry.getById(Filters.PRICE_FILTER_CODE).getVerstkaGeParamName(), "100")
            .build();

        ValidationErrors errors = new ValidationErrors();
        Multimap<String, String> filters = new UrlControllerHelper.FiltersWithTextNamesParser().get(request, errors);
        Assert.assertTrue(errors.isEmpty());

        MultimapComparsionTestUtil.assertMapEquals(
            ImmutableListMultimap.<String, String>builder()
                .put(FiltersRegistry.getById(Filters.PRICE_FILTER_CODE).getVerstkaParamName(), "100")
                .put(FiltersRegistry.getById(Filters.PRICE_FILTER_CODE).getVerstkaGeParamName(), "1000")
                .build(),
            filters);
    }

    /**
     * Проверяем, что корректно обрабатываем, числовые фильтры с тача
     */
    @Test
    public void testTouchNumberFilterValid() {
       HttpServletRequest request = MockRequestBuilder.start()
           .param(FiltersRegistry.getById(Filters.PRICE_FILTER_CODE).getTouchParamName(), "500")
           .param(FiltersRegistry.getById(Filters.PRICE_FILTER_CODE).getTouchGeParamName(), "1000")
           .build();

        ValidationErrors errors = new ValidationErrors();
        Multimap<String, String> filters = new UrlControllerHelper.FiltersWithTextNamesParser().get(request, errors);
        Assert.assertTrue(errors.isEmpty());

        MultimapComparsionTestUtil.assertMapEquals(
            ImmutableListMultimap.<String, String>builder()
                .put(FiltersRegistry.getById(Filters.PRICE_FILTER_CODE).getTouchParamName(), "500")
                .put(FiltersRegistry.getById(Filters.PRICE_FILTER_CODE).getTouchGeParamName(), "1000")
                .build(),
            filters);
    }

    /**
     * Проверяем, что корректно обрабатываем enum-фильтры, по магазинам например
     */
    @Test
    public void testCasualEnumFilterValid() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(FiltersRegistry.getById(Filters.SHOP_FILTER_CODE).getVerstkaParamName(), "3424,110")
            .build();


        ValidationErrors errors = new ValidationErrors();
        Multimap<String, String> filters = new UrlControllerHelper.FiltersWithTextNamesParser().get(request, errors);
        Assert.assertTrue(errors.isEmpty());

        MultimapComparsionTestUtil.assertMapEquals(
            ImmutableListMultimap.<String, String>builder()
                .put(FiltersRegistry.getById(Filters.SHOP_FILTER_CODE).getVerstkaParamName(), "3424,110")
                .build(),
            filters);
    }

    /**
     * Проверяем, что корректно обрабатываем radio-фильтры по доставке
     */
    @Test
    public void testOfferShippingFilterValid() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(FiltersRegistry.getById(Filters.SHIPPING_FILTER_CODE).getVerstkaParamName(), "delivery")
            .build();

        ValidationErrors errors = new ValidationErrors();
        Multimap<String, String> filters = new UrlControllerHelper.FiltersWithTextNamesParser().get(request, errors);
        Assert.assertTrue(errors.isEmpty());

        MultimapComparsionTestUtil.assertMapEquals(
            ImmutableListMultimap.<String, String>builder()
                .put(FiltersRegistry.getById(Filters.SHIPPING_FILTER_CODE).getVerstkaParamName(), "delivery")
                .build(),
            filters);
    }

    /**
     * Проверяем, что игнорим фильтры с именами которых нет а FilterRegistry и правильно обрабатываем булевы фильтры
     */
    @Test
    public void testIgnoreFiltersWithTextNameNotInFiltersRegistry() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param(FiltersRegistry.getById(Filters.FREE_DELIVERY_FILTER_CODE).getVerstkaParamName(), "select")
            .param(Criterion.CriterionType.GFILTER.getType(), "12:34,12")
            .param(Criterion.CriterionType.GFILTER.getType(), "12:34~12")
            .param("asdkal", "askd;las~y~y")
            .build();

        ValidationErrors errors = new ValidationErrors();
        Multimap<String, String> filters = new UrlControllerHelper.FiltersWithTextNamesParser().get(request, errors);
        Assert.assertTrue(errors.isEmpty());

        MultimapComparsionTestUtil.assertMapEquals(
            ImmutableListMultimap.<String, String>builder()
                .put(FiltersRegistry.getById(Filters.FREE_DELIVERY_FILTER_CODE).getVerstkaParamName(), "1")
                .build(),
            filters);

    }
}


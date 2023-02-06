package ru.yandex.market.api.navigation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.catalog.NavigationCategoryV1.Datasource;
import ru.yandex.market.api.domain.v2.criterion.Criterion;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.cataloger.NavigationTreeParamProcessor;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.model.UniversalModelSort;
import ru.yandex.market.api.model.UniversalModelSort.SortField;
import ru.yandex.market.api.util.CriterionTestUtil;
import ru.yandex.market.api.util.functional.Functionals;

/**
 * @author dimkarp93
 */
public class NavigationTreeParamProcessorTest extends BaseTest {
    /**
     * Проверяем, что возвращаем в datasource nid и hid, если они установлены
     */
    @Test
    public void hidAndNidInResultIfSet() {
        Multimap<String, String> params = ImmutableListMultimap.<String, String>builder()
            .put("nid", "1")
            .put("hid", "23")
            .build();

        Datasource actual = new Datasource();
        NavigationTreeParamProcessor.enrichDatasource(actual, params);

        Datasource expected = new Datasource();
        expected.setNid(1);
        expected.setHid(23);

        assertDatasourceEquals(expected, actual);
    }

    /**
     * Провеярем, что возвращаем в datasource sort, если она установлена
     */
    @Test
    public void sortInResultIfSet() {
        Multimap<String, String> params = ImmutableListMultimap.<String, String>builder()
            .put("how", "dpop")
            .build();

        Datasource actual = new Datasource();
        NavigationTreeParamProcessor.enrichDatasource(actual, params);

        Datasource expected = new Datasource();
        expected.setOrder(new UniversalModelSort(SortField.POPULARITY, SortOrder.DESC));

        assertDatasourceEquals(expected, actual);
    }

    /**
     * Проверяем, что возвращаем criterion для glfilter и gfilter
     */
    @Test
    public void gfilterAndGlfilterInResultIfSet() {
        Multimap<String, String> params = ImmutableListMultimap.<String, String>builder()
            .put("glfilter", "12:23")
            .put("gfilter", "45:56")
            .build();

        Datasource actual = new Datasource();
        NavigationTreeParamProcessor.enrichDatasource(actual, params);

        Datasource expected = new Datasource();
        expected.setCriteria(
            Arrays.asList(
                new Criterion("12", "23", Criterion.CriterionType.GLFILTER),
                new Criterion("45", "56", Criterion.CriterionType.GFILTER)
            )
        );

        assertDatasourceEquals(expected, actual);
    }

    /**
     * Проверяем, что вернули фильтр -3, если был установлен in-stock
     */
    @Test
    public void minus3InResultIfInStockSet() {
        Multimap<String, String> params = ImmutableListMultimap.<String, String>builder()
            .put("in-stock", "exclude")
            .build();

        Datasource actual = new Datasource();
        NavigationTreeParamProcessor.enrichDatasource(actual, params);

        Datasource expected = new Datasource();
        expected.setCriteria(
            Collections.singletonList(
                new Criterion(Filters.ON_STOCK_FILTER_CODE, "0", Criterion.CriterionType.API_FILTER)
            )
        );

        assertDatasourceEquals(expected, actual);
    }

    /**
     * Проверяем, что вернули фильтр -3, если был установлен onstock
     */
    @Test
    public void minus3InResultIfOnStockSet() {
        Multimap<String, String> params = ImmutableListMultimap.<String, String>builder()
            .put("onstock", "1")
            .build();

        Datasource actual = new Datasource();
        NavigationTreeParamProcessor.enrichDatasource(actual, params);

        Datasource expected = new Datasource();
        expected.setCriteria(
            Collections.singletonList(
                new Criterion(Filters.ON_STOCK_FILTER_CODE, "1", Criterion.CriterionType.API_FILTER)
            )
        );

        assertDatasourceEquals(expected, actual);
    }

    /**
     * Проверяем, что onstock предпочтительнее, чем in-stock, если оба уставнолены
     */
    @Test
    public void minus3FromOnStockInResultIfOnStockAndInStockSet() {
        Multimap<String, String> params = ImmutableListMultimap.<String, String>builder()
            .put("in-stock", "0")
            .put("onstock", "1")
            .build();

        Datasource actual = new Datasource();
        NavigationTreeParamProcessor.enrichDatasource(actual, params);

        Datasource expected = new Datasource();
        expected.setCriteria(
            Collections.singletonList(
                new Criterion(Filters.ON_STOCK_FILTER_CODE, "1", Criterion.CriterionType.API_FILTER)
            )
        );

        assertDatasourceEquals(expected, actual);
    }

    /**
     * Провеярем, что вернули -1, если установлен только параметр priceFrom
     */
    @Test
    public void priceFilterInResultIfPriceFromSet() {
        Multimap<String, String> params = ImmutableListMultimap.<String, String>builder()
            .put("pricefrom", "100")
            .build();

        Datasource actual = new Datasource();
        NavigationTreeParamProcessor.enrichDatasource(actual, params);

        Datasource expected = new Datasource();
        expected.setCriteria(
            Collections.singletonList(
                new Criterion(Filters.PRICE_FILTER_CODE, "100,", Criterion.CriterionType.API_FILTER)
            )
        );

        assertDatasourceEquals(expected, actual);
    }

    /**
     * Провеярем, что вернули -3, если установлен параметр priceTo
     */
    @Test
    public void priceFilterInResultIfPriceToSet() {
        Multimap<String, String> params = ImmutableListMultimap.<String, String>builder()
            .put("priceto", "1000")
            .build();

        Datasource actual = new Datasource();
        NavigationTreeParamProcessor.enrichDatasource(actual, params);

        Datasource expected = new Datasource();
        expected.setCriteria(
            Collections.singletonList(
                new Criterion(Filters.PRICE_FILTER_CODE, ",1000", Criterion.CriterionType.API_FILTER)
            )
        );

        assertDatasourceEquals(expected, actual);
    }

    /**
     * Провеярем, что вернули -3, если установлен параметр priceFrom и priceTo
     */
    @Test
    public void priceFilterInResultIfPriceFromAndPriceToSet() {
        Multimap<String, String> params = ImmutableListMultimap.<String, String>builder()
            .put("pricefrom", "100")
            .put("priceto", "1000")
            .build();

        Datasource actual = new Datasource();
        NavigationTreeParamProcessor.enrichDatasource(actual, params);

        Datasource expected = new Datasource();
        expected.setCriteria(
            Collections.singletonList(
                new Criterion(Filters.PRICE_FILTER_CODE, "100,1000", Criterion.CriterionType.API_FILTER)
            )
        );

        assertDatasourceEquals(expected, actual);
    }

    private void assertDatasourceEquals(@NotNull Datasource expected,
                                        @NotNull Datasource actual) {

        assertEqualsTestInt(expected, actual, Datasource::getNid);
        assertEqualsTest(expected, actual, Datasource::getHid, Objects::equals);

        Function<Datasource, SortField> extractSortField = datasource ->
            Functionals.getOrNull(datasource, Datasource::getOrder, UniversalModelSort::getSort);
        assertEqualsTest(expected, actual, extractSortField);

        Function<Datasource, SortOrder> extractSortOrder = datasource ->
            Functionals.getOrNull(datasource, Datasource::getOrder, UniversalModelSort::getHow);
        assertEqualsTest(expected, actual, extractSortOrder);

        CriterionTestUtil.assertCriterionEquals(expected.getCriteria(), actual.getCriteria());

    }

    private <T, S> void assertEqualsTest(T obj1, T obj2, Function<T, S> function, BiPredicate<S, S> equality) {
        boolean isEqual = null == obj1 ? null == obj2 : null != obj2 &&
            equality.test(function.apply(obj1), function.apply(obj2));
        Assert.assertTrue(isEqual);
    }

    private <T, S extends Enum<S>> void assertEqualsTest(T obj1, T obj2, Function<T, S> function) {
        boolean isEqual = null == obj1 ? null == obj2 : null != obj2
            && function.apply(obj1) == function.apply(obj2);
        Assert.assertTrue(isEqual);
    }

    private <T> void assertEqualsTestInt(T obj1, T obj2, ToIntFunction<T> function) {
        boolean isEqual = null == obj1 ? null == obj2 : null != obj2
            && function.applyAsInt(obj1) == function.applyAsInt(obj2);
        Assert.assertTrue(isEqual);
    }

}

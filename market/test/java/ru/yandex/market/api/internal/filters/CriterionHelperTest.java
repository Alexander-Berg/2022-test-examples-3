package ru.yandex.market.api.internal.filters;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.criterion.Criterion;
import ru.yandex.market.api.domain.v2.criterion.Criterion.CriterionType;
import ru.yandex.market.api.domain.v2.criterion.CriterionHelper;
import ru.yandex.market.api.util.CriterionTestUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.ApiMatchers.entry;

/**
 * @author dimkarp93
 */
public class CriterionHelperTest {
    private static final String GFILTER = CriterionType.GFILTER.getType();
    private static final String GLFILTER = CriterionType.GLFILTER.getType();

    /**
     * Проверка, что вместо гурушного фильтра на вендора ставим -11
     */
    @Test
    public void testGuruVendorFilterToApiVendorFilter() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put(GFILTER, Filters.GURU_VENDOR_FILTER_ID + ":123")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);
        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(
                new Criterion(Filters.VENDOR_FILTER_CODE, "123", CriterionType.API_FILTER)
            ),
            actual
        );

    }

    /**
     * Проверяем, что вместо визуального визуального фильтра на вендора ставим -11
     */
    @Test
    public void testVisualVendorFilterToApiVendorFilter() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put(GLFILTER, Filters.VISUAL_VENDOR_FILTER_ID + ":123")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);
        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(
                new Criterion(Filters.VENDOR_FILTER_CODE, "123", CriterionType.API_FILTER)
            ),
            actual
        );

    }

    /**
     * Проверяем, что корректно обрабатываем Url кодирование в фиильтре
     */
    @Test
    public void testCorrectProcessUrlEncodingInFilter() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put(GFILTER, "12%3A21")
            .put(GLFILTER, "34%3A45")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);
        CriterionTestUtil.assertCriterionEquals(
            Arrays.asList(
                new Criterion("12", "21", CriterionType.GFILTER),
                new Criterion("34", "45", CriterionType.GLFILTER)
            ),
            actual
        );
    }

    /**
     * Проверяем, что корректно преобразуем буловые значения (select, exclude)
     */
    @Test
    public void testTransformValueInBooleanFilter() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put(GLFILTER, "12:select")
            .put(GFILTER, "34:exclude")
            .put(GLFILTER, "56:1")
            .put(GFILTER, "78:0")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);
        CriterionTestUtil.assertCriterionEquals(
            Arrays.asList(
                new Criterion("12", "1", CriterionType.GLFILTER),
                new Criterion("34", "0", CriterionType.GFILTER),
                new Criterion("56", "1", CriterionType.GLFILTER),
                new Criterion("78", "0", CriterionType.GFILTER)
            ),
            actual
        );
    }

    /**
     * Проверка, что правильно трансформируем фильтры с текстовым названием в отрицательные
     */
    @Test
    public void testTransformCasualFilterFromFilterRegistry() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put("fesh", "3828,114871")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);

        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(
                new Criterion(Filters.SHOP_FILTER_CODE, "114871,3828", CriterionType.API_FILTER)
            ),
            actual
        );
    }

    /**
     * Проверяем, что правильно трансформируем булевы фильтры с текстовым названием в отрицательные
     */
    @Test
    public void testTransformBooleanFilterFromFilterRegistry() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put("onstock", "select")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);

        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(
                new Criterion(Filters.ON_STOCK_FILTER_CODE, "1", CriterionType.API_FILTER)
            ),
            actual
        );
    }

    /**
     * Проверяем, что правильно трансформируем числовые фильтры (только min значение)
     * с текстовым названием в отрицательные
     */
    @Test
    public void testTransformNumberMinValueFilterFromFilterRegistryNoDenomination() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put("pricefrom", "2000")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);

        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(
                new Criterion(Filters.PRICE_FILTER_CODE, "2000~", CriterionType.API_FILTER)
            ),
            actual
        );

    }

    /**
     * Проверяем, что правильно трансформируем числовые фильтры (только max значение)
     * с текстовым названием в отрицательные
     */
    @Test
    public void testTransformNumberMaxValueFilterFromFilterRegistryNoDenomination() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put("priceto", "3000")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);

        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(
                new Criterion(Filters.PRICE_FILTER_CODE, "~3000", CriterionType.API_FILTER)
            ),
            actual
        );

    }

    /**
     * Проверяем, что правильно трансформируем числовые фильтры (min,max значение)
     * с текстовым названием в отрицательные
     */
    @Test
    public void testTransformNumberMinMaxValueFilterFromFilterRegistryNoDenomination() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put("pricefrom", "2000")
            .put("priceto", "3000")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);

        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(
                new Criterion(Filters.PRICE_FILTER_CODE, "2000~3000", CriterionType.API_FILTER)
            ),
            actual
        );

    }

    /**
     * Проверяем, что правильно трансофрмируем числовые фильтры
     * с текстовым названием (из тача) в отрицательные
     */
    @Test
    public void testTransformFilterFromFilterRegistryWithTouchName() {
        Multimap<String, String> filters = ImmutableListMultimap.<String, String>builder()
            .put("mcpricefrom", "1000")
            .put("mcpriceto", "6000")
            .build();

        Collection<Criterion> actual = CriterionHelper.parse(filters);

        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(
                new Criterion(Filters.PRICE_FILTER_CODE, "1000~6000", CriterionType.API_FILTER)
            ),
            actual
        );
    }

    @Test
    public void shouldEmptyGlFiltersWhenKeyValuesNull() {
        Multimap<String, String> glfilters = CriterionHelper.keyValueAsGlfilters(null);
        assertThat(glfilters, notNullValue());
        assertThat(glfilters.entries(), hasSize(0));
    }

    @Test
    public void glfiltersFromKeyValue() {
        Multimap<String, String> glfilters = CriterionHelper.keyValueAsGlfilters(
            ImmutableMultimap.<String, String>builder()
                .put("23", "34")
                .put("10", "01")
                .put("23", "12")
                .put("text", "iphone")
                .build()
        );
        assertThat(glfilters, notNullValue());
        assertThat(
            glfilters.entries(),
            containsInAnyOrder(
                entry(GLFILTER, "10:01"),
                entry(GLFILTER, allOf(
                    startsWith("23:"),
                    containsString("34"),
                    containsString("12")
                )),
                entry("text", "iphone")
            )
        );
    }

}

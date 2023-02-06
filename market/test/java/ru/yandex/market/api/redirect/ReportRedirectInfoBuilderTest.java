package ru.yandex.market.api.redirect;

import java.util.function.Function;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.controller.v2.TextParameters;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.filters.FilterData;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.internal.filters.FiltersRegistry;
import ru.yandex.market.api.internal.report.ReportRedirectInfo;
import ru.yandex.market.api.util.functional.Functionals;

/**
 * @author dimkarp93
 */
public class ReportRedirectInfoBuilderTest extends BaseTest {
    private static final String GLFILTER = "glfilter";
    private static final String GFILTER = "gfilter";

    private static final Function<String, String> EXTRACT_URL_NAME = Functionals.compose(
        FiltersRegistry::getById, FilterData::getVerstkaParamName);

    @Test
    public void frontFiltersAndApiFiltersNotIntersect_mergeFilters_unionFilters() {
        Multimap<String, String> actual = ReportRedirectInfo.builder()
            .setFrontFilters(
                ArrayListMultimap.create(
                    ImmutableListMultimap.<String, String>builder()
                        .put(GFILTER, "12:23")
                        .put(GFILTER, "23:34")
                        .put(GLFILTER, "34:45")
                        .put(GLFILTER, "45:56")
                            .put(EXTRACT_URL_NAME.apply(TextParameters.FILTER_CODE), "text")
                    .build()
                    )
                )
            .setApiFilters(ImmutableMap.<String, String>builder()
                    .put(Filters.SHOP_FILTER_CODE, "3824")
                    .put("111", "222")
                    .put("222", "333")
                .build())
            .build()
            .getFilters();

        Multimap<String, String> expected = ImmutableListMultimap.<String, String>builder()
                .put(GFILTER, "12:23")
                .put(GFILTER, "23:34")
                .put(GLFILTER, "34:45")
                .put(GLFILTER, "45:56")
                .put(GLFILTER, "111:222")
                .put(GLFILTER, "222:333")
                .put(EXTRACT_URL_NAME.apply(TextParameters.FILTER_CODE), "text")
                .put(EXTRACT_URL_NAME.apply(Filters.SHOP_FILTER_CODE), "3824")
            .build();

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void frontFiltersIntersectApiFiltersByGlfilters_mergeFilters_apiGlfiltersOverrideFrontFilters() {
        Multimap<String, String> actual =
            ReportRedirectInfo.builder()
                .setFrontFilters(
                    ArrayListMultimap.create(
                        ImmutableListMultimap.<String, String>builder()
                            .put(GLFILTER, "12:23,34")
                            .put(EXTRACT_URL_NAME.apply(Filters.SHOP_FILTER_CODE), "3824")
                        .build()
                    )
                )
                .setApiFilters(
                    ImmutableMap.<String, String>builder()
                        .put("12", "45")
                        .put(Filters.FREE_DELIVERY_FILTER_CODE, "1")
                    .build()
                )
            .build()
            .getFilters();

        Multimap<String, String> expected = ImmutableListMultimap.<String, String>builder()
                .put(GLFILTER, "12:45")
                .put(EXTRACT_URL_NAME.apply(Filters.SHOP_FILTER_CODE), "3824")
                .put(EXTRACT_URL_NAME.apply(Filters.FREE_DELIVERY_FILTER_CODE), "1")
            .build();

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void frontFiltersIntersectApiFitlerByNamedFilters_mergeFilters_apiNamedFiltersOverrideFrontFilters() {
        Multimap<String, String> actual = ReportRedirectInfo.builder()
                .setFrontFilters(
                    ArrayListMultimap.create(
                        ImmutableListMultimap.<String, String>builder()
                            .put(GLFILTER, "12:34,45")
                                .put(EXTRACT_URL_NAME.apply(TextParameters.FILTER_CODE), "text")
                        .build()
                    )
                )
                .setApiFilters(
                    ImmutableMap.<String, String>builder()
                        .put("45", "57")
                            .put(TextParameters.FILTER_CODE, "newText")
                    .build()
                )
            .build().getFilters();

        Multimap<String, String> expected = ImmutableListMultimap.<String, String>builder()
                .put(GLFILTER, "12:34,45")
                .put(GLFILTER, "45:57")
                .put(EXTRACT_URL_NAME.apply(TextParameters.FILTER_CODE), "newText")
            .build();

        Assert.assertEquals(expected, actual);
    }
}

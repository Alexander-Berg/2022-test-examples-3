package ru.yandex.market.api.controller.v2;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongLists;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.category.FilterSetType;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.FiltersResult;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferFieldV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.ResultFieldV2;
import ru.yandex.market.api.domain.v2.SearchResultV2;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.internal.report.CommonReportOptions;
import ru.yandex.market.api.internal.report.CommonReportOptions.ResultType;
import ru.yandex.market.api.util.concurrent.ApiDeferredResult;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by vdorogin on 24.05.17.
 */
public class SearchControllerV2Test extends BaseTest {

    private static final String REPORT_PRIME_RESPONSE_PATH = "/ru/yandex/market/api/integration/report_prime.json";

    @Inject
    SearchControllerV2 controller;
    @Inject
    ReportTestClient reportTestClient;

    @Test
    public void presenceOnStockFilter() {
        String text = "iPhone";
        // настройка системы
        reportTestClient.searchV2withoutCvRedirect(text, "search_iphone.json");
        // вызов системы
        FiltersResult result = ((ApiDeferredResult<FiltersResult>) controller.getSearchFilters(
                SearchQuery.text(text),
                Collections.emptyList(),
                Collections.emptyMap(),
                FilterSetType.POPULAR,
                genericParams
        )).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getFilters());
        Assert.assertNotNull(result.getFilters().stream()
                .filter(f -> Filters.ON_STOCK_FILTER_CODE.equals(f.getId()))
                .findFirst()
                .orElse(null)
        );
    }

    @Test
    public void categorySearchWithLocalDeliveryParam() {
        int hid = 91491;

        reportTestClient.search(
                "prime",
                x -> x.param("hid", String.valueOf(hid))
                        .param("min-delivery-priority", "priority"),
                "report_prime_with_local_delivery_param.json"
        );

        controller.filterOnCategory(
                hid,
                0,
                Collections.emptyMap(),
                null,
                IntLists.EMPTY_LIST,
                false,
                false,
                true,
                PageInfo.DEFAULT,
                Collections.emptyList(),
                null,
                false,
                null,
                ResultType.ALL,
                false,
                null,
                null,
                genericParams,
                null,
                new ValidationErrors()
        ).waitResult();
    }

    @Test
    public void categorySearchWithoutLocalDeliveryParam() {
        int hid = 91491;

        reportTestClient.search(
                "prime",
                x -> x.param("hid", String.valueOf(hid))
                        .withoutParam("min-delivery-priority"),
                "report_prime_without_local_delivery_param.json"
        );

        SearchResultV2 result = controller.filterOnCategory(
                hid,
                0,
                Collections.emptyMap(),
                null,
                IntLists.EMPTY_LIST,
                false,
                false,
                false,
                PageInfo.DEFAULT,
                Collections.emptyList(),
                null,
                false,
                null,
                ResultType.ALL,
                false,
                null,
                null,
                genericParams,
                null,
                new ValidationErrors()
        ).waitResult();

        assertNotNull(result.getCategories());
        assertEquals(0, result.getCategories().size());
    }

    @Test
    public void sendFilterWarnings() {
        int hid = 13077405;

        reportTestClient.search(
                "prime",
                x -> x.param("hid", String.valueOf(hid))
                        .param("filter-warnings", "medicine_recipe"),
                "report_prime_without_local_delivery_param.json"
        );

        SearchResultV2 result = controller.filterOnCategory(
                hid,
                0,
                Collections.emptyMap(),
                null,
                IntLists.EMPTY_LIST,
                false,
                false,
                false,
                PageInfo.DEFAULT,
                singletonList(ResultFieldV2.FOUND_CATEGORIES),
                null,
                false,
                "medicine_recipe",
                ResultType.ALL,
                false,
                null,
                null,
                genericParams,
                null,
                new ValidationErrors()
        ).waitResult();

        assertNotNull(result.getCategories());
        assertEquals(1, result.getCategories().size());
        assertEquals(91491, result.getCategories().get(0).getId());
    }

    @Test
    public void sendCheckFilters() {
        int hid = 13077405;

        reportTestClient.checkFilters(hid, singletonList("123:456"), "check_filters.json");

        reportTestClient.search(
                "prime",
                x -> x.param("hid", String.valueOf(hid))
                        .param("glfilter", "123:234"),
                "report_prime_without_local_delivery_param.json"
        );

        controller.filterOnCategory(
                hid,
                0,
                ImmutableMap.<String, String>builder().put("123", "456").build(),
                null,
                IntLists.EMPTY_LIST,
                false,
                false,
                false,
                PageInfo.DEFAULT,
                singletonList(ResultFieldV2.FOUND_CATEGORIES),
                null,
                false,
                null,
                ResultType.ALL,
                true,
                null,
                null,
                genericParams,
                null,
                new ValidationErrors()
        ).waitResult();
    }

    @Test
    public void categorySearchNewDeliveryBriefWithCurrency() {
        int hid = 91491;

        reportTestClient.search(
                "prime",
                x -> x.param("hid", String.valueOf(hid)),
                "report_prime_phones.json"
        );

        SearchResultV2 response = controller.filterOnCategoryNew(
                hid,
                0,
                Collections.emptyMap(),
                null,
                IntLists.EMPTY_LIST,
                false,
                PageInfo.DEFAULT,
                Arrays.asList(ModelInfoField.DEFAULT_OFFER, OfferFieldV2.DELIVERY),
                null,
                false,
                null,
                ResultType.ALL,
                null,
                genericParams,
                null,
                new ValidationErrors()
        ).waitResult();

        ModelV2 model = (ModelV2) response.getItems().get(0);
        OfferV2 offer = (OfferV2) model.getOffer();
        assertEquals("в Москву — 249 руб., возможен самовывоз", offer.getDelivery().getBrief());
    }

    @Test
    public void doNotCheckSpellingTest() {
        reportTestClient
                .doRequest("prime", x -> x.param("noreask", "1"))
                .ok().body(REPORT_PRIME_RESPONSE_PATH);
        doSearchRequest(false);
    }

    @Test
    public void checkSpellingTest() {
        reportTestClient
                .doRequest("prime", x -> x.withoutParam("noreask"))
                .ok().body(REPORT_PRIME_RESPONSE_PATH);
        doSearchRequest(true);
    }

    @Test
    public void checkSpellingByDefaultTest() {
        reportTestClient
                .doRequest("prime", x -> x.withoutParam("noreask"))
                .ok().body(REPORT_PRIME_RESPONSE_PATH);
        doSearchRequest(null);
    }

    private void doSearchRequest(Boolean checkSpelling) {
        controller.search(LongLists.EMPTY_LIST, true, null, true, true,
                Collections.emptySet(), SearchQuery.text("abc"), Collections.emptyList(),
                100, 200, PageInfo.DEFAULT,
                null, true, CommonReportOptions.ResultType.ALL,
                Collections.emptyMap(), GenericParams.DEFAULT, 0L, checkSpelling, null
        ).waitResult();
    }

}

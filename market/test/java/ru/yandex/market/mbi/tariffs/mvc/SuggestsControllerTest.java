package ru.yandex.market.mbi.tariffs.mvc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.TestUtils;
import ru.yandex.market.mbi.tariffs.model.Category;
import ru.yandex.market.mbi.tariffs.model.CategoryIds;
import ru.yandex.market.mbi.tariffs.model.DumpFormatEnum;
import ru.yandex.market.mbi.tariffs.model.PagerResponseInfo;
import ru.yandex.market.partner.error.info.model.ErrorInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.get;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.post;
import static ru.yandex.market.mbi.tariffs.matcher.CategoryMatcher.hasAllFields;
import static ru.yandex.market.mbi.tariffs.matcher.ErrorInfoMatcher.hasCode;
import static ru.yandex.market.mbi.tariffs.matcher.ErrorInfoMatcher.hasMessage;

/**
 * Тесты для {@link ru.yandex.market.mbi.tariffs.mvc.controller.SuggestsController}
 */
public class SuggestsControllerTest extends FunctionalTest {

    @ParameterizedTest(name = "[{index}] Тест на поиск по подстроке : [{0}]")
    @MethodSource("getTagsTestData")
    @DbUnitDataSet(
            before = "suggests/getTags.before.csv",
            after = "suggests/getTags.before.csv"
    )
    @DisplayName("Тест на получение тегов с разными конфигурациями")
    void testGetTags(
            String substr,
            long expectedTotalCount,
            List<String> expectedTags
    ) {
        String parameters = "";
        if (substr != null) {
            parameters = "substr=" + substr;
        }

        ResponseEntity<String> response = get(baseUrl() + "/suggests/tags?" + parameters);
        assertOk(response);
        PagerResponseInfo pagerResponse = TestUtils.parsePagerResponse(response.getBody(), String.class);
        assertThat(pagerResponse.getTotalCount(), is(expectedTotalCount));
        assertThat(
                pagerResponse.getItems()
                        .stream()
                        .map(tag -> (String) tag)
                        .collect(Collectors.toList()),
                is(expectedTags)
        );
    }


    private static Stream<Arguments> getTagsTestData() {
        return Stream.of(
                Arguments.of("test", 3, List.of("test", "test1", "test2")),
                Arguments.of("e", 5, List.of("andreybystrov", "qwe", "test", "test1", "test2")),
                Arguments.of("; drop table mbi_tariffs.tag; --", 0, List.of()),
                Arguments.of(null, 6, List.of("andreybystrov", "MBI-51594", "qwe", "test", "test1", "test2")),
                Arguments.of("TEST", 3, List.of("test", "test1", "test2"))
        );
    }

    @ParameterizedTest(name = "[{index}] : Тест на поиск категорий по идентификаторам : {0}")
    @MethodSource("testPostCategoriesByIdsData")
    @DisplayName("Тест на поиск категорий по идентификаторам")
    void testPostCategoriesByIds(CategoryIds ids, int expectedCount, Matcher<Iterable<? extends Category>> matcher) {

        ResponseEntity<String> response = post(
                baseUrl() + "/suggests/categories/by-ids",
                createHttpRequestEntity(ids)
        );
        assertOk(response);
        List<Category> categories = TestUtils.parseListResults(response.getBody(), Category.class);
        assertThat(expectedCount, is(categories.size()));
        assertThat(categories, matcher);
    }

    private static Stream<Arguments> testPostCategoriesByIdsData() {
        Matcher<Iterable<? extends Category>> matcher = containsInAnyOrder(
                hasAllFields(198119L, 90401L, "Электроника"),
                hasAllFields(90509L, 90401L, "Товары для красоты")
        );
        return Stream.of(
                Arguments.of(new CategoryIds().ids(List.of(198119L, 90509L)), 2, matcher),
                Arguments.of(new CategoryIds().ids(List.of(198119L, 90509L, -10L)), 2, matcher)
        );
    }

    @ParameterizedTest(name = "[{index}] : /suggests/{0} => expected count is {1}")
    @MethodSource("testGetEnumsData")
    void testGetEnumTypes(
            String typeUrl,
            long expectedTotalCount,
            List<String> expectedTypes
    ) {
        ResponseEntity<String> response = get(baseUrl() + "/suggests/" + typeUrl);
        assertOk(response);

        PagerResponseInfo pagerResponse = TestUtils.parsePagerResponse(response.getBody(), String.class);
        assertThat(pagerResponse.getTotalCount(), is(expectedTotalCount));
        assertThat(
                pagerResponse.getItems().stream().map(obj -> (String) obj).collect(Collectors.toList()),
                containsInAnyOrder(expectedTypes.toArray(String[]::new))
        );
    }

    private static Stream<Arguments> testGetEnumsData() {
        return Stream.of(
                Arguments.of(
                        "model-types",
                        10L,
                        List.of("fulfillment_by_yandex", "fulfillment_by_yandex_plus", "fulfillment_by_seller",
                                "delivery_by_seller", "click_and_collect", "third_party_logistics_courier",
                                "third_party_logistics_pvz", "sorting_center", "distribution_clid",
                                "third_party_logistics_self_employed_courier"
                        )
                ),
                Arguments.of(
                        "service-types",
                        50L,
                        List.of("fixed_tariffs", "distribution", "ff_partner", "ff_processing", "ff_storage_billing",
                                "ff_storage_billing_multiplier", "ff_withdraw", "delivery_to_customer",
                                "delivery_to_customer_return", "intake", "min_fee", "sorting", "min_daily", "fee",
                                "loyalty_participation_fee", "ff_xdoc_supply", "returned_orders_storage",
                                "self_requested_disposal", "cancelled_express_order_fee", "cancelled_order_fee",
                                "express_delivery", "cash_only_order", "crossregional_delivery", "courier_shift",
                                "pvz_reward", "pvz_cash_compensation", "pvz_card_compensation", "pvz_dropoff",
                                "pvz_dropoff_return", "pvz_return", "pvz_branded_decoration", "pvz_reward_yado",
                                "pvz_reward_dbs", "pvz_dbs_income", "pvz_dbs_outcome", "sc_reward",
                                "sc_minimal_reward", "sc_kgt_reward", "courier_dropoff_return", "courier_fine",
                                "global_fee", "global_agency_commission", "global_delivery", "courier_small_goods",
                                "courier_bulky_cargo", "courier_withdraw", "courier_yandex_drive", "agency_commission", "ff_storage_turnover",
                                "courier_velo_shift"
                        )
                ),
                Arguments.of(
                        "partner-types",
                        8L,
                        List.of("shop", "supplier", "business", "pvz", "pvz_partner", "sorting_center", "distribution", "blue_contract")
                )
        );
    }

    @Test
    void testGetAuditAdditionalFilters() {
        ResponseEntity<String> response = get(baseUrl() + "/suggests/audit-additional-filters");
        assertOk(response);

        PagerResponseInfo pagerResponse = TestUtils.parsePagerResponse(response.getBody(), String.class);
        assertThat(pagerResponse.getTotalCount(), is(2L));
        assertThat(
                pagerResponse.getItems()
                        .stream()
                        .map(key -> (String) key)
                        .collect(Collectors.toList()),
                containsInAnyOrder("tariff_id", "draft_id")
        );
    }

    @Test
    void testGetMetaFieldsFailed() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> get(baseUrl() + "/suggests/meta-fields?serviceType=test")
        );

        List<ErrorInfo> errors = TestUtils.getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode("BAD_PARAM"),
                hasMessage("Unknown serviceType : 'test'.")

        ));
    }

    @Test
    void testGetMetaFieldsSuccess() {
        ResponseEntity<String> response = get(baseUrl() + "/suggests/meta-fields?serviceType=fee");
        assertOk(response);
    }

    @Test
    void testGetTariffDumpFormats() {
        ResponseEntity<String> response = get(baseUrl() + "/suggests/download-formats");
        assertOk(response);
        PagerResponseInfo pagerResponse = TestUtils.parsePagerResponse(response.getBody(), DumpFormatEnum.class);
        assertThat(pagerResponse.getTotalCount(), is(2L));
        assertThat(
                pagerResponse.getItems(),
                containsInAnyOrder(DumpFormatEnum.CSV, DumpFormatEnum.EXCEL)
        );
    }
}

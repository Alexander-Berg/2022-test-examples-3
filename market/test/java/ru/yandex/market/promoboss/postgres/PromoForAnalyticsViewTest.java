package ru.yandex.market.promoboss.postgres;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.utils.ResultSetUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet(
        before = {"PromoForAnalyticsViewTest.csv"}
)
public class PromoForAnalyticsViewTest extends AbstractDbUnitPostgresTest {
    @Builder
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class PromoForAnalyticsViewItem {
        private final String promoId;
        private final Long sourceId;
        private final String typeEnName;
        private final String name;
        private final String statusEnName;
        private final String purposeEnName;
        private final String strategyTypeEnName;
        private final String compensationEnName;
        private final String supplierType;
        private final String assortmentLoadMethod;
        private final Boolean hidden;
        private final String parentPromoId;
        private final Long createdDttm;
        private final Long startDttm;
        private final Long endDttm;
        private final String authorLogin;
        private final String responsible;
        private final String [] responsibleDepartmentsNames;
        private final Long discountRatePcnt;
        private final Long discountValueRub;
        private final Long bucketMinPriceRub;
        private final Long bucketMaxPriceRub;
        private final Long applyTimes;
        private final String promocode;
        private final Boolean firstOrder;
        private final Boolean showOnKm;
        private final Long itemForGiftCnt;
        private final Long discountBudgetRub;
    }

    private final RowMapper<PromoForAnalyticsViewItem> rowMapper = (rs, rowNum) -> PromoForAnalyticsViewItem.builder()
            .promoId(rs.getString("promo_id"))
            .sourceId(ResultSetUtils.getNullableLong(rs, "source_id"))
            .typeEnName(rs.getString("type_en_name"))
            .name(rs.getString("name"))
            .statusEnName(rs.getString("status_en_name"))
            .purposeEnName(rs.getString("purpose_en_name"))
            .strategyTypeEnName(rs.getString("strategy_type_en_name"))
            .compensationEnName(rs.getString("compensation_en_name"))
            .supplierType(rs.getString("supplier_type"))
            .assortmentLoadMethod(rs.getString("assortment_load_method"))
            .hidden(ResultSetUtils.getNullableBoolean(rs, "hidden"))
            .parentPromoId(rs.getString("parent_promo_id"))
            .createdDttm(ResultSetUtils.getNullableLong(rs, "created_dttm"))
            .startDttm(ResultSetUtils.getNullableLong(rs, "start_dttm"))
            .endDttm(ResultSetUtils.getNullableLong(rs, "end_dttm"))
            .authorLogin(rs.getString("author_login"))
            .responsible(rs.getString("responsible"))
            .responsibleDepartmentsNames((String[])rs.getArray("responsible_departments_names").getArray())
            .discountRatePcnt(ResultSetUtils.getNullableLong(rs, "discount_rate_pcnt"))
            .discountValueRub(ResultSetUtils.getNullableLong(rs, "discount_value_rub"))
            .bucketMinPriceRub(ResultSetUtils.getNullableLong(rs, "bucket_min_price_rub"))
            .bucketMaxPriceRub(ResultSetUtils.getNullableLong(rs, "bucket_max_price_rub"))
            .applyTimes(ResultSetUtils.getNullableLong(rs, "apply_times"))
            .promocode(rs.getString("promocode"))
            .firstOrder(ResultSetUtils.getNullableBoolean(rs, "first_order"))
            .showOnKm(ResultSetUtils.getNullableBoolean(rs, "show_on_km"))
            .itemForGiftCnt(ResultSetUtils.getNullableLong(rs, "item_for_gift_cnt"))
            .discountBudgetRub(ResultSetUtils.getNullableLong(rs, "discount_budget_rub"))
            .build();

    @Test
    void select_ok() {
        PromoForAnalyticsViewItem expected = PromoForAnalyticsViewItem.builder()
                .promoId("cf_104547")
                .sourceId(3L)
                .typeEnName("CHEAPEST_AS_GIFT")
                .name("2022-06-06-15-16-1")
                .statusEnName("READY")
                .purposeEnName("GMV_GENERATION-1")
                .strategyTypeEnName("NATIONAL-1")
                .compensationEnName("PARTNER-1")
                .supplierType("1P-1")
                .assortmentLoadMethod("TRACKER-1")
                .hidden(true)
                .parentPromoId("SP#201")
                .createdDttm(1654517851L)
                .startDttm(1657487800L)
                .endDttm(1658438140L)
                .authorLogin("author-1")
                .responsible("tradeManager-1")
                .responsibleDepartmentsNames(new String[]{"FMCG-1", "CEHAC-1"})
                .discountRatePcnt(null)
                .discountValueRub(null)
                .bucketMinPriceRub(null)
                .bucketMaxPriceRub(null)
                .applyTimes(null)
                .promocode(null)
                .firstOrder(false)
                .showOnKm(true)
                .itemForGiftCnt(3L)
                .discountBudgetRub(null)
                .build();

        PromoForAnalyticsViewItem actual = jdbcTemplate.queryForObject(
                "select * from promo_for_analytics_view limit 1",
                rowMapper
        );

        assertEquals(expected, actual);
    }
}



package ru.yandex.market.loyalty.core.service.discount.constants;

import org.junit.Test;

import Market.Promo.Promo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SupplierFlagRestrictionProtoUtilsTest {

    @Test
    public void shouldGetProtoValueForSupplierFlags() {
        assertThat(SupplierFlagRestrictionProtoUtils.getProtoValue(
                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE, true),
                is(Promo.PromoDetails.OffersMatchingRule.SupplierFlagRestriction.newBuilder()
                        .setSupplierFlags(6)
                        .build())
        );
    }

    @Test
    public void shouldGetProtoValueForExcludedSupplierFlags() {
        assertThat(SupplierFlagRestrictionProtoUtils.getProtoValue(
                SupplierFlagRestrictionType.EVERYTHING_EXCEPT_EXPRESS, false),
                is(Promo.PromoDetails.OffersMatchingRule.SupplierFlagRestriction.newBuilder()
                        .setExcludedSupplierFlags(6)
                        .build())
        );
    }

    @Test
    public void shouldGetProtoValueForNothing() {
        assertThat(SupplierFlagRestrictionProtoUtils.getProtoValue(
                SupplierFlagRestrictionType.EVERYTHING, null),
                is(Promo.PromoDetails.OffersMatchingRule.SupplierFlagRestriction.newBuilder()
                        .setSupplierFlags(1)
                        .build())
        );
    }

    @Test
    public void shouldGetProtoValueForDbsAndNotExpress() {
        assertThat(SupplierFlagRestrictionProtoUtils.getProtoValue(
                SupplierFlagRestrictionType.EVERYTHING_EXCEPT_EXPRESS, true),
                is(Promo.PromoDetails.OffersMatchingRule.SupplierFlagRestriction.newBuilder()
                        .setSupplierFlags(4)
                        .setExcludedSupplierFlags(2)
                        .build())
        );
    }

    @Test
    public void shouldGetProtoValueForExpressAndNotDbs() {
        assertThat(SupplierFlagRestrictionProtoUtils.getProtoValue(
                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE, false),
                is(Promo.PromoDetails.OffersMatchingRule.SupplierFlagRestriction.newBuilder()
                        .setSupplierFlags(2)
                        .setExcludedSupplierFlags(4)
                        .build())
        );
    }

    @Test
    public void shouldGetProtoValueForExpressAndEmptyDbs() {
        assertThat(SupplierFlagRestrictionProtoUtils.getProtoValue(
                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE, null),
                is(Promo.PromoDetails.OffersMatchingRule.SupplierFlagRestriction.newBuilder()
                        .setSupplierFlags(2)
                        .build())
        );
    }
}

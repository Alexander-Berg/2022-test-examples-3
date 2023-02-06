package ru.yandex.market.api.internal.report.parsers.json;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.OfferPromo;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

@WithContext
public class OfferPromoParsersTest extends UnitTestBase {

    OfferPromo promo;

    @Before
    public void setUp() {
        byte[] resource = ResourceHelpers.getResource("offer-promo.json");
        promo = new OfferPromoParser().parse(resource);
    }

    @Test
    public void checkType() {
        Assert.assertEquals("megasale", promo.getType());
    }

    @Test
    public void checkDescription() {
        Assert.assertEquals("Super Promo Mega Sale", promo.getDescription());
    }

    @Test
    public void checkTermsAndConditions() {
        Assert.assertEquals("Some hardcoded text here or an empty string", promo.getTermsAndConditions());
    }

    @Test
    public void checkStartDate() {
        Assert.assertEquals(parse("2018-06-01T01:02:03"), promo.getStartDate());
    }

    @Test
    public void checkEndDate() {
        Assert.assertEquals(parse("2018-07-01T05:07:11"), promo.getEndDate());
    }

    @Test
    public void checkStartDateUtc() {
        Assert.assertEquals(parseInstant("2018-05-31T22:02:03Z"), promo.getStartDateUtc());
    }

    @Test
    public void checkEndDateUtc() {
        Assert.assertEquals(parseInstant("2018-07-01T02:07:11Z"), promo.getEndDateUtc());
    }

    @Test
    public void checkTimezoneParse() {
        OfferPromo offerPromo = new OfferPromoParser().parse(ResourceHelpers.getResource("offer-promo-tz.json"));
        Assert.assertEquals(parse("2018-06-01T04:02:03"), offerPromo.getStartDate());
        Assert.assertEquals(parse("2018-07-01T05:07:11"), offerPromo.getEndDate());
        Assert.assertEquals(parseInstant("2018-06-01T01:02:03Z"), offerPromo.getStartDateUtc());
        Assert.assertEquals(parseInstant("2018-07-01T02:07:11Z"), offerPromo.getEndDateUtc());
    }

    @Test
    public void checkPromoSubsidyParse() {
        OfferPromo offerPromo = new OfferPromoParser().parse(ResourceHelpers.getResource("offer-promo_promocode_subsidy.json"));
        Assert.assertEquals("Market", offerPromo.getPromoCode());
        Assert.assertEquals("100", offerPromo.getDiscount().getValue());
        Assert.assertEquals("RUR", offerPromo.getDiscount().getCurrency());
    }

    @Test
    public void checkPromoDiscountParse() {
        OfferPromo offerPromo = new OfferPromoParser().parse(ResourceHelpers.getResource("offer-promo_promocode_discount.json"));
        Assert.assertEquals("ELUX10", offerPromo.getPromoCode());
        Assert.assertEquals("10", offerPromo.getDiscount().getValue());
    }

    @Test
    public void checkItemsInfo() {
        OfferPromo offerPromo = new OfferPromoParser().parse(ResourceHelpers.getResource("offer-promo_items_info.json"));
        String itemsInfo = offerPromo.getItemsInfo();
        Assert.assertEquals("{\"additionalOffers\":[{\"offer\":{\"offerId\":\"jGuzAOzrbLvyDQpszNwTDg\"}}]}", itemsInfo);
    }

    @Test
    public void checkBonusPrice() {
        Assert.assertEquals("100", promo.getBonusPrice());
    }

    @Test
    public void checkUseDefaultDirectDiscount() {
        final OfferPromo offerPromo = new OfferPromoParser()
                .parse(ResourceHelpers.getResource("offer-promo_direct_discount_items_info.json"));
        final String itemsInfo = offerPromo.getItemsInfo();
        Assert.assertTrue(itemsInfo != null && itemsInfo.length() > 0);
    }

    @Test
    public void checkUseDirectDiscount() {
        final boolean excludeItemsInfoDirectDiscount = false;
        final boolean excludePromoCodeWithConditions = false;
        final OfferPromo offerPromo = new OfferPromoParser(excludeItemsInfoDirectDiscount, excludePromoCodeWithConditions)
                .parse(ResourceHelpers.getResource("offer-promo_direct_discount_items_info.json"));
        final String itemsInfo = offerPromo.getItemsInfo();
        Assert.assertTrue(itemsInfo != null && itemsInfo.length() > 0);
    }

    @Test
    public void checkRemoveDirectDiscount() {
        final boolean excludeItemsInfoDirectDiscount = true;
        final boolean excludePromoCodeWithConditions = false;
        final OfferPromo offerPromo = new OfferPromoParser(excludeItemsInfoDirectDiscount, excludePromoCodeWithConditions)
                .parse(ResourceHelpers.getResource("offer-promo_direct_discount_items_info.json"));
        final String itemsInfo = offerPromo.getItemsInfo();
        Assert.assertNull(itemsInfo);
    }

    @Test
    public void checkUseNotDirectDiscountItemsInfoWhenDirectDiscountRemoveNeeded() {
        final boolean excludeItemsInfoDirectDiscount = true;
        final boolean excludePromoCodeWithConditions = false;
        final OfferPromo offerPromo = new OfferPromoParser(excludeItemsInfoDirectDiscount, excludePromoCodeWithConditions)
                .parse(ResourceHelpers.getResource("offer-promo_items_info.json"));
        final String itemsInfo = offerPromo.getItemsInfo();
        Assert.assertTrue(itemsInfo != null && itemsInfo.length() > 0);
    }

    @Test
    public void checkPromoCodeWithConditionsExcludedWithSuchFlagConfiguredTrue() {
        final boolean excludeItemsInfoDirectDiscount = false;
        final boolean excludePromoCodeWithConditions = true;
        final OfferPromo offerPromo = new OfferPromoParser(excludeItemsInfoDirectDiscount, excludePromoCodeWithConditions)
                .parse(ResourceHelpers.getResource("offer-promo_promocode_with_conditions.json"));
        Assert.assertNull(offerPromo);
    }

    @Test
    public void checkPromoCodeWithConditionsNotExcludedSuchFlagConfiguredFalse() {
        final boolean excludeItemsInfoDirectDiscount = false;
        final boolean excludePromoCodeWithConditions = false;
        final OfferPromo offerPromo = new OfferPromoParser(excludeItemsInfoDirectDiscount, excludePromoCodeWithConditions)
                .parse(ResourceHelpers.getResource("offer-promo_promocode_with_conditions.json"));
        Assert.assertNotNull(offerPromo);
    }

    @Test
    public void checkPromoValue() {
        Assert.assertEquals("123", promo.getValue());
    }

    @Test
    public void checkIsPersonal() {
        Assert.assertEquals(true, promo.getIsPersonal());
    }

    private LocalDateTime parse(String v) {
        return LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(v));
    }

    private Instant parseInstant(String v) {
        return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(v));
    }
}

package ru.yandex.market.api.internal.report.parsers.json;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.OfferPromo;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

@WithContext
public class OfferPromoListParserTest {

    @Test
    public void checkSimplePromosParsing() {
        List<OfferPromo> promoList = new OfferPromoListParser(false, false)
                .parse(ResourceHelpers.getResource("offer-promos.json"));
        Assert.assertEquals(promoList.size(), 2);
        checkPromo(
                promoList.get(0),
                "i7p1B4wrKTXlsFjz",
                "megasale",
                "Super Promo Mega Sale",
                "Some hardcoded text here or an empty string",
                "2018-06-01T01:02:03",
                "2018-07-01T05:07:11",
                "100",
                "123",
                false
        );
        checkPromo(
                promoList.get(1),
                "QkL28aQb9xG34xub",
                "megasale2",
                "Super Promo Mega Sale2",
                "no text at all",
                "2018-06-01T01:02:04",
                "2018-07-01T05:07:12",
                "10000",
                "321",
                true
        );
    }

    @Test
    public void checkExcludingWithConditions() {
        List<OfferPromo> promoList = new OfferPromoListParser(false, true)
                .parse(ResourceHelpers.getResource("offer-promos_conditions.json"));
        Assert.assertEquals(promoList.size(), 2);
        checkPromo(
                promoList.get(0),
                "irORsY3fElp6_unhWVSlMg2",
                "promo-code",
                "Скидка 10%",
                null,
                null,
                null,
                null,
                null,
                null
        );
        checkPromo(
                promoList.get(1),
                "i7p1B4wrKTXlsFjz",
                "megasale",
                "Super Promo Mega Sale",
                "Some hardcoded text here or an empty string",
                "2018-06-01T01:02:03",
                "2018-07-01T05:07:11",
                "100",
                "123",
                null
        );
    }

    @Test
    public void checkNotExcludingWithConditions() {
        List<OfferPromo> promoList = new OfferPromoListParser(false, false)
                .parse(ResourceHelpers.getResource("offer-promos_conditions.json"));
        Assert.assertEquals(promoList.size(), 3);
        checkPromo(
                promoList.get(0),
                "irORsY3fElp6_unhWVSlMg",
                "promo-code",
                "Скидка 10% на",
                null,
                null,
                null,
                null,
                null,
                null
        );
        checkPromo(
                promoList.get(1),
                "irORsY3fElp6_unhWVSlMg2",
                "promo-code",
                "Скидка 10%",
                null,
                null,
                null,
                null,
                null,
                null
        );
        checkPromo(
                promoList.get(2),
                "i7p1B4wrKTXlsFjz",
                "megasale",
                "Super Promo Mega Sale",
                "Some hardcoded text here or an empty string",
                "2018-06-01T01:02:03",
                "2018-07-01T05:07:11",
                "100",
                "123",
                null
        );
    }

    private void checkPromo(
            final OfferPromo promo,
            final String key,
            final String type,
            final String description,
            final String termsAndConditions,
            final String startDate,
            final String endDate,
            final String bonusPrice,
            final String value,
            final Boolean isPersonal
    ) {
        Assert.assertEquals(promo.getKey(), key);
        Assert.assertEquals(promo.getType(), type);
        Assert.assertEquals(promo.getDescription(), description);
        Assert.assertEquals(promo.getTermsAndConditions(), termsAndConditions);
        Assert.assertEquals(promo.getStartDate(), parse(startDate));
        Assert.assertEquals(promo.getEndDate(), parse(endDate));
        Assert.assertEquals(promo.getBonusPrice(), bonusPrice);
        Assert.assertEquals(promo.getValue(), value);
        Assert.assertEquals(promo.getIsPersonal(), isPersonal);
    }

    private LocalDateTime parse(String v) {
        if (v == null) {
            return null;
        } else {
            return LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(v));
        }
    }
}

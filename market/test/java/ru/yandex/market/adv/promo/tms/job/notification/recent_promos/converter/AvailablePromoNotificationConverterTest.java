package ru.yandex.market.adv.promo.tms.job.notification.recent_promos.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.adv.promo.tms.job.notification.recent_promos.model.AvailablePromoNotificationData;
import ru.yandex.market.adv.promo.tms.job.notification.recent_promos.model.PartnerAvailablePromoInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvailablePromoNotificationConverterTest {

    @Test
    void testOnePromoConvert() throws Exception {
        String expected = CommonTestUtils.getResource(this.getClass(), "testOnePromoConvert/onePromo.xml").
                replaceAll("\\s+", "");
        PartnerAvailablePromoInfo promo = makeInfoWithName("Name");
        String actual = AvailablePromoNotificationConverter.convertToXml(
                new AvailablePromoNotificationData(List.of(promo))
        );
        assertEquals(expected, actual);
    }

    @Test
    void testMultiplePromosConvert() throws Exception {
        String expected = CommonTestUtils.getResource(this.getClass(), "testMultiplePromosConvert/multiplePromos.xml").
                replaceAll("\\s+", "");
        PartnerAvailablePromoInfo promo1 = makeInfoWithName("Name1");
        PartnerAvailablePromoInfo promo2 = makeInfoWithName("Name2");
        String actual = AvailablePromoNotificationConverter.convertToXml(
                new AvailablePromoNotificationData(List.of(promo1, promo2))
        );
        assertEquals(expected, actual);
    }

    private PartnerAvailablePromoInfo makeInfoWithName(String name) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("y-MM-d");
        return new PartnerAvailablePromoInfo.Builder()
                .withPromoId("promoId")
                .withPromoName(name)
                .withCategoriesCount(2)
                .withStartDate(simpleDateFormat.parse("2021-08-2").getTime() / 1000)
                .withEndDate(simpleDateFormat.parse("2021-08-5").getTime() / 1000)
                .withOffersCount(11)
                .withMinDiscount(11)
                .withMaxDiscount(22)
                .withMechanic(1)
                .withPublishDate(1L)
                .withPartnerId(1L)
                .build();
    }
}

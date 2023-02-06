package ru.yandex.market.core.supplier.promo.model.datacamp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataCampPromoOfferMapperTest {

    @Test
    void findPromoDiscountTest() {
        Assertions.assertEquals(0, DataCampPromoOfferMapper.findPromoDiscountPercentage(10L, 10L));
        Assertions.assertEquals(10, DataCampPromoOfferMapper.findPromoDiscountPercentage(1000L, 100L));
        Assertions.assertEquals(1, DataCampPromoOfferMapper.findPromoDiscountPercentage(1000L, 10L));
        Assertions.assertEquals(5, DataCampPromoOfferMapper.findPromoDiscountPercentage(1000L, 50L));
        Assertions.assertEquals(50, DataCampPromoOfferMapper.findPromoDiscountPercentage(100L, 50L));
        Assertions.assertEquals(10, DataCampPromoOfferMapper.findPromoDiscountPercentage(100L, 10L));
        Assertions.assertEquals(3, DataCampPromoOfferMapper.findPromoDiscountPercentage(100L, 3L));
        Assertions.assertEquals(5, DataCampPromoOfferMapper.findPromoDiscountPercentage(100L, 5L));

        Assertions.assertEquals(0, DataCampPromoOfferMapper.findPromoDiscountPercentage(10000L, 1L));

        Assertions.assertEquals(45, DataCampPromoOfferMapper.findPromoDiscountPercentage(33L, 15L));
        Assertions.assertEquals(97, DataCampPromoOfferMapper.findPromoDiscountPercentage(33L, 32L));
        Assertions.assertEquals(94, DataCampPromoOfferMapper.findPromoDiscountPercentage(17L, 16L));

        Assertions.assertEquals(0, DataCampPromoOfferMapper.findPromoDiscountPercentage(17L, 0L));

        Assertions.assertNull(DataCampPromoOfferMapper.findPromoDiscountPercentage(17L, -4L));
        Assertions.assertNull(DataCampPromoOfferMapper.findPromoDiscountPercentage(-17L, 4L));
        Assertions.assertNull(DataCampPromoOfferMapper.findPromoDiscountPercentage(-17L, -4L));

        Assertions.assertEquals(0, DataCampPromoOfferMapper.findPromoDiscountPercentage(17L, 0L));
        Assertions.assertEquals(0, DataCampPromoOfferMapper.findPromoDiscountPercentage(0L, 4L));

        Assertions.assertNull(DataCampPromoOfferMapper.findPromoDiscountPercentage(null, 4L));
        Assertions.assertNull(DataCampPromoOfferMapper.findPromoDiscountPercentage(5L, null));
    }
}

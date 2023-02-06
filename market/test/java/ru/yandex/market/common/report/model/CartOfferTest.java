package ru.yandex.market.common.report.model;

import java.math.BigDecimal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CartOfferTest {
    @Test
    public void testSimpleWareIdFormat() {
        CartOffer offer = new CartOffer("AAA", 23);
        assertEquals("AAA:23", offer.toRequestFormat());
    }

    @Test
    public void testDimensionFormat() {
        CartOffer offer = new CartOffer("AAA", 23,
                BigDecimal.TEN, 42, CartOffer.formatDimensions("20", "40", "50"),
                145);
        assertEquals("AAA:23;w:10;d:20x40x50;p:42;wh:145", offer.toRequestFormat());
    }

    @Test
    public void testCategoryIdFormat() {
        CartOffer offer = new CartOffer("AAA", 23, 1337,
                BigDecimal.TEN, 42, CartOffer.formatDimensions("20", "40", "50"),
                145);
        assertEquals("AAA:23;hid:1337;w:10;d:20x40x50;p:42;wh:145", offer.toRequestFormat());
    }

    @Test
    public void testFulfilmentWarehouseIdFormat() {
        CartOffer offer = new CartOffer("AAA", 23, 1337,
                BigDecimal.TEN, 42, CartOffer.formatDimensions("20", "40", "50"),
                700, true, 172L);

        assertEquals("AAA:23;hid:1337;w:10;d:20x40x50;p:42;wh:700;ff:1;ffWh:172", offer.toRequestFormat());
    }

    @Test
    public void newFieldsForConsolidate() {
        final CartOffer first = new CartOffer("A1", 1, "M1", null, null, null, null);
        final CartOffer second = new CartOffer("A2", 2, "M2", "B2", null, null, null);
        final CartOffer third = new CartOffer("A3", 3, "M3", "B3", "K3", null, null);
        final CartOffer fourth = new CartOffer("A4", 4, "M4", "B4", "K4", "pt4", null);
        final CartOffer fifth = new CartOffer("A4", 4, "M4", "B4", "K4", "pt4", "faster");

        assertEquals("A1:1;msku:M1", first.toRequestFormat());
        assertEquals("A2:2;msku:M2;bundle_id:B2", second.toRequestFormat());
        assertEquals("A3:3;msku:M3;bundle_id:B3;promo_id:K3", third.toRequestFormat());
        assertEquals("A4:4;msku:M4;bundle_id:B4;promo_id:K4;promo_type:pt4", fourth.toRequestFormat());
        assertEquals("A4:4;msku:M4;bundle_id:B4;promo_id:K4;promo_type:pt4;benefit:faster", fifth.toRequestFormat());
    }
}

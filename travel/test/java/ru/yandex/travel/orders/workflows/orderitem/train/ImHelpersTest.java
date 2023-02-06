package ru.yandex.travel.orders.workflows.orderitem.train;

import org.junit.Test;

import ru.yandex.travel.orders.entities.VatType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.workflows.orderitem.train.ImHelpers.formatImNonContactPhone;

public class ImHelpersTest {
    @Test
    public void testVatFromRateDouble() {
        assertThat(ImHelpers.vatFromRate(0.0)).isEqualTo(VatType.VAT_0);
        assertThat(ImHelpers.vatFromRate(10.0)).isEqualTo(VatType.VAT_10);
        assertThat(ImHelpers.vatFromRate(18.0)).isEqualTo(VatType.VAT_18);
        assertThat(ImHelpers.vatFromRate(20.0)).isEqualTo(VatType.VAT_20);
        assertThat(ImHelpers.vatFromRate(9.09)).isEqualTo(VatType.VAT_10_110);
        assertThat(ImHelpers.vatFromRate(15.25)).isEqualTo(VatType.VAT_18_118);
        assertThat(ImHelpers.vatFromRate(16.67)).isEqualTo(VatType.VAT_20_120);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVatFromRateException() {
        ImHelpers.vatFromRate(200300.0);
    }

    @Test
    public void testFormatImNonContactPhone() {
        // foreign
        assertThat(formatImNonContactPhone("tel.: 375 17 357-19-61", true)).isEqualTo("+375173571961");
        assertThat(formatImNonContactPhone("tel.: +007 495 739-70-00, ext. 1234", true)).isEqualTo("+749573970001234");
        // length corner cases
        assertThat(formatImNonContactPhone("+3751", true)).isEqualTo(null);
        assertThat(formatImNonContactPhone("+37517", true)).isEqualTo("+37517");
        assertThat(formatImNonContactPhone("+30123456789012345678901234567", true))
                .isEqualTo("+30123456789012345678901234567");
        assertThat(formatImNonContactPhone("+301234567890123456789012345678", true)).isEqualTo(null);

        // non foreign
        assertThat(formatImNonContactPhone("tel.: 375 17 357-19-61", false)).isEqualTo(null);
        assertThat(formatImNonContactPhone("tel.: +007 495 739-70-00, ext. 1234", false)).isEqualTo(null);
        assertThat(formatImNonContactPhone("tel.: +007 495 739-70-00", false)).isEqualTo("+74957397000");
        assertThat(formatImNonContactPhone("tel.: +007 495 739, ext. 1234", false)).isEqualTo("+74957391234");
        assertThat(formatImNonContactPhone("тел.: +7 495 739-3 доб. 321", false)).isEqualTo("+74957393321");
        assertThat(formatImNonContactPhone("тел.: 8 495 739-37-77", false)).isEqualTo("+74957393777");
        assertThat(formatImNonContactPhone("195 739 доб 37-77", false)).isEqualTo("+71957393777");
        // length corner case
        assertThat(formatImNonContactPhone("7495", false)).isEqualTo(null);
        assertThat(formatImNonContactPhone("74957", false)).isEqualTo("+74957");
        assertThat(formatImNonContactPhone("74957393777", false)).isEqualTo("+74957393777");
        assertThat(formatImNonContactPhone("749573937771", false)).isEqualTo(null);
    }
}

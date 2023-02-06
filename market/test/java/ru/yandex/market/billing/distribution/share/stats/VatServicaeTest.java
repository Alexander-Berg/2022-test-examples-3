package ru.yandex.market.billing.distribution.share.stats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class VatServicaeTest {
    private static final LocalDateTime DT_2021 = LocalDate.of(2021, 01, 01).atStartOfDay();
    private static VatService vatService = new VatService();

    @Test
    void test20121Vat() {
        BigDecimal withVat = vatService.addVat(new BigDecimal(100L), DT_2021);
        Assertions.assertThat(withVat.doubleValue())
                .isEqualTo(120L);
    }

    @Test
    void test2019Vat() {
        BigDecimal withVat = vatService.addVat(
                new BigDecimal(100L),
                LocalDate.of(2019, 01, 01).atStartOfDay()
        );
        Assertions.assertThat(withVat.doubleValue())
                .isEqualTo(120L);
    }

    @Test
    void test2018Vat() {
        BigDecimal withVat = vatService.addVat(
                new BigDecimal(100L),
                LocalDate.of(2018, 01, 01).atStartOfDay()
        );
        Assertions.assertThat(withVat.doubleValue())
                .isEqualTo(118L);
    }
}

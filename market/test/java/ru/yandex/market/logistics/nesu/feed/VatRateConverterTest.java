package ru.yandex.market.logistics.nesu.feed;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.enums.VatRate;
import ru.yandex.market.logistics.nesu.service.feed.parser.VatRateConverter;

class VatRateConverterTest extends AbstractTest {
    private final VatRateConverter converter = new VatRateConverter();

    @Test
    void convert() {
        softly.assertThat(converter.unmarshal("VAT_20")).isEqualTo(VatRate.VAT_20);
        softly.assertThat(converter.unmarshal("7")).isEqualTo(VatRate.VAT_20);
        softly.assertThat(converter.unmarshal("vat_20")).isEqualTo(VatRate.VAT_20);
        softly.assertThat(converter.unmarshal("")).isNull();
        softly.assertThat(converter.unmarshal(null)).isNull();
        softly.assertThat(converter.unmarshal("unknown_vat_rate")).isNull();
    }
}

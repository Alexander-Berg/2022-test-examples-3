package ru.yandex.avia.booking.partners.gateways.aeroflot.v3;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.v3.AeroflotNdcApiV3Helper.extractPnrFromOrderIdSafe;

public class AeroflotNdcApiV3HelperTest {
    @Test
    public void testExtractPnrFromOrderIdSafe() {
        assertThat(extractPnrFromOrderIdSafe("SU555BOMUAE-...")).isEqualTo("BOMUAE");

        assertThat(extractPnrFromOrderIdSafe("SU55500BOMUAE-...")).isEqualTo(null);
        assertThat(extractPnrFromOrderIdSafe("SUBOMUAE-...")).isEqualTo(null);
        assertThat(extractPnrFromOrderIdSafe("")).isEqualTo(null);
        assertThat(extractPnrFromOrderIdSafe(null)).isEqualTo(null);
    }
}

package ru.yandex.market.core.outlet;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link OutletUtils}.
 *
 * @author avetokhin 03.09.18.
 */
class OutletUtilsTest {

    private static final PhoneNumber PHONE_1 = PhoneNumber.builder()
            .setCountry("7")
            .setCity("923")
            .setNumber("2432555")
            .setExtension("03")
            .setComments("спросить Витю")
            .build();

    private static final PhoneNumber PHONE_2 = PhoneNumber.builder()
            .setCountry("+7 ")
            .setCity("923")
            .setNumber("243-25-55")
            .build();

    @Test
    void formatPhoneNumber() {
        assertThat(OutletUtils.formatPhoneNumber(PHONE_1), equalTo("+ 7 (923) 2432555 доб. 03 (спросить Витю)"));
        assertThat(OutletUtils.formatPhoneNumber(PHONE_2), equalTo("+ 7 (923) 243-25-55"));
    }
}

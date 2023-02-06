package ru.yandex.travel.orders.services.promo;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class PromoCodeUnifierTest {
    @Test
    public void testUnificationProcess() {
        Assertions.assertThat(PromoCodeUnifier.unifyCode("yandex")).isEqualTo("YANDEX");
        Assertions.assertThat(PromoCodeUnifier.unifyCode("yandEx2020")).isEqualTo("YANDEX2O2O");
        Assertions.assertThat(PromoCodeUnifier.unifyCode("i_lОve_yandex_2110")).isEqualTo("I_LOVE_YANDEX_2IIO");
    }
}

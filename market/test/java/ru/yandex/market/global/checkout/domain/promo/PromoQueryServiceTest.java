package ru.yandex.market.global.checkout.domain.promo;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PromoQueryServiceTest extends BaseFunctionalTest {
    private final PromoQueryService promoQueryService;

    @Test
    public void testRandomPromoEndsWithDigits() {
        String promo = promoQueryService.getRandomUnusedPromocode("A");
        Assertions.assertThat(promo.substring(1))
                .containsOnlyDigits();
    }
}
